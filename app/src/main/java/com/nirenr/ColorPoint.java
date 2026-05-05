package com.nirenr;

import android.graphics.Point;

/**
 * 颜色点类。
 * 表示带有颜色值和容差的屏幕坐标点,用于图像识别和匹配。
 */
public class ColorPoint {
    /** 红色分量(0-255) */
    public int red;
    /** 绿色分量(0-255) */
    public int green;
    /** 蓝色分量(0-255) */
    public int blue;
    /** X 坐标 */
    public int x;
    /** Y 坐标 */
    public int y;
    /** 颜色容差(允许的颜色偏差范围) */
    public int offset;

    /**
     * 从 Point 和 Color 对象构造颜色点。
     *
     * @param p 坐标点。
     * @param color 颜色值。
     * @param o 颜色容差。
     */
    public ColorPoint(Point p,Color color,int o){
        this(p.x,p.y,color.red,color.green,color.blue,o);
    }

    /**
     * 从坐标和 RGB 分量构造颜色点。
     *
     * @param x X 坐标。
     * @param y Y 坐标。
     * @param r 红色分量。
     * @param g 绿色分量。
     * @param b 蓝色分量。
     * @param o 颜色容差。
     */
    public ColorPoint(int x, int y, int r, int g, int b, int o) {
        this.x = x;
        this.y = y;
        this.red = r;
        this.green = g;
        this.blue = b;
        this.offset = o;
    }

    /**
     * 从整数数组构造颜色点。
     * 数组格式: [x, y, red, green, blue, offset]
     *
     * @param arg 包含颜色和坐标信息的整数数组。
     */
    public ColorPoint(int[] arg) {
        x = arg[0];
        y = arg[1];
        red = arg[2];
        green = arg[3];
        blue = arg[4];
        offset = arg[5];
    }

    /**
     * 检查像素是否匹配(无偏移)。
     *
     * @param pixels 像素数组。
     * @return true 表示颜色在容差范围内匹配。
     */
    public boolean check(int[][] pixels) {
        return check(pixels, 0, 0);
    }

    /**
     * 检查指定位置的像素是否匹配。
     * 根据容差判断颜色是否在允许范围内。
     *
     * @param pixels 像素数组。
     * @param x X 方向偏移量。
     * @param y Y 方向偏移量。
     * @return true 表示颜色在容差范围内匹配。
     */
    public boolean check(int[][] pixels, int x, int y) {
        int r1 = red - offset; // 红色下限
        int r2 = red + offset; // 红色上限
        int g1 = green - offset; // 绿色下限
        int g2 = green + offset; // 绿色上限
        int b1 = blue - offset; // 蓝色下限
        int b2 = blue + offset; // 蓝色上限
        int color = pixels[this.y + y][this.x + x]; // 获取目标像素
        int r = color << 8 >>> 24; // 提取红色分量
        int g = color << 16 >>> 24; // 提取绿色分量
        int b = color << 24 >>> 24; // 提取蓝色分量
        if (r >= r1 && r <= r2 && g >= g1 && g <= g2 && b >= b1 && b <= b2)
            return true; // 颜色在容差范围内
        return false;
    }
}