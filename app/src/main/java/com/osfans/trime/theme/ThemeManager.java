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
        if(Rime.getRimeOption("_hide_candidate"))
            return getStyle().getSize("height", mCandidateHeight + mKeyboardHeight) - getStyle().getStyle("keyboard").getSize("height", mKeyboardHeight) - getStyle().getStyle("candidate").getSize("height", mCandidateHeight) + getKeyboardHeight();
        return getStyle().getSize("height", mCandidateHeight + mKeyboardHeight) - getStyle().getStyle("keyboard").getSize("height", mKeyboardHeight) - getStyle().getStyle("candidate").getSize("height", mCandidateHeight) + getKeyboardHeight() + getCandidateHeight();
    }

    /**
     * 获取内容区域高度(键盘+候选词栏)。
     *
     * @return 高度值(px)。
     */
    public static int getContentHeight() {
        if(Rime.getRimeOption("_hide_candidate"))
            return getCandidateHeight();
        return getCandidateHeight() + getKeyboardHeight();
    }

    /**
     * 获取原始内容高度(不考虑缩放)。
     *
     * @return 高度值(px)。
     */
    public static int getRawContentHeight() {
        if(Rime.getRimeOption("_hide_candidate"))
            return getStyle().getStyle("keyboard").getSize("height", mKeyboardHeight);
        return getStyle().getStyle("keyboard").getSize("height", mKeyboardHeight) + getStyle().getStyle("candidate").getSize("height", mCandidateHeight);
    }

    /**
     * 获取候选词栏高度(考虑缩放比例)。
     *
     * @return 高度值(px)。
     */
    public static int getCandidateHeight() {
        return (int) (getStyle().getStyle("candidate").getSize("height", mCandidateHeight) * Math.min(1, Config.getKeyboardHeightScale()));
    }

    /**
     * 获取键盘高度(考虑缩放比例)。
     *
     * @return 高度值(px)。
     */
    public static int getKeyboardHeight() {
        return (int) (getStyle().getStyle("keyboard").getSize("height", mKeyboardHeight) * Config.getKeyboardHeightScale());
    }

    // 系统显示度量对象
    private static final DisplayMetrics mDisplayMetrics = Resources.getSystem().getDisplayMetrics();

    /**
     * dp 转 px。
     *
     * @param f dp 值。
     * @return px 值。
     */
    public static int dp2px(float f) {
        return (int) (TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, f, mDisplayMetrics));
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
        Log.w("theme", "loadSound: " + soundPath);
        if (mSoundPool == null) {
            // 创建音频属性
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA) // 提示音类型
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            // 创建 SoundPool,允许同时播放5个声音(防止连打时截断)
            mSoundPool = new SoundPool.Builder()
                    .setMaxStreams(5) // 允许同时播放 5 个声音（防止连打时截断）
                    .setAudioAttributes(attributes)
                    .build();
            mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                    //if (status != 0) {
                    // status 为 0 表示成功，非 0 表示失败
                    Log.e("SoundHelper", "加载失败！ID: " + sampleId + " 状态码: " + status);
                    //}
                }
            });
        }
        // 检查文件是否存在
        java.io.File file = new java.io.File(soundPath);
        if (!file.exists()) {
            Log.e("SoundHelper", "文件根本不存在: " + soundPath);
            return -1;
        }
        return mSoundPool.load(soundPath, 1);
    }

    /**
     * 播放音效。
     *
     * @param soundId 音效 ID。
     */
    public static void play(int soundId) {
        if (mSoundPool != null && soundId > 0) {
            // 参数依次为：左声道、右声道、优先级、循环、速率
            mSoundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f);
        }
    }

    /**
     * 清除所有音效资源。
     */
    public static void clearSound() {
        if (mSoundPool != null) {
            mSoundPool.release();
            mSoundPool = null;
        }
    }

    /**
     * 调用 Lua 全局函数。
     *
     * @param s 函数名。
     * @param args 函数参数。
     * @return 函数返回值。
     */
    public static Object callFunction(String s, Object... args) {
        if(mGlobals==null)
            return null;
        LuaValue f = mGlobals.get(s);
        if(f.isfunction()){
            return f.jcall(args);
        }
        return null;
    }
}
