/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.osfans.trime.core;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.androlua.LuaApplication;
import com.osfans.trime.BuildConfig;
import com.osfans.trime.R;
import com.osfans.trime.TrimeApplication;
//import com.osfans.trime.core.isStorageAvailable;
import com.osfans.trime.data.opencc.OpenCCDictManager;
import com.osfans.trime.util.Function;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import timber.log.Timber;

/**
 * Rime 输入法引擎核心类。
 * 实现了 RimeApi 接口和 RimeLifecycleOwner 生命周期管理,
 * 负责与原生 RIME 引擎(JNI)交互,处理按键输入、候选词选择、方案切换等功能。
 */
@SuppressWarnings("unused") // JNI 方法由原生库实现,IDE 可能无法识别
public class Rime implements RimeApi, RimeLifecycleOwner {
    // ==================== 常量定义 ====================
    
    /** Shift 修饰符掩码 */
    public static final int META_SHIFT_ON = RimeKeyEvent.getModifierByName("Shift");
    /** Control 修饰符掩码 */
    public static final int META_CTRL_ON = RimeKeyEvent.getModifierByName("Control");
    /** Alt 修饰符掩码 */
    public static final int META_ALT_ON = RimeKeyEvent.getModifierByName("Alt");
    /** Release(释放)修饰符掩码 */
    public static final int META_RELEASE_ON = RimeKeyEvent.getModifierByName("Release");
    // ==================== 成员变量 ====================
    
    /** 当前是否为 ASCII(英文)模式 */
    private static boolean isAsciiMode;
    /** 生命周期管理实现 */
    private final RimeLifecycleImpl lifecycleImpl = new RimeLifecycleImpl();

    /** Rime 消息流,用于向观察者发送引擎状态变化消息 */
    private static final SharedFlowImpl<RimeMessage> messageFlow_ = new SharedFlowImpl<>(15);
    //TODO 获取 LuaApplication.getInstance().getApplicationContext();
    /** 应用上下文 */
    private final TrimeApplication appContext = LuaApplication.getInstance();
    /** 是否正在组字(输入中) */
    private static boolean isComposing = false;
    /** 是否有候选菜单 */
    private static boolean hasMenu = false;
    /** 是否正在翻页 */
    private static boolean paging = false;

    /**
     * 获取当前是否正在组字(输入中)。
     *
     * @return true 表示正在输入中,false 表示未输入。
     */
    public static boolean isComposing() {
        return isComposing;
    }

    /**
     * 切换指定的 Rime 选项开关。
     *
     * @param toggle 选项名称(如 "ascii_mode", "full_shape" 等)。
     */
    public static void toggleOption(String toggle) {
        setRimeOption(toggle, !getRimeOption(toggle)); // 切换选项值
        if(toggle.equals("ascii_mode"))
           isAsciiMode =getRimeOption(toggle); // 更新 ASCII 模式状态
    }

    /**
     * 获取当前是否为 ASCII(英文)模式。
     *
     * @return true 表示英文模式,false 表示中文模式。
     */
    public static boolean isAsciiMode() {
        return isAsciiMode;
    }

    /**
     * 检查键码是否为无效键。
     *
     * @param keycode 要检查的键码。
     * @return true 表示是无效键(<=0 或等于 XK_VoidSymbol)。
     */
    public static boolean isVoidKeycode(int keycode) {
        int XK_VoidSymbol = 0xffffff; // X11 空符号常量
        return keycode <= 0 || keycode == XK_VoidSymbol;
    }

    /**
     * 处理文本输入。
     *
     * @param text 要输入的文本。
     * @return true 表示成功处理,false 表示失败或文本为空。
     */
    public boolean onText(CharSequence text) {
        if (text == null || text.length() == 0) return false;
        return Boolean.TRUE.equals(withRimeContext(() -> {
            // 模拟按键序列,将 {} 替换为 {braceleft}{braceright}
            boolean it = simulateRimeKeySequence(text.toString().replace("{}", "{braceleft}{braceright}"));
            if (it) Rime.this.emitResponse(); // 如果成功,发送响应
            return it;
        }));
    }

    /**
     * 设置是否正在组字(私有 setter)。
     *
     * @param composing 组字状态。
     */
    private void setComposing(boolean composing) {
        isComposing = composing;
    }

    /**
     * 获取是否有候选菜单。
     *
     * @return true 表示有候选词菜单。
     */
    public static boolean hasMenu() {
        return hasMenu;
    }

    /**
     * 设置是否有候选菜单(私有 setter)。
     *
     * @param hasMenu 菜单状态。
     */
    private void setHasMenu(boolean hasMenu) {
        this.hasMenu = hasMenu;
    }

    /**
     * 获取是否正在翻页。
     *
     * @return true 表示正在翻页。
     */
    public static boolean isPaging() {
        return paging;
    }

    /**
     * 设置是否正在翻页(私有 setter)。
     *
     * @param paging 翻页状态。
     */
    private void setPaging(boolean paging) {
        this.paging = paging;
    }

    @Override
    public SharedFlowImpl<?> getMessage() {
        return messageFlow_;
    }

    @Override
    public RimeLifecycle getLifecycle() {
        return lifecycleImpl;
    }

    @Override
    public Executor getLifecycleExecutor() {
        return RimeLifecycleOwner.super.getLifecycleExecutor();
    }

    @Override
    public RimeLifecycle.State getState() {
        return lifecycleImpl.getCurrentState();
    }

    @Override
    public boolean isReady() {
        return lifecycleImpl.getCurrentState() == RimeLifecycle.State.READY;
    }

    @Override
    public RimeSchema getSchemaCached() {
        return schemaCached;
    }

    @Override
    public RimeProto.Status getStatusCached() {
        return statusCached;
    }

