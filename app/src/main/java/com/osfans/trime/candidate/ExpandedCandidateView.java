/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.candidate;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.osfans.trime.Event;
import com.osfans.trime.Key;
import com.osfans.trime.TrimeService;
import com.osfans.trime.core.CandidateItem;
import com.osfans.trime.keyboard.KeyView;
import com.osfans.trime.theme.KeyStyle;
import com.osfans.trime.theme.Style;
import com.osfans.trime.theme.ThemeManager;

import org.luaj.LuaValue;

import java.util.ArrayList;

/**
 * 展开候选词视图类。
 * 使用 FlexboxLayoutManager 实现流式布局的候选词展示,支持笔画过滤、单字过滤等功能。
 */
public class ExpandedCandidateView extends LinearLayout {

    // ==================== 成员变量 ====================
    
    /** Trime 服务实例 */
    private final TrimeService mTrime;
    /** 候选词样式配置 */
    private final Style mCandidateStyle;
    /** 按键样式配置 */
    private final KeyStyle mKeyStyle;
    /** 候选词列表视图 */
    private RecyclerView mListView;
    /** Flexbox 适配器 */
    private FlexboxCandidateAdapter mAdapter;
    /** 单字/全词切换按钮 */
    private KeyView mChar;

    /**
     * 构造函数。
     *
     * @param context Android 上下文。
     */
    public ExpandedCandidateView(@NonNull Context context) {
        super(context);
        Style candidateStyle = ThemeManager.getStyle().getStyle("candidate");
        mCandidateStyle = candidateStyle.getStyle("expanded", candidateStyle); // 获取展开视图样式
        mKeyStyle = mCandidateStyle.getKeyStyle("key", ThemeManager.getStyle().getKeyStyle("key")); // 获取按键样式
        mTrime = TrimeService.getInstance(); // 获取服务实例
        setBackground(mCandidateStyle.getBackground(0x00000000)); // 设置背景
        initView(); // 初始化视图
        //setClipChildren(false);
        //setClipToPadding(false);
    }

    /**
     * 初始化视图结构。
     * 创建 Flexbox 列表、过滤按钮栏和工具栏。
     */
    private void initView() {
        // 创建 RecyclerView
        mListView = new RecyclerView(getContext());
        mListView.setClipChildren(false);
        mListView.setClipToPadding(false);
        
        // 创建 FlexboxLayoutManager,支持异常捕获
        FlexboxLayoutManager flexManager = new FlexboxLayoutManager(getContext()) {
            @Override
            public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
                try {
                    super.onLayoutChildren(recycler, state);
                } catch (IndexOutOfBoundsException e) {
                    // 仅记录日志，不让应用崩溃
                    android.util.Log.e("ExpandedCandidate", "Catching Flexbox layout crash", e);
                }
            }
        };
        // 1. 主轴设为横向 (ROW)
        flexManager.setFlexDirection(FlexDirection.ROW); // 横向排列
        flexManager.setFlexWrap(FlexWrap.WRAP);             // 开启换行（换列）
        // 4. 设置对齐方式
        // 1. 改为左对齐，配合 Adapter 里的 flexGrow 实现整齐填充
        flexManager.setJustifyContent(JustifyContent.FLEX_START); // 左对齐
        flexManager.setAlignItems(AlignItems.CENTER); // 居中对齐
        mListView.setLayoutManager(flexManager);
        mListView.setItemViewCacheSize(40); // 增加缓存数量
        mListView.setItemAnimator(null); // 禁用动画提升性能
        //mListView.setInitialPrefetchItemCount(8); // 提前预取
        // 创建过滤按钮栏
        LinearLayout mButtons = new LinearLayout(getContext());
        mButtons.setOrientation(VERTICAL); // 垂直排列
        mButtons.setClipChildren(false);
        mButtons.setClipToPadding(false);

