/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.theme;

import static com.osfans.trime.theme.ThemeManager.dp2px;

import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.fonts.Font;
import android.graphics.fonts.FontFamily;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;

import com.androlua.LuaBitmapDrawable;
import com.osfans.trime.Config;
import com.osfans.trime.util.Function;

import org.luaj.LuaTable;
import org.luaj.LuaValue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 样式基类,封装从 Lua 表中读取主题配置的逻辑。
 * 提供颜色、Drawable、字体、对齐方式等通用样式属性的访问方法。
 * 支持样式继承和缓存优化。
 */
public class Style {

    // Lua 配置表
    private final LuaValue mTable;
    // 子样式缓存(key: 样式名称, value: Style 对象)
    private final HashMap<String, Style> mStyleCache = new HashMap<>();
    // 按键样式缓存(key: 样式名称, value: KeyStyle 对象)
    private final HashMap<String, KeyStyle> mKeyStyleCache = new HashMap<>();

    /**
     * 构造函数。
     *
     * @param t Lua 表,包含样式配置项。
     */
    public Style(LuaValue t) {
        mTable = t.isnil() ? new LuaTable() : t;
    }

    /**
     * 构造函数,带默认样式。
     *
     * @param t Lua 表,包含样式配置项。
     * @param def 默认样式对象。
     */
    public Style(LuaValue t, Style def) {
        this(t);
        setStyle(def);
    }

    /**
     * 设置 Lua 表的元表(用于继承)。
     *
     * @param t 父表对象。
     */
    public void setMeta(LuaValue t) {
        LuaTable mt = new LuaTable();
        mt.set("__index", t);
        mTable.setmetatable(mt);
    }

    /**
     * 设置默认样式(通过元表实现继承)。
     *
     * @param t 默认样式对象。
     */
    public void setStyle(Style t) {
        LuaTable mt = new LuaTable();
        mt.set("__index", t.getTable());
        mTable.setmetatable(mt);
    }

    /**
     * 获取内部 Lua 表。
     *
     * @return LuaValue 对象。
     */
    protected LuaValue getTable() {
        return mTable;
    }


    /**
     * 获取颜色值。
     *
     * @param key 颜色键名。
     * @return ARGB 颜色值。
     */
    public int getColor(String key) {
        return mTable.get(key).toint();
    }

    /**
     * 获取颜色值,带默认值。
     *
     * @param key 颜色键名。
     * @param def 默认颜色值。
     * @return ARGB 颜色值。
     */
    public int getColor(String key, int def) {
        return mTable.get(key).optint(def);
    }

    /**
     * 获取 Drawable 对象。
     * 支持颜色值、图片路径两种配置方式。
     *
     * @param key Drawable 键名。
     * @return Drawable 对象,如果未找到则返回 null。
     */
    public Drawable getDrawable(String key) {
        LuaValue v = mTable.get(key);
        // 预先计算好通用的圆角
        float radiusPx = dp2px((float) mTable.get("corner_radius").optdouble(0f));
        if (v.isint()) {
            // 颜色值:创建 GradientDrawable
            return createGradientDrawable(v.toint(),radiusPx,dp2px((float) mTable.get("stroke_width").optdouble(0)), mTable.get("stroke_color").optint(0));
        } else if (v.isstring()) {
            // 图片路径:先尝试样式目录,再尝试图片目录
            File f=new File(Config.getStylePath(v.tojstring()));
            if(f.exists())
                return new LuaBitmapDrawable(f.getAbsolutePath());
            f=new File(Config.getImagePath(v.tojstring()));
            if(f.exists())
                return new LuaBitmapDrawable(f.getAbsolutePath());
        }
        return null;
    }

    /**
     * 获取 Drawable 对象,带默认值。
     *
     * @param key Drawable 键名。
     * @param def 默认颜色值。
     * @return Drawable 对象。
     */
    public Drawable getDrawable(String key, int def) {
        LuaValue v = mTable.get(key);
        // 预先计算好通用的圆角
        float radiusPx = dp2px((float) mTable.get("corner_radius").optdouble(0f));
        if (v.isint()) {
            return createGradientDrawable(v.toint(),radiusPx,dp2px((float) mTable.get("stroke_width").optdouble(0)), mTable.get("stroke_color").optint(0));
        } else if (v.isstring()) {
            File f=new File(Config.getStylePath(v.tojstring()));
            if(f.exists())
                return new LuaBitmapDrawable(f.getAbsolutePath());
            f=new File(Config.getImagePath(v.tojstring()));
            if(f.exists())
                return new LuaBitmapDrawable(f.getAbsolutePath());
        }
        // 使用默认颜色创建 GradientDrawable
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(v.optint(def));
        drawable.setCornerRadius(radiusPx);
        return drawable;
    }

