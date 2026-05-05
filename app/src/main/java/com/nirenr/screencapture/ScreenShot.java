package com.nirenr.screencapture;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import androidx.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;


import com.androlua.LuaAccessibilityService;
import com.androlua.LuaActivity;
import com.osfans.trime.util.CustomToast;

import java.nio.ByteBuffer;

import static android.content.Context.MEDIA_PROJECTION_SERVICE;

import com.osfans.trime.BuildConfig;


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
/**
 * 屏幕截图工具类。
 * 使用 MediaProjection API 实现屏幕截图功能,支持异步回调和静态方法调用。
 */
public class ScreenShot {


    /** 无障碍服务实例(静态) */
    private static LuaAccessibilityService sService;
    /** 截图监听器(静态) */
    private static ScreenCaptureListener sScreenCaptureListener;
    /** 权限结果数据(静态) */
    private static Intent mResultData = null;

    /**
     * 获取权限结果数据。
     * 如果尚未获得权限,则启动权限请求 Activity。
     *
     * @param mService 无障碍服务实例。
     */
    public static void getResultData(final LuaAccessibilityService mService) {
        if (mService==null)
            return;
        if (mResultData == null) {
            Intent intent = new Intent(mService, ScreenCaptureActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // 新任务栈
            mService.startActivity(intent); // 启动权限请求
        }
    }


    /**
     * 设置权限结果数据。
     * 如果获得权限,则延迟执行截图操作。
     *
     * @param mResultData 权限结果意图数据。
     */
    public static void setResultData(Intent mResultData) {
        if (mResultData == null) {
            if (sService != null)
                CustomToast.show(sService, "未获得权限", Toast.LENGTH_SHORT, true); // 显示错误提示
            if (sScreenCaptureListener != null)
                sScreenCaptureListener.onScreenCaptureError("未获得权限"); // 回调错误
            return;
        }

        ScreenShot.mResultData = mResultData; // 保存权限数据
        if (sService != null) {
            new Handler(sService.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    getScreenCaptureBitmap(sService, sScreenCaptureListener); // 执行截图
                }
            }, 500); // 延迟500ms等待系统准备
        }
        if (sContext != null) {
            new Handler(sContext.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    getScreenshot(sContext, sScreenshotCallback); // 执行截图
                }
            }, 500); // 延迟500ms等待系统准备
        }
    }

    /**
     * 获取屏幕截图位图。
     * 使用 MediaProjection 创建虚拟显示并捕获屏幕内容。
     *
     * @param mService 无障碍服务实例。
     * @param screenCaptureListener 截图完成监听器。
     */
    public static void getScreenCaptureBitmap(final LuaAccessibilityService mService, final ScreenCaptureListener screenCaptureListener) {
        if(mService==null)
            return;
        
        ImageReader mImageReader = null;
        MediaProjection mMediaProjection = null;
        VirtualDisplay mVirtualDisplay = null;
        Image mImage;
        sService = mService;
        sScreenCaptureListener = screenCaptureListener;
        try {
            if (mResultData == null) {
                 Intent intent = new Intent(mService, ScreenCaptureActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                mService.startActivity(intent);
            } else {
                WindowManager mWindowManager = (WindowManager) mService.getSystemService(Context.WINDOW_SERVICE);
                DisplayMetrics metrics = new DisplayMetrics();
                int mScreenDensity;
                int mScreenWidth;
                int mScreenHeight;
                if (mWindowManager != null) {
                    mWindowManager.getDefaultDisplay().getRealMetrics(metrics);
                    mScreenDensity = metrics.densityDpi;
                    mScreenWidth = metrics.widthPixels;
                    mScreenHeight = metrics.heightPixels;
                }else{
                    mScreenHeight = mService.getHeight();
                    mScreenWidth = mService.getWidth();
                    mScreenDensity = mService.getDensity();
                }
                mImageReader = ImageReader.newInstance(mScreenWidth, mScreenHeight, PixelFormat.RGBA_8888, 1); // 创建图像读取器
                
                mMediaProjection = ((MediaProjectionManager) mService.getSystemService(Context.MEDIA_PROJECTION_SERVICE)).getMediaProjection(Activity.RESULT_OK, mResultData); // 获取媒体投影
                
                mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
                        mScreenWidth, mScreenHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        mImageReader.getSurface(), null, null); // 创建虚拟显示
                

                mImage = mImageReader.acquireLatestImage(); // 获取最新图像
                for (int i = 0; i < 40; i++) {
                    try {
                        Thread.sleep(5); // 等待5ms
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mImage = mImageReader.acquireLatestImage(); // 重试获取
                    
                    if (mImage != null)
                        break; // 成功获取则退出循环
                }

                if (mImage == null) {
                    screenCaptureListener.onScreenCaptureError("请重试"); // 获取失败,回调错误
                } else {
                    int width = mImage.getWidth();
                    int height = mImage.getHeight();
                    final Image.Plane[] planes = mImage.getPlanes();
                    final ByteBuffer buffer = planes[0].getBuffer();
                    //每个像素的间距
                    int pixelStride = planes[0].getPixelStride();
                    //总的间距
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * width; // 计算行填充
                    Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_4444); // 创建位图
                    bitmap.copyPixelsFromBuffer(buffer); // 从缓冲区复制像素
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height); // 裁剪多余填充
                    mImage.close(); // 关闭图像
                    screenCaptureListener.onScreenCaptureDone(bitmap); // 回调成功
                }
                sService = null;
                sScreenCaptureListener = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            sScreenCaptureListener.onScreenCaptureError("请重试");
            sService = null;
            sScreenCaptureListener = null;
        } finally {
            if (mVirtualDisplay != null)
                mVirtualDisplay.release();
            if (mImageReader != null) {
                mImageReader.close();
            }
            if (mMediaProjection != null) {
                mMediaProjection.stop();
            }
        }
    }

    /** 服务上下文 */
    private final Context mService;
    /** 屏幕截图位图(静态) */
    public static Bitmap mScreenCaptureBitmap;
    /** 应用名称 */
    public static String appName = "";
    /** 虚拟显示回调 */
    private final VirtualDisplay.Callback mCallback;
    /** 截图监听器 */
    private ScreenCaptureListener mScreenCaptureListener;
    /** 当前图像 */
    private Image mImage;
    /** 单例实例 */
    private static ScreenShot mScreenShot;
    /** 媒体投影 */
    private MediaProjection mMediaProjection;
    /** 虚拟显示 */
    private VirtualDisplay mVirtualDisplay;

    /** 图像读取器 */
    private ImageReader mImageReader;

    /** 屏幕宽度 */
    private int mScreenWidth;
    /** 屏幕高度 */
    private int mScreenHeight;
    /** 屏幕密度 */
    private int mScreenDensity;


    /**
     * 构造函数。
     *
     * @param service 服务上下文。
     * @param callback 虚拟显示回调。
     */
    public ScreenShot(Context service,VirtualDisplay.Callback callback) {
        mService = service;
        mCallback=callback;
        init(); // 初始化屏幕参数
        if (mResultData == null) {
            Intent intent = new Intent(mService, ScreenCaptureActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mService.startActivity(intent); // 请求权限
        }else{
            startVirtual(); // 启动虚拟显示
        }
        //createImageReader();
    }

    /**
     * 初始化屏幕参数。
     * 获取屏幕宽度、高度和密度,并创建图像读取器。
     */
    private void init() {
        WindowManager mWindowManager = (WindowManager) mService.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getRealMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mScreenWidth = metrics.widthPixels;
        mScreenHeight = metrics.heightPixels;
        createImageReader();
    }


    /**
     * 开始截图(带监听器)。
     *
     * @param listener 截图完成监听器。
     */
    public void startScreenShot(ScreenCaptureListener listener) {
        if (mScreenCaptureListener != null)
            return; // 已有监听器,直接返回
        mScreenCaptureListener = listener;
        startScreenShot(); // 调用无参版本
    }

    /**
     * 开始截图。
     * 延迟启动虚拟显示和捕获操作。
     */
    public void startScreenShot() {

        Handler handler1 = new Handler();
        handler1.postDelayed(new Runnable() {
            public void run() {
                //start virtual
                startVirtual(); // 5ms后启动虚拟显示
            }
        }, 5);

        handler1.postDelayed(new Runnable() {
            public void run() {
                //capture the screen
                startCapture(); // 100ms后开始捕获

            }
        }, 100);
    }

    /**
     * 获取截图位图。
     *
     * @return 屏幕截图的 Bitmap 对象。
     */
    public Bitmap getScreenShot() {
        return getCapture();
    }

    /**
     * 创建图像读取器。
     * 配置为 RGBA_8888 格式,缓冲区大小为1。
     */
    private void createImageReader() {
        mImageReader = ImageReader.newInstance(mScreenWidth, mScreenHeight, PixelFormat.RGBA_8888, 1);
    }

    /**
     * 重新调整大小。
     * 停止虚拟显示,关闭图像读取器,重新初始化并启动。
     */
    public void reSize() {
        stopVirtual();
        closeImageReader();
        init();
        startVirtual();
    }

    /**
     * 启动虚拟显示。
     * 如果媒体投影已存在则直接创建虚拟显示,否则先设置媒体投影。
     */
    public void startVirtual() {
        if (mMediaProjection != null) {
            virtualDisplay(); // 已有投影,直接创建虚拟显示
        } else {
            setUpMediaProjection(); // 设置媒体投影
            virtualDisplay(); // 创建虚拟显示
        }
    }

    /**
     * 设置媒体投影。
     * 使用权限结果数据初始化 MediaProjection 对象。
     */
    public void setUpMediaProjection() {
        if (mMediaProjection != null)
            return;
        if (mResultData == null) {
            Intent intent = new Intent(mService, ScreenCaptureActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mService.startActivity(intent);
        } else {
            mMediaProjection = getMediaProjectionManager().getMediaProjection(Activity.RESULT_OK, mResultData);
        }
    }

    /**
     * 获取媒体投影管理器。
     *
     * @return MediaProjectionManager 实例。
     */
    private MediaProjectionManager getMediaProjectionManager() {
        return (MediaProjectionManager) mService.getSystemService(MEDIA_PROJECTION_SERVICE);
    }

    /**
     * 创建虚拟显示。
     * 将屏幕内容镜像到 ImageReader 的 Surface 上。
     */
    private void virtualDisplay() {
        if (mMediaProjection == null)
            setUpMediaProjection();
        if (mMediaProjection == null)
            return;
        if (mVirtualDisplay != null)
            return;
        try {
            //init();
            mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
                    mScreenWidth, mScreenHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mImageReader.getSurface(), mCallback, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 开始捕获屏幕。
     * 从 ImageReader 获取最新图像并异步保存。
     */
    private void startCapture() {
        if (mImage != null)
            return;
        mImage = mImageReader.acquireLatestImage();

        if (mImage == null) {
            if (mScreenCaptureListener != null) {
                mScreenCaptureListener.onScreenCaptureDone(null);
                mScreenCaptureListener = null;
            }
        } else {
            SaveTask mSaveTask = new SaveTask();
            mSaveTask.execute(mImage);
            //AsyncTaskCompat.executeParallel(mSaveTask, image);
        }
    }

    /**
     * 获取捕获的位图。
     * 从 ImageReader 读取像素数据并转换为 Bitmap。
     *
     * @return 屏幕截图的 Bitmap,如果获取失败则返回 null。
     */
    private Bitmap getCapture() {
        if(mImageReader==null)
            return null;
        mImage = mImageReader.acquireLatestImage();

        if (mImage == null) {
            return null;
        } else {
            int width = mImage.getWidth();
            int height = mImage.getHeight();
            final Image.Plane[] planes = mImage.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer();
            //每个像素的间距
            int pixelStride = planes[0].getPixelStride();
            //总的间距
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width; // 计算行填充
            Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888); // 创建位图
            bitmap.copyPixelsFromBuffer(buffer); // 复制像素
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height); // 裁剪多余填充
            mImage.close(); // 关闭图像
            mImage = null;
            return bitmap;
        }
    }


    /**
     * 设置屏幕截图位图(静态)。
     *
     * @param bitmap 要设置的位图对象。
     */
    public void setScreenCaptureBitmap(Bitmap bitmap) {
        mScreenCaptureBitmap = bitmap;
    }


    /**
     * 保存任务异步类。
     * 在后台线程中将 Image 转换为 Bitmap 并回调结果。
     */
    public class SaveTask extends AsyncTask<Image, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Image... params) {

            if (params == null || params.length < 1 || params[0] == null) {

                return null;
            }

            Image image = params[0];

            int width = image.getWidth();
            int height = image.getHeight();
            final Image.Plane[] planes = image.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer();
            //每个像素的间距
            int pixelStride = planes[0].getPixelStride();
            //总的间距
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width; // 计算行填充
            Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888); // 创建位图
            bitmap.copyPixelsFromBuffer(buffer); // 复制像素
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height); // 裁剪多余填充
            image.close(); // 关闭图像
            mImage = null;
            if (mScreenCaptureListener != null) {
                mScreenCaptureListener.onScreenCaptureDone(bitmap);
                mScreenCaptureListener = null;
                return null;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            //预览图片
            if (bitmap != null) {
                setScreenCaptureBitmap(bitmap); // 保存截图
                Log.e("ryze", "获取图片成功");
                //mService.startActivity(PreviewPictureActivity.newIntent(mService));
            }
        }
    }


    /**
     * 停止媒体投影。
     * 释放 MediaProjection 资源。
     */
    private void tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    /**
     * 停止虚拟显示。
     * 释放 VirtualDisplay 资源。
     */
    private void stopVirtual() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        mVirtualDisplay = null;
    }

    /**
     * 关闭图像读取器。
     * 释放 ImageReader 资源。
     */
    private void closeImageReader() {
        if (mImageReader != null)
            mImageReader.close();
        mImageReader = null;
    }


    /**
     * 释放所有资源。
     * 停止虚拟显示、媒体投影和图像读取器,并清空单例引用。
     */
    public void release() {
        stopVirtual(); // 停止虚拟显示
        tearDownMediaProjection(); // 停止媒体投影
        closeImageReader(); // 关闭图像读取器
        mScreenShot=null; // 清空单例
    }

    /** 截图回调(静态) */
    private static ScreenshotCallback sScreenshotCallback;
    /** LuaActivity 上下文(静态) */
     private static LuaActivity sContext;

    /**
     * 获取截图(LuaActivity 版本)。
     * 如果未获得权限则先请求,否则直接执行截图。
     *
     * @param context LuaActivity 上下文。
     * @param callback 截图完成回调。
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static void getScreenshot(LuaActivity context, ScreenshotCallback callback) {
        if (context == null)
            return;

        if (BuildConfig.DEBUG) Log.w("Screenshot", "getScreenshot: " + mResultData);
        if (mResultData == null) {
            sScreenshotCallback = callback; // 保存回调
            sContext = context; // 保存上下文
            getResultData(context); // 请求权限
            return;
        }
        sScreenshotCallback = null;
        sContext = null;
        getScreenshot(context, ((MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)).getMediaProjection(Activity.RESULT_OK, mResultData), callback); // 执行截图
    }


    /**
     * 获取截图(内部方法)。
     * 创建虚拟显示并监听图像可用事件。
     *
     * @param context LuaActivity 上下文。
     * @param mediaProjection 媒体投影对象。
     * @param callback 截图完成回调。
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static void getScreenshot(final LuaActivity context, final MediaProjection mediaProjection, final ScreenshotCallback callback) {
        if (BuildConfig.DEBUG) Log.w("Screenshot", "getScreenshot: " + mediaProjection);
        int width_s = context.getWidth();//自己实现获取
        int height_s = context.getHeight();//自己实现获取
        int densityDpi = context.getDensity();//自己实现获取
        @SuppressLint("WrongConstant") final ImageReader mImageReader = ImageReader.newInstance(width_s, height_s, PixelFormat.RGBA_8888, 2);
        final VirtualDisplay virtualDisplay1 = mediaProjection.createVirtualDisplay("screen-mirror", width_s, height_s, densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mImageReader.getSurface(), null, null);
        mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireLatestImage(); // 获取最新图像
                if (image != null) {
                    final Image.Plane[] planes = image.getPlanes();
                    final ByteBuffer buffer = planes[0].getBuffer();
                    int width = image.getWidth();
                    int height = image.getHeight();
                    if (BuildConfig.DEBUG) Log.w("Screenshot", "onImageAvailable:1 " + width);
                    if (height < 1) {
                        image.close(); // 高度无效,关闭图像
                        return;
                    }
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * width; // 计算行填充
                    int bmpWidth = width + rowPadding / pixelStride;
                    int bmpHeight = height;
                    if (BuildConfig.DEBUG) Log.w("Screenshot", "onImageAvailable:2 " + bmpWidth);
                    Bitmap bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888); // 创建位图
                    bitmap.copyPixelsFromBuffer(buffer); // 复制像素
                    callback.onSuccess(bitmap); // 回调成功
                    bitmap.recycle(); // 回收位图
                    image.close(); // 关闭图像
                    virtualDisplay1.release(); // 释放虚拟显示
                    mediaProjection.stop(); // 停止媒体投影
                }
                reader.close(); // 关闭读取器
            }
        }, null);
    }

    /**
     * 获取权限结果数据(LuaActivity 版本)。
     *
     * @param activity LuaActivity 实例。
     */
    public static void getResultData(final LuaActivity activity) {
        if (activity == null)
            return;
        if (mResultData == null) {
            try {
                Intent intent = new Intent(activity, ScreenCaptureActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY);
                if (BuildConfig.DEBUG)
                    Log.w("Screenshot", "getResultData:ScreenCaptureActivity intent " + intent);
                activity.startActivity(intent);
            } catch (Exception e) {
                if (BuildConfig.DEBUG)
                    e.printStackTrace();
                else if (BuildConfig.DEBUG) Log.w("Screenshot", "getResultData: " + e.toString());
            }
        }
    }



    /**
     * 截图回调接口。
     * 用于接收截图成功或失败的通知。
     */
    public interface ScreenshotCallback {
        /**
         * 截图成功回调。
         *
         * @param bitmap 截取的位图对象。
         */
        public void onSuccess(Bitmap bitmap);

        /**
         * 截图失败回调。
         *
         * @param err 错误信息。
         */
        public void onFailure(String err);
    }



}
