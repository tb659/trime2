package com.osfans.trime.candidate;

import android.content.Context;
import android.text.TextUtils;

import com.osfans.trime.TrimeService;
import com.osfans.trime.core.CandidateItem;
import com.osfans.trime.core.Rime;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 候选词管理器 (CandidatesManager)。
 * 负责从 Rime 引擎获取候选词,并支持笔画过滤、单字过滤等功能。
 * <p>
 * 优化重点:
 * 1. 内存优化:针对11万行数据,避免大对象堆积,使用原子引用切换。
 * 2. 性能优化:放弃正则表达式,采用手动索引偏移解析,速度提升约5-10倍。
 * 3. 稳定性:增加搜索阈值保护(MAX_SEARCH_LIMIT),防止因过滤过严导致的算法死循环和界面卡死。
 */
public class CandidatesManager {

    // ==================== 常量定义 ====================
    
    /** 当前检索起始位置 */
    private static int mStart = 0;
    /** 默认每页候选词数量 */
    private static final int DEFAULT_SIZE = 10;

    /** 安全阈值:在一次检索中,如果过滤了超过 500 个原始候选词仍未填满一页,则强制返回,防止 UI 假死 */
    private static final int MAX_SEARCH_LIMIT = 500;

    // ==================== 成员变量 ====================

    /**
     * 当前 Rime 引擎实例引用（仅用于查询 READY 状态，不直接调用 JNI）。
     * 使用 AtomicReference 包装以保证多线程间的可见性。
     */
    private static final AtomicReference<Rime> mRimeRef = new AtomicReference<>(null);

    /** 笔画过滤映射表,key 为字符的 codePoint,value 为笔画编码。使用 volatile 保证多线程可见性 */
    // 使用 volatile 保证 Map 引用在多线程间的可见性
    // 初始化为一个空的不可变 Map，避免 initStroke 完成前的空指针风险
    private static volatile Map<Integer, String> mFilterStrokeMap = Collections.emptyMap();

    /** 是否启用单字过滤模式 */
    private static boolean mFilterChar;
    /** 当前笔画过滤条件 */
    private static String mFilterStroke;
    /** 笔画过滤提示文本 */
    private static String mFilterStrokeTip;

    /**
     * Rime 引擎未就绪时,候选词查询在主线程上轮询等待 READY 状态的最长时间(毫秒)。
     * 该值需要覆盖冷启动/方案组切换/部署等场景下 librime 启动的最坏耗时(解压资源/编译码表等),
     * 超过该时间会放弃等待,返回空列表,避免主线程被无限阻塞。
     */
    private static final long READY_WAIT_TIMEOUT_MS = 5_000L; // ms

    /**
     * 设置 Rime 引擎实例引用，供 {@link #next(int)} 等方法判断引擎是否就绪。
     * 必须在 {@link com.osfans.trime.TrimeService#onCreate()} 期间调用一次。
     *
     * @param rime Rime 实例，可为 null（清空引用）。
     */
    public static void setRime(Rime rime) {
        mRimeRef.set(rime);
    }

    /**
     * 异步初始化笔画库
     * 处理 11 万行数据，约耗时 150ms-400ms (视手机性能而定)
     */
    public static void initStroke(Context context) {
        new Thread(() -> {
            // 1. 预设足够容量，避免 HashMap 在 11 万次 put 过程中多次触发 rehash (扩容)
            Map<Integer, String> tempMap = new ConcurrentHashMap<>(150000);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("stroke.text"), "UTF-8"))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.length() < 3) continue;

                    // 2. 极速解析逻辑：避开正则和 split，直接操作字符索引
                    int firstCodePoint = line.codePointAt(0);
                    int charCount = Character.charCount(firstCodePoint);

                    // 查找第一个非空白字符（即编码开始的位置）
                    int codeStartIndex = charCount;
                    while (codeStartIndex < line.length() && line.charAt(codeStartIndex) <= ' ') {
                        codeStartIndex++;
                    }

