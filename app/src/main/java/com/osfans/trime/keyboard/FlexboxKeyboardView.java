/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.keyboard;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayout;
import com.google.android.flexbox.JustifyContent;
import com.osfans.trime.Key;
import com.osfans.trime.theme.KeyStyle;
import com.osfans.trime.theme.Style;
import com.osfans.trime.theme.ThemeManager;

import org.luaj.Globals;
import org.luaj.LuaTable;
import org.luaj.LuaValue;

/**
 * Flexbox 键盘视图类。
 * 使用 FlexboxLayout 实现灵活的键盘布局,支持从 Lua 配置中递归解析嵌套容器和按键。
 */
public class FlexboxKeyboardView extends KeyboardView {

    // ==================== 成员变量 ====================
    /** Lua 全局环境 */
    private final Globals globals;

    /**
     * 构造函数。
     *
     * @param context 上下文。
     * @param globals Lua 全局环境,包含键盘配置(style)。
     */
    public FlexboxKeyboardView(@NonNull Context context, Globals globals) {
        super(context, globals);
        this.globals = globals;
        String style = globals.get("style").optjstring("keyboard");
        long time = System.currentTimeMillis();
        setBackground(ThemeManager.getStyle().getStyle(style).getBackground(0xffdddddd));
        loadRows();
        Log.w("FlexboxKeyboardView", "init time: " + (System.currentTimeMillis() - time));
    }

    /**
     * 加载键盘行配置。
     * 从 Lua 配置中读取 flex_box,创建根容器并递归解析所有子元素。
     */
    private void loadRows() {
        LuaValue flexBoxConfig = globals.get("flex_box");
        if (!flexBoxConfig.istable()) return;

        FlexboxLayout rootLayout = createFlexContainer(flexBoxConfig.checktable());
        // 根容器必须填满父类 KeyboardView
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        parseRecursive(rootLayout, flexBoxConfig.checktable());

        addView(rootLayout);
    }

    /**
     * 递归解析 Lua 结构。
     * 遍历 Lua 表,识别嵌套容器和按键,并创建对应的 FlexboxLayout 或 KeyView。
     *
     * @param parent 父容器。
     * @param table Lua 配置表。
     */
    private void parseRecursive(FlexboxLayout parent, LuaTable table) {
        int len = table.length();
        int parentDirection = parent.getFlexDirection();

        for (int i = 1; i <= len; i++) {
            LuaValue item = table.get(i);
            if (!item.istable()) continue;

            // 判断是嵌套容器还是具体的 Key
            if (item.get("keys").istable() || item.length() > 0) {
                // 这是一个子容器（行或列）
                FlexboxLayout childLayout = createFlexContainer(item.checktable());
                FlexboxLayout.LayoutParams lp = createLayoutParams(parentDirection, item);

                parent.addView(childLayout, lp);

                // 递归处理子容器内部
                parseRecursive(childLayout, item.checktable());

                // 处理该层级下直接定义的 keys
                LuaValue keys = item.get("keys");
                if (keys.istable()) {
                    parseKeys(childLayout, keys.checktable());
                }
            }
        }
    }

    /**
     * 解析按键列表。
     * 遍历 Lua 表中的按键配置,创建 KeyView 并添加到父容器中。
     *
     * @param parent 父容器。
     * @param keys Lua 按键配置表。
     */
    private void parseKeys(FlexboxLayout parent, LuaTable keys) {
        int len = keys.length();
        int direction = parent.getFlexDirection();
        for (int i = 1; i <= len; i++) {
            LuaValue keyConfig = keys.get(i);
            if (keyConfig.istable()) {
                LuaTable keyTable = keyConfig.checktable();
                // 解析主样式的默认值
                LuaTable styleDefaults = ThemeManager.resolveKeyStyleDefaults(keyTable, null, globals);
                keyTable.set("__style", styleDefaults);
                
                // 为子样式（hint、long_click、pressed、preview、popup）解析默认值
                String[] subStyleNames = {"hint", "long_click", "pressed", "preview", "popup"};
                for (String subStyleName : subStyleNames) {
                    LuaTable resolvedSubStyle = ThemeManager.resolveSubKeyStyleDefaults(keyTable, null, globals, subStyleName);
                    if (resolvedSubStyle != null) {
                        // 将解析后的子样式设置回原 key 表的对应字段
                        keyTable.set(subStyleName, resolvedSubStyle);
                    }
                }
            }
            KeyView keyView = new KeyView(getContext(), new Key(keyConfig));
            parent.addView(keyView, createLayoutParams(direction, keyConfig));
        }
    }

