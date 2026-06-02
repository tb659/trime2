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
 */
public class ToolbarView extends LinearLayout implements View.OnClickListener {

    // ==================== 成员变量 ====================

    /** Trime 服务实例 */
    private final TrimeService mTrime;
    /** 工具栏样式配置 */
    private final Style mToolbarStyle;
    /** 按键样式配置 */
    private final KeyStyle mKeyStyle;
    /** 横向滚动容器 */
    private HorizontalScrollView mListView;
    /** 上行按键列表（switch 未选中状态） */
    private ArrayList<KeyView> mTopKeys = new ArrayList<>();
    /** 下行按键列表（switch 当前状态 + 自定义按键） */
    private ArrayList<KeyView> mBottomKeys = new ArrayList<>();
    /** 隐藏按钮 */
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
     * 每个 switch 为一列（上：未选中，有注释时显示；下：当前选中，始终显示），自定义按键占整列高度。
     */
    private void initView() {
        int elevation = mToolbarStyle.getSize("elevation", 2);
        int toolbarHeight = ThemeManager.getCandidateHeight();
        boolean showTwoRows = !Config.is_hide_comment();

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(HORIZONTAL);
        root.setBackground(mToolbarStyle.getBackground(0xffdddddd));
        root.setElevation(elevation);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            int dShadowColor = mToolbarStyle.getColor("shadow_color", 0);
            if (dShadowColor != 0) {
                root.setOutlineAmbientShadowColor(dShadowColor);
                root.setOutlineSpotShadowColor(dShadowColor);
            }
        }
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        // 读取工具栏容器外边距配置；未配置时 bottom 默认等于 elevation，避免阴影被裁剪
        Style margins = mToolbarStyle.getStyle("margins");
        int marginBottom = margins.getSize("bottom", elevation);
        // 工具栏 root 内可用高度 = 工具栏总高 - bottom margin；这样当 bottom > elevation 时仍能包住两行 switches
        int height = toolbarHeight - marginBottom;
        // 让 rowHeight 至少为「文字高度(含 font padding 估计)+ key 上下 margin」,保证 KeyView 内 keyRoot 装得下文字
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

        // 创建横向滚动容器
        mListView = new HorizontalScrollView(getContext());
        mListView.setHorizontalScrollBarEnabled(false);
        mListView.setVerticalScrollBarEnabled(false);
        LinearLayout itemsLayout = new LinearLayout(getContext());
        itemsLayout.setGravity(Gravity.CENTER);
        mListView.addView(itemsLayout, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LuaValue hide = mToolbarStyle.get("hide");
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

        root.addView(mListView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height, 1));
        root.addView(mHide, new LayoutParams(hideWidth > 0 ? hideWidth : ViewGroup.LayoutParams.WRAP_CONTENT, height));

        mTopKeys.clear();
        mBottomKeys.clear();

        // 上行（未选中）用 comment 样式，下行（当前）用 key 样式
        KeyStyle topStyle = mToolbarStyle.getKeyStyle("comment", mKeyStyle);

        try {
            if (mToolbarStyle.get("schema_switches").optboolean(false) && !Rime.getCurrentRimeSchema().equals(".default")) {
                RimeSchema currentRimeSchema = new RimeSchema(Rime.getCurrentRimeSchema());
                List<RimeSchema.Switch> switches = currentRimeSchema.getSwitches();
                for (RimeSchema.Switch aSwitch : switches) {
                    if (aSwitch.getStates().isEmpty()) continue;

                    LinearLayout column = new LinearLayout(getContext());
                    column.setOrientation(VERTICAL);

                    int rowHeight = Math.max(height / 2, minRowHeight);

                    // 上：未选中选项（comment 样式，有注释时显示）
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

                    // 下：当前选中选项（key 样式，始终显示）
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

                    column.addView(topKey, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, showTwoRows ? rowHeight : 0));
                    column.addView(bottomKey, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, showTwoRows ? rowHeight : height));
                    itemsLayout.addView(column, new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

                    mTopKeys.add(topKey);
                    mBottomKeys.add(bottomKey);

                    Rime.setRimeOption(aSwitch.getName(), aSwitch.getReset() != 0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 自定义按键（占整列高度）
        LuaValue keys = mToolbarStyle.get("keys").opttable(new LuaTable());
        int len = keys.length();
        for (int i = 0; i < len; i++) {
            LuaValue o = keys.get(i + 1);
            if (o.istable()) {
                LuaValue s = o.get("style");
                KeyStyle style = mKeyStyle;
                if (s.isstring()) {
                    style = ThemeManager.getStyle().getKeyStyle(s.tojstring(), mKeyStyle);
                }
                KeyView key = new KeyView(getContext(), o.get("click").isnil() ? new Key(new Event(o)) : new Key(o), style);
                int keyWidth = style.getSize("width", 0);
                itemsLayout.addView(key, new ViewGroup.LayoutParams(keyWidth > 0 ? keyWidth : ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
                if (keyWidth > 0) key.setMinimumWidth(keyWidth); else key.setMinimumWidth(height);
                mBottomKeys.add(key);
            } else if (o.isstring()) {
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
        mTrime.requestHideSelf(0); // 隐藏输入法
    }

    /**
     * 刷新所有按键视图。
     * 更新列中行高可见性和全部按键文本。
     */
    public void invalidateAllKeys() {
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

        ViewGroup.LayoutParams lvLp = mListView.getLayoutParams();
        lvLp.height = height;
        mListView.setLayoutParams(lvLp);
        ViewGroup.LayoutParams hideLp = mHide.getLayoutParams();
        hideLp.height = height;
        mHide.setLayoutParams(hideLp);

        int switchCount = mTopKeys.size();
        for (int i = 0; i < switchCount; i++) {
            View topKey = mTopKeys.get(i);
            View bottomKey = mBottomKeys.get(i);
            ViewGroup.LayoutParams topLp = topKey.getLayoutParams();
            topLp.height = showTwoRows ? rowHeight : 0;
            topKey.setLayoutParams(topLp);
            topKey.setVisibility(showTwoRows ? VISIBLE : GONE);
            ViewGroup.LayoutParams bottomLp = bottomKey.getLayoutParams();
            bottomLp.height = showTwoRows ? rowHeight : height;
            bottomKey.setLayoutParams(bottomLp);
            mTopKeys.get(i).invalidateKey();
            mBottomKeys.get(i).invalidateKey();
        }
        for (int i = switchCount; i < mBottomKeys.size(); i++) {
            mBottomKeys.get(i).invalidateKey();
        }
    }

    /**
     * 设置当前方案 ID。
     * 重新初始化视图以加载新方案的开关。
     *
     * @param id 方案 ID。
     */
    public void setSchema(String id) {
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
