/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.theme;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.VibrationEffect;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
import android.view.Gravity;

import com.androlua.LuaBitmap;
import com.osfans.trime.Config;

import org.luaj.LuaValue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

/**
 * 按键样式类,继承自 Style,提供缓存优化的按键外观配置。
 * 针对高频访问的属性(如动画参数、颜色)进行了字段级缓存,避免重复查询 LuaTable。
 * 支持文本样式、背景、震动效果、音效、字体等完整配置。
 */
public class KeyStyle extends Style {
    // ==================== 默认常量 ====================
    // 默认文本颜色(黑色)
    private static final int DEFAULT_TEXT_COLOR = 0xff000000;
    // 默认文本大小(18sp)
    private static final int DEFAULT_TEXT_SIZE = 18;
    // 默认背景颜色(白色)
    private static final int DEFAULT_BG_COLOR = 0xffffffff;

    // ==================== 缓存字段 - 使用基本类型避免装箱开销 ====================
    // 文本大小缓存(-1 表示未缓存)
    private float mTextSize = -1;
    // 文本颜色缓存
    private int mTextColor = 0;
    private boolean mHasCachedTextColor = false;

    // 背景颜色缓存
    private int mBackgroundColor = 0;
    private boolean mHasCachedBgColor = false;

    // 动画相关属性缓存(NaN 或极小值标记未缓存状态)
    private float mScaleX = Float.NaN; // X轴缩放比例
    private float mScaleY = Float.NaN; // Y轴缩放比例
    private float mTranslationX = Float.MIN_VALUE; // X轴平移
    private float mTranslationY = Float.MIN_VALUE; // Y轴平移
    private float mTranslationZ = Float.MIN_VALUE; // Z轴平移(高度)

    // 阴影和高度缓存
    private int mElevation = -1; // 阴影高度
    private int mShadowColor = 0; // 阴影颜色
    private boolean mHasCachedShadowColor = false;

    // 嵌套样式引用缓存
    private KeyStyle mHintStyle, mLongClickStyle, mPressedStyle;
    // 对齐方式缓存
    private int mGravity;
    // 震动效果缓存
    private VibrationEffect mVibrationEffect;
    private boolean mHasCachedVibrationEffect;
    // 震动开关缓存
    private boolean mHasCachedVibrationEnabled;
    private boolean mVibrationEnabled;
    // 音效缓存
    private int[] mSoundEffectIDs;
    private boolean mHasCachedSoundEffect;
    private final Random mSoundRandom = new Random();
    // 音效开关缓存
    private boolean mHasCachedSoundEnabled;
    private boolean mSoundEnabled;
    // 长按时间缓存
    private long mLongClickTime;
    // 重复点击时间缓存
    private long mRepeatClickTime;
    // 字体缓存
    private Typeface mTypeface;
    // 显示开关缓存
    private boolean mShow;
    private boolean mHasCachedShow;
    // SpannableString 缓存(用于图文混排)
    private final HashMap<String, CharSequence> mSpanCache = new HashMap<>();

    /**
     * 构造函数,从 Lua 表中加载按键样式配置。
     *
     * @param t Lua 表,包含样式配置项。
     */
    public KeyStyle(LuaValue t) {
        super(t);
    }

    /**
     * 构造函数,带默认样式。
     *
     * @param t Lua 表,包含样式配置项。
     * @param def 默认样式对象。
     */
    public KeyStyle(LuaValue t, Style def) {
        this(t);
        setStyle(def);
        // setStyle 设置了元表继承链，需要重置音效缓存以便重新查找
        mHasCachedSoundEffect = false;
        mSoundEffectIDs = null;
    }

    /**
     * 构造函数,带默认按键样式。
     * 用于按键级别的样式覆盖，基于父级 KeyStyle 创建新的样式对象。
     *
     * @param t Lua 表,包含样式配置项。
     * @param def 默认按键样式对象。
     */
    public KeyStyle(LuaValue t, KeyStyle def) {
        this(t);
        // 复制默认 KeyStyle 的所有属性
        setStyle(def);
        // setStyle 设置了元表继承链，需要重置音效缓存以便重新查找
        mHasCachedSoundEffect = false;
        mSoundEffectIDs = null;
    }

    // ==================== 核心属性获取(带缓存逻辑) ====================

    /**
     * 获取文本大小。
     *
     * @return 文本大小(sp)。
     */
    public float getTextSize() {
        if (mTextSize < 0) {
            mTextSize = getTextSize(DEFAULT_TEXT_SIZE);
        }
        return mTextSize;
    }