    @Override
    public RimeProto.Context.Composition getCompositionCached() {
        return compositionCached;
    }

    @Override
    public RimeProto.Context.Menu getMenuCached() {
        return menuCached;
    }

    // ==================== 缓存数据 ====================
    
    /** 当前方案缓存 */
    private RimeSchema schemaCached;
    /** 状态缓存 */
    private RimeProto.Status statusCached = new RimeProto.Status();
    /** 组字上下文缓存 */
    private RimeProto.Context.Composition compositionCached = new RimeProto.Context.Composition();
    /** 候选菜单缓存 */
    private RimeProto.Context.Menu menuCached = new RimeProto.Context.Menu();

    /**
     * 获取是否显示 ASCII 模式切换提示。
     * <p>
     * 该方法模拟了 Kotlin 中的委托属性访问，实际逻辑应从应用偏好设置中读取。
     * 目前暂时返回 false，待 AppPrefs 实现后替换为真实配置读取逻辑。
     *
     * @return true 表示显示提示，false 表示不显示。
     */
    private boolean getShowAsciiSwitchTips() {
        // TODO: 接入真实的偏好设置读取逻辑
        // return AppPrefs.defaultInstance().general.getAsciiSwitchTips();
        return false;
    }

    /** 上次 ASCII 切换提示文本 */
    private String lastAsciiTipsText = "";
    /** 调度器,用于延迟任务 */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    /** ASCII 切换提示任务的引用,用于取消之前的任务 */
    private AtomicReference<ScheduledFuture<?>> asciiSwitchTipsJob = new AtomicReference<>(null);

    /** Rime 调度器,用于在独立线程中执行 Rime 操作 */
    private final RimeDispatcher dispatcher;

    /**
     * 构造函数。
     *
     * @param runnable 初始化完成后要执行的回调函数。
     * @throws IllegalStateException 如果 Rime 已经创建过。
     */
    public Rime(Runnable runnable) {
        if (lifecycleImpl.getCurrentState() != RimeLifecycle.State.STOPPED) {
            throw new IllegalStateException("Rime has already been created!");
        }

        this.dispatcher = new RimeDispatcher(new RimeDispatcher.RimeController() {
            @Override
            public void nativeStartup() {
                Log.w("rime", "nativeStartup: " );
                startRime(false); // 启动 Rime 引擎
                runnable.run(); // 执行初始化回调
                lifecycleImpl.emitState(RimeLifecycle.State.READY); // 更新状态为就绪
            }

            @Override
            public void nativeFinalize() {
                Log.w("rime", "nativeFinalize: " );
                exitRime(); // 退出 Rime 引擎
            }
        });
    }

    /**
     * 在 Rime 上下文中执行代码块(模拟 Kotlin 的 withContext)。
     *
     * @param block 要在 Rime 上下文中执行的代码块。
     * @param <T> 返回值类型。
     * @return 代码块的执行结果,失败时返回 null。
     */
    private <T> T withRimeContext(Callable<T> block) {
        // In a real Android Java environment, you would use an Executor or a specific Handler/Thread
        // here to synchronize access to Rime JNI calls. For this conversion, we assume
        // RimeDispatcher handles execution.
        try {
            return dispatcher.submit(block);
        } catch (Exception e) {
            Timber.e(e, "Rime context execution failed.");
            return null; // Handle error appropriately
        }
    }

    // ==================== RimeApi 接口实现 ====================

