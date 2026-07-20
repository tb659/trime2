/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.candidate;

import android.content.Context;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.osfans.trime.Config;
import com.osfans.trime.Event;
import com.osfans.trime.Key;
import com.osfans.trime.TrimeService;
import com.osfans.trime.core.Rime;
import com.osfans.trime.core.RimeSchema;
import com.osfans.trime.keyboard.KeyView;
import com.osfans.trime.theme.KeyStyle;
import com.osfans.trime.theme.Style;
import com.osfans.trime.theme.ThemeManager;

import org.luaj.LuaTable;
import org.luaj.LuaValue;

import java.util.ArrayList;
import java.util.List;

/**
 * 工具栏视图类。
 * 显示方案切换开关和自定义功能按键,支持横向滚动。
 * 工具栏可替换候选栏显示（通过 CandidateView.showToolbarView() 切换）。
 * 每个输入方案开关（switch）占一列，分为上下两行：
 *   上行：未选中选项（使用 comment 样式，仅在显示注释时可见）
 *   下行：当前选中选项（使用 key 样式，始终可见）
 * 自定义按键占整列高度，直接显示为按键。
 */
public class ToolbarView extends LinearLayout implements View.OnClickListener {

    // ==================== 成员变量 ====================

    /** Trime 服务实例 */
    private final TrimeService mTrime;
    /** toolbar 样式配置，从主题中读取 "toolbar" 样式节点 */
    private final Style mToolbarStyle;
    /** toolbar 中按键的默认 KeyStyle，取自 toolbar/key 或全局 key */
    private final KeyStyle mKeyStyle;
    /** 横向滚动容器，内含所有 switch 列和自定义按键 */
    private HorizontalScrollView mListView;
    /** 上行按键列表（switch 未选中状态），索引与 mBottomKeys 一一对应 */
    private ArrayList<KeyView> mTopKeys = new ArrayList<>();
    /** 下行按键列表（switch 当前状态 + 自定义按键） */
    private ArrayList<KeyView> mBottomKeys = new ArrayList<>();
    /** 隐藏键盘按钮，位于工具栏最右侧 */
    private KeyView mHide;

    /**
     * 构造函数。
     *
     * @param context Android 上下文。
     */
    public ToolbarView(Context context) {
        super(context);
        mTrime = TrimeService.getInstance(); // 获取服务实例
        mToolbarStyle = ThemeManager.getStyle().getStyle("toolbar"); // 获取工具栏样式
        mKeyStyle = mToolbarStyle.getKeyStyle("key", ThemeManager.getStyle().getKeyStyle("key")); // 获取按键样式
        setClipChildren(false); // 允许子视图超出边界
        setClipToPadding(false);
        initView(); // 初始化视图
    }

