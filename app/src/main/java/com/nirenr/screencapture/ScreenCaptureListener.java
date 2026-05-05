package com.nirenr.screencapture;

import android.graphics.Bitmap;

/**
 * 截图监听器接口。
 * 用于接收截图完成或失败的通知。
 */

public interface ScreenCaptureListener {
     /**
      * 截图完成回调。
      *
      * @param bitmap 截取的位图对象。
      */
     public void onScreenCaptureDone(Bitmap bitmap);

     /**
      * 截图失败回调。
      *
      * @param msg 错误信息。
      */
     public void onScreenCaptureError(String msg);
}
