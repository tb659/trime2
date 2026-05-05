package com.nirenr;


/**
 * 点类。
 * 表示二维坐标点,包含 x、y 坐标和时间戳 t。
 */
public class Point {
    /** X 坐标 */
    public int x;
    /** Y 坐标 */
    public int y;
    /** 时间戳(用于触摸事件等) */
    public int t;



    /**
     * 构造带时间戳的点。
     *
     * @param x X 坐标。
     * @param y Y 坐标。
     * @param t 时间戳。
     */
    public Point(int x, int y,int t) {
        this.x = x;
        this.y = y;
        this.t = t;
    }
    
    /**
     * 构造点。
     *
     * @param x X 坐标。
     * @param y Y 坐标。
     */
    public Point(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * 从另一个点复制构造。
     *
     * @param src 源点。
     */
    public Point(Point src) {
        this.x = src.x;
        this.y = src.y;
    }

    /**
     * 设置点的坐标。
     *
     * @param x X 坐标。
     * @param y Y 坐标。
     */
    public void set(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * 取反点的坐标。
     * 将 x 和 y 坐标都变为负值。
     */
    public final void negate() {
        x = -x;
        y = -y;
    }

    /**
     * 偏移点的坐标。
     *
     * @param dx X 方向偏移量。
     * @param dy Y 方向偏移量。
     */
    public final void offset(int dx, int dy) {
        x += dx;
        y += dy;
    }

    /**
     * 检查坐标是否等于指定值。
     *
     * @param x X 坐标。
     * @param y Y 坐标。
     * @return true 表示坐标相等。
     */
    public final boolean equals(int x, int y) {
        return this.x == x && this.y == y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Point point = (Point) o;

        if (x != point.x) return false;
        if (y != point.y) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        return result;
    }

    @Override
    public String toString() {
        return "Point(" + x + ", " + y + ": " + t +")";
    }

}