    // 提取公共方法:创建 GradientDrawable
    /**
     * 创建带圆角和边框的 GradientDrawable。
     *
     * @param color 背景颜色。
     * @param radius 圆角半径(px)。
     * @param strokeWidth 边框宽度(px)。
     * @param strokeColor 边框颜色。
     * @return GradientDrawable 对象。
     */
    private Drawable createGradientDrawable(int color, float radius, int strokeWidth, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(color);
        drawable.setCornerRadius(radius);

        // 添加边框逻辑
        if (strokeWidth > 0) {
            drawable.setStroke(strokeWidth, strokeColor);
        }

        return drawable;
    }

    /**
     * 获取子样式对象(带缓存)。
     *
     * @param key 样式键名。
     * @return Style 对象。
     */
    public Style getStyle(String key) {
        Style style = mStyleCache.get(key);
        if (style == null) {
            style = new Style(mTable.get(key));
            mStyleCache.put(key, style);
        }
        return style;
    }

    /**
     * 获取子样式对象(通过索引,带缓存)。
     *
     * @param key 样式索引。
     * @return Style 对象。
     */
    public Style getStyle(int key) {
        Style style = mStyleCache.get(Integer.toString(key));
        if (style == null) {
            style = new Style(mTable.get(key+1));
            mStyleCache.put(Integer.toString(key), style);
        }
        return style;
    }

    /**
     * 获取子样式对象,带默认值。
     *
     * @param key 样式键名。
     * @param def 默认样式对象。
     * @return Style 对象。
     */
    public Style getStyle(String key, Style def) {
        if(TextUtils.isEmpty(key))
            return def;
        Style style = mStyleCache.get(key);
        if (style == null) {
            LuaValue v = mTable.get(key);
            if (!v.istable())
                return def;
            style = new Style(v, def);
            mStyleCache.put(key, style);
        }
        return style;
    }
    /**
     * 获取按键样式对象(带缓存)。
     *
     * @param key 样式键名。
     * @return KeyStyle 对象。
     */
    public KeyStyle getKeyStyle(String key){
        KeyStyle style = mKeyStyleCache.get(key);
        if (style == null) {
            LuaValue v = mTable.get(key);
            style = new KeyStyle(v);
            mKeyStyleCache.put(key, style);
        }
        return style;
    }

    /**
     * 获取默认按键样式对象(键名为 "key")。
     *
     * @return KeyStyle 对象。
     */
    public KeyStyle getKeyStyle(){
        String key="key";
        KeyStyle style = mKeyStyleCache.get(key);
        if (style == null) {
            LuaValue v = mTable.get(key);
            style = new KeyStyle(v);
            mKeyStyleCache.put(key, style);
        }
        return style;
    }

    /**
     * 获取按键样式对象,带默认值。
     *
     * @param key 样式键名。
     * @param def 默认按键样式对象。
     * @return KeyStyle 对象。
     */
    public KeyStyle getKeyStyle(String key, KeyStyle def) {
        if(TextUtils.isEmpty(key))
            return def;
        KeyStyle style = mKeyStyleCache.get(key);
        if (style == null) {
            LuaValue v = mTable.get(key);
            if (!v.istable())
                return def;
            style = new KeyStyle(v, def);
            mKeyStyleCache.put(key, style);
        }
        return style;
    }


    /**
     * 获取文本大小。
     *
     * @param def 默认大小。
     * @return 文本大小(sp)。
     */
    public float getTextSize(int def) {
        return mTable.get("text_size").optint(def);
    }

    /**
     * 获取文本颜色。
     *
     * @param def 默认颜色值。
     * @return ARGB 颜色值。
     */
    public int getTextColor(int def) {
        return mTable.get("text_color").optint(def);
    }

    /**
     * 获取尺寸值(自动转换为 px)。
     *
     * @param key 尺寸键名。
     * @param def 默认值(dp)。
     * @return 尺寸值(px)。
     */
    public int getSize(String key, int def) {
        return dp2px(mTable.get(key).optint(def));
    }

    /**
     * 获取背景 Drawable。
     *
     * @param def 默认颜色值。
     * @return Drawable 对象。
     */
    public Drawable getBackground(int def) {
        return getDrawable("background", def);
    }

    /**
     * 获取背景颜色。
     *
     * @param def 默认颜色值。
     * @return ARGB 颜色值。
     */
    public int getBackgroundColor(int def) {
        return getColor("background", def);
    }

    /**
     * 获取浮点数值。
     *
     * @param key 键名。
     * @param def 默认值。
     * @return 浮点数值。
     */
    public float getFloat(String key, float def) {
        return (float) mTable.get(key).optdouble(def);
    }

    /**
     * 获取高度值。
     *
     * @param def 默认高度(dp)。
     * @return 高度值(px)。
     */
    public int getHeight(int def) {
        return getSize("height", def);
    }

    /**
     * 检查是否存在指定键。
     *
     * @param key 键名。
     * @return 是否存在。
     */
    public boolean hasKey(String key) {
        return !mTable.get(key).isnil();
    }

    /**
     * 获取对齐方式。
     * 支持字符串解析,如 "center|bottom"。
     *
     * @param def 默认对齐方式。
     * @return Gravity 常量。
     */
    public int getGravity(int def) {
        return parse(getString("gravity", ""), def);
    }