    /**
     * 获取文本颜色。
     *
     * @return ARGB 颜色值。
     */
    public int getTextColor() {
        if (!mHasCachedTextColor) {
            mTextColor = getTextColor(DEFAULT_TEXT_COLOR);
            mHasCachedTextColor = true;
        }
        return mTextColor;
    }

    /**
     * 获取背景颜色。
     *
     * @return ARGB 颜色值。
     */
    public int getBackgroundColor() {
        if (!mHasCachedBgColor) {
            mBackgroundColor = getColor("background", DEFAULT_BG_COLOR);
            mHasCachedBgColor = true;
        }
        return mBackgroundColor;
    }

    /**
     * 获取背景 Drawable。
     *
     * @return Drawable 对象。
     */
    public Drawable getBackground() {
        // Drawable 涉及对象创建,由父类实现或按需获取,此处通常不适合在 Style 中长期常驻缓存
        return getBackground(DEFAULT_BG_COLOR);
    }

    // ==================== 动画相关属性(极高频调用) ====================

    /**
     * 获取 X 轴缩放比例。
     *
     * @return 缩放比例(默认 1.0)。
     */
    public float getScaleX() {
        if (Float.isNaN(mScaleX)) {
            mScaleX = getFloat("scale_x", 1.0f);
        }
        return mScaleX;
    }

    /**
     * 获取 Y 轴缩放比例。
     *
     * @return 缩放比例(默认 1.0)。
     */
    public float getScaleY() {
        if (Float.isNaN(mScaleY)) {
            mScaleY = getFloat("scale_y", 1.0f);
        }
        return mScaleY;
    }

    /**
     * 获取 X 轴平移距离。
     *
     * @return 平移距离(dp)。
     */
    public float getTranslationX() {
        if (mTranslationX == Float.MIN_VALUE) {
            mTranslationX = getSize("translation_x", 0);
        }
        return mTranslationX;
    }

    /**
     * 获取 Y 轴平移距离。
     *
     * @return 平移距离(dp)。
     */
    public float getTranslationY() {
        if (mTranslationY == Float.MIN_VALUE) {
            mTranslationY = getSize("translation_y", 0);
        }
        return mTranslationY;
    }

    /**
     * 获取 Z 轴平移距离(高度)。
     *
     * @return 平移距离(dp)。
     */
    public float getTranslationZ() {
        if (mTranslationZ == Float.MIN_VALUE) {
            mTranslationZ = getSize("translation_z", 0);
        }
        return mTranslationZ;
    }

    // ==================== 其他样式属性 ====================

    /**
     * 获取阴影高度。
     *
     * @return 阴影高度(dp)。
     */
    public int getElevation() {
        if (mElevation < 0) {
            mElevation = getSize("elevation", 0);
        }
        return mElevation;
    }

    /**
     * 获取阴影颜色。
     *
     * @return ARGB 颜色值。
     */
    public int getShadowColor() {
        if (!mHasCachedShadowColor) {
            mShadowColor = getColor("shadow_color", 0);
            mHasCachedShadowColor = true;
        }
        return mShadowColor;
    }

    /**
     * 获取对齐方式。
     *
     * @return Gravity 常量。
     */
    public int getGravity() {
        if (mGravity == -1) {
            mGravity = getGravity(Gravity.CENTER);
        }
        return mGravity;
    }


    // ==================== 嵌套样式引用缓存 ====================

    /**
     * 获取提示文本样式。
     *
     * @return KeyStyle 对象。
     */
    public KeyStyle getHintKeyStyle() {
        if (mHintStyle == null) mHintStyle = getKeyStyle("hint", this);
        return mHintStyle;
    }

    /**
     * 获取长按样式。
     *
     * @return KeyStyle 对象。
     */
    public KeyStyle getLongClickKeyStyle() {
        if (mLongClickStyle == null) mLongClickStyle = getKeyStyle("long_click", this);
        return mLongClickStyle;
    }

    /**
     * 获取按下状态样式。
     *
     * @return KeyStyle 对象。
     */
    public KeyStyle getPressedStyle() {
        if (mPressedStyle == null) mPressedStyle = getKeyStyle("pressed", this);
        return mPressedStyle;
    }