    /**
     * 初始化视图结构。
     *
     * 布局层次：
     *   ToolbarView (LinearLayout, VERTICAL — 但实际通过 root 横向布局)
     *     └─ root (LinearLayout, HORIZONTAL)
     *          ├─ mListView (HorizontalScrollView)  ← 可横向滚动
     *          │    └─ itemsLayout (LinearLayout)
     *          │         ├─ [column 0] (LinearLayout, VERTICAL)  ← 第 1 个 switch
     *          │         │    ├─ topKey (KeyView, comment 样式)  ← 未选中选项
     *          │         │    └─ bottomKey (KeyView, key 样式)   ← 当前选中选项
     *          │         ├─ [column 1] ...                      ← 第 2 个 switch
     *          │         ├─ [custom key N] (KeyView)            ← 自定义按键（占整列）
     *          │         └─ ...
     *          └─ mHide (KeyView)                               ← 隐藏按钮
     *
     * 每个 switch 为一列，上下两行分别显示未选中/选中状态。
     * 自定义按键（toolbar/keys）独立占一整列高度。
     */
    private void initView() {
        // 读取阴影高度，用于调整底部边距避免阴影被裁切
        int elevation = mToolbarStyle.getSize("elevation", 2);
        // 工具栏总高度 = 候选栏高度（由候选词文字大小 + 边距决定）
        int toolbarHeight = ThemeManager.getCandidateHeight();
        // 是否显示上行注释（受选项 _hide_comment 控制）
        boolean showTwoRows = !Config.is_hide_comment();

        // —— 创建外层的 root 容器（水平排列，包含滚动区 + 隐藏按钮） ——
        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(HORIZONTAL);
        root.setBackground(mToolbarStyle.getBackground(0xffdddddd));
        root.setElevation(elevation);
        // Android 9+ 支持分别设置阴影颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            int dShadowColor = mToolbarStyle.getColor("shadow_color", 0);
            if (dShadowColor != 0) {
                root.setOutlineAmbientShadowColor(dShadowColor);
                root.setOutlineSpotShadowColor(dShadowColor);
            }
        }
        // root 填满 ToolbarView 整体，通过 margins 控制与外部的间距
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        // 读取工具栏容器外边距配置；未配置时 bottom 默认等于 elevation，避免阴影被裁剪
        Style margins = mToolbarStyle.getStyle("margins");
        int marginBottom = margins.getSize("bottom", elevation);
        // 工具栏 root 内可用高度 = 工具栏总高 - bottom margin
        // 这样当 bottom > elevation 时仍能包住两行 switches
        int height = toolbarHeight - marginBottom;
        // 计算单行最小高度：确保 KeyView 内 keyRoot 装得下文字
        // 最小行高 = 字号 + 4px 内边距 + key 上下 margin
        Style keyMargins = mKeyStyle.getStyle("margins");
        int keyMarginTop = keyMargins.getSize("top", 0);
        int keyMarginBottom = keyMargins.getSize("bottom", 0);
        int minRowHeight = Math.round(mKeyStyle.getTextSize()) + 4 + keyMarginTop + keyMarginBottom;
        lp.setMargins(
                margins.getSize("left", 0),
                margins.getSize("top", 0),
                margins.getSize("right", 0),
                marginBottom
        );
        addView(root, lp);

        // —— 创建横向滚动容器（HorizontalScrollView）——
        mListView = new HorizontalScrollView(getContext());
        mListView.setHorizontalScrollBarEnabled(false);
        mListView.setVerticalScrollBarEnabled(false);
        // itemsLayout 是 HorizontalScrollView 内部的唯一子 View，用于放置所有按键
        LinearLayout itemsLayout = new LinearLayout(getContext());
        itemsLayout.setGravity(Gravity.CENTER);
        mListView.addView(itemsLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // —— 创建隐藏按钮（右侧固定的 "▽" 按钮）——
        LuaValue hide = mToolbarStyle.get("hide");
        // 隐藏按钮可以使用独立的样式（toolbar/hide），未配置时复用 key 样式
        KeyStyle hideStyle = hide.istable() ? mToolbarStyle.getKeyStyle("hide", mKeyStyle) : mKeyStyle;
        mHide = new KeyView(getContext(), hideStyle);
        if (hide.istable()) {
            mHide.setText(hide.get("text").optjstring(""));
        } else {
            mHide.setText(hide.optjstring("▽"));
        }
        mHide.setContentDescription("收起键盘");
        mHide.setOnClickListener(this);
        int hideWidth = hideStyle.getSize("width", 0);
        if (hideWidth > 0) mHide.setMinimumWidth(hideWidth); else mHide.setMinimumWidth(height);

        // 将横向滚动区和隐藏按钮添加到 root（滚动区占剩余空间权重 1）
        root.addView(mListView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height, 1));
        root.addView(mHide, new LayoutParams(hideWidth > 0 ? hideWidth : ViewGroup.LayoutParams.WRAP_CONTENT, height));

        mTopKeys.clear();
        mBottomKeys.clear();

        // 上行（未选中状态）使用 comment 样式，下行（当前状态）使用 key 样式
        KeyStyle topStyle = mToolbarStyle.getKeyStyle("comment", mKeyStyle);

