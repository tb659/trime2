/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.candidate;

import static android.widget.LinearLayout.HORIZONTAL;
import static com.osfans.trime.keyboard.KeyboardView.isTouchExplorationEnabled;
import static com.osfans.trime.theme.ThemeManager.dp2px;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.osfans.trime.TrimeService;
import com.osfans.trime.core.CandidateItem;
import com.osfans.trime.core.Rime;
import com.osfans.trime.keyboard.KeyView;
import com.osfans.trime.theme.Style;
import com.osfans.trime.theme.ThemeManager;

import org.luaj.LuaValue;

import java.util.ArrayList;

/**
 * 候选词视图类。
 * 包含横向 RecyclerView 列表和隐藏按钮,用于展示和交互候选词。
 */
public class CandidateView extends LinearLayout implements View.OnClickListener {

    // ==================== 成员变量 ====================
    
    /** Trime 服务实例 */
    private final TrimeService mTrime;
    /** 候选词样式配置 */
    private final Style mCandidateStyle;
    /** 候选词列表视图 */
    private RecyclerView mListView;
    /** 隐藏/展开按钮 */
    private KeyView mHide;
    /** 候选词适配器 */
    private CandidateAdapter mAdapter;
    /** 工具栏视图 */
    private ToolbarView mToolbarView;
    /** 根布局容器 */
    private LinearLayout root;

    /**
     * 构造函数。
     *
     * @param context Android 上下文。
     */
    public CandidateView(@NonNull Context context) {
        super(context);
        mTrime = TrimeService.getInstance(); // 获取服务实例
        mCandidateStyle = ThemeManager.getStyle().getStyle("candidate"); // 获取候选词样式
        setClipChildren(false); // 允许子视图超出边界
        setClipToPadding(false);
        initView(); // 初始化视图
    }
    /**
     * 测量视图尺寸。
     *
     * @param widthMeasureSpec 宽度测量规格。
     * @param heightMeasureSpec 高度测量规格。
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        try {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }catch (Exception e){
            e.printStackTrace(); // 捕获测量异常,防止崩溃
        }
    }
    /**
     * 初始化视图结构。
     * 创建根布局、RecyclerView 列表和隐藏按钮。
     */
    private void initView() {
        root = new LinearLayout(getContext()) {
            @Override
            public CharSequence getAccessibilityClassName() {
                return "com.nirenr.trime.candidate.CandidateItem";
            }
        };
        root.setOrientation(HORIZONTAL);
        root.setBackground(mCandidateStyle.getBackground(0xffdddddd));
        int elevation = mCandidateStyle.getSize("elevation", 2);
        root.setElevation(elevation);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            int dShadowColor = mCandidateStyle.getColor("shadow_color", 0);
            if (dShadowColor != 0) {
                root.setOutlineAmbientShadowColor(dShadowColor);
                root.setOutlineSpotShadowColor(dShadowColor);
            }
        }
        // 设置 CandidateView 自身的高度,防止输入法界面闪烁
        int height = mCandidateStyle.getHeight(48) - elevation; // 计算实际高度
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.setMargins(0, 0, 0, elevation); // 设置底部边距为阴影高度
        addView(root, lp);
        
