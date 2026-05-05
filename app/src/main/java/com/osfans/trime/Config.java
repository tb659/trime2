/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime; // 定义包名为com.osfans.trime

// 导入静态常量：屏幕方向（横屏）、夜间模式掩码、夜间模式开启
import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.UI_MODE_NIGHT_MASK;
import static android.content.res.Configuration.UI_MODE_NIGHT_YES;

import android.os.Build; // 系统版本信息
import android.os.Environment; // 外部存储目录
import android.util.Log; // 日志类
import android.view.Gravity; // 重力方向（用于布局对齐）

import com.androlua.LuaApplication; // Lua应用类
import com.osfans.trime.core.Rime; // Rime输入法核心
import com.osfans.trime.theme.ThemeManager; // 主题管理器
import com.osfans.trime.util.Function; // 工具函数类

import java.io.File; // 文件类
import java.io.FilenameFilter; // 文件名过滤器接口

// 配置管理类，负责管理系统主题、样式、键盘等配置
public class Config {

    // 当前选中的方案组（schema group）
    private static String mGroup = null;
    // 当前主题名称
    private static String mTheme = null;
    // 当前样式名称
    private static String mStyle = null;
    // 当前键盘布局ID
    private static String mKeyboard;


    // 是否朗读按键标签（语音反馈功能，当前未启用）
    public static boolean isSpeakKeyLabel() {
        return false; // 默认返回false，不朗读按键标签
    }


