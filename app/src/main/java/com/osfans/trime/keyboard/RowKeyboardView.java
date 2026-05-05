/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.keyboard;

import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.osfans.trime.Key;
import com.osfans.trime.TrimeService;
import com.osfans.trime.theme.ThemeManager;

import org.luaj.Globals;
import org.luaj.LuaTable;

/**
 * 行式键盘视图类。
 * 从 Lua 配置中按行加载按键,支持自定义每行的高度和按键宽度。
 */
public class RowKeyboardView extends KeyboardView implements View.OnClickListener, View.OnLongClickListener {

    // ==================== 成员变量 ====================
    /** Lua 全局环境 */
    private final Globals globals;
    /** 键盘高度(像素) */
    private double mHeight;
    /** 键盘宽度(像素) */
    private double mWidth;
    /** 行列表(Lua 表) */
    private LuaTable mRows;
    /** 行高百分比 */
    private double mRowHeight;
    /** 顶部偏移量(像素) */
    private double mTop;
    /** 键宽百分比 */
    private double mKeyWidth;
    /** 布局配置(Lua 表) */
    private LuaTable mLayout;
    /** 左侧偏移量(像素) */
    private double mLeft;

    /**
     * 构造函数。
     *
     * @param context 上下文。
     * @param globals Lua 全局环境,包含键盘配置。
     */
    public RowKeyboardView(@NonNull Context context, Globals globals) {
        super(context, globals);
        this.globals = globals;
        String style = globals.get("style").optjstring("keyboard");
        long time=System.currentTimeMillis();
        setBackground(ThemeManager.getStyle().getStyle(style).getBackground(0xffdddddd));
        loadRows();
        Log.w("RowKeyboardView", "init time: "+(System.currentTimeMillis()-time) );
    }

    /**
     * 加载键盘行配置。
     * 从 Lua 表中读取行列表,并逐行创建按键。
     */
    private void loadRows() {
        TrimeService mTrime= TrimeService.getInstance();
        mHeight = ThemeManager.getKeyboardHeight();
        mWidth = mTrime.getWidth();

        mLayout = globals.get("layout").opttable(null);
        mRows = globals.get("rows").checktable();
        int len = mRows.length();
        // 计算默认行高(100% / 行数)
        mRowHeight = 100.0 / len;
        mRowHeight = globals.get("key_height").optdouble(mRowHeight);
        // 计算默认键宽(100% / 第一行的按键数)
        mKeyWidth = globals.get("key_width").optdouble(100.0/mRows.get(1).get("keys").checktable().length());
        mTop=0;
        for (int i = 0; i < len; i++) {
            loadRow(mRows.get(i + 1).checktable());
        }
    }

    /**
     * 加载单行按键。
     * 遍历行中的所有按键,并逐个创建。
     *
     * @param row Lua 表,包含行的配置信息(height、width、keys)。
     */
    private void loadRow(LuaTable row) {
        double height = row.get("height").optdouble(mRowHeight);
        double width = row.get("width").optdouble(mKeyWidth);
        LuaTable keys = row.get("keys").checktable();
        int len = keys.length();
        mLeft=0;
        for (int i = 0; i < len; i++) {
            loadKey(keys.get(i+1).checktable(),width,height);
        }
        // 累加顶部偏移量,为下一行做准备
        mTop+=mHeight*height/100;
    }

    /**
     * 加载单个按键。
     * 根据 Lua 配置创建 KeyView,并设置位置和大小。
     *
     * @param key Lua 表,包含按键的配置信息(width、height)。
     * @param width 默认宽度百分比。
     * @param height 默认高度百分比。
     */
    private void loadKey(LuaTable key, double width, double height) {
        //LuaTable layout=key.get("layout").opttable(mLayout);
        // 计算按键宽度(百分比转换为像素)
        width = (mWidth*key.get("width").optdouble(width)/100);
        // 计算按键高度(百分比转换为像素)
        height = (mHeight*key.get("height").optdouble(height)/100);
        KeyView keyView = new KeyView(getContext(),new Key(key));

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams((int) width, (int) height, Gravity.TOP| Gravity.LEFT);
        layoutParams.leftMargin= (int) mLeft;
        layoutParams.topMargin= (int) mTop;
           mLeft+=width;
        addView(keyView,layoutParams);
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }
}
