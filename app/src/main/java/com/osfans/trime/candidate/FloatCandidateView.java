/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.candidate;

import static com.osfans.trime.keyboard.KeyboardView.isTouchExplorationEnabled;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

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
 * 浮动候选词视图类。
 * 用于显示浮动候选词窗口,包含预编辑文本和垂直候选词列表。
 */
public class FloatCandidateView extends LinearLayout implements View.OnClickListener {

    // ==================== 成员变量 ====================
    
    /** Trime 服务实例 */
    private final TrimeService mTrime;
    /** 候选词样式配置 */
    private final Style mCandidateStyle;
    /** 候选词列表视图 */
    private RecyclerView mListView;
    /** 隐藏/展开按钮 */
    private KeyView mHide;
    /** 隐藏/展开按钮的默认文案(非联想候选状态使用) */
    private String mDefaultHideText;
    /** 候选词适配器 */
    private FloatCandidateAdapter mAdapter;
    /** 工具栏视图 */
    private ToolbarView mToolbarView;
    /** 根布局容器 */
    private LinearLayout root;
    /** 预编辑文本视图 */
    private TextView mPreedit;

    /**
     * 构造函数。
     *
     * @param context Android 上下文。
     */
    public FloatCandidateView(@NonNull Context context) {
        super(context);
        mTrime = TrimeService.getInstance(); // 获取服务实例
        mCandidateStyle = ThemeManager.getStyle().getStyle("candidate"); // 获取候选词样式
        setClipChildren(false); // 允许子视图超出边界
        setClipToPadding(false);
        initView(); // 初始化视图
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        try {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    private void initView() {
        root = new LinearLayout(getContext()) {
            @Override
            public CharSequence getAccessibilityClassName() {
                return "com.nirenr.trime.candidate.CandidateItem";
            }
        };
        root.setOrientation(VERTICAL);
        int elevation = mCandidateStyle.getSize("elevation", 2);
        root.setElevation(elevation);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            int dShadowColor = mCandidateStyle.getColor("shadow_color", 0);
            if (dShadowColor != 0) {
                root.setOutlineAmbientShadowColor(dShadowColor);
                root.setOutlineSpotShadowColor(dShadowColor);
            }
        }
        mPreedit = new TextView(getContext());
        Style preeditColor = ThemeManager.getStyle().getStyle("preedit");
        mPreedit.setTextColor(preeditColor.getTextColor(0xffaaaaaa));
        mPreedit.setBackground(preeditColor.getBackground(0xff888888));
        mPreedit.setTextSize(TypedValue.COMPLEX_UNIT_DIP, preeditColor.getTextSize(18));
        int pd = ThemeManager.dp2px(4);
        mPreedit.setPadding(pd, pd, pd, pd);
        mPreedit.setVisibility(View.VISIBLE);
        mPreedit.setText(" ");
        // 设置 CandidateView 自身的高度，防止输入法界面闪烁
        int height = mCandidateStyle.getHeight(48) - elevation;
        mPreedit.setMinHeight(height);
        root.addView(mPreedit, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.setMargins(0, 0, 0, elevation);
        addView(root, lp);
        mListView = new RecyclerView(getContext());
        mListView.setClipChildren(false);
        mListView.setClipToPadding(false);
        // 性能优化：横向候选栏通常变动频繁，关闭动画更流畅
        mListView.setItemAnimator(null);
        // 2. 设置布局管理器 (核心步骤：指定为 HORIZONTAL)
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                getContext(),
                LinearLayoutManager.VERTICAL, // 横向布局
                false
        );
        mListView.setLayoutManager(layoutManager);
        // 必须设为 false，否则 RecyclerView 会跳过尺寸计算
        mListView.setHasFixedSize(false);
        LuaValue hide = mCandidateStyle.get("key");
        mHide = new KeyView(getContext(), mCandidateStyle.getKeyStyle("key", ThemeManager.getStyle().getKeyStyle("key")));
        if (hide.istable()) {
            mHide.setText(hide.get("text").optjstring("▽"));
        }

        mHide.setContentDescription("更多候选");
        mHide.setOnClickListener(this);
        mDefaultHideText = mHide.getText() != null ? mHide.getText().toString() : "▽"; // 记录默认文案,联想候选态需临时替换
        mHide.setMinimumWidth(height);
        root.addView(mListView, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(mHide, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, height));
        mListView.setAdapter(mAdapter = new FloatCandidateAdapter(new ArrayList<>()));
    }

    /**
     * 向下翻页。
     *
     * @return true 表示翻页成功。
     */
    public boolean pageDown() {
        mListView.smoothScrollBy(mListView.getWidth(), 0); // 平滑滚动
        return true;
    }

    /**
     * 向上翻页。
     *
     * @return true 表示翻页成功。
     */
    public boolean pageUp() {
        mListView.smoothScrollBy(-mListView.getWidth(), 0); // 向上滚动
        return true;
    }
    /**
     * 点击事件处理。
     *
     * @param v 被点击的视图。
     */
    @Override
    public void onClick(View v) {
        if (mTrime.isPredicting()) {
            mTrime.clearPredictionCandidates(); // 清除联想候选并关闭候选栏
        } else if (mAdapter.getItemCount()>0)
            mTrime.showExtractedCandidatesView(true); // 显示展开候选词视图
        else
            mTrime.requestHideSelf(0); // 隐藏输入法
    }

    /**
     * 根据是否处于联想候选状态,刷新隐藏/展开按钮的文案与描述。
     */
    private void refreshHideButton() {
        if (mTrime.isPredicting()) {
            mHide.setText("✕");
            mHide.setContentDescription("清除候选并关闭候选栏");
        } else {
            mHide.setText(mDefaultHideText);
            mHide.setContentDescription("更多候选");
        }
    }


    /**
     * 更新候选词列表。
     * 重置 CandidatesManager,加载新数据并动态调整宽度。
     */
    public void update() {
        if (mListView.isComputingLayout()) {
            mListView.post(this::update);
            return;
        }
        refreshHideButton(); // 刷新展开/关闭按钮状态
        CandidatesManager.reset();
        mAdapter.setData(CandidatesManager.next(5));
        if (hideCandidateBarIfEmpty()) {
            return;
        }
        if (mTrime != null) {
            mTrime.showCandidateAreaForMenu();
        }
        mListView.scrollToPosition(0);
        //mListView.invalidateItemDecorations();
        announceCandidate(0);
        mListView.post(new Runnable() {
            @Override
            public void run() {
                // 动态调整宽度
                int maxWidth = mAdapter.getMaxItemWidth(getContext()); // 获取最大项宽度
                ViewGroup.LayoutParams lp = mListView.getLayoutParams();
                lp.width = maxWidth; // 设置新宽度
                lp.height= ViewGroup.LayoutParams.WRAP_CONTENT;
                mListView.setLayoutParams(lp); // 应用新布局参数
            }
        });
   }

    /**
     * 显示候选词列表。
     * 如果有高亮候选词则定位到该位置,否则重新加载数据。
     */
    public void show() {
        refreshHideButton(); // 刷新展开/关闭按钮状态
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
        CandidatesManager.reset();
        CandidatesManager.resetFilter();
        mAdapter.setData(CandidatesManager.next(5));
        if (hideCandidateBarIfEmpty()) {
            return;
        }
        if (mTrime != null) {
            mTrime.showCandidateAreaForMenu();
        }
        // 第一种方式：直接调用 RecyclerView 的方法
        mListView.scrollToPosition(0);
        //mListView.invalidateItemDecorations();
        mListView.requestLayout();
        announceCandidate(0);

        mListView.post(new Runnable() {
            @Override
            public void run() {
                // 动态调整宽度
                int maxWidth = mAdapter.getMaxItemWidth(getContext());
                ViewGroup.LayoutParams lp = mListView.getLayoutParams();
                lp.width = maxWidth;
                lp.height= ViewGroup.LayoutParams.WRAP_CONTENT;
                mListView.setLayoutParams(lp);
            }
        });
    }

    /**
     * 当刷新后的候选列表为空时，直接收起候选栏。
     *
     * <p>悬浮候选栏和普通候选栏都需要遵守同一条行为：删除后如果已经没有候选，
     * 就不再保留空白面板。</p>
     *
     * @return true 表示已经收起候选栏；false 表示当前仍有候选可显示。
     */
    private boolean hideCandidateBarIfEmpty() {
        if (mAdapter.getItemCount() > 0) {
            return false;
        }
        if (mTrime != null) {
            mTrime.hideCandidateAreaForEmptyMenu();
        }
        return true;
    }

    /**
     * 无障碍播报指定索引的候选词。
     *
     * @param index 候选词索引。
     */
    private void announceCandidate(int index) {
        if (index < 0 || index >= mAdapter.getItemCount()) return;

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
            mToolbarView = new ToolbarView(getContext());
            addView(mToolbarView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        mToolbarView.setVisibility(b ? View.VISIBLE : View.GONE);
        root.setVisibility(!b ? View.VISIBLE : View.GONE);
    }

    /**
     * 刷新所有按键视图。
     */
    public void invalidateAllKeys() {
        if (mToolbarView != null) {
            mToolbarView.invalidateAllKeys();
        }
    }

    /**
     * 设置当前方案 ID。
     *
     * @param id 方案 ID。
     */
    public void setSchema(String id) {
        mToolbarView.setSchema(id);
    }

    /**
     * 选中上一个候选词。
     *
     * @return true 表示成功移动。
     */
    public boolean prevCandidate() {
        boolean ret = mAdapter.prevCandidate();
        if(ret)
            announceCandidate(Rime.getHighlightRimeCandidate());
        return ret;
    }

    /**
     * 选中下一个候选词。
     *
     * @return true 表示成功移动。
     */
    public boolean nextCandidate() {
        boolean ret = mAdapter.nextCandidate();
        if(ret)
            announceCandidate(Rime.getHighlightRimeCandidate());
        return ret;
    }

    /**
     * 获取候选词数据列表。
     *
     * @return 候选词列表。
     */
    public ArrayList<CandidateItem> getData() {
        return mAdapter.getData();
    }

    /**
     * 设置候选词数据列表。
     *
     * @param data 新的候选词列表。
     */
    public void setData(ArrayList<CandidateItem> data) {
        mAdapter.setData(data);
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

            // 2. 获取第一个“可见”项的序号（只要露出一像素就算）
            int firstVisibleItemPosition = linearManager.findFirstVisibleItemPosition();

            // 3. 获取第一个“完全可见”项的序号（整个 Item 都在屏幕内）
            int firstCompletelyVisibleItemPosition = linearManager.findFirstCompletelyVisibleItemPosition();

            return firstCompletelyVisibleItemPosition;
        }
        return 0;
    }

    /**
     * 设置选中索引并滚动到该位置。
     *
     * @param idx 目标索引。
     */
    public void setIdx(int idx) {
        if(idx<0||idx>=mAdapter.getItemCount()-1)
            return;
        LinearLayoutManager layoutManager = (LinearLayoutManager) mListView.getLayoutManager();
        if (layoutManager != null) {
            // 参数1：目标索引
            // 参数2：偏移量（0 表示完全置顶）
            layoutManager.scrollToPositionWithOffset(idx, 0);
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

    /**
     * 设置预编辑文本。
     *
     * @param text 预编辑文本内容。
     */
    public void setText(String text){
        mPreedit.setText(text); // 设置文本
        if(!TextUtils.isEmpty(text))
            show(); // 如果文本不为空,显示候选词
    }

    /**
     * 设置预编辑文本颜色。
     *
     * @param textColor 文本颜色。
     */
    public void setTextColor(int textColor) {
        mPreedit.setTextColor(textColor);
    }

    /**
     * 设置预编辑文本大小。
     *
     * @param complexUnitDip 单位类型。
     * @param textSize 文本大小。
     */
    public void setTextSize(int complexUnitDip, float textSize) {
        mPreedit.setTextSize(complexUnitDip,textSize);
    }
}
