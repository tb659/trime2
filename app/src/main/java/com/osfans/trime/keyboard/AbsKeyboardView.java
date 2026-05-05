/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.keyboard;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.osfans.trime.Key;
import com.osfans.trime.TrimeService;
import com.osfans.trime.theme.ThemeManager;

import org.luaj.Globals;
import org.luaj.LuaTable;

/**
 * 抽象键盘视图基类。
 * 从 Lua 配置中加载按键布局,支持自定义按键位置和大小。
 */
public class AbsKeyboardView extends KeyboardView{

    // ==================== 成员变量 ====================
    /** 行高百分比 */
    private final double mRowHeight;
    /** 键宽百分比 */
    private final double mKeyWidth;
    /** 键盘高度(像素) */
    private double mHeight;
    /** 键盘宽度(像素) */
    private double mWidth;
    /** 按键列表(Lua 表) */
    private LuaTable mKeys;
    /** 布局配置(Lua 表) */
    private LuaTable mLayout;

    /**
     * 构造函数。
     *
     * @param context 上下文。
     * @param globals Lua 全局环境,包含键盘配置。
     */
    public AbsKeyboardView(@NonNull Context context, Globals globals) {
        super(context,globals);
        this.globals = globals;
        String style = globals.get("style").optjstring("keyboard");
        long time=System.currentTimeMillis();
        setBackground(ThemeManager.getStyle().getStyle(style).getBackground(0xffdddddd));
        mRowHeight = globals.get("key_height").optdouble(10);
        mKeyWidth = globals.get("key_width").optdouble(20);
        loadRows();
        Log.w("RowKeyboardView", "init time: "+(System.currentTimeMillis()-time) );
    }

    /**
     * 加载键盘行配置。
     * 从 Lua 表中读取按键列表,并逐个创建按键视图。
     */
    private void loadRows() {
        TrimeService mTrime= TrimeService.getInstance();
        mHeight = ThemeManager.getKeyboardHeight();
        mWidth = mTrime.getWidth();

        mLayout = globals.get("layout").opttable(null);
        mKeys = globals.get("keys").checktable();
        int len = mKeys.length();
        for (int i = 0; i < len; i++) {
            loadKey(mKeys.get(i + 1).checktable());
        }
    }

    /**
     * 加载单个按键。
     * 根据 Lua 配置创建 KeyView,并设置位置和大小。
     *
     * @param key Lua 表,包含按键的配置信息(width、height、x、y)。
     */
    private void loadKey(LuaTable key) {
        //LuaTable layout=key.get("layout").opttable(mLayout);
        // 计算按键宽度(百分比转换为像素)
        int width = (int) (mWidth*key.get("width").optdouble(mKeyWidth)/100);
        // 计算按键高度(百分比转换为像素)
        int height = (int) (mHeight*key.get("height").optdouble(mRowHeight)/100);
        // 计算 X 坐标(百分比转换为像素)
        int x = (int) (mWidth*key.get("x").optdouble(0)/100);
        // 计算 Y 坐标(百分比转换为像素)
        int y = (int) (mHeight*key.get("y").optdouble(0)/100);
        KeyView keyView = new KeyView(getContext(),new Key(key));
        keyView.setShapeDetectionEnabled(true);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(width, height, Gravity.TOP| Gravity.LEFT);
        layoutParams.leftMargin=x;
        layoutParams.topMargin=y;
        addView(keyView,layoutParams);
    }
}