    /**
     * 获取震动效果配置。
     * 从 Lua 表中读取震动波形数据,创建 VibrationEffect 对象。
     *
     * @return VibrationEffect 对象,如果未配置则返回 null。
     */
    public VibrationEffect getVibrationEffect() {
        if (!mHasCachedVibrationEffect) {
            mHasCachedVibrationEffect = true;
            LuaValue ve = get("vibration_effect");

            if (ve.istable()) {
                LuaValue vt = ve.get(1); // 时间轴
                LuaValue va = ve.get(2); // 强度轴

                // 关键:取两者长度的最小值,防止 Lua 配置不一致导致崩溃
                int len = Math.min(vt.length(), va.length());

                if (len > 0) {
                    long[] timings = new long[len];
                    int[] amplitudes = new int[len];
                    for (int i = 0; i < len; i++) {
                        timings[i] = vt.get(i + 1).optlong(0);
                        // 强制约束振幅在 0-255,防止 Lua 填错导致非法参数异常
                        int amp = va.get(i + 1).optint(0);
                        amplitudes[i] = Math.max(0, Math.min(255, amp));
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        try {
                            mVibrationEffect = VibrationEffect.createWaveform(timings, amplitudes, -1);
                        } catch (Exception e) {
                            // 最后的兜底,防止意外的非法参数
                            mVibrationEffect = null;
                        }
                    }
                }
            }
        }
        return mVibrationEffect;
    }

    /**
     * 检查震动是否启用。
     *
     * @return 是否启用震动。
     */
    public boolean isVibrationEnabled(){
        if(!mHasCachedVibrationEnabled) {
            mHasCachedVibrationEnabled = true;
            mVibrationEnabled = get("vibration_enabled").toboolean();
        }
        return mVibrationEnabled;
    }

    private int loadSingleSound(String name) {
        String path = Config.getStylePath(name);
        if (new File(path).exists()) return ThemeManager.loadSound(path);
        path = Config.getSoundPath(name);
        if (new File(path).exists()) return ThemeManager.loadSound(path);
        return -1;
    }

    /**
     * 获取音效 ID。
     * 从样式目录或音效目录中查找音效文件并加载。
     * 支持单个字符串或字符串数组（随机播放）。
     *
     * @return 音效 ID,如果未找到则返回 -1。
     */
    public int getSoundEffect() {
        if (!mHasCachedSoundEffect) {
            mHasCachedSoundEffect = true;
            LuaValue ve = get("sound_effect");
            if (ve.isstring()) {
                int id = loadSingleSound(ve.tojstring());
                if (id > 0) mSoundEffectIDs = new int[]{id};
            } else if (ve.istable()) {
                int len = ve.length();
                if (len > 0) {
                    ArrayList<Integer> ids = new ArrayList<>();
                    for (int i = 1; i <= len; i++) {
                        LuaValue item = ve.get(i);
                        if (item.isstring()) {
                            int id = loadSingleSound(item.tojstring());
                            if (id > 0) ids.add(id);
                        }
                    }
                    if (!ids.isEmpty()) {
                        mSoundEffectIDs = new int[ids.size()];
                        for (int i = 0; i < ids.size(); i++)
                            mSoundEffectIDs[i] = ids.get(i);
                    }
                }
            }
        }
        if (mSoundEffectIDs != null && mSoundEffectIDs.length > 0) {
            return mSoundEffectIDs[mSoundRandom.nextInt(mSoundEffectIDs.length)];
        }
        return -1;
    }

    /**
     * 检查音效是否启用。
     *
     * @return 是否启用音效。
     */
    public boolean isSoundEnabled(){
        if(!mHasCachedSoundEnabled) {
            mHasCachedSoundEnabled = true;
            mSoundEnabled = get("sound_enabled").toboolean();
        }
        return mSoundEnabled;
    }

    /**
     * 获取长按时间阈值。
     *
     * @return 长按时间(毫秒),默认 1000ms。
     */
    public long getLongClickTime() {
        if(mLongClickTime==0)
            mLongClickTime=get("long_click_time").optlong(1000);
        return mLongClickTime;
    }

    /**
     * 获取重复点击时间间隔。
     *
     * @return 重复点击间隔(毫秒),默认 200ms。
     */
    public long getRepeatClickTime() {
        if(mRepeatClickTime==0)
            mRepeatClickTime=get("repeat_click_time").optlong(200);
        return mRepeatClickTime;
    }

    /**
     * 获取字体配置。
     *
     * @return Typeface 对象。
     */
    public Typeface getFont(){
        if(mTypeface==null)
            mTypeface = getFont("font");
        return mTypeface;
    }

    /**
     * 检查按键是否显示。
     *
     * @return 是否显示。
     */
    public boolean isShow(){
        if(!mHasCachedShow) {
            LuaValue show = get("show");
            mShow = show.isnil() || show.toboolean();
            mHasCachedShow = true;
        }
        return mShow;
    }

