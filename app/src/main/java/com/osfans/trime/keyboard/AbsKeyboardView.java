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

import com.osfans.trime.Config;
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
    /** 行高(dp) */
    private final double mRowHeightDp;
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
    /** 是否为 dp 高度模式 */
    private boolean mIsDpMode;

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
        mRowHeightDp = globals.get("key_height").optdouble(50);
        mKeyWidth = globals.get("key_width").optdouble(20);
        loadRows();
        Log.w("RowKeyboardView", "init time: "+(System.currentTimeMillis()-time) );
    }

    /**
     * 加载键盘行配置。
     * 从 Lua 表中读取按键列表,计算总高度后逐个创建按键视图。
     * 支持 dp/百分比双模式。
     */
    private void loadRows() {
        TrimeService mTrime= TrimeService.getInstance();
        mWidth = mTrime.getWidth();

        mLayout = globals.get("layout").opttable(null);
        mKeys = globals.get("keys").checktable();
        int len = mKeys.length();

        mIsDpMode = ThemeManager.keyRowHeight(globals) > 0;

        if (mIsDpMode) {
            // === dp 模式: y、height 为 dp 值 ===
            double totalDp = 0;
            for (int i = 0; i < len; i++) {
                LuaTable key = mKeys.get(i + 1).checktable();
                double y = key.get("y").optdouble(0);
                double h = key.get("height").optdouble(mRowHeightDp);
                totalDp = Math.max(totalDp, y + h);
            }
            mHeight = ThemeManager.dp2px((float) (totalDp * Config.getKeyboardHeightScale()));
            setComputedKeyboardHeight((int) mHeight);
            ThemeManager.setComputedKeyboardHeight((int) mHeight);

            for (int i = 0; i < len; i++) {
                loadKey(mKeys.get(i + 1).checktable());
            }
        } else {
            // === 百分比模式(旧行为) ===
            setComputedKeyboardHeight(0);
            ThemeManager.setComputedKeyboardHeight(0);
            mHeight = ThemeManager.getKeyboardHeight();
            for (int i = 0; i < len; i++) {
                loadKey(mKeys.get(i + 1).checktable());
            }
        }
    }

    /**
     * 加载单个按键。
     * dp 模式下 y、height 为 dp 值(width、x 保持百分比)。
     * 百分比模式下 y、height 为百分比(旧行为)。
     *
     * @param key Lua 表,包含按键的配置信息(width、height、x、y)。
     */
    private void loadKey(LuaTable key) {
        int width = (int) (mWidth * key.get("width").optdouble(mKeyWidth) / 100);
        int x = (int) (mWidth * key.get("x").optdouble(0) / 100);
        int height, y;
        if (mIsDpMode) {
            height = ThemeManager.dp2px((float) key.get("height").optdouble(mRowHeightDp));
            y = ThemeManager.dp2px((float) key.get("y").optdouble(0));
        } else {
            height = (int) (mHeight * key.get("height").optdouble(mRowHeightDp) / 100);
            y = (int) (mHeight * key.get("y").optdouble(0) / 100);
        }
        LuaTable styleDefaults = ThemeManager.resolveKeyStyleDefaults(key, null, globals);
        key.set("__style", styleDefaults);
        KeyView keyView = new KeyView(getContext(), new Key(key));
        keyView.setShapeDetectionEnabled(true);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(width, height, Gravity.TOP | Gravity.LEFT);
        layoutParams.leftMargin = x;
        layoutParams.topMargin = y;
        addView(keyView, layoutParams);
    }
}