        // 创建工具栏
        LinearLayout mButtonBar = new LinearLayout(getContext());
        mButtonBar.setOrientation(VERTICAL); // 垂直排列
        mButtonBar.setClipChildren(false);
        mButtonBar.setClipToPadding(false);

        // 创建主布局容器
        LinearLayout layout = new LinearLayout(getContext());

        // 根据配置设置过滤栏位置
        switch (mCandidateStyle.getKeyStyle("filter_bar").getGravity(Gravity.LEFT)) {
            case Gravity.LEFT:
                mButtons.setOrientation(VERTICAL); // 垂直排列
                layout.setOrientation(HORIZONTAL); // 水平排列
                layout.addView(mButtons, new LinearLayout.LayoutParams(ThemeManager.getCandidateHeight(), ViewGroup.LayoutParams.MATCH_PARENT));
                layout.addView(mListView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
                addView(layout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
                break;
            case Gravity.RIGHT:
                mButtons.setOrientation(VERTICAL);
                layout.setOrientation(HORIZONTAL);
                layout.addView(mListView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
                layout.addView(mButtons, new LinearLayout.LayoutParams(ThemeManager.getCandidateHeight(), ViewGroup.LayoutParams.MATCH_PARENT));
                addView(layout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
                break;
            case Gravity.TOP:
                mButtons.setOrientation(HORIZONTAL); // 水平排列
                layout.setOrientation(VERTICAL); // 垂直排列
                layout.addView(mButtons, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeManager.getCandidateHeight()));
                layout.addView(mListView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
                addView(layout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
                break;
            case Gravity.BOTTOM:
                mButtons.setOrientation(HORIZONTAL);
                layout.setOrientation(VERTICAL);
                layout.addView(mListView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
                layout.addView(mButtons, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeManager.getCandidateHeight()));
                addView(layout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
                break;
        }
        if (!mCandidateStyle.getKeyStyle("filter_bar").getBoolean("show", true)) {
            mButtons.setVisibility(GONE); // 隐藏过滤栏
        }
        // 根据配置设置工具栏位置
        switch (mCandidateStyle.getKeyStyle("tool_bar").getGravity(Gravity.RIGHT)) {
            case Gravity.LEFT:
                setOrientation(HORIZONTAL); // 水平排列
                mButtonBar.setOrientation(VERTICAL);
                addView(mButtonBar, 0, new LinearLayout.LayoutParams(ThemeManager.getCandidateHeight(), ViewGroup.LayoutParams.MATCH_PARENT));
                break;
            case Gravity.RIGHT:
                setOrientation(HORIZONTAL);
                mButtonBar.setOrientation(VERTICAL);
                addView(mButtonBar, new LinearLayout.LayoutParams(ThemeManager.getCandidateHeight(), ViewGroup.LayoutParams.MATCH_PARENT));
                break;
            case Gravity.TOP:
                setOrientation(VERTICAL); // 垂直排列
                mButtonBar.setOrientation(HORIZONTAL);
                addView(mButtonBar, 0, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeManager.getCandidateHeight()));
                break;
            case Gravity.BOTTOM:
                setOrientation(VERTICAL);
                mButtonBar.setOrientation(HORIZONTAL);
                addView(mButtonBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ThemeManager.getCandidateHeight()));
                break;
        }

        //addView(mButtons,new LinearLayout.LayoutParams(ThemeManager.getCandidateHeight(), ViewGroup.LayoutParams.MATCH_PARENT));
        //addView(mListView,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT,1));
        //addView(mButtonBar,new LinearLayout.LayoutParams(ThemeManager.getCandidateHeight(), ViewGroup.LayoutParams.MATCH_PARENT));

        // 添加左侧六个笔画过滤/功能键
        addStrokeKey(mButtons, "h", "一", "横"); // 横笔画过滤
        addStrokeKey(mButtons, "s", "丨", "竖"); // 竖笔画过滤
        addStrokeKey(mButtons, "p", "丿", "撇"); // 撇笔画过滤
        addStrokeKey(mButtons, "n", "丶", "点/捺"); // 点/捺笔画过滤
        addStrokeKey(mButtons, "z", "乙", "折"); // 折笔画过滤
        addStrokeKey(mButtons, null, "X", "清空过滤"); //  (对应你代码中的 x) - 清空过滤条件

        mListView.setAdapter(mAdapter = new FlexboxCandidateAdapter(new ArrayList<>())); // 设置适配器

        // 创建隐藏按钮
        KeyView mHide = new KeyView(getContext(), mKeyStyle);
        mHide.setText("△");
        mHide.setContentDescription("收起候选面板");
        mHide.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mTrime.showExtractedCandidatesView(false); // 收起展开视图
            }
        });
        
        // 创建上一页按钮
        KeyView mPrev = new KeyView(getContext(), mKeyStyle);
        mPrev.setText("⇑");
        mPrev.setContentDescription("上一页");
        mPrev.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                pageUp(); // 向上翻页
            }
        });
        
        // 创建下一页按钮
        KeyView mNext = new KeyView(getContext(), mKeyStyle);
        mNext.setText("⇓");
        mNext.setContentDescription("下一页");
        mNext.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                pageDown(); // 向下翻页
            }
        });
        
        // 创建单字/全词切换按钮
        mChar = new KeyView(getContext(), mKeyStyle);
        mChar.setText("全/单");
        mChar.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mChar.setText(CandidatesManager.toggleFilterChar() ? "单/全" : "全/单"); // 切换文本
                update(); // 更新列表
            }
        });

        // 根据配置添加工具栏按键
        LuaValue keys = mCandidateStyle.getKeyStyle("tool_bar").get("keys");
        if (keys.istable()) {
            int len = keys.length();
            for (int i = 0; i < len; i++) {
                String k = keys.get(i + 1).tojstring();
                switch (k) {
                    case "hide":
                        mButtonBar.addView(mHide, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1)); // 添加隐藏按钮
                        break;
                    case "page_up":
                        mButtonBar.addView(mPrev, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1)); // 添加上一页按钮
                        break;
                    case "page_down":
                        mButtonBar.addView(mNext, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1)); // 添加下一页按钮
                        break;
                    case "char_filter":
                        mButtonBar.addView(mChar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1)); // 添加单字过滤按钮
                        break;
                    default:
                        KeyView key = new KeyView(getContext(), new Key(new Event(k)), mKeyStyle); // 创建自定义按键
                        mButtonBar.addView(key, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
                }
            }
        } else {
            // 默认添所有按钮
            mButtonBar.addView(mHide, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
            mButtonBar.addView(mPrev, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
            mButtonBar.addView(mNext, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
            mButtonBar.addView(mChar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1));
        }
    }

    /**
     * 向下翻页。
     *
     * @return true 表示翻页成功。
     */
    public boolean pageDown() {
        mListView.smoothScrollBy(0, mListView.getHeight()); // 平滑滚动一个屏幕高度
        return true;
    }

    /**
     * 向上翻页。
     *
     * @return true 表示翻页成功。
     */
    public boolean pageUp() {
        mListView.smoothScrollBy(0, -mListView.getHeight()); // 向上滚动一个屏幕高度
        return true;
    }

    /**
     * 添加笔画过滤按键。
     *
     * @param parent 父容器。
     * @param stroke 笔画代码。
     * @param label 显示文本。
     * @param cd 无障碍描述。
     */
    private void addStrokeKey(LinearLayout parent, String stroke, String label, String cd) {
        KeyView key = new KeyView(getContext(), mKeyStyle);
        key.setText(label); // 设置显示文本
        key.setContentDescription(cd); // 设置无障碍描述
        key.setOnClickListener(v -> {
            CandidatesManager.filterStroke(stroke, label); // 设置笔画过滤
            update(); // 更新列表
        });
        parent.addView(key, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1)); // 添加到父容器,权重为1
    }

    /**
     * 更新候选词列表。
     * 重置 CandidatesManager,加载新数据并滚动到顶部。
     */
    public void update() {
        if (mListView.isComputingLayout()) {
            // 如果正在布局，延迟一帧更新，防止冲突
            mListView.post(this::update); // 延迟执行
            return;
        }
        CandidatesManager.reset(); // 重置管理器
        mListView.stopScroll(); // 刷新数据前停止可能的滑动
        mAdapter.setData(CandidatesManager.next(40)); // 加载40个候选词
        mListView.scrollToPosition(0); // 滚动到顶部
        //mListView.invalidateItemDecorations();
    }

    /**
     * 显示候选词列表。
     * 重置所有过滤器,加载第一页数据。
     */
    public void show() {
        if (mListView.isComputingLayout()) {
            // 如果正在布局，延迟一帧更新，防止冲突
            mListView.post(this::show); // 延迟执行
            return;
        }
        CandidatesManager.reset(); // 重置管理器
        CandidatesManager.resetFilter(); // 重置过滤器
        mAdapter.setData(CandidatesManager.next(40)); // 加载40个候选词
        mListView.scrollToPosition(0); // 滚动到顶部
        mChar.setText(CandidatesManager.isFilterChar() ? "单/全" : "全/单"); // 更新按钮文本
        //mListView.invalidateItemDecorations();
        if (mAdapter.getItemCount() == 0) {
            mTrime.showExtractedCandidatesView(false); // 如果没有候选词,收起视图
        }
    }

    /**
     * 获取候选词数据列表。
     *
     * @return 候选词列表。
     */
    public ArrayList<CandidateItem> getData() {
        return mAdapter.getData(); // 委托给适配器
    }

    /**
     * 设置候选词数据列表。
     *
     * @param data 新的候选词列表。
     */
    public void setData(ArrayList<CandidateItem> data) {
        mAdapter.setData(data); // 委托给适配器
    }

    /**
     * 获取当前完全可见的第一个候选词索引。
     *
     * @return 完全可见项的索引,如果未找到则返回0。
     */
    public int getIdx() {
        // 1. 获取 LayoutManager 并转型
        RecyclerView.LayoutManager layoutManager = mListView.getLayoutManager();

        if (layoutManager instanceof FlexboxLayoutManager) {
            FlexboxLayoutManager linearManager = (FlexboxLayoutManager) layoutManager;

            // 2. 获取第一个“可见”项的序号(只要露出一像素就算)
            int firstVisibleItemPosition = linearManager.findFirstVisibleItemPosition();

            // 3. 获取第一个“完全可见”项的序号(整个 Item 都在屏幕内)
            int firstCompletelyVisibleItemPosition = linearManager.findFirstCompletelyVisibleItemPosition();

            return firstCompletelyVisibleItemPosition; // 返回完全可见的索引
        }
        return 0; // 默认返回0
    }

    /**
     * 设置选中索引并滚动到该位置。
     *
     * @param idx 目标索引。
     */
    public void setIdx(int idx) {
        if (idx < 0 || idx >= mAdapter.getItemCount() - 1)
            return; // 索引越界,直接返回
        FlexboxLayoutManager layoutManager = (FlexboxLayoutManager) mListView.getLayoutManager();
        if (layoutManager == null) return;


        layoutManager.scrollToPosition(idx); // 先瞬间移动到目标位置
        // 2. 延迟一点点进行平滑置顶
        mListView.post(() -> {
            // 1. 先瞬间移动到目标位置（虽然不保证置顶，但能把目标拉入预加载范围）
            LinearSmoothScroller smoothScroller = new LinearSmoothScroller(getContext()) {
                @Override
                protected int getVerticalSnapPreference() {
                    return SNAP_TO_START; // 对齐到顶部
                }
            };
            smoothScroller.setTargetPosition(idx); // 设置目标位置
            layoutManager.startSmoothScroll(smoothScroller); // 开始平滑滚动
        });
    }
}