    /**
     * 获取图文混排的 SpannableString。
     * 如果文本对应 PNG 图片存在,则创建 ImageSpan 实现图文混排。
     * 支持单色图标着色和灰度转换。
     *
     * @param text 文本内容或图片名称。
     * @return SpannableString 对象,如果无图片则返回原文本。
     */
    public CharSequence getSpan(final String text) {
        if (TextUtils.isEmpty(text))
            return text;
        // 先从缓存中查找
        CharSequence s = mSpanCache.get(text);
        if (s != null)
            return s;
        // 查找对应的 PNG 图片
        String path = Config.findImagePath(text.endsWith(".png")?text:text + ".png");
        if (!TextUtils.isEmpty(path)) {
            SpannableString span = new SpannableString(text);
            try {
                Bitmap bitmap = LuaBitmap.getLocalBitmap(path);
                int targetColor = getTextColor(); // 目标颜色(例如 Trime 主题色)
                BitmapDrawable bmp = new BitmapDrawable(bitmap);
                // 判断是否为单色图标
                if(getSingleColorIfPure(bitmap)==null){
                    // 非单色图:使用 ColorMatrix 进行灰度转换并着色
                    if(targetColor!=0){
                        float r = Color.red(targetColor) / 255f;
                        float g = Color.green(targetColor) / 255f;
                        float b = Color.blue(targetColor) / 255f;
                        float a = Color.alpha(targetColor) / 255f;

                        // 灰度转换系数(标准生理亮度公式)
                        float lr = 0.213f;
                        float lg = 0.715f;
                        float lb = 0.072f;

                        ColorMatrix cm = new ColorMatrix(new float[] {
                                lr * r, lg * r, lb * r, 0, 0,  // 新的 R = (原R*lr + 原G*lg + 原B*lb) * 目标R
                                lr * g, lg * g, lb * g, 0, 0,  // 新的 G = (原R*lr + 原G*lg + 原B*lb) * 目标G
                                lr * b, lg * b, lb * b, 0, 0,  // 新的 B = (原R*lr + 原G*lg + 原B*lb) * 目标B
                                0,      0,      0,      a, 0   // 保持原图透明度并乘以目标Alpha
                        });

                        ColorFilter filter = new ColorMatrixColorFilter(cm);
                        bmp.setColorFilter(filter);
                    }
                } else {
                    // 单色图:直接使用 PorterDuff 模式着色
                    PorterDuffColorFilter colorFilter = new PorterDuffColorFilter(getTextColor(), PorterDuff.Mode.SRC_IN);
                    bmp.setColorFilter(colorFilter);
                }

                // 设置图片大小为文本大小
                bmp.setBounds(0,0, ThemeManager.dp2px(getTextSize()), ThemeManager.dp2px(getTextSize()));
                ImageSpan image = new ImageSpan(bmp, DynamicDrawableSpan.ALIGN_CENTER);
                span.setSpan(image, 0, text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                mSpanCache.put(text, span);
                return span;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 无图片,缓存原文本
        mSpanCache.put(text, text);
        return text;
    }

    /**
     * 判断图片是否为单色图标(忽略透明区域)。
     * 用于决定使用哪种着色策略(ColorMatrix 或 PorterDuff)。
     *
     * @param bitmap 待检测的位图。
     * @return 如果图片除透明外只有一种 RGB 颜色,返回该颜色值;否则返回 null。
     */
    public static Integer getSingleColorIfPure(Bitmap bitmap) {
        if (bitmap == null) return null;

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Integer baseColor = null; // 用于存储第一个找到的非透明像素的 RGB

        // 采样步长:如果图片很大,可以设置步长(如 2)来提高性能
        int step = 1;

        for (int x = 0; x < width; x += step) {
            for (int y = 0; y < height; y += step) {
                int pixel = bitmap.getPixel(x, y);

                // 1. 去除全透明区域:如果 Alpha 为 0,跳过不计入判断
                if (Color.alpha(pixel) == 0) {
                    continue;
                }

                // 2. 提取 RGB 部分 (忽略透明度进行比较)
                int currentColorRGB = pixel & 0x00FFFFFF;

                if (baseColor == null) {
                    // 记录第一个非透明像素的 RGB 作为基准
                    baseColor = currentColorRGB;
                } else {
                    // 3. 与基准色对比
                    if (currentColorRGB != baseColor) {
                        return null; // 发现第二种颜色,不是单色图
                    }
                }
            }
        }

        // 如果循环结束 baseColor 仍为 null,说明是全透明图片
        return baseColor;
    }
}
