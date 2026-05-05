/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.IBinder;
import android.view.Window;
import android.view.WindowManager;

import com.osfans.trime.Config;
import com.osfans.trime.TrimeService;
import com.osfans.trime.core.Rime;
import com.osfans.trime.theme.ThemeManager;

import org.luaj.Globals;
import org.luaj.LuaTable;
import org.luaj.LuaValue;
import org.luaj.lib.jse.JsePlatform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 主题选择对话框。
 * 用于切换输入法主题,支持从 Lua 配置中读取主题显示名称,并按名称排序。
 */
public class ThemeDialog {
    // ==================== 成员变量 ====================
    /** 对话框实例 */
    private AlertDialog mDialog;
    /** 是否需要更新 Rime 选项 */
    private boolean mNeedUpdateRimeOption;
    /** 窗口 Token(用于依附于输入法窗口) */
    private IBinder mWindowToken;

    /**
     * 内部类：用于绑定主题 ID 和 Lua 中定义的显示名称。
     */
    private static class ThemeItem {
        /** 主题 ID(文件夹名) */
        String id;
        /** 显示名称(从 main.lua 读取) */
        String displayName;

        ThemeItem(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
    }

    /**
     * 构造函数。
     * 加载所有可用主题,从 Lua 配置中读取显示名称,按名称排序后显示单选对话框。
     *
     * @param context 上下文。
     */
    public ThemeDialog(Context context) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(context, ThemeManager.getDialogTheme())
                        .setTitle("选择主题")
                        .setPositiveButton(
                                "样式",
                                (dialog, id) -> {
                                    dialog.dismiss();
                                    new StyleDialog(context).show(mWindowToken);
                                })
                        .setNegativeButton(android.R.string.cancel, null);

        if (Rime.getCurrentRimeSchema().equals(".default")) {
            builder.setMessage("请先正确配置");
        } else {
            // 1. 获取当前选中的主题 ID 和所有文件夹名
            String currentThemeId = Config.getTheme();
            String[] rawThemeIds = Config.getThemes();

            // 2. 初始化 Lua 环境
            Globals luaGlobals = JsePlatform.standardGlobals();
            luaGlobals.finder = ThemeManager.getFinder();

            // 3. 遍历并解析每个主题的显示名称
            List<ThemeItem> themeItems = new ArrayList<>();
            for (String id : rawThemeIds) {
                String displayName = getThemeNameFromLua(luaGlobals, id);
                themeItems.add(new ThemeItem(id, displayName));
            }

            // 4. 【核心排序】按解析后的显示名称进行本地化排序
            Collections.sort(themeItems, new Comparator<ThemeItem>() {
                private final OptionsDialog.LocaleComparator localeComp = new OptionsDialog.LocaleComparator();
                @Override
                public int compare(ThemeItem t1, ThemeItem t2) {
                    // 1. 首先比较显示名称
                    int nameComparison = localeComp.compare(t1.displayName, t2.displayName);
                    // 2. 如果显示名称相同，则按 ID 字符串排序
                    if (nameComparison == 0) {
                        // ID 通常是英文/数字文件夹名，直接用 String 的 compareTo 即可
                        return localeComp.compare(t1.id, t2.id);
                    }

                    return nameComparison;
                }
            });

            // 5. 准备 UI 数组
            int count = themeItems.size();
            String[] displayNames = new String[count];
            String[] sortedIds = new String[count];
            int currentSelectedIndex = 0;

            for (int i = 0; i < count; i++) {
                ThemeItem item = themeItems.get(i);
                displayNames[i] = item.displayName;
                sortedIds[i] = item.id;
                // 重新定位当前选中的索引
                if (item.id.equals(currentThemeId)) {
                    currentSelectedIndex = i;
                }
            }

            builder.setSingleChoiceItems(
                    displayNames,
                    currentSelectedIndex,
                    (dialog, index) -> {
                        dialog.dismiss();
                        String selectedThemeId = sortedIds[index];
                        Config.setTheme(selectedThemeId);
                        mNeedUpdateRimeOption = true;
                        TrimeService trimeService = TrimeService.getInstance();
                        if (trimeService != null) {
                            trimeService.setTheme(selectedThemeId);
                        }
                    });
        }
        mDialog = builder.create();
    }

    /**
     * 从主题目录下的 main.lua 文件中读取 name 变量。
     * 如果 Lua 中定义了 name,则返回 "显示名称 (ID)" 格式,否则返回 ID。
     *
     * @param globals Lua 全局环境。
     * @param themeId 主题 ID(文件夹名)。
     * @return 主题的显示名称。
     */
    private String getThemeNameFromLua(Globals globals, String themeId) {
        LuaTable env = new LuaTable();
        LuaTable mt = new LuaTable();
        mt.set("__index", globals);
        env.setmetatable(mt);

        try {
            String scriptPath = Config.getThemePath(themeId, "main.lua");
            LuaValue chunk = globals.loadfilex(scriptPath, env);
            if (chunk.isfunction()) {
                chunk.call();
                LuaValue luaName = env.get("name");
                if (luaName.isstring()) {
                    // 返回 Lua 定义的名称
                    return luaName.tojstring()+ " (" + themeId + ")";
                }
            }
        } catch (Exception e) {
            return themeId + " ("+e+")";
        }
        return themeId; // 默认返回文件夹 ID
    }

    /**
     * 显示对话框(无 Token)。
     */
    public void show() {
        if (mDialog == null) return;
        mDialog.show();
    }

    /**
     * 显示对话框(带 Token,依附于输入法窗口)。
     * 设置对话框类型为 TYPE_APPLICATION_ATTACHED_DIALOG,使其能依附于输入法窗口显示。
     *
     * @param token 窗口 Token。
     */
    public void show(IBinder token) {
        if (mDialog == null) return;
        mWindowToken = token;

        if (mWindowToken == null) {
            show();
            return;
        }

        Window window = mDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            // 确保对话框在输入法窗口层级正确显示
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
            window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            layoutParams.token = token;
            window.setAttributes(layoutParams);
        }
        mDialog.show();
    }
}
