/*
 * SPDX-FileCopyrightText: 2015 - 2026 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;

import com.androlua.LuaBitmapDrawable;
import com.androlua.LuaContext;
import com.osfans.trime.Config;
import com.osfans.trime.TrimeService;
import com.osfans.trime.core.DataManager;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;

/**
 * 背景资源管理工具类,负责加载和管理键盘、候选词栏等 UI 组件的背景图片。
 * 支持竖屏和横屏两种方向的背景图片,以及主题包和用户自定义背景。
 */
public class BackUtil {
    // 缓存竖屏背景图片路径(key: 背景名称, value: 文件绝对路径)
    private static final HashMap<String, String> cache = new HashMap<>();
    // 缓存横屏背景图片路径
    private static final HashMap<String, String> cache_land = new HashMap<>();
    // 缓存按数字索引的背景图片(用于动态切换背景)
    private static final HashMap<Integer, String> icache = new HashMap<>();
    // 缓存按数字索引的背景图片2(备用)
    private static final HashMap<Integer, String> icache2 = new HashMap<>();
    // 缓存颜色值(key: 颜色键名, value: ARGB 颜色值)
    private static final HashMap<String, Integer> colors = new HashMap<>();
    // 缓存像素值(key: 尺寸键名, value: 像素值)
    private static final HashMap<String, Float> pixels = new HashMap<>();

