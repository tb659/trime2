/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.theme;

import static android.content.res.Configuration.UI_MODE_NIGHT_MASK;
import static android.content.res.Configuration.UI_MODE_NIGHT_YES;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.inputmethod.EditorInfo;

import com.androlua.LuaApplication;
import com.androlua.LuaBitmap;
import com.androlua.LuaUtil;
import com.osfans.trime.BuildConfig;
import com.osfans.trime.Config;
import com.osfans.trime.Key;
import com.osfans.trime.TrimeService;
import com.osfans.trime.core.Rime;
import com.osfans.trime.core.RimeSchema;
import com.osfans.trime.util.Function;

import org.luaj.Globals;
import org.luaj.LuaTable;
import org.luaj.LuaValue;
import org.luaj.lib.ResourceFinder;
import org.luaj.lib.jse.JsePlatform;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 主题管理器,负责加载和管理输入法主题。
 * 提供 Lua 环境初始化、主题切换、样式管理、震动/音效控制等功能。
 */
public class ThemeManager {
    private static final String TAG = "ThemeManager";

    // 默认候选词栏高度(dp)
    private static final int mCandidateHeight = 48;
    // 默认键盘高度(dp)
    private static final int mKeyboardHeight = 240;

    // Lua 全局环境
    private static Globals mGlobals;
    // 颜色配置表
    private static LuaTable mColor;
    // 当前样式对象
    private static Style mStyle;
    // 震动器对象
    private static Vibrator vibrator;
    // 音效池对象
    private static SoundPool mSoundPool;
    // 音效文件路径到 ID 的缓存，避免同一文件重复加载
    private static final HashMap<String, Integer> mSoundCache = new HashMap<>();

    // 完整样式字段名列表
    private static final String[] STYLE_FIELD_NAMES = {
        "text_color",
        "text_size",
        "background",
        "padding",
        "margins",
        "elevation",
        "corner_radius",
        "shadow_color",
        "offset_x",
        "offset_y",
        "font",
        "long_click_time",
        "long_click_style",
        "repeat_click_time",
        "sound_enabled",
        "sound_effect",
        "vibration_enabled",
        "vibration_effect",
        "swipe_repeatable"
    };

    // 样式字段名（不含 swipe_repeatable，key 级别用不到该字段）
    private static final String[] STYLE_FIELD_NAMES_NO_SWIPE = {
        "text_color",
        "text_size",
        "background",
        "padding",
        "margins",
        "elevation",
        "corner_radius",
        "shadow_color",
        "offset_x",
        "offset_y",
        "font",
        "long_click_time",
        "long_click_style",
        "repeat_click_time",
        "sound_enabled",
        "sound_effect",
        "vibration_enabled",
        "vibration_effect",
    };

    /**
     * 获取资源查找器。
     *
     * @return ResourceFinder 对象。
     */
    public static ResourceFinder getFinder() {
        return mResourceFinder;
    }

    /**
     * 获取 Lua 全局环境。
     *
     * @return Globals 对象。
     */
    public static Globals getGlobals() {
        return mGlobals;
    }

