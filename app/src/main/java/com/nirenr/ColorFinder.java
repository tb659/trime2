package com.nirenr;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

import java.util.ArrayList;


/**
 * 颜色查找器类。
 * 用于在位图中查找特定颜色的像素位置,支持精确匹配和容差匹配,
 * 以及基于多个颜色点的复杂模式匹配。
 */

public class ColorFinder {

    /** 位图宽度 */
    private int mWidth;
    /** 位图高度 */
    private int mHeight;
    /** 二维像素数组 [x][y] */
    private int[][] mPixels;
    /** HSV 亮度值数组 [x][y] */
    private float[][] mValues;
    /** 平均亮度值 */
    private float mValue;

    /**
     * 从文件路径构造颜色查找器。
     *
     * @param bitmap 位图文件路径。
     */
    public ColorFinder(String bitmap) {
        init(BitmapFactory.decodeFile(bitmap)); // 解码文件并初始化
    }

    /**
     * 从 Bitmap 对象构造颜色查找器。
     *
     * @param bitmap 位图对象。
     */
    public ColorFinder(Bitmap bitmap) {
        init(bitmap); // 初始化
    }

    /**
     * 初始化位图像素数据。
     * 将一维像素数组转换为二维数组 [x][y] 格式,便于按坐标访问。
     *
     * @param bitmap 要初始化的位图对象。
     */
    private void init(Bitmap bitmap) {
        mWidth = bitmap.getWidth(); // 获取宽度
        mHeight = bitmap.getHeight(); // 获取高度
        int[] pixels = new int[mWidth * mHeight];
        bitmap.getPixels(pixels, 0, mWidth, 0, 0, mWidth, mHeight); // 获取所有像素
        mPixels = new int[mWidth][mHeight]; // 创建二维数组
        for (int h = 0; h < mHeight; h++) {
            for (int w = 0; w < mWidth; w++) {
                mPixels[w][h] = pixels[h * mWidth + w]; // 转换为一维到二维的映射
            }
        }
    }

    /**
     * 获取像素数组。
     *
     * @return 二维像素数组 [x][y]。
     */
    public int[][] getPixels() {
        return mPixels;
    }

    /**
     * 查找指定颜色的像素位置(全图搜索)。
     *
     * @param color ARGB 颜色值。
     * @return 找到的第一个匹配点的坐标,未找到返回 (-1, -1)。
     */
    public Point find(int color) {
        for (int h = 0; h < mHeight; h++) {
            for (int w = 0; w < mWidth; w++) {
                if (mPixels[w][h] == color)
                    return new Point(w, h);
            }
        }
        return new Point(-1, -1);
    }

    /**
     * 在指定区域内查找指定颜色的像素位置。
     *
     * @param x 区域左上角 X 坐标。
     * @param y 区域左上角 Y 坐标。
     * @param x2 区域右下角 X 坐标。
     * @param y2 区域右下角 Y 坐标。
     * @param color ARGB 颜色值。
     * @return 找到的第一个匹配点的坐标,未找到返回 (-1, -1)。
     */
    public Point find(int x, int y, int x2, int y2, int color) {
        for (int h = y; h < y2; h++) {
            for (int w = x; w < x2; w++) {
                if (mPixels[w][h] == color)
                    return new Point(w, h);
            }
        }
        return new Point(-1, -1);
    }

    /**
     * 查找指定 Color 对象的像素位置(全图搜索)。
     *
     * @param color Color 对象。
     * @return 找到的第一个匹配点的坐标,未找到返回 (-1, -1)。
     */
    public Point find(Color color) {
        return find(color.red, color.green, color.blue); // 调用 RGB 分量版本
    }

    /**
     * 查找指定 RGB 分量的像素位置(全图搜索)。
     *
     * @param red 红色分量。
     * @param green 绿色分量。
     * @param blue 蓝色分量。
     * @return 找到的第一个匹配点的坐标,未找到返回 (-1, -1)。
     */
    public Point find(int red, int green, int blue) {
        for (int h = 0; h < mHeight; h++) {
            for (int w = 0; w < mWidth; w++) {
                int color = mPixels[w][h];
                int r = color << 8 >>> 24;
                int g = color << 16 >>> 24;
                int b = color << 24 >>> 24;
                if (r == red && g == green && b == blue)
                    return new Point(w, h);
            }
        }
        return new Point(-1, -1);
    }