        // —— 创建方案开关（switches）——
        try {
            // 仅在 schema_switches 启用且当前方案非 .default 时显示 switches
            if (mToolbarStyle.get("schema_switches").optboolean(false) && !Rime.getCurrentRimeSchema().equals(".default")) {
                RimeSchema currentRimeSchema = new RimeSchema(Rime.getCurrentRimeSchema());
                // 获取当前方案的所有开关选项（如简繁切换、全角半角等）
                List<RimeSchema.Switch> switches = currentRimeSchema.getSwitches();
                for (RimeSchema.Switch aSwitch : switches) {
                    // 跳过没有状态列表的 switch（无法显示）
                    if (aSwitch.getStates().isEmpty()) continue;

                    // 每个 switch 占一列（上下两行）
                    LinearLayout column = new LinearLayout(getContext());
                    column.setOrientation(VERTICAL);

                    // 单行高度 = 可用高度的一半，但不得低于最小行高
                    int rowHeight = Math.max(height / 2, minRowHeight);

                    // 上行：未选中选项
                    // 使用 comment 样式显示，区别于 key 样式（如颜色、字号不同）
                    // 仅在 _hide_comment = false 时显示
                    KeyView topKey = new KeyView(getContext(), topStyle) {
                        @Override
                        public void invalidateKey() {
                            super.invalidateKey();
                            setText(aSwitch.getUnState());
                        }
                    };
                    topKey.setOnClickListener(v -> aSwitch.toggleOption());
                    topKey.setText(aSwitch.getUnState());
                    int topKeyWidth = topStyle.getSize("width", 0);
                    if (topKeyWidth > 0) topKey.setMinimumWidth(topKeyWidth); else topKey.setMinimumWidth(rowHeight);
                    topKey.setVisibility(showTwoRows ? VISIBLE : GONE);

                    // 下行：当前选中选项（始终可见）
                    KeyView bottomKey = new KeyView(getContext(), mKeyStyle) {
                        @Override
                        public void invalidateKey() {
                            super.invalidateKey();
                            setText(aSwitch.getState());
                        }
                    };
                    bottomKey.setOnClickListener(v -> aSwitch.toggleOption());
                    bottomKey.setText(aSwitch.getState());
                    int bottomKeyWidth = mKeyStyle.getSize("width", 0);
                    if (bottomKeyWidth > 0) bottomKey.setMinimumWidth(bottomKeyWidth); else bottomKey.setMinimumWidth(rowHeight);

                    // 将上下行添加到列中
                    // 当 showTwoRows=false 时上行高度为 0 且 GONE，下行占满整列
                    column.addView(topKey, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, showTwoRows ? rowHeight : 0));
                    column.addView(bottomKey, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, showTwoRows ? rowHeight : height));
                    itemsLayout.addView(column, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

                    mTopKeys.add(topKey);
                    mBottomKeys.add(bottomKey);

                    // 不在工具栏重建时回写 reset 默认值，避免覆盖当前运行时开关状态。
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // —— 创建自定义按键（toolbar/keys）——
        // 这些按键由主题配置直接定义，可以指定文本、点击事件、样式等
        LuaValue keys = mToolbarStyle.get("keys").opttable(new LuaTable());
        int len = keys.length();
        for (int i = 0; i < len; i++) {
            LuaValue o = keys.get(i + 1);
            if (o.istable()) {
                // 表格式定义：可指定 style、click 事件、text 等
                LuaValue s = o.get("style");
                KeyStyle style = mKeyStyle;
                if (s.isstring()) {
                    // 使用指定的样式名称从全局查找
                    style = ThemeManager.getStyle().getKeyStyle(s.tojstring(), mKeyStyle);
                }
                // 若无 click 事件，则自动从表内容构造 Event 对象
                KeyView key = new KeyView(getContext(), o.get("click").isnil() ? new Key(new Event(o)) : new Key(o), style);
                int keyWidth = style.getSize("width", 0);
                itemsLayout.addView(key, new ViewGroup.LayoutParams(keyWidth > 0 ? keyWidth : ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
                if (keyWidth > 0) key.setMinimumWidth(keyWidth); else key.setMinimumWidth(height);
                mBottomKeys.add(key);
            } else if (o.isstring()) {
                // 字符串格式：直接用字符串文本创建按键
                KeyView key = new KeyView(getContext(), new Key(o.tojstring()), mKeyStyle);
                int keyWidth = mKeyStyle.getSize("width", 0);
                itemsLayout.addView(key, new ViewGroup.LayoutParams(keyWidth > 0 ? keyWidth : ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
                if (keyWidth > 0) key.setMinimumWidth(keyWidth); else key.setMinimumWidth(height);
                mBottomKeys.add(key);
            }
        }
    }

    /**
     * 点击事件处理。
     * 点击隐藏按钮时隐藏输入法。
     *
     * @param v 被点击的视图。
     */
    @Override
    public void onClick(View v) {
        // 隐藏输入法面板（所有候选、工具栏等一起消失）
        mTrime.requestHideSelf(0);
    }

    /**
     * 刷新所有按键视图。
     * 当 _hide_comment 选项变化或候选栏高度变化时调用，
     * 重新计算行高和可见性，并刷新所有按键文本。
     */
    public void invalidateAllKeys() {
        // 重新读取配置
        boolean showTwoRows = !Config.is_hide_comment();
        int toolbarHeight = ThemeManager.getCandidateHeight();
        int elevation = (int) mToolbarStyle.getSize("elevation", 2);
        Style margins = mToolbarStyle.getStyle("margins");
        int marginBottom = margins.getSize("bottom", elevation);
        int height = toolbarHeight - marginBottom;
        Style keyMargins = mKeyStyle.getStyle("margins");
        int keyMarginTop = keyMargins.getSize("top", 0);
        int keyMarginBottom = keyMargins.getSize("bottom", 0);
        int minRowHeight = Math.round(mKeyStyle.getTextSize()) + 4 + keyMarginTop + keyMarginBottom;
        int rowHeight = Math.max(height / 2, minRowHeight);

        // 更新滚动容器和隐藏按钮的高度
        ViewGroup.LayoutParams lvLp = mListView.getLayoutParams();
        lvLp.height = height;
        mListView.setLayoutParams(lvLp);
        ViewGroup.LayoutParams hideLp = mHide.getLayoutParams();
        hideLp.height = height;
        mHide.setLayoutParams(hideLp);

        // 更新 switch 列（前 mTopKeys.size() 个为 switch，后续为自定义按键）
        int switchCount = mTopKeys.size();
        for (int i = 0; i < switchCount; i++) {
            View topKey = mTopKeys.get(i);
            View bottomKey = mBottomKeys.get(i);
            // 上行：可见性随 showTwoRows 变化
            ViewGroup.LayoutParams topLp = topKey.getLayoutParams();
            topLp.height = showTwoRows ? rowHeight : 0;
            topKey.setLayoutParams(topLp);
            topKey.setVisibility(showTwoRows ? VISIBLE : GONE);
            // 下行：高度自适应（两行时占一半，单行时占满）
            ViewGroup.LayoutParams bottomLp = bottomKey.getLayoutParams();
            bottomLp.height = showTwoRows ? rowHeight : height;
            bottomKey.setLayoutParams(bottomLp);
            // 刷新按键文本（switch 状态可能已变化）
            mTopKeys.get(i).invalidateKey();
            mBottomKeys.get(i).invalidateKey();
        }
        // 刷新自定义按键（仅下行，索引 >= switchCount）
        for (int i = switchCount; i < mBottomKeys.size(); i++) {
            mBottomKeys.get(i).invalidateKey();
        }
    }

    /**
     * 设置当前方案 ID。
     * 当输入方案切换时调用，重新初始化视图以加载新方案的开关配置。
     *
     * @param id 方案 ID。
     */
    public void setSchema(String id) {
        // 移除所有子 View，重新执行 initView() 构建新工具栏
        removeAllViews();
        initView();
    }

    /**
     * 获取隐藏按钮。
     *
     * @return 隐藏按钮 KeyView 实例。
     */
    public KeyView getHide() {
        return mHide;
    }
}
