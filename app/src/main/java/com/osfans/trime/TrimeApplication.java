/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime;

import android.Manifest;
import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import com.androlua.LuaUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Trime输入法应用程序类，继承自Application。
 * 负责应用程序级别的初始化，提供单例访问点。
 * 包含存储权限检查和APK资源解压功能。
 */
public class TrimeApplication extends Application {
    /** 单例实例，提供全局访问点 */
    private static TrimeApplication sInstance;

    public static TrimeApplication getInstance() {
        return sInstance;
    }

    /**
     * 应用创建时的回调。
     * 初始化单例实例。
     */
    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    /**
     * 检查外部存储是否可用（根据系统版本使用不同的权限检查方式）。
     * Android R及以上：检查是否拥有外部存储管理权限。
     * Android R以下：检查是否有写入外部存储的权限。
     * @return true表示存储可用，false表示不可用
     */
    public boolean isStorageAvailable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager(); // Android 11+：检查存储管理权限
        } else {
            return checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED; // Android 10及以下：检查写入权限
        }
    }

    /**
     * 从APK文件中解压指定目录的资源到外部目录。
     * 通过比较文件大小和MD5值来避免重复解压未变化的文件。
     * @param dir APK中的目录路径（如"assets/lua"）
     * @param extDir 外部存储的目标目录路径
     * @throws IOException 解压过程中发生IO异常
     */
    public void unApk(String dir, String extDir) throws IOException {
        int i = dir.length() + 1; // 计算相对路径的起始位置（+1跳过路径分隔符）
        ZipFile zip = new ZipFile(getApplicationInfo().publicSourceDir); // 打开当前APK文件
        Enumeration<? extends ZipEntry> entries = zip.entries(); // 获取所有条目
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.indexOf(dir) != 0) // 只处理指定目录下的条目
                continue;
            String path = name.substring(i); // 提取相对路径
            if (entry.isDirectory()) {
                // 处理目录：创建对应的文件夹
                File f = new File(extDir + File.separator + path);
                if (!f.exists()) {
                    f.mkdirs(); // 创建目录（包括必要的父目录）
                }
            } else {
                // 处理文件：解压并写入外部存储
                String fname = extDir + File.separator + path;
                File ff = new File(fname);
                File temp = new File(fname).getParentFile(); // 获取父目录
                if (!temp.exists()) {
                    if (!temp.mkdirs()) { // 创建父目录
                        continue; // 创建失败则跳过此文件
                    }
                }
                try {
                    // 优化：如果文件已存在且大小和MD5都相同，则跳过解压
                    if (ff.exists() && entry.getSize() == ff.length() && LuaUtil.getFileMD5(zip.getInputStream(entry)).equals(LuaUtil.getFileMD5(ff)))
                        continue;
                } catch (NullPointerException ignored) {
                }
                // 解压文件：从APK读取并写入外部存储
                FileOutputStream out = new FileOutputStream(extDir + File.separator + path);
                InputStream in = zip.getInputStream(entry);
                byte[] buf = new byte[40960]; // 40KB缓冲区
                int count = 0;
                while ((count = in.read(buf)) != -1) {
                    out.write(buf, 0, count); // 写入数据
                }
                out.close();
                in.close();
            }
        }
        zip.close(); // 关闭ZIP文件
    }
}