        // 创建 RecyclerView
        mListView = new RecyclerView(getContext());
        mListView.setClipChildren(false);
        mListView.setClipToPadding(false);
        // 性能优化:横向候选栏通常变动频繁,关闭动画更流畅
        mListView.setItemAnimator(null); // 禁用动画提升性能
        // 2. 设置布局管理器 (核心步骤:指定为 HORIZONTAL)
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                getContext(),
                LinearLayoutManager.HORIZONTAL, // 横向布局
                false
        );
        mListView.setLayoutManager(layoutManager);
        // 必须设为 false,否则 RecyclerView 会跳过尺寸计算
        mListView.setHasFixedSize(false); // 允许动态调整大小
        // 创建隐藏/展开按钮
        LuaValue hide = mCandidateStyle.get("key");
        mHide = new KeyView(getContext(), mCandidateStyle.getKeyStyle("key", ThemeManager.getStyle().getKeyStyle("key")));
        if (hide.istable()) {
            mHide.setText(hide.get("text").optjstring("▽")); // 从配置读取按钮文本
        }else {
            mHide.setText("▽"); // 默认文本

        }

        mHide.setContentDescription("更多候选"); // 无障碍描述
        mHide.setOnClickListener(this); // 设置点击监听
        int btnWidth = mCandidateStyle.getKeyStyle("key", ThemeManager.getStyle().getKeyStyle("key")).getSize("width", 0);
        if (btnWidth > 0) mHide.setMinimumWidth(btnWidth); else mHide.setMinimumWidth(height);
        root.addView(mListView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height, 1)); // 添加列表,权重为1
        root.addView(mHide, new LayoutParams(btnWidth > 0 ? btnWidth : ViewGroup.LayoutParams.WRAP_CONTENT, height)); // 添加按钮
        mListView.setAdapter(mAdapter = new CandidateAdapter(new ArrayList<>())); // 设置适配器
    }

    /**
     * 向下翻页。
     *
     * @return true 表示翻页成功。
     */
    public boolean pageDown() {
        mListView.smoothScrollBy(mListView.getWidth(), 0); // 平滑滚动一个屏幕宽度
        return true;
    }

    /**
     * 向上翻页。
     *
     * @return true 表示翻页成功。
     */
    public boolean pageUp() {
        mListView.smoothScrollBy(-mListView.getWidth(), 0); // 向上滚动一个屏幕宽度
        return true;
    }
    /**
     * 点击事件处理。
     * 点击隐藏按钮时,如果有候选词则显示展开视图,否则隐藏输入法。
     *
     * @param v 被点击的视图。
     */
    @Override
    public void onClick(View v) {
        if (mAdapter.getItemCount()>0)
            mTrime.showExtractedCandidatesView(true); // 显示展开候选词视图
        else
            mTrime.requestHideSelf(0); // 隐藏输入法
    }


    /**
     * 更新候选词列表。
     * 重置 CandidatesManager,加载第一页数据并滚动到顶部。
     */
    public void update() {
        if (mListView.isComputingLayout()) {
            mListView.post(this::update); // 如果正在布局,延迟执行
            return;
        }
        CandidatesManager.reset(); // 重置管理器
        mAdapter.setData(CandidatesManager.next()); // 加载第一页数据
        mListView.scrollToPosition(0); // 滚动到顶部
        //mListView.invalidateItemDecorations();
        announceCandidate(0); // 无障碍播报第一个候选词
    }

    /**
     * 显示候选词列表。
     * 如果有高亮候选词则定位到该位置,否则重新加载数据。
     */
    public void show() {
        int mIdx = Rime.getHighlightRimeCandidate(); // 获取高亮索引
        if (mIdx > 0) {
            mAdapter.setIdx(mIdx); // 设置选中索引
            announceCandidate(mIdx); // 无障碍播报
            return;
        }

        if (mListView.isComputingLayout()) {
            mListView.post(this::show); // 如果正在布局,延迟执行
            return;
        }
        CandidatesManager.reset(); // 重置管理器
        CandidatesManager.resetFilter(); // 重置过滤器
        mAdapter.setData(CandidatesManager.next()); // 加载第一页数据
        // 第一种方式:直接调用 RecyclerView 的方法
        mListView.scrollToPosition(0); // 滚动到顶部
        //mListView.invalidateItemDecorations();
        mListView.requestLayout(); // 请求重新布局
        announceCandidate(0); // 无障碍播报第一个候选词
    }

    /**
     * 显示候选词列表并定位到指定索引。
     *
     * @param idx 目标索引位置。
     */
    public void show(int idx) {
        int mIdx = Rime.getHighlightRimeCandidate();
        if (mIdx > 0) {
            mAdapter.setIdx(mIdx);
            announceCandidate(mIdx);
            return;
        }

        if (mListView.isComputingLayout()) {
            mListView.post(this::show);
            return;
        }
        CandidatesManager.reset(); // 重置管理器
        CandidatesManager.resetFilter(); // 重置过滤器
        //CandidatesManager.setStart(idx);
        mAdapter.setData(CandidatesManager.next()); // 加载第一页数据
        // 第一种方式:直接调用 RecyclerView 的方法
        //mListView.invalidateItemDecorations();
        mListView.requestLayout(); // 请求重新布局
        announceCandidate(0); // 无障碍播报第一个候选词
        setIdx(idx); // 设置选中索引
    }

    /**
     * 无障碍播报指定索引的候选词。
     *
     * @param index 候选词索引。
     */
    private void announceCandidate(int index) {
        if (index < 0 || index >= mAdapter.getItemCount()) return; // 索引越界检查

        if(!isTouchExplorationEnabled())
            return; // 如果未启用触摸探索,不播报
        // 设置朗读文本
        String text = mAdapter.getItem(index).getText(); // 获取候选词文本
        root.setContentDescription(text); // 设置无障碍描述
        // 发送事件
        root.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED); // 发送选中事件
    }

    /**
     * 显示或隐藏工具栏视图。
     *
     * @param b true 显示工具栏,false 显示候选词列表。
     */
    public void showToolbarView(boolean b) {
        if (mToolbarView == null) {
            mToolbarView = new ToolbarView(getContext()); // 创建工具栏
            addView(mToolbarView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        mToolbarView.setVisibility(b ? View.VISIBLE : View.GONE); // 设置工具栏可见性
        root.setVisibility(!b ? View.VISIBLE : View.GONE); // 设置根布局可见性(与工具栏相反)
    }

    /**
     * 刷新所有按键视图。
     * 更新工具栏和候选列表容器高度,以响应 _hide_comment 等选项变更。
     */
    public void invalidateAllKeys() {
        if (mToolbarView != null) {
            mToolbarView.invalidateAllKeys();
        }
        int elevation = mCandidateStyle.getSize("elevation", 2);
        int height = ThemeManager.getCandidateHeight() - elevation;
        ViewGroup.LayoutParams lp = mListView.getLayoutParams();
        lp.height = height;
        mListView.setLayoutParams(lp);
        int btnWidth = mCandidateStyle.getKeyStyle("key", ThemeManager.getStyle().getKeyStyle("key")).getSize("width", 0);
        if (btnWidth > 0) mHide.setMinimumWidth(btnWidth); else mHide.setMinimumWidth(height);
        ViewGroup.LayoutParams hideLp = mHide.getLayoutParams();
        hideLp.width = btnWidth > 0 ? btnWidth : ViewGroup.LayoutParams.WRAP_CONTENT;
        hideLp.height = height;
        mHide.setLayoutParams(hideLp);
    }

    /**
     * 设置当前方案 ID。
     *
     * @param id 方案 ID。
     */
    public void setSchema(String id) {
        mToolbarView.setSchema(id); // 通知工具栏更新方案
    }

    /**
     * 选中上一个候选词。
     *
     * @return true 表示成功移动。
     */
    public boolean prevCandidate() {
        boolean ret = mAdapter.prevCandidate(); // 委托给适配器处理
        if(ret)
            announceCandidate(Rime.getHighlightRimeCandidate()); // 无障碍播报
        return ret;
    }

    /**
     * 选中下一个候选词。
     *
     * @return true 表示成功移动。
     */
    public boolean nextCandidate() {
        boolean ret = mAdapter.nextCandidate(); // 委托给适配器处理
        if(ret)
            announceCandidate(Rime.getHighlightRimeCandidate()); // 无障碍播报
        return ret;
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

        if (layoutManager instanceof LinearLayoutManager) {
            LinearLayoutManager linearManager = (LinearLayoutManager) layoutManager;

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
        if(idx<0||idx>=mAdapter.getItemCount()-1)
            return; // 索引越界,直接返回
        LinearLayoutManager layoutManager = (LinearLayoutManager) mListView.getLayoutManager();
        if (layoutManager != null) {
            // 参数1:目标索引
            // 参数2:偏移量(0 表示完全置顶)
            layoutManager.scrollToPositionWithOffset(idx, 0); // 滚动到指定位置
        }
    }

    /**
     * 获取工具栏视图。
     *
     * @return ToolbarView 实例。
     */
    public ToolbarView getToolbar() {
        return mToolbarView;
    }

    public boolean isToolbarVisible() {
        return mToolbarView != null && mToolbarView.getVisibility() == VISIBLE;
    }
}