    /**
     * 执行震动。
     *
     * @param ve 震动效果对象。
     */
    public static void vibrate(VibrationEffect ve) {
        if (vibrator == null) {
            Context context = LuaApplication.getInstance();
            if (context != null) {
                // 根据 Android 版本选择 Vibrator API
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                    vibrator = vibratorManager.getDefaultVibrator();
                } else {
                    vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                }
            }
        }
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(ve);
            }
        }
    }

    // Lua 资源查找器,负责从不同目录加载 Lua 脚本
    private final static ResourceFinder mResourceFinder = new ResourceFinder() {
        @Override
        public InputStream findResource(String name) {
            if (TextUtils.isEmpty(name)) {
                try {
                    return LuaApplication.getInstance().getAssets().open("themes/default/main.lua");
                } catch (Exception ioe) {
                    ioe.printStackTrace();
                }
                return null;
            }
            try {
                // 先尝试从默认主题 assets 中加载
                if (name.startsWith("themes/default/"))
                    return LuaApplication.getInstance().getAssets().open(name);
            } catch (Exception ioe) {
                ioe.printStackTrace();
            }
            try {
                // 再尝试从绝对路径加载
                if (new File(name).exists())
                    return new FileInputStream(name);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                // 最后尝试从当前主题目录加载
                File f = new File(Config.getThemeDir(), Config.getTheme() + "/" + name);
                if (f.exists())
                    return new FileInputStream(f);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!name.endsWith(".lua"))
                return null;
            return null;
        }

        @Override
        public String findFile(String filename) {
            if (TextUtils.isEmpty(filename))
                return null;
            if (filename.startsWith("/"))
                return filename;
            return new File(Config.getThemeDir(), filename).getAbsolutePath();
        }
    };

    /**
     * 设置主题。
     * 加载主题 Lua 脚本,初始化 Lua 环境,并应用样式配置。
     *
     * @param name 主题名称。
     */
    public static void setTheme(String name) {
        mStyle = null;
        clearSound();
        Config.setTheme(name);
        // 初始化 Lua 全局环境
        mGlobals = JsePlatform.standardGlobals();
        mGlobals.finder = mResourceFinder;
        try {
            // 加载主题的 main.lua
            LuaValue func = mGlobals.loadfilex("main.lua");
            if (func.isfunction()) {
                func.call();
            } else {
                sendMsg("setTheme " + func.tojstring());
                // 如果失败,则使用默认主题
                func = mGlobals.loadfilex("themes/default/main.lua");
                if (func.isfunction())
                    func.call();
            }
        } catch (Exception e) {
            sendMsg("setTheme " + e.toString());
        }
        // 加载样式配置
        String styleName = Function.loadString(LuaApplication.getInstance(), Config.getStyleKey(name), mGlobals.get("style").optjstring("light"));
        setStyle(styleName);
        // 更新预设按键配置
        Key.presetKeys = getPresetKeys();
    }

    /**
     * 发送消息到 TrimeService。
     *
     * @param s 消息内容。
     */
    public static void sendMsg(String s) {
        TrimeService trime = TrimeService.getInstance();
        if (trime != null)
            trime.sendMsg(s);
    }

    /**
     * 设置样式。
     * 加载样式 Lua 脚本并创建 Style 对象。
     *
     * @param name 样式名称。
     */
    public static void setStyle(String name) {
        mStyle = null;
        clearSound();
        Config.setStyle(name);
        // 创建颜色配置表
        mColor = new LuaTable(mGlobals);
        LuaTable mt = new LuaTable(mGlobals);
        mt.set("__index", mGlobals);
        mColor.setmetatable(mt);
        // 加载样式脚本
        String path = "styles/" + name + "/main.lua";
        try {
            LuaValue func = mGlobals.loadfilex(path, mColor);
            if (func.isfunction()) {
                func.call();
                return;
            } else {
                sendMsg("setStyle " + func.tojstring());
            }
        } catch (Exception e) {
            sendMsg("setStyle " + e);
        }
        // 如果失败,则使用默认 light 样式
        try {
            LuaValue func = mGlobals.loadfilex("themes/default/styles/light/main.lua", mColor);
            if (func.isfunction()) {
                func.call();
            }
        } catch (Exception e) {
            Log.e("theme", "setStyle: " + e);
        }
    }

    /**
     * 获取当前样式对象。
     *
     * @return Style 对象。
     */
    public static Style getStyle() {
        if (mStyle == null)
            mStyle = new Style(mColor);
        return mStyle;
    }

    /**
     * 获取输入法总高度。
     * 根据是否隐藏候选词栏动态计算高度。
     *
     * @return 高度值(px)。
     */
    public static int getHeight() {
        if (Rime.getRimeOption("_hide_candidate"))
            return getStyle().getSize("height", mCandidateHeight + mKeyboardHeight) - getStyle().getStyle("keyboard").getSize("height", mKeyboardHeight) - getStyle().getStyle("candidate").getSize("height", mCandidateHeight) + getKeyboardHeight();
        return getStyle().getSize("height", mCandidateHeight + mKeyboardHeight) - getStyle().getStyle("keyboard").getSize("height", mKeyboardHeight) - getStyle().getStyle("candidate").getSize("height", mCandidateHeight) + getKeyboardHeight() + getCandidateHeight();
    }

    /**
     * 获取内容区域高度(键盘+候选词栏)。
     *
     * @return 高度值(px)。
     */
    public static int getContentHeight() {
        if (Rime.getRimeOption("_hide_candidate"))
            return getKeyboardHeight();
        return getCandidateHeight() + getKeyboardHeight();
    }

    /**
     * 获取原始内容高度(不考虑缩放)。
     *
     * @return 高度值(px)。
     */
    public static int getRawContentHeight() {
        int kbHeight;
        if (sComputedKeyboardHeight > 0) {
            kbHeight = sComputedKeyboardHeight;
        } else {
            kbHeight = getStyle().getStyle("keyboard").getSize("height", mKeyboardHeight);
        }
        // 如果隐藏候选词栏,则返回键盘高度
        if (Rime.getRimeOption("_hide_candidate")) return kbHeight;
        return kbHeight + getStyle().getStyle("candidate").getSize("height", mCandidateHeight);
    }

    /**
     * 获取候选词栏高度(考虑缩放比例)。
     *
     * @return 高度值(px)。
     */
    public static int getCandidateMinWidth() {
        int w = getStyle().getStyle("candidate").getSize("min_width", 0);
        if (w <= 0 && mGlobals != null) {
            LuaValue candidate = mGlobals.get("candidate");
            if (candidate.istable()) {
                w = dp2px(candidate.get("min_width").optint(0));
            }
        }
        return Math.max(0, w);
    }

    public static int getCandidateHeight() {
        int h = (int) (getStyle().getStyle("candidate").getSize("height", mCandidateHeight) * Math.min(1, Config.getKeyboardHeightScale()));
        if (Config.is_hide_comment()) h /= 2;
        return h;
    }

    /**
     * 获取键盘高度(考虑缩放比例)。
     *
     * @return 高度值(px)。
     */
    private static int sComputedKeyboardHeight = 0;

    public static void setComputedKeyboardHeight(int height) {
        sComputedKeyboardHeight = height;
    }

    /**
     * 获取键盘 Lua 中 height 的 dp 值。
     * 优先级: Lua height → 主题 key.height。
     * 返回 0 表示无 dp 配置,应使用百分比模式(旧行为)。
     */
    public static double keyRowHeight(Globals globals) {
        // 优先级1: Lua 中的 height (dp)
        double h = globals.get("height").optdouble(0);
        Log.d(TAG, "height:" + h);
        if (h > 0) return h;

        // 优先级2: 主题 key.height (dp)
        // Style.getSize() 返回的是 px,需要转回 dp
        int px = getStyle().getStyle("key").getSize("height", 0);
        if (px > 0) {
            h = px2dp(px);
            Log.d(TAG, "key.height(px->dp):" + h);
            return h;
        }

        return 0;
    }

    /**
     * 为按键解析样式默认值，遵循 key → row → keyboard → theme 的多级优先级。
     * 返回一个包含已解析样式字段的 LuaTable。
     * key 级的字段具有最高优先级，其次为 row 级，最后为 keyboard 级（key_xxx 等全局变量）。
     * 不覆盖已在 key 上直接设置的字段。
     *
     * @param key     按键级别的 Lua 表。
     * @param row     行级别的 Lua 表。
     * @param globals Lua 全局环境。
     * @return 解析后的样式 LuaTable。
     */
    public static LuaTable resolveKeyStyleDefaults(LuaTable key, LuaTable row, Globals globals) {
        // 创建用于存储最终样式的 LuaTable
        LuaTable style = new LuaTable();

        // Level 1: Key-level - 处理按键级别的配置，优先级最高
        copyFieldsIfPresent(style, key, STYLE_FIELD_NAMES_NO_SWIPE);

        // Level 2: Row-level defaults - 处理行级别的默认配置，优先级次之
        if (row != null) {
            applyDefaults(style, row, STYLE_FIELD_NAMES);

            // Level 2.5: Row-level named style - 若 row 指定了 style 名称，从主题风格系统解析该命名样式并应用其字段
            LuaValue rowStyle = row.get("style");
            if (rowStyle.isstring() && mColor != null) {
                LuaValue namedStyleTable = mColor.get(rowStyle.tojstring());
                if (namedStyleTable.istable()) {
                    applyDefaults(style, namedStyleTable.checktable(), STYLE_FIELD_NAMES);
                }
            }
        }

        // Level 3: Keyboard-level defaults - 处理键盘级别的全局默认配置，优先级最低
        applyDefaultsFrom(style, globals, STYLE_FIELD_NAMES);

        // Level 3.5: Global-level named style - 若全局环境指定了 style 名称，从主题风格系统解析该命名样式并应用其字段
        LuaValue globalStyle = globals.get("style");
        if (globalStyle.isstring()) {
            LuaValue namedStyleTable = mColor.get(globalStyle.tojstring());
            if (namedStyleTable.istable()) {
                applyDefaults(style, namedStyleTable.checktable(), STYLE_FIELD_NAMES);
            }
        }

        return style;
    }

    /**
     * 为按键的子样式（hint、long_click、pressed、preview、popup）解析样式默认值。
     * 遵循 key.sub > row.sub > keyboard.key.sub > theme.key.sub 的多级优先级。
     *
     * @param key          按键级别的 Lua 表。
     * @param row          行级别的 Lua 表。
     * @param globals      Lua 全局环境。
     * @param subStyleName 子样式名称（如 "hint"、"long_click"、"pressed"、"preview"、"popup"）。
     * @return 解析后的子样式 LuaTable，如果不存在则返回 null。
     */
    public static LuaTable resolveSubKeyStyleDefaults(LuaTable key, LuaTable row, Globals globals, String subStyleName) {
        // 获取按键级别的子样式表
        LuaValue keySubStyleValue = key.get(subStyleName);

        LuaTable keySubStyle = null;
        boolean hasKeyLevelStyleFields = false;
        if (keySubStyleValue.istable()) {
            keySubStyle = keySubStyleValue.checktable();
            for (String fieldName : STYLE_FIELD_NAMES) {
                if (!keySubStyle.get(fieldName).isnil()) {
                    hasKeyLevelStyleFields = true;
                    break;
                }
            }

            // popup 等配置可能是纯数据表（数字索引键列表），这类配置不应被样式解析覆盖
            if (!hasKeyLevelStyleFields && "popup".equals(subStyleName)) {
                return null;
            }
        }

        // 创建用于存储最终样式的 LuaTable
        LuaTable resolvedStyle = new LuaTable();

        // Level 1: Key.sub-level - 按键级别的子样式配置，优先级最高
        if (hasKeyLevelStyleFields && keySubStyle != null) {
            copyFieldsIfPresent(resolvedStyle, keySubStyle, STYLE_FIELD_NAMES);
        }

        // Level 2: Row.sub-level defaults - 行级别的子样式默认配置
        if (row != null) {
            LuaTable rowSubStyle = row.get(subStyleName).opttable(null);
            if (rowSubStyle != null) {
                applyDefaults(resolvedStyle, rowSubStyle, STYLE_FIELD_NAMES);
            }

            // Level 2.5: Row-level named style sub defaults - 支持从 row.style 对应的命名样式里继承子样式
            LuaValue rowStyle = row.get("style");
            if (rowStyle.isstring() && mColor != null) {
                LuaValue namedStyleTable = mColor.get(rowStyle.tojstring());
                if (namedStyleTable.istable()) {
                    LuaTable namedSubStyle = namedStyleTable.checktable().get(subStyleName).opttable(null);
                    if (namedSubStyle != null) {
                        applyDefaults(resolvedStyle, namedSubStyle, STYLE_FIELD_NAMES);
                    }
                }
            }
        }

        // Level 3: Global-level sub-style defaults (e.g. globals.hint / globals.long_click)
        LuaValue globalSubStyle = globals.get(subStyleName);
        if (globalSubStyle.istable()) {
            LuaTable globalSubStyleTable = globalSubStyle.checktable();
            applyDefaults(resolvedStyle, globalSubStyleTable, STYLE_FIELD_NAMES);
        }

        // Level 4: Keyboard.key.sub-level defaults - 键盘级别的主按键子样式默认配置
        LuaValue keyboardKeyStyle = globals.get("key");
        if (keyboardKeyStyle.istable()) {
            LuaTable keyboardKeySubStyle = keyboardKeyStyle.checktable().get(subStyleName).opttable(null);
            if (keyboardKeySubStyle != null) {
                applyDefaults(resolvedStyle, keyboardKeySubStyle, STYLE_FIELD_NAMES);
            }
        }

        // Level 4.5: Global-level named style sub defaults - 支持从 keyboard.style 对应的命名样式里继承子样式
        LuaValue globalStyle = globals.get("style");
        if (globalStyle.isstring() && mColor != null) {
            LuaValue namedStyleTable = mColor.get(globalStyle.tojstring());
            if (namedStyleTable.istable()) {
                LuaTable namedSubStyle = namedStyleTable.checktable().get(subStyleName).opttable(null);
                if (namedSubStyle != null) {
                    applyDefaults(resolvedStyle, namedSubStyle, STYLE_FIELD_NAMES);
                }
            }
        }

        for (String fieldName : STYLE_FIELD_NAMES) {
            if (!resolvedStyle.get(fieldName).isnil()) {
                return resolvedStyle;
            }
        }
        return null;
    }

    /**
     * 如果源表中存在指定键且值不为 nil，则将其复制到目标表中。
     *
     * @param target 目标 LuaTable。
     * @param source 源 LuaTable。
     * @param key    要复制的键名。
     */
    private static void copyFieldIfPresent(LuaTable target, LuaTable source, String key) {
        LuaValue v = source.get(key); // 从源表获取值
        if (!v.isnil()) target.set(key, v); // 如果值不为 nil，则设置到目标表
    }

    /**
     * 如果目标表中指定键的值为 nil，且源表中该键的值不为 nil，则将源表的值应用到目标表。
     *
     * @param target 目标 LuaTable。
     * @param source 源 LuaTable。
     * @param key    键名。
     */
    private static void applyDefault(LuaTable target, LuaTable source, String key) {
        LuaValue v = source.get(key); // 从源表获取值
        if (!v.isnil() && target.get(key).isnil()) target.set(key, v); // 如果源值不为 nil 且目标值为 nil，则设置
    }

    /**
     * 如果目标表中指定目标键的值为 nil，且全局环境中指定源键的值不为 nil，则将全局环境的值应用到目标表。
     *
     * @param target  目标 LuaTable。
     * @param globals Lua 全局环境。
     * @param key     键名。
     */
    private static void applyDefaultFrom(LuaTable target, Globals globals, String key) {
        LuaValue v = globals.get(key); // 从全局环境获取值
        if (!v.isnil() && target.get(key).isnil()) target.set(key, v); // 如果全局值不为 nil 且目标值为 nil，则设置
    }

    /**
     * 批量复制源表中存在的字段到目标表（覆盖模式）。
     *
     * @param target 目标 LuaTable。
     * @param source 源 LuaTable。
     * @param fields 要复制的字段名数组。
     */
    private static void copyFieldsIfPresent(LuaTable target, LuaTable source, String[] fields) {
        for (String field : fields) {
            copyFieldIfPresent(target, source, field);
        }
    }

    /**
     * 批量将源表中的字段作为默认值应用到目标表（仅当目标表字段值为 nil）。
     *
     * @param target 目标 LuaTable。
     * @param source 源 LuaTable。
     * @param fields 要应用的字段名数组。
     */
    private static void applyDefaults(LuaTable target, LuaTable source, String[] fields) {
        for (String field : fields) {
            applyDefault(target, source, field);
        }
    }

    /**
     * 批量从全局环境将字段作为默认值应用到目标表（仅当目标表字段值为 nil）。
     *
     * @param target  目标 LuaTable。
     * @param globals Lua 全局环境。
     * @param fields  要应用的字段名数组。
     */
    private static void applyDefaultsFrom(LuaTable target, Globals globals, String[] fields) {
        for (String field : fields) {
            applyDefaultFrom(target, globals, field);
        }
    }

    /**
     * 获取键盘高度(考虑缩放比例)。
     *
     * @return 高度值(px)。
     */
    public static int getKeyboardHeight() {
        if (sComputedKeyboardHeight > 0) {
            // 如果已计算过键盘高度，则使用计算值并应用缩放比例
            return (int) (sComputedKeyboardHeight * Config.getKeyboardHeightScale());
        }
        // 否则从样式配置中获取键盘高度默认值，并应用缩放比例
        return (int) (getStyle().getStyle("keyboard").getSize("height", mKeyboardHeight) * Config.getKeyboardHeightScale());
    }

    // 系统显示度量对象
    private static final DisplayMetrics mDisplayMetrics = Resources.getSystem().getDisplayMetrics();

    /**
     * dp 转 px。
     *
     * @param dp dp 值。
     * @return px 值。
     */
    public static int dp2px(float dp) {
        return (int) (TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, mDisplayMetrics));
    }

    /**
     * px 转 dp。
     *
     * @param px px 值。
     * @return dp 值。
     */
    public static double px2dp(int px) {
        return px / ((double) mDisplayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }


    /**
     * 获取预设按键配置。
     * 从主题 Lua 脚本中加载 preset_keys 表。
     *
     * @return LuaValue 对象。
     */
    public static LuaValue getPresetKeys() {
        LuaTable keys = mGlobals.get("preset_keys").opttable(new LuaTable());
        LuaTable env = new LuaTable();
        try {
            env.setmetamethod("__index", mGlobals);
            LuaValue func = mGlobals.loadfilex("themes/default/main.lua", env);
            if (func.isfunction())
                func.call();
        } catch (Exception e) {
            e.printStackTrace();
            return keys;
        }
        if (env.rawget("preset_keys").istable())
            keys.setmetamethod("__index", env.get("preset_keys").opttable(new LuaTable()));
        return keys;
    }

    /**
     * 获取 Action 标签文本。
     *
     * @param key 键名。
     * @param def 默认值。
     * @return 标签文本。
     */
    public static String getActionLabel(String key, String def) {
        LuaValue action = mGlobals.get("action_labels");
        if (!action.istable())
            return def;
        return action.get(key).optjstring(def);
    }

    /**
     * 获取对话框主题。
     * 根据当前深色模式返回对应的对话框样式。
     *
     * @return 对话框主题 ID。
     */
    public static int getDialogTheme() {
        if ((LuaApplication.getInstance().getResources().getConfiguration().uiMode & UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES) {
            return android.R.style.Theme_DeviceDefault_Dialog_Alert;
        } else {
            return android.R.style.Theme_DeviceDefault_Light_Dialog_Alert;
        }
    }

    /**
     * 获取键盘布局名称。
     * 根据方案 ID 和字母表动态确定键盘布局。
     *
     * @param id 方案 ID。
     * @return 键盘布局名称。
     */
    public static String getKeyboard(String id) {
        String k = Config.getKeyboard();
        Log.w("theme", "getKeyboard:Config " + id + ";" + k);
        if (!TextUtils.isEmpty(k))
            return k;
        // 尝试调用 Lua 函数 get_keyboard 动态获取
        LuaValue fun = mGlobals.get("get_keyboard");
        if (fun.isfunction()) {
            try {
                String alphabet = "abcdefghijklmnopqrstuvwxyz";
                try {
                    RimeSchema schema = new RimeSchema(id);
                    alphabet = schema.getAlphabet();
                } catch (Exception e) {
                    // 如果 RimeSchema 加载失败,则从 YAML 文件中解析 alphabet
                    File f = new File(Config.getUserDataDir(), id + ".schema.yaml");
                    if (f.exists()) {
                        String input = new String(LuaUtil.readAll(f.getAbsolutePath()));
                        String regex = "alphabet:\\s*(.*)";
                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(input);
                        if (matcher.find()) {
                            // matcher.group(1) 提取第一个括号内的内容
                            String result = matcher.group(1);
                            if (!TextUtils.isEmpty(result)) {
                                alphabet = result;
                                Log.w("theme", "getKeyboard:alphabet " + alphabet);
                            }
                        }
                    }

                }
                LuaValue ret = fun.call(LuaValue.valueOf(id), LuaValue.valueOf(alphabet));
                Log.w("theme", "getKeyboard:ret " + ret);
                if (ret.isstring())
                    id = ret.tojstring();
            } catch (Exception e) {
                if (BuildConfig.DEBUG)
                    e.printStackTrace();
            }
        }
        return id;
    }

    /**
     * 加载音效文件。
     * 创建 SoundPool 并加载音效文件,返回音效 ID。
     *
     * @param soundPath 音效文件路径。
     * @return 音效 ID,失败返回 -1。
     */
    public static int loadSound(String soundPath) {
        // 检查缓存中是否已加载该音效文件
        Integer cachedId = mSoundCache.get(soundPath);
        if (cachedId != null) {
            return cachedId;
        }
        // 检查文件是否存在
        java.io.File file = new java.io.File(soundPath);
        if (!file.exists()) {
            return -1;
        }
        if (mSoundPool == null) {
            // 创建音频属性
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            mSoundPool = new SoundPool.Builder()
                    .setMaxStreams(5)
                    .setAudioAttributes(attributes)
                    .build();
            mSoundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> {
                if (status != 0) {
                    Log.e("SoundHelper", "加载失败！ID: " + sampleId + " 状态码: " + status);
                }
            });
        }
        try {
            int id = mSoundPool.load(soundPath, 1);
            if (id > 0) {
                mSoundCache.put(soundPath, id);
            }
            return id;
        } catch (Exception e) {
            Log.e("SoundHelper", "loadSound error: " + soundPath + " " + e.getMessage());
            return -1;
        }
    }

    /**
     * 播放音效（默认参数:左右声道 1.0,速率 1.0,无循环）。
     *
     * @param soundId 音效 ID。
     */
    public static void play(int soundId) {
        play(soundId, 1.0f, 1.0f);
    }

    /**
     * 播放音效（指定播放速率/音量），用于实现按键音效的"律动"效果。
     *
     * @param soundId 音效 ID,小于等于 0 时直接返回。
     * @param rate    播放速率(0.5~2.0),1.0 为原速,>1.0 音调更高、节奏更紧凑。
     * @param volume  音量(0.0~1.0),0.0 为静音,1.0 为满音量。
     */
    public static void play(int soundId, float rate, float volume) {
        if (mSoundPool == null || soundId <= 0) return;
        // 防御性夹紧
        if (rate < 0.5f) rate = 0.5f;
        if (rate > 2.0f) rate = 2.0f;
        if (volume < 0.0f) volume = 0.0f;
        if (volume > 1.0f) volume = 1.0f;
        // 参数依次为:左声道、右声道、优先级、循环(0=不循环)、速率
        mSoundPool.play(soundId, volume, volume, 1, 0, rate);
    }

    /**
     * 清除所有音效资源。
     */
    public static void clearSound() {
        if (mSoundPool != null) {
            mSoundPool.release();
            mSoundPool = null;
        }
        mSoundCache.clear();
    }

    /**
     * 调用 Lua 全局函数。
     *
     * @param s    函数名。
     * @param args 函数参数。
     * @return 函数返回值。
     */
    public static Object callFunction(String s, Object... args) {
        if (mGlobals == null)
            return null;
        LuaValue f = mGlobals.get(s);
        if (f.isfunction()) {
            return f.jcall(args);
        }
        return null;
    }
}
