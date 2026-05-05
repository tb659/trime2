/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowManager;

import com.osfans.trime.BuildConfig;
import com.osfans.trime.Config;
import com.osfans.trime.TrimeService;
import com.osfans.trime.core.Rime;
import com.osfans.trime.theme.ThemeManager;

import org.luaj.Globals;
import org.luaj.LuaTable;
import org.luaj.LuaValue;
import org.luaj.lib.ResourceFinder;
import org.luaj.lib.jse.JsePlatform;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 键盘选择对话框。
 * 用于切换默认键盘布局,实现 ResourceFinder 接口以支持 Lua 脚本加载键盘配置文件。
 */
public class KeyboardDialog implements ResourceFinder {
    // ==================== 成员变量 ====================
    /** 对话框实例 */
    private final AlertDialog mDialog;
    /** 窗口 Token(用于依附于输入法窗口) */
    private IBinder mWindowToken;

    /**
     * 查找资源文件(实现 ResourceFinder 接口)。
     * 按优先级查找:绝对路径 -> 键盘目录。
     *
     * @param name 资源文件名。
     * @return 输入流,未找到则返回 null。
     */
    @Override
    public InputStream findResource(String name) {
        if (TextUtils.isEmpty(name))
            return null;
        try {
            if (new File(name).exists())
                return new FileInputStream(name);
        } catch (Exception e) {
            if (BuildConfig.DEBUG)
                e.printStackTrace();
        }
        try {
            return new FileInputStream(new File(Config.getKeyboardDir(), name));
        } catch (Exception e) {
            if (BuildConfig.DEBUG)
                e.printStackTrace();
        }
        return null;
    }

    /**
     * 查找文件路径(实现 ResourceFinder 接口)。
     * 如果是绝对路径则直接返回,否则返回键盘目录下的完整路径。
     *
     * @param filename 文件名。
     * @return 文件的绝对路径。
     */
    @Override
    public String findFile(String filename) {
        if (TextUtils.isEmpty(filename))
            return null;
        if (filename.startsWith("/"))
            return filename;
        return new File(Config.getKeyboardDir(), filename).getAbsolutePath();
    }

    /**
     * 内部辅助类：用于绑定键盘 ID 和解析出的显示名称。
     */
    private static class KeyboardItem {
        /** 键盘 ID(文件名,不含 .lua) */
        String id;
        /** 显示名称(从 Lua 配置读取) */
        String displayName;

        KeyboardItem(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
    }

    /**
     * 构造函数。
     * 加载所有可用键盘布局,从 Lua 配置中读取显示名称,按名称排序后显示单选对话框。
     *
     * @param context 上下文。
     */
    public KeyboardDialog(Context context) {
        AlertDialog.Builder builder =
                new AlertDialog.Builder(context, ThemeManager.getDialogTheme())
                        .setTitle("选择默认键盘")
                        //setPositiveButton(
                        //       "主题",
                        //       (dialog, id) -> new ThemeDialog(context).show(mWindowToken))
                        .setNegativeButton(android.R.string.cancel, null);

        if (Rime.getCurrentRimeSchema().equals(".default")) {
            builder.setMessage("请先正确配置");
        } else {
            // 1. 获取当前选中的 ID 和所有样式文件夹名
            String currentKeyboardId = Config.getKeyboard();
            String[] rawKeyboardIds = Config.getKeyboards();

            // 2. 准备 Lua 环境
            Globals luaGlobals = JsePlatform.standardGlobals();
            luaGlobals.finder = this;

            // 3. 遍历并解析每个样式的显示名称
            List<KeyboardItem> KeyboardItems = new ArrayList<>();
            for (String id : rawKeyboardIds) {
                id=id.replace(".lua","");
                String displayName = getKeyboardNameFromLua(luaGlobals, id);
                if(TextUtils.isEmpty(displayName))
                    continue;
                KeyboardItems.add(new KeyboardItem(id, displayName));
            }

            // 4. 【核心】按显示名称进行本地化排序
            Collections.sort(KeyboardItems, new Comparator<KeyboardItem>() {
                private final OptionsDialog.LocaleComparator localeComp = new OptionsDialog.LocaleComparator();

                @Override
                public int compare(KeyboardItem s1, KeyboardItem s2) {
                    // 1. 首先比较显示名称
                    int nameComparison = localeComp.compare(s1.displayName, s2.displayName);
                    // 2. 如果显示名称相同，则按 ID 字符串排序
                    if (nameComparison == 0) {
                        // ID 通常是英文/数字文件夹名，直接用 String 的 compareTo 即可
                        return localeComp.compare(s1.id, s2.id);
                    }

                    return nameComparison;
                }
            });
            KeyboardItems.add(0,new KeyboardItem("","自动匹配"));
            // 5. 拆分排序后的数据供 Dialog 使用
            int size = KeyboardItems.size();
            String[] sortedNames = new String[size];
            String[] sortedIds = new String[size];
            int currentSelectedIndex = 0;

            for (int i = 0; i < size; i++) {
                KeyboardItem item = KeyboardItems.get(i);
                sortedNames[i] = item.displayName;
                sortedIds[i] = item.id;
                // 重新匹配当前选中的索引位置
                if (item.id.equals(currentKeyboardId)) {
                    currentSelectedIndex = i;
                }
            }

            builder.setSingleChoiceItems(
                    sortedNames,
                    currentSelectedIndex,
                    (dialog, index) -> {
                        dialog.dismiss();
                        String selectedId = sortedIds[index];
                        Config.setKeyboard(selectedId);
                        TrimeService trime = TrimeService.getInstance();
                        if (trime != null) {
                            trime.setKeyboard(selectedId);
                        }
                    });
        }
        mDialog = builder.create();
    }

    /**
     * 从键盘 Lua 配置文件中提取 name 变量。
     * 检查 lock 字段,如果未锁定则返回 null(不显示)。
     *
     * @param globals Lua 全局环境。
     * @param keyboardId 键盘 ID(文件名,不含 .lua)。
     * @return 键盘的显示名称,未锁定或出错则返回 null/ID。
     */
    private String getKeyboardNameFromLua(Globals globals, String keyboardId) {
        LuaTable env = new LuaTable();
        LuaTable mt = new LuaTable();
        mt.set("__index", globals);
        env.setmetatable(mt);

        try {
            String path = Config.getKeyboardPath(keyboardId);
            LuaValue chunk = globals.loadfilex(path, env);
            if (chunk.isfunction()) {
                chunk.call();
                LuaValue nameValue = env.get("name");
                if(!env.get("lock").toboolean())
                    return null;
                if (nameValue.isstring()) {
                    return nameValue.tojstring()+ " (" + keyboardId + ")"; // 仅返回名称，ID 在 UI 组装处处理
                }
            }
        } catch (Exception e) {
            return keyboardId + " (" + e + ")";
        }
        return keyboardId;
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
        Window win = mDialog.getWindow();
        if (win != null) {
            WindowManager.LayoutParams attr = win.getAttributes();
            attr.type = WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG;
            win.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            attr.token = token;
            win.setAttributes(attr);
        }
        mDialog.show();
    }
}