    /**
     * 重置并重新加载所有背景资源。
     * 从用户数据目录、主题包目录和当前主题目录中扫描背景图片文件。
     *
     * @param context Android 上下文对象。
     */
    public static void reset(Context context) {
        // 清空所有缓存
        cache.clear();
        cache_land.clear();
        icache.clear();
        icache2.clear();
        colors.clear();
        pixels.clear();
        // 获取用户数据目录
        String userDataDir = DataManager.getUserDataDir().getAbsolutePath();

        // ==================== 第一步: 加载 backgrounds 目录下的背景图片 ====================
        File dir = new File(userDataDir, "backgrounds");
        if (dir.exists()) {
            File[] fs = dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isFile();
                }

            });
            if (fs != null) {
                for (File ff : fs) {
                    String f = ff.getName();
                    f = f.toLowerCase();
                    // 根据文件名前缀分类存储到不同的缓存键
                    if (f.startsWith("keyboard.")) {
                        cache.put("keyboard", ff.getAbsolutePath());
                    } else if (f.startsWith("popup_keyboard.")) {
                        cache.put("popup_keyboard", ff.getAbsolutePath());
                    } else if (f.startsWith("key.")) {
                        cache.put("key", ff.getAbsolutePath());
                    } else if (f.startsWith("background.")) {
                        cache.put("background", ff.getAbsolutePath());
                    } else if (f.startsWith("candidate.")) {
                        cache.put("candidate", ff.getAbsolutePath());
                    } else if (f.startsWith("composition.")) {
                        cache.put("composition", ff.getAbsolutePath());
                    }
                    // 同时以完整文件名作为键存储
                    cache.put(f, ff.getAbsolutePath());

                    // 加载横屏图片(文件名包含 _land 后缀)
                    if (f.startsWith("keyboard_land.")) {
                        cache_land.put("keyboard", ff.getAbsolutePath());
                    } else if (f.startsWith("popup_keyboard_land.")) {
                        cache_land.put("popup_keyboard", ff.getAbsolutePath());
                    } else if (f.startsWith("key_land.")) {
                        cache_land.put("key", ff.getAbsolutePath());
                    } else if (f.startsWith("background_land.")) {
                        cache_land.put("background", ff.getAbsolutePath());
                    } else if (f.startsWith("candidate_land.")) {
                        cache_land.put("candidate", ff.getAbsolutePath());
                    } else if (f.startsWith("composition_land.")) {
                        cache_land.put("composition", ff.getAbsolutePath());
                    } else if (f.contains("_land.")) {
                        // 通用横屏图片:将 _land. 替换为 . 后存入横屏缓存
                        cache_land.put(f.replace("_land.", "."), ff.getAbsolutePath());
                    }
                }
            }
        }

        // ==================== 第二步: 加载主题包目录下的背景图片 ====================
        String pkg = Function.getPref(context).getString("background_package", "none");
        if (!pkg.equals("none")) {
            dir = new File(new File(userDataDir, "backgrounds"), pkg);
            if (dir.exists()) {
                File[] fs = dir.listFiles();
                if (fs != null) {
                    for (File ff : fs) {
                        String f = ff.getName();
                        String n = f;
                        try {
                            // 尝试将文件名解析为数字索引(用于动态切换背景)
                            int i = f.indexOf(".");
                            if (i > 0)
                                n = f.substring(0, i);
                            i = Integer.valueOf(n);
                            icache.put(i, ff.getAbsolutePath());
                        } catch (Exception e) {
                            // 如果解析失败,则忽略
                        }
                        f = f.toLowerCase();
                        // 按文件名前缀分类存储(与第一步相同逻辑)
                        if (f.startsWith("keyboard.")) {
                            cache.put("keyboard", ff.getAbsolutePath());
                        } else if (f.startsWith("popup_keyboard.")) {
                            cache.put("popup_keyboard", ff.getAbsolutePath());
                        } else if (f.startsWith("key.")) {
                            cache.put("key", ff.getAbsolutePath());
                        } else if (f.startsWith("background.")) {
                            cache.put("background", ff.getAbsolutePath());
                        } else if (f.startsWith("candidate.")) {
                            cache.put("candidate", ff.getAbsolutePath());
                        } else if (f.startsWith("composition.")) {
                            cache.put("composition", ff.getAbsolutePath());
                        }
                        cache.put(n, ff.getAbsolutePath());
                        cache.put(f, ff.getAbsolutePath());
                        // 加载横屏图片
                        if (f.startsWith("keyboard_land.")) {
                            cache_land.put("keyboard", ff.getAbsolutePath());
                        } else if (f.startsWith("popup_keyboard_land.")) {
                            cache_land.put("popup_keyboard", ff.getAbsolutePath());
                        } else if (f.startsWith("key_land.")) {
                            cache_land.put("key", ff.getAbsolutePath());
                        } else if (f.startsWith("background_land.")) {
                            cache_land.put("background", ff.getAbsolutePath());
                        } else if (f.startsWith("candidate_land.")) {
                            cache_land.put("candidate", ff.getAbsolutePath());
                        } else if (f.startsWith("composition_land.")) {
                            cache_land.put("composition", ff.getAbsolutePath());
                        } else if (f.contains("_land.")) {
                            cache_land.put(f.replace("_land.", "."), ff.getAbsolutePath());
                        }
                    }
                }
            }
        }
        // ==================== 第三步: 加载当前主题目录下的背景图片 ====================
        dir = new File(userDataDir, Config.getTheme());
        if (dir.exists()) {
            File[] fs = dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isFile();
                }

            });
            if (fs != null) {
                for (File ff : fs) {
                    String f = ff.getName();
                    f = f.toLowerCase();
                    // 按文件名前缀分类存储(与前两步相同逻辑)
                    if (f.startsWith("keyboard.")) {
                        cache.put("keyboard", ff.getAbsolutePath());
                    } else if (f.startsWith("popup_keyboard.")) {
                        cache.put("popup_keyboard", ff.getAbsolutePath());
                    } else if (f.startsWith("key.")) {
                        cache.put("key", ff.getAbsolutePath());
                    } else if (f.startsWith("background.")) {
                        cache.put("background", ff.getAbsolutePath());
                    } else if (f.startsWith("candidate.")) {
                        cache.put("candidate", ff.getAbsolutePath());
                    } else if (f.startsWith("composition.")) {
                        cache.put("composition", ff.getAbsolutePath());
                    }
                    cache.put(f, ff.getAbsolutePath());

                    // 加载横屏图片
                    if (f.startsWith("keyboard_land.")) {
                        cache_land.put("keyboard", ff.getAbsolutePath());
                    } else if (f.startsWith("popup_keyboard_land.")) {
                        cache_land.put("popup_keyboard", ff.getAbsolutePath());
                    } else if (f.startsWith("key_land.")) {
                        cache_land.put("key", ff.getAbsolutePath());
                    } else if (f.startsWith("background_land.")) {
                        cache_land.put("background", ff.getAbsolutePath());
                    } else if (f.startsWith("candidate_land.")) {
                        cache_land.put("candidate", ff.getAbsolutePath());
                    } else if (f.startsWith("composition_land.")) {
                        cache_land.put("composition", ff.getAbsolutePath());
                    } else if (f.contains("_land.")) {
                        cache_land.put(f.replace("_land.", "."), ff.getAbsolutePath());
                    }
                }
            }
        }

        // ==================== 第四步: 加载用户自定义颜色和尺寸配置 ====================
        SharedPreferences pref = Function.getPref(context);
        // 如果未启用自定义颜色,则直接返回
        if (!pref.getBoolean("custom_color", false))
            return;
        // 定义所有需要缓存的颜色键名
        String[] color_keys = new String[]{
                "background_color",
                "keyboard_back_color",
                "candidate_back_color",
                "composition_back_color",
                "navigation_bar_color",

                "hilited_on_key_text_color",
                "on_key_text_color",

                "hilited_off_key_text_color",
                "off_key_text_color",

                "hilited_key_text_color",
                "key_text_color",

                "preview_text_color",
                "preview_back_color",

                "hilited_key_symbol_color",
                "key_symbol_color",

                "hilited_on_key_back_color",
                "on_key_back_color",

                "hilited_off_key_back_color",
                "off_key_back_color",

                "hilited_key_back_color",
                "key_back_color",

                "hilited_text_color",
                "text_color",

                "hilited_back_color",
                "back_color",

                "candidate_separator_color",

                "hilited_comment_text_color",
                "comment_text_color",

                "hilited_candidate_back_color",

                "hilited_candidate_text_color",
                "candidate_text_color",

                "hilited_label_color",
                "label_color",
                "text_back_color",

                "shadow_color",
                "hilited_key_border_color",
                "key_border_color",
                "hilited_candidate_border_color",
                "candidate_border_color",
                "border_color"
        };

        // 将所有颜色值存入缓存
        for (String key : color_keys) {
            colors.put(key, pref.getInt(key, 0));
        }

        // 定义所有需要缓存的尺寸和间距键名
        String[] boards = new String[]{
                "hilited_candidate_border",
                "candidate_border",
                "key_border",
                "layout/border",
                "hilited_candidate_round_corner",
                "candidate_round_corner",
                "key_round_corner",
                "round_corner",
                "layout/round_corner",

                "vertical_gap",
                "horizontal_gap",

                "candidate_text_size",
                "comment_text_size",
                "text_size",
                "key_text_size",
                "key_long_text_size",
                "label_text_size",

        };
        // 根据键名后缀类型,以不同比例转换像素值
        for (String key : boards) {
            if (key.endsWith("_size")) {
                // 字体大小:直接使用整数值
                int f = pref.getInt(key, 0);
                if (f > 0) {
                    pixels.put(key, (float) f);
                }
            } else if (key.endsWith("_gap"))
                // 间距:除以20转换为像素
                pixels.put(key, (float) pref.getInt(key, 0) / 20);
            else if (key.endsWith("_corner"))
                // 圆角:直接使用整数值
                pixels.put(key, (float) pref.getInt(key, 0));
            else
                // 其他(如边框):除以10转换为像素
                pixels.put(key, (float) pref.getInt(key, 0) / 10);
        }
    }

    /**
     * 获取背景图片资源。
     * 优先返回横屏图片(如果当前是横屏模式且存在横屏图片),否则返回竖屏图片。
     *
     * @param context Android 上下文对象。
     * @param name 背景名称(不区分大小写)。
     * @return LuaBitmapDrawable 对象,如果未找到则返回 null。
     */
    public static LuaBitmapDrawable get(Context context, String name) {
        name = name.toLowerCase();
        // 如果是横屏模式且存在横屏图片,则优先使用横屏图片
        if(TrimeService.getInstance()!=null&& TrimeService.getInstance().isLandscape()&&cache_land.containsKey(name)){
            return new LuaBitmapDrawable(context, cache_land.get(name));
        }
        // 尝试从竖屏缓存中获取
        if (cache.containsKey(name))
            return new LuaBitmapDrawable(context, cache.get(name));
        // 如果键名以 _color 结尾,则去掉后缀后重试(兼容旧版主题)
        if (name.endsWith("_color")) {
            name = name.substring(0, name.length() - 6);
            return get(context, name);
        }
        return null;
    }

    /**
     * 根据数字索引获取背景图片。
     * 用于动态切换背景场景。
     *
     * @param context Lua 上下文对象。
     * @param name 背景索引号。
     * @return Drawable 对象,如果未找到则返回 null。
     */
    public static Drawable get(LuaContext context, int name) {
        if (icache.containsKey(name))
            return new LuaBitmapDrawable(context, icache.get(name));
        return null;
    }

    /**
     * 获取颜色值。
     *
     * @param key 颜色键名。
     * @return ARGB 颜色值,如果未找到则返回 0。
     */
    public static int get(String key) {
        if (colors.containsKey(key))
            return colors.get(key);
        return 0;
    }

    /**
     * 获取颜色值(可空版本)。
     *
     * @param key 颜色键名。
     * @return ARGB 颜色值,如果未找到则返回 null。
     */
    public static Integer getColor(String key) {
        if (colors.containsKey(key))
            return colors.get(key);
        return null;
    }

    /**
     * 获取像素值(尺寸、间距、圆角等)。
     *
     * @param key 尺寸键名。
     * @return 像素值,如果未找到则返回 null。
     */
    public static Float getPixel(String key) {
        if (pixels.containsKey(key))
            return pixels.get(key);
        return null;
    }
}