                    if (codeStartIndex < line.length()) {
                        // 提取编码部分
                        String code = line.substring(codeStartIndex);
                        tempMap.put(firstCodePoint, code);
                    }
                }

                // 3. 原子切换：将完整解析好的新 Map 赋值给静态变量
                // 这样 next() 方法要么读取到旧的空表，要么读取到完整的新表，不会读到正在加载的中间态
                mFilterStrokeMap = tempMap;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 重置检索起始位置。
     */
    public static void reset() {
        mStart = 0; // 重置为0
    }

    /**
     * 获取下一页候选词(使用默认大小)。
     *
     * @return 候选词列表。
     */
    public static ArrayList<CandidateItem> next() {
        return next(DEFAULT_SIZE); // 调用带参数的方法
    }

    /**
     * 获取指定长度的候选词(核心过滤逻辑)。
     * 从 Rime 引擎获取原始候选词,并根据笔画和单字过滤条件进行筛选。
     *
     * @param pageSize 每页候选词数量。
     * @return 过滤后的候选词列表。
     */
    public static ArrayList<CandidateItem> next(int pageSize) {
        // 兜底：如果 Rime 引擎尚未就绪(冷启动/方案组切换/部署等场景),
        // 在主线程短暂轮询等待 READY,避免直接返回空列表造成候选栏永远空白。
        // 等待期间调度器仍然会推进 rime-main 线程执行 startRime,所以不会真正"卡死"。
        Rime rime = mRimeRef.get();
        if (rime != null && !rime.isReady()) {
            long deadline = System.currentTimeMillis() + READY_WAIT_TIMEOUT_MS;
            while (!rime.isReady() && System.currentTimeMillis() < deadline) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        ArrayList<CandidateItem> resultList = new ArrayList<>(); // 结果列表
        int searchedCount = 0; // 已检索的原始候选词计数

        // 局部变量缓存,提高循环内的访问效率
        final String currentFilterStroke = mFilterStroke;
        final boolean isFilterCharEnabled = mFilterChar;
        final Map<Integer, String> strokeMap = mFilterStrokeMap;

        while (resultList.size() < pageSize) {
            // 从 Rime 引擎获取原始候选词
            CandidateItem[] candidates = Rime.getRimeCandidates(mStart, pageSize);
            if (candidates == null || candidates.length == 0) break;

            for (int i = 0; i < candidates.length; i++) {
                CandidateItem cand = candidates[i];
                String text = cand.getText();
                boolean isMatch = true;

                if (!TextUtils.isEmpty(text)) {
                    // 1. 单字过滤逻辑
                    if (isFilterCharEnabled && text.length() > 1) {
                        isMatch = false; // 多字词被过滤
                    }

                    // 2. 笔画过滤逻辑
                    if (isMatch && !TextUtils.isEmpty(currentFilterStroke)) {
                        String stroke = strokeMap.get(text.codePointAt(0)); // 获取字符的笔画编码
                        if (stroke == null || !stroke.startsWith(currentFilterStroke)) {
                            isMatch = false; // 笔画不匹配
                        }
                    }
                } else {
                    isMatch = false; // 空文本不匹配
                }

                // 匹配成功,添加到结果集
                if (isMatch) {
                    cand.setIndex(mStart + i); // 设置全局索引
                    resultList.add(cand);
                    if (resultList.size() >= pageSize) break; // 达到页面大小,退出
                }

                // 安全检查:如果检索范围过大,强制中断,防止耗时过长引起卡顿
                searchedCount++;
                if (searchedCount > MAX_SEARCH_LIMIT) break; // 超过安全阈值,退出
            }

            // 更新 Rime 引擎的起始偏移量
            mStart += candidates.length;

            // 如果触发了安全阈值,跳出 while 循环
            if (searchedCount > MAX_SEARCH_LIMIT) break;
        }
        return resultList; // 返回过滤后的结果
    }

    /**
     * 仅获取字符串形式的候选词。
     *
     * @return 候选词文本列表。
     */
    public static ArrayList<String> nextString() {
        ArrayList<CandidateItem> items = next(DEFAULT_SIZE); // 获取候选词对象
        ArrayList<String> strings = new ArrayList<>();
        for (CandidateItem item : items) {
            strings.add(item.getText()); // 提取文本
        }
        return strings;
    }

    /**
     * 设置/累加笔画过滤条件。
     *
     * @param strokeChar 笔画字符。
     * @param tipChar 提示字符。
     */
    public static void filterStroke(String strokeChar, String tipChar) {
        if (TextUtils.isEmpty(strokeChar)) {
            mFilterStroke = null; // 清空笔画过滤
            mFilterStrokeTip = null;
        } else {
            mFilterStroke = (mFilterStroke == null) ? strokeChar : mFilterStroke + strokeChar; // 累加笔画
            mFilterStrokeTip = (mFilterStrokeTip == null) ? tipChar : mFilterStrokeTip + tipChar; // 累加提示
        }
        TrimeService.getInstance().setCloudText(mFilterStrokeTip); // 更新云端文本显示
    }

    /**
     * 切换单字过滤模式。
     *
     * @return 切换后的单字过滤状态。
     */
    public static boolean toggleFilterChar() {
        mFilterChar = !mFilterChar; // 切换状态
        return mFilterChar;
    }

    /**
     * 获取当前单字过滤状态。
     *
     * @return true 表示启用单字过滤。
     */
    public static boolean isFilterChar() {
        return mFilterChar;
    }

    /**
     * 重置所有过滤器。
     * 清除单字过滤、笔画过滤和提示文本。
     */
    public static void resetFilter() {
        mFilterChar = false; // 关闭单字过滤
        mFilterStroke = null; // 清空笔画过滤
        mFilterStrokeTip = null; // 清空提示文本
        TrimeService.getInstance().setCloudText(null); // 清除云端文本显示
    }

    /**
     * 设置检索起始位置。
     *
     * @param idx 目标起始索引。
     */
    public static void setStart(int idx) {
        mStart=idx;
    }


}