    /**
     * 获取字符串值。
     *
     * @param key 键名。
     * @return 字符串值。
     */
    public String getString(String key) {
        return mTable.get(key).optjstring("");
    }

    /**
     * 获取字符串值,带默认值。
     *
     * @param key 键名。
     * @param def 默认值。
     * @return 字符串值。
     */
    public String getString(String key, String def) {
        return mTable.get(key).optjstring(def);
    }

    // 建立字符串到常量值的映射表
    private static final Map<String, Integer> GRAVITY_MAP = new HashMap<>();

    static {
        GRAVITY_MAP.put("top", Gravity.TOP);
        GRAVITY_MAP.put("bottom", Gravity.BOTTOM);
        GRAVITY_MAP.put("left", Gravity.LEFT);
        GRAVITY_MAP.put("right", Gravity.RIGHT);
        GRAVITY_MAP.put("center", Gravity.CENTER);
        GRAVITY_MAP.put("center_vertical", Gravity.CENTER_VERTICAL);
        GRAVITY_MAP.put("center_horizontal", Gravity.CENTER_HORIZONTAL);
        GRAVITY_MAP.put("start", Gravity.START);
        GRAVITY_MAP.put("end", Gravity.END);
        // 可以根据需要继续添加 fill, clip_vertical 等
    }

    /**
     * 解析 Gravity 字符串。
     * 支持类似 "center|bottom" 的组合写法。
     *
     * @param gravityStr Gravity 字符串。
     * @param defaultGravity 默认对齐方式。
     * @return Gravity 常量值。
     */
    public static int parse(String gravityStr, int defaultGravity) {
        if (TextUtils.isEmpty(gravityStr)) return defaultGravity;

        int result = 0;
        // 支持类似 "center|bottom" 的写法
        String[] parts = gravityStr.toLowerCase().split("\\|");

        for (String part : parts) {
            String key = part.trim();
            if (GRAVITY_MAP.containsKey(key)) {
                result |= GRAVITY_MAP.get(key); // 按位或运算
            }
        }

        return result == 0 ? defaultGravity : result;
    }

    /**
     * 获取 Lua 值。
     *
     * @param key 键名。
     * @return LuaValue 对象。
     */
    public LuaValue get(String key) {
        return mTable.get(key);
    }

    /**
     * 获取字体配置。
     * 支持单个字体文件路径或字体列表(Android Q+)。
     *
     * @param key 字体键名。
     * @return Typeface 对象。
     */
    public Typeface getFont(String key) {
        LuaValue n = get(key);
        if (n.isnil()) {
            // 如果当前样式未配置,则从键盘样式中查找
            n=ThemeManager.getStyle().getStyle("keyboard").get(key);
            if(n.isnil())
                return Typeface.DEFAULT;
        }
        if (n.istable()) {
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    // Android Q+ 支持自定义字体回退链
                    ArrayList<FontFamily> lt = new ArrayList<>();
                    for (int i = 1; i <= n.length(); i++) {
                        try {
                            File f = new File(Config.getFontPath(n.get(i).optjstring("")));
                            if(f.exists())
                                lt.add(new FontFamily.Builder(new Font.Builder(f).build()).build());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (lt.isEmpty())
                        return Typeface.DEFAULT;
                    // 构建字体回退链
                    Typeface.CustomFallbackBuilder tf = new Typeface.CustomFallbackBuilder(lt.get(0));
                    for (int i = 1; i < lt.size(); i++) {
                        tf.addCustomFallback(lt.get(i));
                    }
                    return tf.build();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 单个字体文件
        String name = n.optjstring("");
        if (name != null) {
            try {
                File f = new File(Config.getFontPath(name));
                if (f.exists()) return Typeface.createFromFile(f);
            } catch (Exception e) {
                  e.printStackTrace();
            }
        }
        return Typeface.DEFAULT;
    }

    /**
     * 获取整数值。
     *
     * @param s 键名。
     * @return 整数值,失败返回 0。
     */
    public int getInt(String s) {
        LuaValue i = get(s);
        if(i.isint())
            return i.toint();
        return 0;
    }

    /**
     * 获取整数值,带默认值。
     *
     * @param s 键名。
     * @param def 默认值。
     * @return 整数值。
     */
    public int getInt(String s,int def) {
        LuaValue i = get(s);
        if(i.isint())
            return i.toint();
        return def;
    }


    /**
     * 获取布尔值。
     *
     * @param s 键名。
     * @return 布尔值,默认 false。
     */
    public boolean getBoolean(String s) {
        return get(s).optboolean(false);
    }

    /**
     * 获取布尔值,带默认值。
     *
     * @param s 键名。
     * @param def 默认值。
     * @return 布尔值。
     */
    public boolean getBoolean(String s,boolean def) {
        return get(s).optboolean(def);
    }

    /**
     * 获取 Lua 表长度。
     *
     * @return 表长度。
     */
    public int getLength() {
        return mTable.length();
    }
}
