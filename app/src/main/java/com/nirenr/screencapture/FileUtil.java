package com.nirenr.screencapture;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 文件工具类。
 * 提供截图保存路径和文件名的生成方法。
 */
public class FileUtil {

  /** 系统保存截图的路径 */
  public static final String SCREENCAPTURE_PATH = "ScreenCapture" + File.separator + "Screenshots" + File.separator;
//  public static final String SCREENCAPTURE_PATH = "ZAKER" + File.separator + "Screenshots" + File.separator;

  /** 截图文件名前缀 */
  public static final String SCREENSHOT_NAME = "Screenshot";

  /**
   * 获取应用存储路径。
   * 优先使用外部存储,如果不可用则使用内部存储。
   *
   * @param context Android 上下文。
   * @return 存储根目录路径。
   */
  public static String getAppPath(Context context) {

    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {


      return Environment.getExternalStorageDirectory().toString(); // 返回外部存储路径

    } else {

      return context.getFilesDir().toString(); // 返回内部存储路径
    }

  }


  /**
   * 获取截图保存目录。
   * 如果目录不存在则自动创建。
   *
   * @param context Android 上下文。
   * @return 截图目录的完整路径。
   */
  public static String getScreenShots(Context context) {

    StringBuffer stringBuffer = new StringBuffer(getAppPath(context));
    stringBuffer.append(File.separator);

    stringBuffer.append(SCREENCAPTURE_PATH); // 添加截图子目录

    File file = new File(stringBuffer.toString());

    if (!file.exists()) {
      file.mkdirs(); // 创建目录
    }

    return stringBuffer.toString();

  }

  /**
   * 生成截图文件名。
   * 格式: Screenshot_yyyy-MM-dd-hh-mm-ss.png
   *
   * @param context Android 上下文。
   * @return 完整的截图文件路径。
   */
  public static String getScreenShotsName(Context context) {

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss", Locale.CHINESE);

    String date = simpleDateFormat.format(new Date()); // 格式化当前时间

    StringBuffer stringBuffer = new StringBuffer(getScreenShots(context));
    stringBuffer.append(SCREENSHOT_NAME); // 添加文件名前缀
    stringBuffer.append("_");
    stringBuffer.append(date); // 添加时间戳
    stringBuffer.append(".png"); // 添加扩展名

    return stringBuffer.toString();

  }


}