    /**
     * 在指定区域内查找指定 Color 对象的像素位置。
     *
     * @param p1 区域左上角坐标点。
     * @param p2 区域右下角坐标点。
     * @param color Color 对象。
     * @return 找到的第一个匹配点的坐标,未找到返回 (-1, -1)。
     */
    public Point find(Point p1, Point p2, Color color) {
        return find(p1.x, p1.y, p2.x, p2.y, color.red, color.green, color.blue); // 调用坐标版本
    }

    /**
     * 在指定区域内查找指定 RGB 分量的像素位置。
     *
     * @param x 区域左上角 X 坐标。
     * @param y 区域左上角 Y 坐标。
     * @param x2 区域右下角 X 坐标。
     * @param y2 区域右下角 Y 坐标。
     * @param red 红色分量。
     * @param green 绿色分量。
     * @param blue 蓝色分量。
     * @return 找到的第一个匹配点的坐标,未找到返回 (-1, -1)。
     */
    public Point find(int x, int y, int x2, int y2, int red, int green, int blue) {
        for (int h = y; h < y2; h++) {
            for (int w = x; w < x2; w++) {
                int color = mPixels[w][h];
                int r = color << 8 >>> 24;
                int g = color << 16 >>> 24;
                int b = color << 24 >>> 24;
                if (r == red && g == green && b == blue)
                    return new Point(w, h);
            }
        }
        return new Point(-1, -1);
    }

    /**
     * 查找指定 Color 对象且带容差的像素位置(全图搜索)。
     *
     * @param color Color 对象。
     * @param offset 颜色容差值。
     * @return 找到的第一个匹配点的坐标,未找到返回 (-1, -1)。
     */
    public Point find(Color color, int offset) {
        return find(color.red, color.green, color.blue, offset); // 调用 RGB 分量版本
    }

    /**
     * 查找指定 RGB 分量且带容差的像素位置(全图搜索)。
     * 容差匹配允许颜色在 [value-offset, value+offset] 范围内。
     *
     * @param red 红色分量。
     * @param green 绿色分量。
     * @param blue 蓝色分量。
     * @param offset 颜色容差值。
     * @return 找到的第一个匹配点的坐标,未找到返回 (-1, -1)。
     */
    public Point find(int red, int green, int blue, int offset) {
        int r1 = red - offset; // 红色下限
        int r2 = red + offset; // 红色上限
        int g1 = green - offset; // 绿色下限
        int g2 = green + offset; // 绿色上限
        int b1 = blue - offset; // 蓝色下限
        int b2 = blue + offset; // 蓝色上限

        for (int h = 0; h < mHeight; h++) {
            for (int w = 0; w < mWidth; w++) {
                int color = mPixels[w][h];
                int r = color << 8 >>> 24;
                int g = color << 16 >>> 24;
                int b = color << 24 >>> 24;
                if (r >= r1 && r <= r2 && g >= g1 && g <= g2 && b >= b1 && b <= b2)
                    return new Point(w, h);
            }
        }
        return new Point(-1, -1);
    }

    /**
     * 在指定区域内查找带容差的 Color 对象像素位置。
     *
     * @param p1 区域左上角坐标点。
     * @param p2 区域右下角坐标点。
     * @param color Color 对象。
     * @param offset 颜色容差值。
     * @return 找到的第一个匹配点的坐标,未找到返回 (-1, -1)。
     */
    public Point find(Point p1, Point p2, Color color, int offset) {
        return find(p1.x, p1.y, p2.x, p2.y, color.red, color.green, color.blue, offset); // 调用坐标版本
    }

    /**
     * 在指定区域内查找带容差的 RGB 分量像素位置。
     *
     * @param x 区域左上角 X 坐标。
     * @param y 区域左上角 Y 坐标。
     * @param x2 区域右下角 X 坐标。
     * @param y2 区域右下角 Y 坐标。
     * @param red 红色分量。
     * @param green 绿色分量。
     * @param blue 蓝色分量。
     * @param offset 颜色容差值。
     * @return 找到的第一个匹配点的坐标,未找到返回 (-1, -1)。
     */
    public Point find(int x, int y, int x2, int y2, int red, int green, int blue, int offset) {
        int r1 = red - offset;
        int r2 = red + offset;
        int g1 = green - offset;
        int g2 = green + offset;
        int b1 = blue - offset;
        int b2 = blue + offset;

        for (int h = y; h < y2; h++) {
            for (int w = x; w < x2; w++) {
                int color = mPixels[w][h];
                int r = color << 8 >>> 24;
                int g = color << 16 >>> 24;
                int b = color << 24 >>> 24;
                if (r >= r1 && r <= r2 && g >= g1 && g <= g2 && b >= b1 && b <= b2)
                    return new Point(w, h);
            }
        }
        return new Point(-1, -1);
    }

