/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.keyboard;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;

import androidx.annotation.NonNull;

import com.osfans.trime.Key;
import com.osfans.trime.TrimeService;
import com.osfans.trime.theme.KeyStyle;
import com.osfans.trime.theme.Style;
import com.osfans.trime.theme.ThemeManager;

import org.luaj.Globals;
import org.luaj.LuaTable;

import java.util.ArrayList;
import java.util.List;

/**
 * 浮动键盘类。
 * 用于显示弹出窗口中的按键,如长按弹出的候选键列表。
 * 支持网格布局,最多5列。
 */
public class FloatKeyboard extends KeyboardView {

    // ==================== 成员变量 ====================
    /** 按键文本列表 */
    private final List<String> mKeys;
    /** 弹出窗口样式 */
    private final Style mPopupStyle;
    /** 按键样式 */
    private final KeyStyle mKeyStyle;
    /** 键盘高度(像素) */
    private double mHeight;
    /** 键盘宽度(像素) */
    private double mWidth;

    /** 按键宽度百分比 */
    private double mKeyWidth;
    /** 布局配置(Lua 表) */
    private LuaTable mLayout;
    /** 左侧偏移量 */
    private double mLeft;
    /** 按键高度百分比 */
    private int mKeyHeight;
    /** X 轴偏移量(用于触摸事件坐标修正) */
    private int mOffsetX;

    /**
     * 构造函数。
     *
     * @param context 上下文。
     * @param globals Lua 全局环境。
     * @param keys 按键文本列表。
     */
    public FloatKeyboard(@NonNull Context context, Globals globals, List<String> keys) {
        super(context, globals);
        setClipChildren(false);
        setClipToPadding(false);
        mKeys = keys;
        long time = System.currentTimeMillis();
        mPopupStyle=ThemeManager.getStyle().getStyle("popup",ThemeManager.getStyle().getStyle("keyboard"));
        mKeyStyle=mPopupStyle.getKeyStyle("key",ThemeManager.getStyle().getKeyStyle());
        setBackground(mPopupStyle.getBackground(0xffdddddd));
        loadRows();
        Log.w("FloatKeyboard", "init time: " + (System.currentTimeMillis() - time));
        setKeySwipe(true);
        setElevation(mPopupStyle.getInt("elevation"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            int dShadowColor = mPopupStyle.getColor("shadow_color");
            if (dShadowColor != 0) {
                setOutlineAmbientShadowColor(dShadowColor);
                setOutlineSpotShadowColor(dShadowColor);
            }
        }
    }

    /**
     * 加载键盘行配置。
     * 创建 GridLayout,并将按键添加到网格中。
     */
    private void loadRows() {
        GridLayout grid = new GridLayout(getContext());
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        // 设置为5列布局
        grid.setColumnCount(5);
        TrimeService mTrime = TrimeService.getInstance();
        mHeight = ThemeManager.getKeyboardHeight();
        mWidth = mTrime.getWidth();
        int len = mKeys.size();
        mKeyWidth = mKeyStyle.getInt("width",10);
        mKeyHeight = mKeyStyle.getInt("height",15);
        for (int i = 0; i < len; i++) {
            loadKey(grid, mKeys.get(i), mKeyWidth, mKeyHeight);
        }
        addView(grid, new ViewGroup.LayoutParams(getRawWidth(), getRawHeight()));
    }


    /**
     * 加载单个按键。
     * 将按键添加到 GridLayout 中。
     *
     * @param grid GridLayout 容器。
     * @param key 按键文本。
     * @param width 按键宽度百分比。
     * @param height 按键高度百分比。
     */
    private void loadKey(GridLayout grid, String key, double width, double height) {
        width = (mWidth * width / 100);
        height = (mHeight * height / 100);
        KeyView keyView = new KeyView(getContext(), new Key(key), mKeyStyle);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams((int) width, (int) height, Gravity.TOP | Gravity.LEFT);
        grid.addView(keyView, layoutParams);
    }

    /**
     * 获取键盘原始高度。
     * 根据按键数量和行数计算总高度。
     *
     * @return 键盘高度(像素)。
     */
    public int getRawHeight() {
        return (int) (mHeight * mKeyHeight / 100) * ((mKeys.size() - 1) / 5 + 1);
    }

    /**
     * 获取键盘原始宽度。
     * 根据按键数量(最多5个)计算总宽度。
     *
     * @return 键盘宽度(像素)。
     */
    public int getRawWidth() {
        return (int) (mWidth * mKeyWidth / 100) * Math.min(mKeys.size(), 5);
    }

    /**
     * 设置 X 轴偏移量。
     * 用于修正触摸事件的坐标。
     *
     * @param x X 轴偏移量。
     */
    public void setOffsetX(int x) {
        mOffsetX=x;
    }

    /**
     * 分发触摸事件。
     * 对触摸事件的坐标进行偏移修正。
     *
     * @param ev 触摸事件。
     * @return true 表示事件已处理。
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        ev.offsetLocation(mOffsetX,0);
        return super.dispatchTouchEvent(ev);
    }
}