    // 获取Rime数据目录（主目录）
    public static String getDataDir() {
        // 在外部存储的Documents目录下创建rime目录
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "rime");
        if (!dir.exists()) { // 如果目录不存在
            dir.mkdirs(); // 创建目录及其父目录
        }
        return dir.getAbsolutePath(); // 返回绝对路径
    }

    // 获取当前选中的方案组（schema group）
    public static String getGroup() {
        if (mGroup == null) // 如果未初始化
            mGroup = Function.loadString(LuaApplication.getInstance(), "select_schema_group", "default"); // 从配置中加载，默认"default"
        return mGroup; // 返回方案组名称
    }

    // 获取用户数据目录（当前方案组的数据目录）
    public static String getUserDataDir() {
        // 在数据目录下的schemas/方案组名目录
        File dir = new File(getDataDir(), "schemas/" + getGroup());
        if (!dir.exists()) { // 如果目录不存在
            dir.mkdirs(); // 创建目录及其父目录
        }
        return dir.getAbsolutePath(); // 返回绝对路径
    }

    // 获取脚本目录（全局脚本目录）
    public static String getScriptsDir() {
        // 在数据目录下的scripts目录
        File dir = new File(getDataDir(), "scripts");
        if (!dir.exists()) { // 如果目录不存在
            dir.mkdirs(); // 创建目录及其父目录
        }
        return dir.getAbsolutePath(); // 返回绝对路径
    }

    // 获取脚本文件路径（优先从主题目录查找，否则从全局脚本目录查找）
    public static String getScriptsPath(String name) {
        // 首先在主题的scripts目录下查找
        File f = new File(getThemeDir(getTheme()), "scripts/" + name);
        if (f.exists()) // 如果文件存在
            return f.getAbsolutePath(); // 返回主题目录下的路径
        // 否则返回全局脚本目录下的路径
        return new File(getScriptsDir(), name).getAbsolutePath();
    }

    // 获取所有方案组列表
    public static String[] getGroups() {
        // 在数据目录下的schemas目录
        File dir = new File(getDataDir(), "schemas");
        String[] list = dir.list(); // 列出目录下的所有子目录和文件
        if (list == null) // 如果目录为空或不存在
            list = new String[0]; // 返回空数组
        return list; // 返回方案组列表
    }

    // 设置当前方案组并保存到配置
    public static void setGroup(String s) {
        mGroup = s; // 更新内存中的方案组名称
        Function.saveString(LuaApplication.getInstance(), "select_schema_group", s); // 保存到配置
    }

    // 获取当前主题名称
    public static String getTheme() {
        if (mTheme == null) // 如果未初始化
            mTheme = Function.loadString(LuaApplication.getInstance(), "theme", "default"); // 从配置中加载，默认"default"
        return mTheme; // 返回主题名称
    }

    // 设置当前主题并保存到配置
    public static void setTheme(String s) {
        mTheme = s; // 更新内存中的主题名称
        Function.saveString(LuaApplication.getInstance(), "theme", s); // 保存到配置
    }

    // 获取主题目录（全局主题目录）
    public static String getThemeDir() {
        // 在数据目录下的themes目录
        File dir = new File(getDataDir(), "themes");
        if (!dir.exists()) { // 如果目录不存在
            dir.mkdirs(); // 创建目录及其父目录
        }
        return dir.getAbsolutePath(); // 返回绝对路径
    }

    // 获取指定主题名的目录路径
    public static String getThemeDir(String s) {
        // 在主题目录下的s子目录
        File dir = new File(getThemeDir(), s);
        return dir.getAbsolutePath(); // 返回绝对路径
    }

    // 获取当前主题下的文件路径
    public static String getThemePath(String s) {
        // 在当前主题目录下的s文件路径
        File dir = new File(getThemeDir(getTheme()), s);
        return dir.getAbsolutePath(); // 返回绝对路径
    }

    // 获取指定主题下的文件路径
    public static String getThemePath(String d, String s) {
        // 在指定主题d目录下的s文件路径
        File dir = new File(getThemeDir(d), s);
        return dir.getAbsolutePath(); // 返回绝对路径
    }

    // 获取键盘布局目录
    public static String getKeyboardDir() {
        // 在当前主题下的keyboards目录
        File dir = new File(getThemeDir(getTheme()), "keyboards");
        if (!dir.exists()) { // 如果目录不存在
            dir.mkdirs(); // 创建目录及其父目录
        }
        return dir.getAbsolutePath(); // 返回绝对路径
    }

    // 获取所有样式列表（过滤条件：包含main.lua文件）
    public static String[] getStyles() {
        File dir = new File(getStyleDir()); // 获取样式目录
        String[] list = dir.list(new FilenameFilter() { // 列出目录并过滤
            @Override
            public boolean accept(File dir, String name) {
                // 只接受包含main.lua的子目录
                return new File(dir, name + "/main.lua").exists();
            }
        });
        if (list == null) // 如果目录为空或不存在
            list = new String[0]; // 返回空数组

        return list; // 返回样式列表
    }

    // 获取指定样式键名（用于配置存储，区分日夜间模式）
    public static String getStyleKey(String s) {
        // 如果是夜间模式
        if ((LuaApplication.getInstance().getResources().getConfiguration().uiMode & UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES) {
            return s + "_style_night"; // 返回带_night后缀的键名
        } else {
            return s + "_style"; // 返回普通键名
        }
    }

    // 获取当前主题的样式键名
    public static String getStyleKey() {
        // 如果是夜间模式
        if ((LuaApplication.getInstance().getResources().getConfiguration().uiMode & UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES) {
            return getTheme() + "_style_night"; // 返回当前主题+_style_night
        } else {
            return getTheme() + "_style"; // 返回当前主题+_style
        }
    }

    // 设置当前样式（保存到配置）
    public static void setStyle(String s) {
        Function.saveString(LuaApplication.getInstance(), getStyleKey(), s); // 根据日夜间模式保存到对应键
    }

    // 获取当前样式名称
    public static String getStyle() {
        // 如果是夜间模式，优先读取夜间样式，否则读取普通样式，默认"light"
        if ((LuaApplication.getInstance().getResources().getConfiguration().uiMode & UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES) {
            return Function.loadString(LuaApplication.getInstance(), getTheme() + "_style_night", Function.loadString(LuaApplication.getInstance(), getTheme() + "_style", "light"));
        } else {
            return Function.loadString(LuaApplication.getInstance(), getTheme() + "_style", "light");
        }
    }

    // 获取样式目录（当前主题下的styles目录）
    public static String getStyleDir() {
        // 在当前主题下的styles目录
        File dir = new File(getThemeDir(getTheme()), "styles");
        if (!dir.exists()) { // 如果目录不存在
            dir.mkdirs(); // 创建目录及其父目录
        }
        return dir.getAbsolutePath(); // 返回绝对路径
    }

    // 获取指定样式名的目录路径
    public static String getStyleDir(String s) {
        // 在样式目录下的s子目录
        File dir = new File(getStyleDir(), s);
        return dir.getAbsolutePath(); // 返回绝对路径
    }

    // 获取指定主题和样式的文件路径
    public static String getStylePath(String d, String s) {
        // 在指定主题的样式目录d下的s文件路径
        File dir = new File(getStyleDir(d), s);
        return dir.getAbsolutePath(); // 返回绝对路径
    }

    // 获取当前样式下的文件路径
    public static String getStylePath(String s) {
        // 在当前样式目录下的s文件路径
        File dir = new File(getStyleDir(getStyle()), s);
        return dir.getAbsolutePath(); // 返回绝对路径
    }
    // 获取对话框主题（根据系统版本和日夜间模式）
    public static int getDialogTheme(){
        // 如果系统版本低于LOLLIPOP_MR1（5.1），使用Material对话框主题
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            return android.R.style.Theme_Material_Dialog;
        }
        // 如果是夜间模式，使用深色对话框主题
        if ((LuaApplication.getInstance().getResources().getConfiguration().uiMode & UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES) {
            return android.R.style.Theme_DeviceDefault_Dialog_Alert;
        } else {
            // 否则使用浅色对话框主题
            return android.R.style.Theme_DeviceDefault_Light_Dialog_Alert;
        }
    }

    // 获取所有主题列表（过滤条件：包含main.lua文件）
    public static String[] getThemes() {
        File dir = new File(getThemeDir()); // 获取主题目录
        String[] list = dir.list(new FilenameFilter() { // 列出目录并过滤
            @Override
            public boolean accept(File dir, String name) {
                // 只接受包含main.lua的子目录
                return new File(dir, name + "/main.lua").exists();
            }
        });
        if (list == null) // 如果目录为空或不存在
            list = new String[0]; // 返回空数组
        return list; // 返回主题列表
    }

    // 获取图片文件路径（在当前主题下的images目录）
    public static String getImagePath(String s) {
        return new File(getThemeDir(getTheme()), "images/" + s).getAbsolutePath(); // 返回图片绝对路径
    }

    // 查找图片文件路径（按优先级：样式目录 > 主题目录 > 数据目录）
    public static String findImagePath(String s) {
        // 首先在样式目录下查找
        File f = new File(getStylePath(s));
        if (f.exists()) // 如果文件存在
            return f.getAbsolutePath(); // 返回样式目录下的路径
        // 然后在主题目录下的images目录查找
        f = new File(getThemePath("images/" + s));
        if (f.exists()) // 如果文件存在
            return f.getAbsolutePath(); // 返回主题目录下的路径
        // 最后在数据目录下的images目录查找
        f = new File(getDataDir(), "images/" + s);
        if (f.exists()) // 如果文件存在
            return f.getAbsolutePath(); // 返回数据目录下的路径
        return ""; // 未找到返回空字符串
    }

    // 获取声音文件路径（在当前主题下的sounds目录）
    public static String getSoundPath(String s) {
        return new File(getThemeDir(getTheme()), "sounds/" + s).getAbsolutePath(); // 返回声音文件绝对路径
    }

    // 查找字体文件路径（按优先级：样式目录 > 主题目录 > 数据目录）
    public static String getFontPath(String s) {
        // 首先在样式目录下查找
        File f = new File(getStylePath(s));
        if (f.exists()) // 如果文件存在
            return f.getAbsolutePath(); // 返回样式目录下的路径
        // 然后在主题目录下的fonts目录查找
        f = new File(getThemePath("fonts/" + s));
        if (f.exists()) // 如果文件存在
            return f.getAbsolutePath(); // 返回主题目录下的路径
        // 最后在数据目录下的fonts目录查找
        f = new File(getDataDir(), "fonts/" + s);
        if (f.exists()) // 如果文件存在
            return f.getAbsolutePath(); // 返回数据目录下的路径
        return "null"; // 未找到返回"null"字符串
    }

    // 获取当前键盘布局ID
    public static String getKeyboard() {
        // 从配置中加载当前主题+当前Rime方案对应的键盘布局
        mKeyboard = Function.loadString(LuaApplication.getInstance(), getTheme() + "_" + Rime.getCurrentRimeSchema() + "_keyboard", "");
        Log.w("config", "getKeyboard: " + Rime.getCurrentRimeSchema() + ";" + mKeyboard); // 输出日志
        setKeyboard(".default", mKeyboard); // 保存默认键盘配置
        return mKeyboard; // 返回键盘布局ID
    }

    // 获取指定ID的键盘布局配置
    public static String getKeyboard(String id) {
        //if (mKeyboard == null) // 注释掉的代码
        // 从配置中加载指定主题+指定ID对应的键盘布局
        return Function.loadString(LuaApplication.getInstance(), getTheme() + "_" + id + "_keyboard", "");
    }

    // 设置当前键盘布局并保存到配置
    public static void setKeyboard(String s) {
        mKeyboard = s; // 更新内存中的键盘布局ID
        // 保存到配置：当前主题+当前Rime方案对应的键盘布局
        Function.saveString(LuaApplication.getInstance(), getTheme() + "_" + Rime.getCurrentRimeSchema() + "_keyboard", s);
    }

    // 设置指定ID的键盘布局配置
    public static void setKeyboard(String id, String s) {
        // 保存到配置：当前主题+指定ID对应的键盘布局
        Function.saveString(LuaApplication.getInstance(), getTheme() + "_" + id + "_keyboard", s);
    }

    // 获取所有键盘布局列表（过滤条件：.lua后缀）
    public static String[] getKeyboards() {
        return new File(getKeyboardDir()).list(new FilenameFilter() { // 列出键盘目录并过滤
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".lua"); // 只接受.lua文件
            }
        });
    }


    // 获取键盘布局文件路径
    public static String getKeyboardPath(String keyboardId) {
        // 在键盘目录下查找keyboardId.lua文件
        return new File(getKeyboardDir(), keyboardId + ".lua").getAbsolutePath(); // 返回绝对路径
    }

    // 获取小屏模式下的对齐方式（重力方向）
    public static int getSmallModeGravity() {
        return Function.getPref(LuaApplication.getInstance()).getInt("small_mode_gravity", Gravity.LEFT); // 从配置读取，默认左对齐
    }

    // 设置小屏模式下的对齐方式
    public static void setSmallModeGravity(int g) {
        Function.getPref(LuaApplication.getInstance()).edit().putInt("small_mode_gravity", g).commit(); // 保存到配置
    }

    // 判断是否处于小屏模式
    public static boolean isSmallMode() {
        return Rime.getRimeOption("small_mode"); // 从Rime选项读取
        //return Function.getPref(LuaApplication.getInstance()).getBoolean("small_mode", false)|| isFloatMode(); // 备选方案：从配置读取
    }

    // 设置小屏模式
    public static void setSmallMode(boolean b) {
        Function.getPref(LuaApplication.getInstance()).edit().putBoolean("small_mode", b).commit(); // 保存到配置
    }


    // 获取小屏模式下的宽度
    public static int getSmallModeWidth() {
        // 从配置读取，如果没有则使用默认宽度（屏幕最大宽度的80%）
        return Function.getPref(LuaApplication.getInstance()).getInt(getKey("small_mode_width"), Function.getPref(LuaApplication.getInstance()).getInt("small_mode_width", (int) (TrimeService.getInstance().getMaxWidth() * 0.8)));
    }

    // 设置小屏模式下的宽度
    public static void setSmallModeWidth(int w) {
        Function.getPref(LuaApplication.getInstance()).edit().putInt(getKey("small_mode_width"), w).commit(); // 保存到配置
    }

    // 设置浮动模式
    public static void setFloatMode(boolean b) {
        Function.getPref(LuaApplication.getInstance()).edit().putBoolean("float_mode", b).commit(); // 保存到配置
    }

    // 判断是否处于浮动模式
    public static boolean isFloatMode() {
        return Rime.getRimeOption("float_mode"); // 从Rime选项读取
        //return Function.getPref(LuaApplication.getInstance()).getBoolean("float_mode", false); // 备选方案：从配置读取
    }

    // 设置浮动模式的X坐标
    public static void setFloatModeX(float x) {
        Function.getPref(LuaApplication.getInstance()).edit().putFloat(getKey("float_mode_x"), x).commit(); // 保存到配置
    }

    // 设置浮动模式的Y坐标
    public static void setFloatModeY(float y) {
        Function.getPref(LuaApplication.getInstance()).edit().putFloat(getKey("float_mode_y"), y).commit(); // 保存到配置
    }

    // 获取浮动模式的X坐标
    public static float getFloatModeX() {
        // 从配置读取，优先使用带后缀的键，否则使用无后缀的键
        return Function.getPref(LuaApplication.getInstance()).getFloat(getKey("float_mode_x"),  Function.getPref(LuaApplication.getInstance()).getFloat("float_mode_x", 0));
    }

    // 获取浮动模式的Y坐标
    public static float getFloatModeY() {
        // 从配置读取，优先使用带后缀的键，否则使用无后缀的键
        return Function.getPref(LuaApplication.getInstance()).getFloat(getKey("float_mode_y"), Function.getPref(LuaApplication.getInstance()).getFloat("float_mode_y", 0));
    }

    // 设置键盘高度缩放比例（会进行范围限制）
    public static void setKeyboardHeightScale(float height) {
        height = getKeyboardHeightScale() * height; // 计算新的高度比例
        if (height < 0.5) // 最小0.5
            height = 0.5f;
        else if (height > 2) { // 最大2
            height = 2f;
        }
        Function.getPref(LuaApplication.getInstance()).edit().putFloat(getKey("keyboard_height"), height).commit(); // 保存到配置
    }

    // 获取键盘高度缩放比例
    public static float getKeyboardHeightScale() {
        int height = LuaApplication.getInstance().getResources().getDisplayMetrics().heightPixels; // 获取屏幕高度像素
        float scale = Function.getPref(LuaApplication.getInstance()).getFloat(getKey("keyboard_height"), Function.getPref(LuaApplication.getInstance()).getFloat("keyboard_height", 1f)); // 从配置读取缩放比例
        int raw=ThemeManager.getRawContentHeight(); // 获取原始内容高度
        if(raw*scale>height*0.95){ // 如果缩放后超过屏幕高度的95%
            return height*0.95f/raw; // 返回调整后的比例
        }
        return scale; // 返回原始比例
    }

    // 生成带后缀的配置键名（根据屏幕方向和模式）
    private static String getKey(String s) {
        StringBuilder buf = new StringBuilder(s); // 创建字符串构建器
        // 如果是横屏模式，添加_landscape后缀
        if (LuaApplication.getInstance().getResources().getConfiguration().orientation == ORIENTATION_LANDSCAPE)
            buf.append("_landscape");
        // 如果是浮动模式，添加_float后缀
        if (isFloatMode())
            buf.append("_float");
        else if (isSmallMode()) // 如果是小屏模式，添加_small后缀
            buf.append("_small");
        return buf.toString(); // 返回带后缀的键名
    }

    // 是否隐藏注释的标记
    private static boolean _hide_comment;
    // 设置是否隐藏注释
    public static void set_hide_comment(boolean b) {
        _hide_comment=b; // 更新内存中的标记
    }

    // 检查是否隐藏注释
    public static boolean is_hide_comment() {
        return _hide_comment; // 返回标记值
    }

    // 是否隐藏按键提示的标记
    private static boolean _hide_key_hint;
    // 设置是否隐藏按键提示
    public static void set_hide_key_hint(boolean b) {
        _hide_key_hint=b; // 更新内存中的标记
    }

    // 检查是否隐藏按键提示
    public static boolean is_hide_key_hint() {
        return _hide_key_hint; // 返回标记值
    }

} // 类结束