    /**
     * 在指定区域内查找带容差且基于多个颜色点的像素位置。
     * 先找到基准颜色点,然后验证其他相对颜色点是否匹配。
     *
     * @param x 区域左上角 X 坐标。
     * @param y 区域左上角 Y 坐标。
     * @param x2 区域右下角 X 坐标。
     * @param y2 区域右下角 Y 坐标。
     * @param red 基准红色分量。
     * @param green 基准绿色分量。
     * @param blue 基准蓝色分量。
     * @param offset 颜色容差值。
     * @param p 相对颜色点数组,每个点包含相对坐标和颜色信息。
     * @return 找到的第一个匹配点的坐标,未找到返回 (-1, -1)。
     */
    public Point find(int x, int y, int x2, int y2, int red, int green, int blue, int offset, int[][] p) {
        ColorPoint[] cp = new ColorPoint[p.length];
        for (int i = 0; i < p.length; i++) {
            cp[i] = new ColorPoint(p[i]); // 转换为 ColorPoint 对象
        }
        return find(x, y, x2, y2, red, green, blue, offset, cp); // 调用 ColorPoint 版本
    }

    /**
     * 在指定区域内查找带容差且基于多个 ColorPoint 的像素位置。
     * 先找到基准颜色点,然后验证所有相对颜色点是否都匹配。
     *
     * @param x 区域左上角 X 坐标。
     * @param y 区域左上角 Y 坐标。
     * @param x2 区域右下角 X 坐标。
     * @param y2 区域右下角 Y 坐标。
     * @param red 基准红色分量。
     * @param green 基准绿色分量。
     * @param blue 基准蓝色分量。
     * @param offset 颜色容差值。
     * @param cp ColorPoint 数组,包含相对坐标和颜色信息。
     * @return 找到的第一个匹配点的坐标,未找到返回 (-1, -1)。
     */
    public Point find(int x, int y, int x2, int y2, int red, int green, int blue, int offset, ColorPoint[] cp) {
        int r1 = red - offset;
        int r2 = red + offset;
        int g1 = green - offset;
        int g2 = green + offset;
        int b1 = blue - offset;
        int b2 = blue + offset;

        for (int h = y; h < y2; h++) {
            for (int w = x; w < x2; w++) {
                int color = mPixels[w][h];
                int r = color << 8 >>> 24;
                int g = color << 16 >>> 24;
                int b = color << 24 >>> 24;
                if (r >= r1 && r <= r2 && g >= g1 && g <= g2 && b >= b1 && b <= b2) {
                    boolean ok = true;
                    for (ColorPoint c : cp) {
                        if (!c.check(mPixels, x, y)) {
                            ok = false;
                            break;
                        }
                    }
                    if (ok)
                        return new Point(w, h);
                }
            }
        }
        return new Point(-1, -1);
    }

    /**
     * 查找垂直线条(默认参数)。
     *
     * @param o 偏移量参数,用于调整搜索区域和检测参数。
     * @return 找到的矩形区域列表。
     */
    public ArrayList<Rect> findLine(int o) {
        return findLine(mWidth / 2, 10, mWidth - 10, mHeight - o * 16, 0.5f, o * 8, o * 4, o);
    }

    /**
     * 查找垂直线条(自定义亮度阈值)。
     *
     * @param n 亮度阈值系数(0-1),值越小越容易检测到暗色线条。
     * @param o 偏移量参数。
     * @return 找到的矩形区域列表。
     */
    public ArrayList<Rect> findLine(float n, int o) {
        return findLine(mWidth / 2, 10, mWidth - 10, mHeight - o * 16, n, o * 8, o * 4, o);
    }

