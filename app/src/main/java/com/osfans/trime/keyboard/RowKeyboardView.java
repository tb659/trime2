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

import com.osfans.trime.Config;
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

    private static final String TAG = "RowKeyboardView";

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
    /** 是否为 dp 高度模式 */
    private boolean mIsDpMode;
    /** 当前正在加载的行（用于 resolveKeyStyleDefaults） */
    private LuaTable mCurrentRow;

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
     * 支持 dp 和百分比双模式:
     * - dp 模式(height 存在时): 行高取 row.height → height → 50dp,按键 height 为 dp 单独覆盖
     * - 百分比模式(height 不存在时): 回落旧行为,使用主题 keyboard.height 等分
     */
    private void loadRows() {
        TrimeService mTrime= TrimeService.getInstance();
        mWidth = mTrime.getWidth();

        mLayout = globals.get("layout").opttable(null);
        mRows = globals.get("rows").checktable();
        int len = mRows.length();

        mIsDpMode = ThemeManager.keyRowHeight(globals) > 0;

        if (mIsDpMode) {
            // === dp 模式: 行高 = row.height → height → 主题 key.height ===
            double keyHeightDp = ThemeManager.keyRowHeight(globals);
            Log.d(TAG, "keyHeightDp:" + keyHeightDp);
            double[] rowDp = new double[len];
            double totalDp = 0;
            for (int i = 0; i < len; i++) {
                LuaTable row = mRows.get(i + 1).checktable();
                Log.d(TAG, "row.get(height)" + row.get("height").optdouble(0));
                double h = row.get("height").optdouble(0);
                if (h <= 0) h = globals.get("height").optdouble(0);
                if (h <= 0) h = keyHeightDp;
                rowDp[i] = h;
                totalDp += h;
            }
            mHeight = ThemeManager.dp2px((float) (totalDp * Config.getKeyboardHeightScale()));
            setComputedKeyboardHeight((int) mHeight);
            ThemeManager.setComputedKeyboardHeight((int) mHeight);

            mKeyWidth = globals.get("key_width").optdouble(100.0/mRows.get(1).get("keys").checktable().length());
            mTop = 0;
            for (int i = 0; i < len; i++) {
                mRowHeight = rowDp[i] / totalDp * 100;
                loadRow(mRows.get(i + 1).checktable());
            }
        } else {
            // === 百分比模式(旧行为) ===
            setComputedKeyboardHeight(0);
            ThemeManager.setComputedKeyboardHeight(0);
            mHeight = ThemeManager.getKeyboardHeight();
            mRowHeight = 100.0 / len;
            mKeyWidth = globals.get("key_width").optdouble(100.0/mRows.get(1).get("keys").checktable().length());
            mTop = 0;
            for (int i = 0; i < len; i++) {
                loadRow(mRows.get(i + 1).checktable());
            }
        }
    }

    /**
     * 加载单行按键。
     * 遍历行中的所有按键,并逐个创建。
     *
     * @param row Lua 表,包含行的配置信息(width、keys)。
     */
    private void loadRow(LuaTable row) {
        double width = row.get("width").optdouble(mKeyWidth);
        LuaTable keys = row.get("keys").checktable();
        int len = keys.length();
        mLeft=0;
        mCurrentRow = row;
        for (int i = 0; i < len; i++) {
            loadKey(keys.get(i+1).checktable(), width);
        }
        mTop += mHeight * mRowHeight / 100;
    }

    /**
     * 加载单个按键。
     * dp 模式下按键 height 为 dp 值,单独覆盖该按键高度;无 height 时填充行高。
     * 百分比模式下 height 为百分比(旧行为)。
     *
     * @param key Lua 表,包含按键的配置信息(width、height)。
     * @param width 默认宽度百分比。
     */
    private void loadKey(LuaTable key, double width) {
        width = (mWidth * key.get("width").optdouble(width) / 100);
        double height;
        double rowHeightPx = mHeight * mRowHeight / 100;
        if (mIsDpMode) {
            double h = key.get("height").optdouble(0);
            height = h > 0 ? ThemeManager.dp2px((float) h) : rowHeightPx;
        } else {
            height = mHeight * key.get("height").optdouble(mRowHeight) / 100;
        }
        // 解析主样式的默认值
        LuaTable styleDefaults = ThemeManager.resolveKeyStyleDefaults(key, mCurrentRow, globals);
        key.set("__style", styleDefaults);
        
        // 为子样式（hint、long_click、pressed、preview、popup）解析默认值
        String[] subStyleNames = {"hint", "long_click", "pressed", "preview", "popup"};
        for (String subStyleName : subStyleNames) {
            LuaTable resolvedSubStyle = ThemeManager.resolveSubKeyStyleDefaults(key, mCurrentRow, globals, subStyleName);
            if (resolvedSubStyle != null) {
                // 将解析后的子样式设置回原 key 表的对应字段
                key.set(subStyleName, resolvedSubStyle);
            }
        }
        
        KeyView keyView = new KeyView(getContext(), new Key(key));
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams((int) width, (int) height, Gravity.TOP| Gravity.LEFT);
        layoutParams.leftMargin = (int) mLeft;
        layoutParams.topMargin = (int) mTop;
        mLeft += width;
        addView(keyView, layoutParams);
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    public boolean onLongClick(View v) {
        return false;
    }
}
