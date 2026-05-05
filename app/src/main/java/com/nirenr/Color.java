package com.nirenr;

/**
 * 颜色类。
 * 表示 RGB 颜色值,支持从整数颜色值和 RGB 分量构造。
 */
public class Color {
    /** 红色分量(0-255) */
    public int red;
    /** 绿色分量(0-255) */
    public int green;
    /** 蓝色分量(0-255) */
    public int blue;

    /**
     * 从整数颜色值构造颜色对象。
     *
     * @param color ARGB 格式的整数颜色值。
     */
    public Color(int color) {
        red = color << 8 >>> 24; // 提取红色分量
        green = color << 16 >>> 24; // 提取绿色分量
        blue = color << 24 >>> 24; // 提取蓝色分量
    }

    /**
     * 从 RGB 分量构造颜色对象。
     *
     * @param r 红色分量(0-255)。
     * @param g 绿色分量(0-255)。
     * @param b 蓝色分量(0-255)。
     */
    public Color(int r, int g, int b) {
        red = r;
        green = g;
        blue = b;
    }

    /**
     * 获取整数格式的颜色值。
     *
     * @return ARGB 格式的整数颜色值(Alpha 固定为 0xFF)。
     */
    public int getInt() {
        return 0xFF000000|red<<16|green<<8|blue; // 组合为 ARGB 格式
    }

    @Override
    public String toString() {
        return "Color(" + red + ", " + green + ", " + blue + ")";
    }

}