    /**
     * 查找垂直线条(自定义高度和亮度阈值)。
     *
     * @param n 亮度阈值系数。
     * @param h 搜索区域高度偏移。
     * @param o 偏移量参数。
     * @return 找到的矩形区域列表。
     */
    public ArrayList<Rect> findLine(float n, int h, int o) {
        if (mHeight < mWidth)
            return findLine(mWidth / 2, 0, mWidth - 10, mHeight - h, n, h, o * 4, o);
        else
            return findLine(mWidth / 2, mWidth / 3, mWidth - 10, mWidth, n, h, o * 4, o);
    }

    /**
     * 查找垂直线条(自定义高度、宽度和亮度阈值)。
     *
     * @param n 亮度阈值系数。
     * @param h 搜索区域高度偏移。
     * @param w 检测宽度。
     * @param o 偏移量参数。
     * @return 找到的矩形区域列表。
     */
    public ArrayList<Rect> findLine(float n, int h, int w, int o) {
        if (mHeight < mWidth)
            return findLine(mWidth / 2, 0, mWidth - 10, mHeight - h, n, h, w, o);
        else
            return findLine(mWidth / 2, mWidth / 3, mWidth - 10, mWidth, n, h, w, o);
    }

    /**
     * 查找垂直线条的核心方法。
     * 基于 HSV 亮度值检测屏幕上的垂直线条,用于识别分割线等 UI 元素。
     *
     * @param x1 搜索区域左边界 X 坐标。
     * @param y1 搜索区域上边界 Y 坐标。
     * @param x2 搜索区域右边界 X 坐标。
     * @param y2 搜索区域下边界 Y 坐标。
     * @param n 亮度阈值系数,实际阈值为 mValue * n。
     * @param h 最小检测高度。
     * @param w 检测宽度偏移。
     * @param o 步进偏移量。
     * @return 找到的矩形区域列表,每个 Rect 表示一条垂直线。
     */
    public ArrayList<Rect> findLine(int x1, int y1, int x2, int y2, float n, int h, int w, int o) {
        if (mValues == null) {
            // 首次调用时计算所有像素的 HSV 亮度值
            mValues = new float[mWidth][mHeight];
            float[] hsv = new float[3];
            float v = 0;
            for (int y = 0; y < mHeight; y++) {
                for (int x = 0; x < mWidth; x++) {
                    int color1 = mPixels[x][y];
                    android.graphics.Color.colorToHSV(color1, hsv); // 转换为 HSV
                    mValues[x][y] = hsv[2]; // 保存 V(亮度)分量
                    v += hsv[2]; // 累加亮度值
                }
            }
            mValue = v / (mWidth * mHeight); // 计算平均亮度
        }

        int[][] colors = new int[mWidth][mHeight];
        float vv = mValue * n; // 计算实际亮度阈值
        for (int y = 0; y < mHeight; y++) {
            for (int x = 0; x < mWidth; x++) {
                int i = x + mWidth * y;
                if (mValues[x][y] > vv) {
                    colors[x][y] = 1; // 高于阈值为亮色
                } else {
                    colors[x][y] = 0; // 低于阈值为暗色
                }
            }
        }
        ArrayList<Rect> ret = new ArrayList<>();
        for (int x = x1; x < x2; x++) {
            for (int y = y1; y < y2; y++) {
                int l = check(x, y, colors, h, w, o); // 检查当前位置是否有垂直线
                if (l > -1) {
                    x += o; // 跳过已检测区域
                    ret.add(new Rect(x, y, x,x+l)); // 添加找到的线条矩形
                    break;
                }
            }
        }
        /*for (int x = 10; x < x1; x++) {
            for (int y = 10; y < mHeight-h*2; y++) {
                int l = check2(x, y, colors, h, w, o);
                    if (l > -1) {
                        x += o;
                        ret.add(new Rect(x, y, l,2));
                        break;
                    }
            }
        }
        for (int y = 10; y < mHeight-10; y++) {
            for (int x = 10; x < x1; x++) {
                int l = check3(x, y, colors, h, w, o);
                if (l > -1) {
                    y += o;
                    ret.add(new Point(y, l, 3));
                    break;
                } else {
                    l = check4(x, y, colors, h, w, o);
                    if (l > -1) {
                        y += o;
                        ret.add(new Point(y, l, 4));
                        break;
                    }
                }
            }
        }*/

        /*Collections.sort(ret, new Comparator<Rect>() {
            @Override
            public int compare(Rect o1, Rect o2) {
                if (o1.t < o2.t)
                    return -1;
                /*else  if (o1.y > o2.y)
                    return -1;
                else
                    return 1;
            }
        });*/
        return ret;
    }