    /**
     * 检查当前是否为空方案(未选择任何输入方案)。
     *
     * @return true 表示当前是默认方案(无方案)。
     */
    @Override
    public boolean isEmpty() {
        return Boolean.TRUE.equals(withRimeContext(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                return getCurrentRimeSchema().equals(".default");
            }
        })); // 無方案
    }

    /**
     * 重新部署 Rime 引擎(完全重启)。
     * 会退出并重新启动 Rime,用于应用配置更改后的生效。
     */
    @Override
    public void deploy() {
        withRimeContext((Callable<String>) () -> {
            Rime.exitRime();
            Rime.startRime(true);
            return null;
        });
    }
    /**
     * 重启 Rime 引擎(不完全检查)。
     * 与 deploy() 不同,不会进行完整的方案检查。
     */
    public void restart() {
        withRimeContext((Callable<String>) () -> {
            Rime.exitRime();
            Rime.startRime(false);
            return null;
        });
    }

    /**
     * 同步用户数据。
     *
     * @return true 表示同步成功。
     */
    @Override
    public boolean syncUserData() {
        return withRimeContext(Rime::syncRimeUserData);
    }

    /**
     * 同步用户数据（等待更久）。
     *
     * <p>词库同步可能超过普通提交的 2 秒等待上限（大词库合并/写入快照），
     * 造词后立即同步等场景应调用此方法；最多等待 30 秒。</p>
     *
     * @return true 表示同步成功。
     */
    public boolean syncUserDataNow() {
        return Boolean.TRUE.equals(dispatcher.submitLong(Rime::syncRimeUserData));
    }

    /**
     * 处理按键事件。
     *
     * @param value 键值(Rime 键码)。
     * @param modifiers 修饰符掩码。
     * @param isVirtual 是否为虚拟按键(软键盘)。
     * @return true 表示按键被处理,false 表示未处理。
     */
    @Override
    public boolean processKey(int value, long modifiers, boolean isVirtual) {
        return Boolean.TRUE.equals(withRimeContext(() -> Rime.this.processKeyInner(value, (int) modifiers, isVirtual)));
    }

    @Override
    public boolean processKey(int value) {
        return RimeApi.super.processKey(value);
    }

    @Override
    public boolean processKey(int value, long modifiers) {
        return RimeApi.super.processKey(value, modifiers);
    }

    @Override
    public boolean processKey(KeyValue value, KeyModifiers modifiers) {
        return RimeApi.super.processKey(value, modifiers);
    }

    @Override
    public boolean processKey(KeyValue value, KeyModifiers modifiers, boolean isVirtual) {
        return Boolean.TRUE.equals(withRimeContext(() -> processKeyInner(value.getValue(), modifiers.toInt(), isVirtual)));
    }

    /**
     * 模拟按键序列输入。
     *
     * @param sequence 按键序列字符串。
     * @return true 表示成功处理,false 表示失败。
     */
    @Override
    public boolean simulateKeySequence(String sequence) {
        boolean composing = isComposing();
        if(BuildConfig.DEBUG)Log.w("rime", "simulateKeySequence:old1 "+composing);
        return Boolean.TRUE.equals(withRimeContext(() -> {
            Timber.d("simulateKeySequence: " + sequence);
            String old = compositionCached.getCommitTextPreview();
            if(BuildConfig.DEBUG)Log.w("rime", "simulateKeySequence:old2 "+old );
            boolean success = simulateRimeKeySequence(sequence);
            if (success) {
                RimeProto.Commit commit = getRimeCommit();
                String input = getRimeRawInput();
                if(BuildConfig.DEBUG)Log.w("rime", "simulateKeySequence:1 "+commit);
                if(BuildConfig.DEBUG)Log.w("rime", "simulateKeySequence:2 "+input );
                if (commit.getText() != null && !commit.getText().isEmpty()) {
                    emitResponse(commit);
                    if(BuildConfig.DEBUG)Log.w("rime", "simulateKeySequence:3 "+isComposing() );
                    if(composing&&!isComposing()&&commit.getText().equals(old))
                        emitResponse(new RimeProto.Commit(sequence));
                    return true;
                } else if(!input.isEmpty()){
                   if(BuildConfig.DEBUG)Log.w("rime", "simulateKeySequence:4 "+input);
                    emitResponse(commit);
                    return true;
                } else {
                    //if(!TextUtils.isEmpty(old)) {
                    //    emitResponse(new RimeProto.Commit(old));
                    //    clearRimeComposition();
                    //}
                    if(BuildConfig.DEBUG)Log.w("rime", "simulateKeySequence:5 "+sequence);
                    emitResponse(new RimeProto.Commit(sequence));
                    return false;
                }
            } else {
                return false;
            }
        }));//.also(result -> Timber.d("simulateKeySequence " + (result ? "success" : "failed")));
    }

    /**
     * 选择指定索引的候选词。
     *
     * @param idx 候选词索引(从0开始)。
     * @return true 表示选择成功。
     */
    @Override
    public boolean selectCandidate(int idx) {
        return Boolean.TRUE.equals(withRimeContext(() -> {
            boolean it = selectRimeCandidate(idx);
            if (it) Rime.this.emitResponse();
            return it;
        }));
    }

    /**
     * 忘记指定索引的候选词(从用户词典中删除)。
     *
     * @param idx 候选词索引。
     * @return true 表示删除成功。
     */
    @Override
    public boolean forgetCandidate(int idx) {
        return Boolean.TRUE.equals(withRimeContext(() -> {
            boolean it = forgetRimeCandidate(idx);
            if (it) Rime.this.emitResponse();
            return it;
        }));
    }

    /**
     * 选择当前页中指定索引的候选词。
     *
     * @param idx 当前页中的候选词索引。
     * @return true 表示选择成功。
     */
    @Override
    public boolean selectPagedCandidate(int idx) {
        return Boolean.TRUE.equals(withRimeContext(() -> {
            boolean it = selectRimeCandidateOnCurrentPage(idx);
            if (it) Rime.this.emitResponse();
            return it;
        }));
    }

    /**
     * 删除当前页中指定索引的候选词。
     *
     * @param idx 当前页中的候选词索引。
     * @return true 表示删除成功。
     */
    @Override
    public boolean deletedPagedCandidate(int idx) {
        return Boolean.TRUE.equals(withRimeContext(() -> {
            boolean it = deleteRimeCandidateOnCurrentPage(idx);
            if (it) Rime.this.emitResponse();
            return it;
        }));
    }

    /**
     * 切换候选词页面。
     *
     * @param backward true 表示向前翻页,false 表示向后翻页。
     * @return true 表示翻页成功。
     */
    @Override
    public boolean changeCandidatePage(boolean backward) {
        return Boolean.TRUE.equals(withRimeContext(() -> {
            boolean it = changeRimeCandidatePage(backward);
            if (it) Rime.this.emitResponse();
            return it;
        }));
    }

    /**
     * 移动光标位置。
     *
     * @param position 目标光标位置。
     */
    @Override
    public void moveCursorPos(int position) {
         withRimeContext((Callable<String>) () -> {
             setRimeCaretPos(position);
             Rime.this.emitResponse();
             return null;
         });
    }

    /**
     * 获取所有可用的输入方案列表。
     *
     * @return 可用方案数组。
     */
    @Override
    public SchemaItem[] availableSchemata() {
        return withRimeContext(Rime::getAvailableRimeSchemaList);
    }

    /**
     * 获取已启用的输入方案列表。
     *
     * @return 已启用方案数组。
     */
    @Override
    public SchemaItem[] enabledSchemata() {
        return withRimeContext(Rime::getSelectedRimeSchemaList);
    }

    /**
     * 设置启用的输入方案列表。
     *
     * @param schemaIds 方案 ID 数组。
     * @return true 表示设置成功。
     */
    @Override
    public boolean setEnabledSchemata(String[] schemaIds) {
        return Boolean.TRUE.equals(withRimeContext(() -> selectRimeSchemas(schemaIds)));
    }

    /**
     * 获取当前选中的输入方案列表。
     *
     * @return 选中方案数组。
     */
    @Override
    public SchemaItem[] selectedSchemata() {
        return withRimeContext(Rime::getRimeSchemaList);
    }

    /**
     * 获取当前选中的方案 ID。
     *
     * @return 当前方案 ID。
     */
    @Override
    public String selectedSchemaId() {
        return withRimeContext(Rime::getCurrentRimeSchema);
    }

    /**
     * 选择指定的输入方案。
     *
     * @param schemaId 方案 ID。
     * @return true 表示选择成功。
     */
    @Override
    public boolean selectSchema(String schemaId) {
        return Boolean.TRUE.equals(withRimeContext(() -> selectRimeSchema(schemaId)));
    }

    /**
     * 获取当前输入方案的详细信息。
     *
     * @return 当前方案对象。
     */
    @Override
    public RimeSchema currentSchema() {
        return withRimeContext(() -> new RimeSchema(getCurrentRimeSchema()));
    }

    /**
     * 提交当前组字内容(上屏)。
     *
     * @return true 表示提交成功。
     */
    @Override
    public boolean commitComposition() {
        return Boolean.TRUE.equals(withRimeContext(() -> {
            boolean it = commitRimeComposition();
            if (it) Rime.this.emitResponse();
            return it;
        }));
    }

    public boolean learnRawInput(String text) {
        if (TextUtils.isEmpty(text)) return false;
        return Boolean.TRUE.equals(withRimeContext(() -> learnRimeRawInput(text)));
    }

    /**
     * 按指定编码和文本向当前方案 user_dict 写入一条显式自造词。
     *
     * <p>该接口服务于“无候选时手动造词”弹窗：用户明确输入编码与词语后，
     * 直接把这条词写入当前方案用户词典，并立即刷新当前未确认的 composition。</p>
     *
     * @param code 当前词条对应的编码。
     * @param text 需要写入 user_dict 的词语文本。
     * @return true 表示写入成功。
     */
    public boolean addUserPhrase(String code, String text) {
        if (TextUtils.isEmpty(code) || TextUtils.isEmpty(text)) return false;
        return Boolean.TRUE.equals(withRimeContext(() -> addRimeUserPhrase(code, text)));
    }

    /**
     * 按当前虎码编码规则把整段词语一次性编码进 user_dict。
     *
     * <p>该接口服务于“造词开/关”会话模式：开启时仅记录上屏文本，关闭时再把本轮
     * 累积的目标词整体交给 UnityTableEncoder 编码，并顺带清理同词链上遗留的更短自动
     * 编码子串，避免即时学习历史留下单字或前缀垃圾词。</p>
     *
     * @param text 需要按虎码编码规则写入 user_dict 的目标词语。
     * @return true 表示编码写入成功。
     */
    public boolean encodeUserPhrase(String text) {
        if (TextUtils.isEmpty(text)) return false;
        return Boolean.TRUE.equals(withRimeContext(() -> encodeRimeUserPhrase(text)));
    }

    /**
     * 按指定编码和文本从当前方案 user_dict 删除一条显式自造词。
     *
     * <p>该接口服务于候选栏长按删除：用户确认删除后，直接按保存时的完整编码和词语
     * 精确移除对应用户词条，避免误删同文不同码的其他记录。</p>
     *
     * @param code 当前词条对应的完整编码。
     * @param text 需要从 user_dict 移除的词语文本。
     * @return true 表示删除成功。
     */
    public boolean removeUserPhrase(String code, String text) {
        if (TextUtils.isEmpty(code) || TextUtils.isEmpty(text)) return false;
        return Boolean.TRUE.equals(withRimeContext(() -> removeRimeUserPhrase(code, text)));
    }

    /**
     * 按当前方案的 user_dict 前缀查询 mixed rawInput 补全项。
     *
     * <p>该接口主要服务于 `a1显`：像输入 `tb` 时，把此前学习过的 `tb659` 这类字母+数字词
     * 直接从 user_dict 中查回并补到候选栏，而不依赖主码表本身支持这种编码。</p>
     *
     * @param prefix 当前输入前缀。
     * @param limit  最多返回多少条结果。
     * @return 匹配到的补全项数组；没有结果时返回空数组。
     */
    public String[] queryRawInputCompletions(String prefix, int limit) {
        if (TextUtils.isEmpty(prefix) || limit <= 0) return new String[0];
        String[] result = queryRimeRawInputCompletions(prefix, limit);
        return result != null ? result : new String[0];
    }

    /**
     * 判断当前方案的 user_dict 中，是否存在“编码以前缀命中当前输入”的指定词条。
     */
    public boolean hasUserPhraseWithPrefix(String prefix, String text) {
        if (TextUtils.isEmpty(prefix) || TextUtils.isEmpty(text)) return false;
        return Boolean.TRUE.equals(withRimeContext(() -> hasRimeUserPhraseWithPrefix(prefix, text)));
    }

    /**
     * 按输入前缀和词语回查当前方案 user_dict 中保存的完整编码。
     *
     * @param prefix 当前输入前缀。
     * @param text 候选词文本。
     * @return 命中的完整编码；未命中时返回空字符串。
     */
    public String getUserPhraseCodeWithPrefix(String prefix, String text) {
        if (TextUtils.isEmpty(prefix) || TextUtils.isEmpty(text)) return "";
        String code = withRimeContext(() -> getRimeUserPhraseCodeWithPrefix(prefix, text));
        return code != null ? code : "";
    }

    /**
     * 清除当前组字内容(取消输入)。
     */
    @Override
    public void clearComposition() {
        withRimeContext((Callable<String>) () -> {
            clearRimeComposition();
            Rime.this.emitResponse();
            return null;
        });
    }

    /**
     * 设置运行时选项。
     *
     * @param option 选项名称。
     * @param value 选项值。
     */
    @Override
    public void setRuntimeOption(String option, boolean value) {
        withRimeContext((Callable<String>) () -> {
            setRimeOption(option, value);
            return null;
        });
    }

    /**
     * 切换运行时选项的开关状态。
     *
     * @param option 选项名称。
     */
    public void toggleRuntimeOption(String option) {
        withRimeContext((Callable<String>) () -> {
            toggleOption(option);
            return null;
        });
    }

    /**
     * 获取运行时选项的值。
     *
     * @param option 选项名称。
     * @return 选项值。
     */
    @Override
    public boolean getRuntimeOption(String option) {
        return Boolean.TRUE.equals(withRimeContext(() -> getRimeOption(option)));
    }

    /**
     * 获取候选词列表。
     *
     * @param startIndex 起始索引。
     * @param limit 获取数量限制。
     * @return 候选词数组。
     */
    @Override
    public CandidateItem[] getCandidates(int startIndex, int limit) {
        return withRimeContext(() -> getRimeCandidates(startIndex, limit));
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 启动 Rime 引擎。
     *
     * @param fullCheck 是否进行完整检查(首次启动时为 true)。
     */
    @SuppressLint("StringFormatInTimber")
    public static void startRime(boolean fullCheck) {
        DataManager.sync();
        String sharedDataDir = DataManager.getSharedDataDir().getAbsolutePath();
        String userDataDir = DataManager.getUserDataDir().getAbsolutePath();
        Timber.d(
                String.format(Locale.CHINA,
                        "Starting rime with: sharedDataDir: %s userDataDir: %s fullCheck: %b",
                        sharedDataDir,
                        userDataDir,
                        fullCheck
                )
        );
        startupRime(sharedDataDir, userDataDir, BuildConfig.BUILD_VERSION_NAME, fullCheck);
    }

    /**
     * 内部处理按键事件。
     *
     * @param value 键值。
     * @param modifiers 修饰符掩码。
     * @param isVirtual 是否为虚拟按键。
     * @return true 表示按键被处理。
     */
    private boolean processKeyInner(int value, int modifiers, boolean isVirtual) {
        //lastAsciiTipsText = getAsciiTipsText();
        boolean handled = processRimeKey(value, modifiers);
        emitResponse();
        if (!handled) {
            handleRimeMessage(
                    9, // RimeMessage.MessageType.Key,
                    new Object[]{value, modifiers, isVirtual}
            );
        }
        return handled;
    }

    /**
     * 获取 ASCII 切换提示文本。
     *
     * @return 提示文本(如 "En" 或方案名称前两个字符)。
     */
    private String getAsciiTipsText() {
        RimeProto.Status status = getRimeStatus();
        if (status.isAsciiMode()) {
            return "En";
        } else if (status.getSchemaName() != null &&
                !status.getSchemaName().startsWith(".")) {
            // Java equivalent of take(2)
            return status.getSchemaName();
            //return status.getSchemaName().length() >= 2 ? status.getSchemaName().substring(0, 2) : status.getSchemaName();
        } else {
            return "";
        }
    }

    /**
     * 发送响应消息(无提交内容)。
     * 会触发所有相关消息的更新和发送。
     */
    public void emitResponse() {
        emitResponse(Rime.getRimeCommit());
    }

    /**
     * 发送响应消息(带提交内容)。
     * 会向消息流发送组字、菜单、状态等更新消息。
     *
     * @param commit 提交内容。
     */
    private void emitResponse(RimeProto.Commit commit) {
        handleRimeMessage(4, new Object[]{commit});

        RimeProto.Context context = getRimeContext();
        handleRimeMessage(5, new Object[]{context.getComposition()});

        //if (context.getComposition().getLength() <= 0 && !lastAsciiTipsText.equals(getAsciiTipsText())) {
        //    showAsciiSwitchTips();
        //}

        if (getRimeOption("paging_mode")) {
            handleRimeMessage(6, new Object[]{context.getMenu()});
        } else {
            CandidateItem[] candidates = getRimeCandidates(0, 1);
            handleRimeMessage(
                    8,
                    new Object[]{candidates.length, candidates}
            );
        }
        handleRimeMessage(7, new Object[]{getRimeStatus()});
    }

    /**
     * 处理 Rime 消息(静态方法)。
     * 创建消息对象,通知所有处理器,并发送到消息流。
     *
     * @param type 消息类型。
     * @param params 消息参数。
     */
    public static void handleRimeMessage(int type, Object[] params) {
        // 1. 调用静态工厂方法创建消息
        RimeMessage<?> message = RimeMessage.nativeCreate(type, params);

        // 2. 日志记录 (Timber 在 Java 中推荐使用占位符或字符串拼接)
        Timber.d("Handling %s", message);

        // 3. 遍历并执行所有处理器
        // 假设 rimeMessageHandlers 是一个 List<Consumer<RimeMessage<?>>>
        // 或者类似的函数式接口集合
        for (Consumer<RimeMessage<?>> handler : rimeMessageHandlers) {
            handler.accept(message);
        }

        // 4. 发送到 Flow (MutableSharedFlow 的 tryEmit 在 Java 中可以直接调用)
        messageFlow_.tryEmit(message);
    }

    /**
     * 处理 Rime 消息(实例方法)。
     * 根据消息类型更新缓存状态和 UI。
     *
     * @param message Rime 消息对象。
     */
    public void handleRimeMessage(RimeMessage<?> message) {
        if (message instanceof RimeMessage.SchemaMessage) {
            RimeMessage.SchemaMessage msg = (RimeMessage.SchemaMessage) message;
            this.statusCached = getRimeStatus();
            isComposing=statusCached.isComposing();
            isAsciiMode =statusCached.isAsciiMode();
            this.schemaCached = new RimeSchema(msg.getData().getId());
         } else if (message instanceof RimeMessage.OptionMessage) {
            RimeMessage.OptionMessage msg = (RimeMessage.OptionMessage) message;
            RimeProto.Status status = getRimeStatus();
            isComposing=status.isComposing();
            this.statusCached = status;
            updateSchemaCached(status);

            if ("ascii_mode".equals(msg.getData().getOption())) {
                isAsciiMode =status.isAsciiMode();
                showAsciiSwitchTips();
            }

        } else if (message instanceof RimeMessage.DeployMessage) {
            RimeMessage.DeployMessage msg = (RimeMessage.DeployMessage) message;
            Context context = LuaApplication.getInstance();
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            int notificationId = 1001; // 固定 ID 以便更新同一条通知
            long startTime = SystemClock.elapsedRealtime(); // 记录起始点

            if (msg.getData() == RimeMessage.DeployMessage.State.Start) {
                OpenCCDictManager.buildOpenCCDict();

                // 发送通知，并实时显示耗时
                Notification notification = new NotificationCompat.Builder(context, "rime_deploy_channel")
                        .setContentTitle("正在部署")
                        .setContentText("请稍候...")
                        .setSmallIcon(R.drawable.icon) // 替换为你的图标
                        .setOngoing(true) // 设置为正在进行，防止被划掉
                        .setUsesChronometer(true) // 核心：开启计时器
                        .setWhen(System.currentTimeMillis())
                        .build();
                notificationManager.notify(notificationId, notification);

            } else if (msg.getData() == RimeMessage.DeployMessage.State.Success) {
                // 计算总耗时（秒）
                long duration = (SystemClock.elapsedRealtime() - startTime) / 1000;

                // 显示完成通知，并显示总耗时
                Notification notification = new NotificationCompat.Builder(context, "rime_deploy_channel")
                        .setContentTitle("部署成功")
                        .setContentText("部署已完成，总耗时：" + duration + "秒")
                        .setSmallIcon(R.drawable.icon) // 替换为你的图标
                        .setOngoing(false) // 允许划掉
                        .setUsesChronometer(false) // 停止计时
                        .setTimeoutAfter(duration<5?1000:30000)
                        .build();
                notificationManager.notify(notificationId, notification);
            } else if (msg.getData() == RimeMessage.DeployMessage.State.Failure) {
                long duration = (SystemClock.elapsedRealtime() - startTime) / 1000;

                // 显示失败通知，并显示总耗时
                Notification notification = new NotificationCompat.Builder(context, "rime_deploy_channel")
                        .setContentTitle("部署失败")
                        .setContentText("部署过程中出现错误，已停止。耗时：" + duration + "秒")
                        .setSmallIcon(R.drawable.icon) // 替换为你的图标
                        .setOngoing(false)
                        .build();
                notificationManager.notify(notificationId, notification);
            }

        } else if (message instanceof RimeMessage.CompositionMessage) {
            RimeProto.Context.Composition data = ((RimeMessage.CompositionMessage) message).getData();
            // CompositionMessage 先于 StatusMessage 到达时，不能继续依赖上一拍的 statusCached。
            isComposing = inferComposingFromComposition(data);
            this.compositionCached = data;
         } else if (message instanceof RimeMessage.CandidateMenuMessage) {
            RimeProto.Context.Menu menu = ((RimeMessage.CandidateMenuMessage) message).getData();
            paging = menu.getPageNumber() != 0;
            hasMenu = menu.getCandidates() != null && menu.getCandidates().length!=0;

        } else if (message instanceof RimeMessage.CandidateListMessage) {
            RimeMessage.CandidateListMessage.Data list = ((RimeMessage.CandidateListMessage) message).getData();
            hasMenu = list.getCandidates() != null && list.getCandidates().length!=0;

        } else if (message instanceof RimeMessage.StatusMessage) {
            RimeProto.Status status = ((RimeMessage.StatusMessage) message).getData();
            this.statusCached = status;
            isComposing=statusCached.isComposing();
            isAsciiMode =statusCached.isAsciiMode();
            updateSchemaCached(status);
        }
    }

    /**
     * 根据当前 composition 快照推断是否仍处于组字态。
     */
    private boolean inferComposingFromComposition(RimeProto.Context.Composition data) {
        if (data != null) {
            if (data.getLength() > 0) {
                return true;
            }
            if (!TextUtils.isEmpty(data.getPreedit())) {
                return true;
            }
            if (!TextUtils.isEmpty(data.getCommitTextPreview())) {
                return true;
            }
        }
        return !TextUtils.isEmpty(getRimeRawInput());
    }

    /**
     * 更新方案缓存。
     * 如果方案 ID 发生变化,会创建新的方案对象并发送方案变更消息。
     *
     * @param status 当前状态。
     */
    private void updateSchemaCached(RimeProto.Status status) {
        try {
            String schemaId = status.getSchemaId();
            String schemaName = status.getSchemaName();
            // Engine response update won't send SchemaMessage, but usually update RimeStatus
            if (schemaCached==null||!schemaId.equals(schemaCached.getSchemaId())) {
                schemaCached = new RimeSchema(schemaId);
                // notify downstream consumers that schema has changed
                RimeMessage.SchemaMessage message = new RimeMessage.SchemaMessage(
                        new SchemaItem(schemaId, schemaName)
                );
                messageFlow_.tryEmit(message);
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * 显示 ASCII 模式切换提示。
     * 会在组字区显示提示文本,并在 1 秒后自动清除。
     */
    private void showAsciiSwitchTips() {
        //Function.printStackTrace("showAsciiSwitchTips");
        if (!getShowAsciiSwitchTips()) return;
        String tipsText = getAsciiTipsText();
        if (tipsText.isEmpty()) return;

        RimeProto.Context.Composition tips = new RimeProto.Context.Composition(tipsText);
        handleRimeMessage(5, new Object[]{tips});

        // Cancel previous job if running
        ScheduledFuture<?> prevJob = asciiSwitchTipsJob.getAndSet(null);
        if (prevJob != null) {
            prevJob.cancel(true);
        }

        // Schedule the new job (1000L delay)
        Runnable job = () -> {
            RimeProto.Context ctx = getRimeContext();
            handleRimeMessage(5, new Object[]{ctx.getComposition()});
        };
        asciiSwitchTipsJob.set(scheduler.schedule(job, 1000L, TimeUnit.MILLISECONDS));
    }
    // ==================== 生命周期管理 ====================
    
    /** Rime 消息处理器引用 */
    private final Rime.Consumer<RimeMessage<?>> mMessageHandler = this::handleRimeMessage;

    /**
     * 启动 Rime 引擎。
     * 检查存储可用性,注册消息处理器,并启动调度器。
     */
    public void startup() {
        if (lifecycleImpl.getCurrentState() != RimeLifecycle.State.STOPPED) {
            Timber.w("Skip starting rime: not at stopped state!");
            return;
        }
        if (appContext.isStorageAvailable()) {
            registerRimeMessageHandler(mMessageHandler);
            lifecycleImpl.emitState(RimeLifecycle.State.STARTING);
            dispatcher.start();
        }
    }

    /**
     * 停止 Rime 引擎。
     * 更新生命周期状态,停止调度器,取消所有待处理任务,并注销消息处理器。
     */
    public void finalize() {
        if (lifecycleImpl.getCurrentState() != RimeLifecycle.State.READY) {
            Timber.w("Skip stopping rime: not at ready state!");
            return;
        }
        lifecycleImpl.emitState(RimeLifecycle.State.STOPPING);
        Timber.i("Rime finalize()");
        List<Runnable> pendingJobs = dispatcher.stop();
        if (!pendingJobs.isEmpty()) {
            Timber.w(pendingJobs.size() + " job(s) didn't get a chance to run!");
        }
        lifecycleImpl.emitState(RimeLifecycle.State.STOPPED);
        unregisterRimeMessageHandler(mMessageHandler);
        scheduler.shutdownNow();
    }


    // ==================== JNI 原生方法 ====================

    static {
        System.loadLibrary("rime_jni"); // 加载原生库
    }

    /** Rime 消息处理器列表 */
    private static final List<Consumer<RimeMessage<?>>> rimeMessageHandlers = new ArrayList<>();

    /**
     * 初始化 Rime 引擎(原生方法)。
     *
     * @param sharedDir 共享数据目录。
     * @param userDir 用户数据目录。
     * @param versionName 版本号。
     * @param fullCheck 是否进行完整检查。
     */
    public static native void startupRime(
            String sharedDir,
            String userDir,
            String versionName,
            boolean fullCheck
    );

    /**
     * 退出 Rime 引擎(原生方法)。
     */
    public static native void exitRime();

    /**
     * 部署 Rime 方案文件(原生方法)。
     *
     * @param schemaFile 方案文件路径。
     * @return true 表示部署成功。
     */
    public static native boolean deployRimeSchemaFile(String schemaFile);

    /**
     * 部署 Rime 配置文件(原生方法)。
     *
     * @param fileName 配置文件名。
     * @param versionKey 版本键。
     * @return true 表示部署成功。
     */
    public static native boolean deployRimeConfigFile(
            String fileName,
            String versionKey
    );

    /**
     * 同步用户数据(原生方法)。
     *
     * @return true 表示同步成功。
     */
    public static native boolean syncRimeUserData();

    // ==================== 输入相关原生方法 ====================

    /**
     * 处理 Rime 按键(原生方法)。
     *
     * @param keycode 键码。
     * @param mask 修饰符掩码。
     * @return true 表示按键被处理。
     */
    public static native boolean processRimeKey(
            int keycode,
            int mask
    );

    /**
     * 提交当前组字内容(原生方法)。
     *
     * @return true 表示提交成功。
     */
    public static native boolean commitRimeComposition();

    /**
     * 清除当前组字内容(原生方法)。
     */
    public static native void clearRimeComposition();

    // ==================== 输出相关原生方法 ====================

    /**
     * 获取提交内容(原生方法)。
     *
     * @return 提交对象。
     */
    public static native RimeProto.Commit getRimeCommit();

    /**
     * 获取组字上下文(原生方法)。
     *
     * @return 上下文对象。
     */
    public static native RimeProto.Context getRimeContext();

    /**
     * 获取引擎状态(原生方法)。
     *
     * @return 状态对象。
     */
    public static native RimeProto.Status getRimeStatus();

    // ==================== 运行时选项相关原生方法 ====================

    /**
     * 设置 Rime 选项(原生方法)。
     *
     * @param option 选项名称。
     * @param value 选项值。
     */
    public static native void setRimeOption(
            String option,
            boolean value
    );

    /**
     * 获取 Rime 选项值(原生方法)。
     *
     * @param option 选项名称。
     * @return 选项值。
     */
    public static native boolean getRimeOption(String option);

    /**
     * 获取方案列表(原生方法)。
     *
     * @return 方案数组。
     */
    public static native SchemaItem[] getRimeSchemaList();

    /**
     * 获取当前方案 ID(原生方法)。
     *
     * @return 当前方案 ID。
     */
    public static native String getCurrentRimeSchema();

    /**
     * 选择指定方案(原生方法)。
     *
     * @param schemaId 方案 ID。
     * @return true 表示选择成功。
     */
    public static native boolean selectRimeSchema(String schemaId);

    // ==================== 测试相关原生方法 ====================

    /**
     * 模拟按键序列(原生方法)。
     *
     * @param keySequence 按键序列字符串。
     * @return true 表示模拟成功。
     */
    public static native boolean simulateRimeKeySequence(String keySequence);

    /**
     * 获取原始输入内容(原生方法)。
     *
     * @return 原始输入字符串。
     */
    public static native String getRimeRawInput();

    /**
     * 将 mixed 原始输入按“文本=编码”的形式写回当前方案 user_dict。
     */
    public static native boolean learnRimeRawInput(String text);

    /**
     * 按完整编码和词语向当前方案 user_dict 写入一条显式自造词。
     */
    public static native boolean addRimeUserPhrase(String code, String text);

    public static native boolean encodeRimeUserPhrase(String text);

    /**
     * 按完整编码和词语从当前方案 user_dict 删除一条显式自造词。
     */
    public static native boolean removeRimeUserPhrase(String code, String text);

    /**
     * 按输入前缀查询当前方案 user_dict 中的 mixed 补全项。
     */
    public static native String[] queryRimeRawInputCompletions(String prefix, int limit);

    /**
     * 判断当前方案 user_dict 中是否存在以前缀命中的指定词条。
     */
    public static native boolean hasRimeUserPhraseWithPrefix(String prefix, String text);

    /**
     * 按输入前缀和词语回查当前方案 user_dict 中保存的完整编码。
     */
    public static native String getRimeUserPhraseCodeWithPrefix(String prefix, String text);

    /**
     * 获取光标位置(原生方法)。
     *
     * @return 光标位置索引。
     */
    public static native int getRimeCaretPos();

    /**
     * 设置光标位置(原生方法)。
     *
     * @param caretPos 目标光标位置。
     */
    public static native void setRimeCaretPos(int caretPos);

    /**
     * 选择当前页中的候选词(原生方法)。
     *
     * @param index 候选词索引。
     * @return true 表示选择成功。
     */
    public static native boolean selectRimeCandidateOnCurrentPage(int index);

    /**
     * 删除当前页中的候选词(原生方法)。
     *
     * @param index 候选词索引。
     * @return true 表示删除成功。
     */
    public static native boolean deleteRimeCandidateOnCurrentPage(int index);

    /**
     * 选择指定索引的候选词(原生方法)。
     *
     * @param index 候选词索引。
     * @return true 表示选择成功。
     */
    public static native boolean selectRimeCandidate(int index);

    /**
     * 忘记指定索引的候选词(原生方法)。
     *
     * @param index 候选词索引。
     * @return true 表示忘记成功。
     */
    public static native boolean forgetRimeCandidate(int index);

    /**
     * 切换候选词页面(原生方法)。
     *
     * @param backward true 向前翻页,false 向后翻页。
     * @return true 表示翻页成功。
     */
    public static native boolean changeRimeCandidatePage(boolean backward);

    /**
     * 高亮指定索引的候选词(原生方法)。
     *
     * @param index 候选词索引。
     * @return true 表示高亮成功。
     */
    public static native boolean highlightRimeCandidate(int index);

    /**
     * 获取当前高亮的候选词索引(原生方法)。
     *
     * @return 高亮索引。
     */
    public static native int getHighlightRimeCandidate();

    /**
     * 获取可用方案列表(原生方法)。
     *
     * @return 可用方案数组。
     */
    public static native SchemaItem[] getAvailableRimeSchemaList();

    /**
     * 获取已选中方案列表(原生方法)。
     *
     * @return 已选中方案数组。
     */
    public static native SchemaItem[] getSelectedRimeSchemaList();

    /**
     * 选择多个方案(原生方法)。
     *
     * @param schemaIds 方案 ID 数组。
     * @return true 表示选择成功。
     */
    public static native boolean selectRimeSchemas(String[] schemaIds);

    /**
     * 获取候选词列表(原生方法)。
     *
     * @param startIndex 起始索引。
     * @param limit 数量限制。
     * @return 候选词数组。
     */
    public static native CandidateItem[] getRimeCandidates(
            int startIndex,
            int limit
    );

    /**
     * 获取当前组字文本。
     *
     * @return 组字文本,如果没有组字则返回空字符串。
     */
    public String getComposingText() {
        RimeProto.Context.Composition cc = getCompositionCached();
        if(cc!=null)
           return cc.getCommitTextPreview();
        return "";
    }


    /**
     * 消费者函数式接口(用于消息处理)。
     *
     * @param <T> 输入参数类型。
     */
    @FunctionalInterface
    public interface Consumer<T> {

        /**
         * Performs this operation on the given argument.
         *
         * @param t the input argument
         */
        void accept(T t);

        /**
         * Returns a composed {@code Consumer} that performs, in sequence, this
         * operation followed by the {@code after} operation. If performing either
         * operation throws an exception, it is relayed to the caller of the
         * composed operation.  If performing this operation throws an exception,
         * the {@code after} operation will not be performed.
         *
         * @param after the operation to perform after this operation
         * @return a composed {@code Consumer} that performs in sequence this
         * operation followed by the {@code after} operation
         * @throws NullPointerException if {@code after} is null
         */
        default Consumer<T> andThen(Consumer<? super T> after) {
            Objects.requireNonNull(after);
            return (T t) -> {
                accept(t);
                after.accept(t);
            };
        }
    }

    /**
     * 注册 Rime 消息处理器。
     *
     * @param handler 消息处理器。
     */
    public static void registerRimeMessageHandler(Consumer<RimeMessage<?>> handler) {
        if (rimeMessageHandlers.contains(handler)) return;
        rimeMessageHandlers.add(handler);
    }

    /**
     * 注销 Rime 消息处理器。
     *
     * @param handler 要注销的消息处理器。
     */
    public static void unregisterRimeMessageHandler(Consumer<RimeMessage<?>> handler) {
         rimeMessageHandlers.remove(handler);
    }

    /**
     * 注销所有 Rime 消息处理器。
     */
    public static void unregisterAllRimeMessageHandlers() {
        rimeMessageHandlers.clear();
    }
}