    /**
     * 根据 Lua 配置创建 FlexboxLayout 容器。
     * 解析方向、背景、阴影等样式属性。
     *
     * @param config Lua 配置表。
     * @return 创建的 FlexboxLayout 实例。
     */
    private FlexboxLayout createFlexContainer(LuaTable config) {
        FlexboxLayout layout = new FlexboxLayout(getContext());
        layout.setClipChildren(false);
        layout.setClipToPadding(false);
        LuaValue s = config.get("style");
        if(s.isstring()) {
            KeyStyle style = ThemeManager.getStyle().getKeyStyle(s.tojstring());
            layout.setBackground(style.getBackground(0));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                int dShadowColor = style.getShadowColor();
                if (dShadowColor != 0) {
                    layout.setOutlineAmbientShadowColor(dShadowColor);
                    layout.setOutlineSpotShadowColor(dShadowColor);
                }
            }
            layout.setElevation(style.getElevation());
        }

        // 解析方向
        String dir = config.get("direction").optjstring("row");
        layout.setFlexDirection(dir.equals("column") ? FlexDirection.COLUMN : FlexDirection.ROW);

        // 默认配置
        layout.setJustifyContent(JustifyContent.FLEX_START);
        layout.setAlignItems(AlignItems.STRETCH);
        layout.setFlexWrap(FlexWrap.NOWRAP);
        return layout;
    }

    /**
     * 核心修复：根据父容器方向生成 LayoutParams。
     * 横向布局时,未设固定宽度的项目靠权重分配空间;纵向布局时同理。
     *
     * @param parentDirection 父容器的方向(ROW 或 COLUMN)。
     * @param config Lua 配置表,包含 width、height、grow 等属性。
     * @return 创建的 LayoutParams 实例。
     */
    private FlexboxLayout.LayoutParams createLayoutParams(int parentDirection, LuaValue config) {
        // 获取 Lua 中定义的宽和高（假设单位是 dp，实际使用建议转换成 px）
        int fixWidth = config.get("width").optint(-1);
        int fixHeight = config.get("height").optint(-1);
        float grow = (float) config.get("grow").optdouble(1.0f);

        int width, height;

        if (parentDirection == FlexDirection.ROW) {
            // 横向布局时：如果没设固定宽度，则宽度为0靠权重；高度默认填满
            width = (fixWidth > 0) ? dp2px(fixWidth) : 0;
            height = (fixHeight > 0) ? dp2px(fixHeight) : ViewGroup.LayoutParams.MATCH_PARENT;
            if (fixWidth > 0) grow = 0; // 如果固定了宽度，通常就不再伸展
        } else {
            // 纵向布局时：宽度默认填满；如果没设固定高度，则高度为0靠权重
            width = (fixWidth > 0) ? dp2px(fixWidth) : ViewGroup.LayoutParams.MATCH_PARENT;
            height = (fixHeight > 0) ? dp2px(fixHeight) : 0;
            if (fixHeight > 0) grow = 0; // 如果固定了高度，就不再伸展
        }

        FlexboxLayout.LayoutParams lp = new FlexboxLayout.LayoutParams(width, height);
        lp.setFlexGrow(grow);
        lp.setFlexShrink((fixWidth > 0 || fixHeight > 0) ? 0.0f : 1.0f); // 固定尺寸的项目不收缩
        return lp;
    }

    /**
     * 辅助函数：DP 转 PX。
     *
     * @param dp DP 值。
     * @return 对应的像素值。
     */
    private int dp2px(int dp) {
        return (int) (dp * getContext().getResources().getDisplayMetrics().density);
    }
}