    /**
     * 检查指定位置是否有垂直线条。
     * 检测模式:左侧亮色,右侧两个偏移位置为暗色。
     *
     * @param x 检测起始 X 坐标。
     * @param y 检测起始 Y 坐标。
     * @param color 二值化后的颜色数组(0=暗,1=亮)。
     * @param h 最小检测高度。
     * @param w 右侧暗色区域的宽度偏移。
     * @param o 左右侧的间距偏移。
     * @return 检测到的线条长度,未找到返回 -1。
     */
    private int check(int x, int y, int[][] color, int h, int w, int o) {
        for (int i = 0; i < mHeight - y - h; i++) {
            // 检查:当前点为亮色,右侧o位置为暗色,右侧o+w位置为暗色
            if (!(color[x][y + i] == 1 && color[x + o][y + i] == 0 && color[x + o + w][y + i] == 0)) {
                if (i > h)
                    return i; // 达到最小高度,返回长度
                else
                    return -1; // 未达到最小高度,不匹配
            }
        }
        return mHeight - y - h; // 到达边界,返回剩余长度
    }

    /**
     * 检查指定位置是否有反向垂直线条(备用方法,未使用)。
     * 检测模式:左侧暗色,右侧两个偏移位置为亮色。
     *
     * @param x 检测起始 X 坐标。
     * @param y 检测起始 Y 坐标。
     * @param color 二值化后的颜色数组。
     * @param h 最小检测高度。
     * @param w 右侧亮色区域的宽度偏移。
     * @param o 左右侧的间距偏移。
     * @return 检测到的线条长度,未找到返回 -1。
     */
    private int check2(int x, int y, int[][] color, int h, int w, int o) {
        for (int i = 0; i < mHeight - y - h; i++) {
            // 检查:当前点为暗色,右侧o位置为亮色,右侧o+w位置为亮色
            if (!(color[x][y + i] == 0 && color[x + o][y + i] == 1 && color[x + o + w][y + i] == 1)) {
                if (i > h)
                    return i;
                else
                    return -1;
            }
        }
        return mHeight - y - h;
    }

    /**
     * 检查指定位置是否有水平线条(备用方法,未使用)。
     * 检测模式:上方亮色,下方两个偏移位置为暗色。
     *
     * @param x 检测起始 X 坐标。
     * @param y 检测起始 Y 坐标。
     * @param color 二值化后的颜色数组。
     * @param h 最小检测宽度。
     * @param w 下方暗色区域的高度偏移。
     * @param o 上下方的间距偏移。
     * @return 检测到的线条长度,未找到返回 -1。
     */
    private int check3(int x, int y, int[][] color, int h, int w, int o) {
        for (int i = 0; i < mWidth - x - h; i++) {
            // 检查:当前点为亮色,下方o位置为暗色,下方o+w位置为暗色
            if (!(color[x + i][y] == 1 && color[x + i][y + o] == 0 && color[x + i][y + o + w] == 0)) {
                if (i > h)
                    return i;
                else
                    return -1;
            }
        }
        return mHeight - y - h;
    }

    /**
     * 检查指定位置是否有反向水平线条(备用方法,未使用)。
     * 检测模式:上方暗色,下方两个偏移位置为亮色。
     *
     * @param x 检测起始 X 坐标。
     * @param y 检测起始 Y 坐标。
     * @param color 二值化后的颜色数组。
     * @param h 最小检测宽度。
     * @param w 下方亮色区域的高度偏移。
     * @param o 上下方的间距偏移。
     * @return 检测到的线条长度,未找到返回 -1。
     */
    private int check4(int x, int y, int[][] color, int h, int w, int o) {
        for (int i = 0; i < mWidth - x - h; i++) {
            // 检查:当前点为暗色,下方o位置为亮色,下方o+w位置为亮色
            if (!(color[x + i][y] == 0 && color[x + i][y + o] == 1 && color[x + i][y + o + w] == 1)) {
                if (i > h)
                    return i;
                else
                    return -1;
            }
        }
        return mHeight - y - h;
    }
}
