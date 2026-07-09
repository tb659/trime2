// SPDX-FileCopyrightText: 2015 - 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.androlua.LuaApplication;
import com.androlua.LuaUtil;
import com.osfans.trime.BuildConfig;
import com.osfans.trime.Config;
import com.osfans.trime.TrimeApplication;
import timber.log.Timber;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 数据管理器。
 * 管理 Rime 输入法的数据目录、配置文件同步和资源解压。
 */
public class DataManager {

    // ==================== 常量 ====================
    /** 默认自定义配置文件名 */
    private static final String DEFAULT_CUSTOM_FILE_NAME = "default.custom.yaml";
    /** 数据校验和文件名 */
    private static final String DATA_CHECKSUMS_NAME = "checksums.json";

    /** 方案列表自定义补丁 */
    private static final String SCHEMA_LIST_CUSTOM_PATCH =
            "patch:\n" +
                    "  schema_list:\n" +
                    "    - schema: luna_pinyin\n" +
                    "    - schema: luna_pinyin_simp";

    /** 重入锁,用于线程同步 */
    private static final ReentrantLock lock = new ReentrantLock();

    // ==================== 成员变量 ====================

    // 懒加载 DataDir
    /** 数据目录(懒加载) */
    private static final File dataDir;

    static {
        // 初始化 dataDir
        Context context = getAppContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Timber.d("Using device protected storage");
            dataDir = context.createDeviceProtectedStorageContext().getDataDir();
        } else {
            dataDir = new File(context.getApplicationInfo().dataDir);
        }
    }

    // 懒加载 AppPrefs
    /** SharedPreferences 实例(懒加载,volatile 保证可见性) */
    private static volatile SharedPreferences prefsInstance;

    /**
     * 私有构造函数,防止实例化。
     */
    private DataManager() {
        // 防止实例化
    }

    /**
     * 获取应用上下文。
     * 对应 Kotlin 的 import com.osfans.trime.util.appContext。
     *
     * @return 应用上下文。
     */
    private static Context getAppContext() {
        // 实际项目中请替换为真实的调用方式,例如 LuaApplication.getInstance() 或 UtilKt.getAppContext()
        return LuaApplication.getInstance();
    }

    /**
     * 获取 SharedPreferences 实例(懒加载,双重检查锁定)。
     *
     * @return SharedPreferences 实例。
     */
    private static SharedPreferences getPrefs() {
        if (prefsInstance == null) {
            synchronized (DataManager.class) {
                if (prefsInstance == null) {
                    prefsInstance = PreferenceManager.getDefaultSharedPreferences(getAppContext());
                }
            }
        }
        return prefsInstance;
    }

    /**
     * 获取默认数据目录(外部存储/rime)。
     *
     * @return 默认数据目录。
     */
    public static File getDefaultDataDir() {
        return new File(Environment.getExternalStorageDirectory(), "rime");
    }

    /**
     * 获取共享数据目录(应用外部文件目录/shared)。
     *
     * @return 共享数据目录。
     */
    public static File getSharedDataDir() {
        File dir = new File(getAppContext().getExternalFilesDir(null), "shared");
        dir.mkdirs();
        return dir;
    }

    /**
     * 获取用户数据目录。
     *
     * @return 用户数据目录。
     */
    public static File getUserDataDir() {
        File dir = new File(Config.getUserDataDir());
        dir.mkdirs();
        return dir;
    }

    /**
     * 获取预构建数据目录(共享数据目录/build)。
     *
     * @return 预构建数据目录。
     */
    public static File getPrebuiltDataDir() {
        return new File(getSharedDataDir(), "build");
    }

    /**
     * 获取暂存目录(用户数据目录/build)。
     *
     * @return 暂存目录。
     */
    public static File getStagingDir() {
        return new File(getUserDataDir(), "build");
    }

    /**
     * 返回已部署配置文件的绝对路径。
     * 根据给定的资源 ID,优先查找暂存目录,如果不存在则回退到预构建数据目录。
     *
     * @param resourceId 通常为不含扩展名的配置文件名。
     * @return 已部署配置文件的绝对路径。
     */
    public static String resolveDeployedResourcePath(String resourceId) {
        File defaultPath = new File(getStagingDir(), resourceId + ".yaml");
        if (!defaultPath.exists()) {
            File fallbackPath = new File(getPrebuiltDataDir(), resourceId + ".yaml");
            if (fallbackPath.exists()) {
                return fallbackPath.getAbsolutePath();
            }
        }
        return defaultPath.getAbsolutePath();
    }

    /**
     * 同步数据。
     * 从 APK 中解压共享数据、主题和脚本资源到相应目录,
     * 如果用户数据目录中没有 default.custom.yaml 且主题为 default,则创建默认配置。
     */
    public static void sync() {
        try {
            LuaApplication.getInstance().unApk("assets/shared",getSharedDataDir().getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
        }
        File f = new File(Config.getUserDataDir(), DEFAULT_CUSTOM_FILE_NAME);
        if(!f.exists()&&Config.getTheme().equals("default")){
            try {
                LuaUtil.save(f.getAbsolutePath(),
                "patch:\n" +
                    "  schema_list:\n" +
                    "    - schema: pinyin_simp\n" +
                    "    - schema: easy_english\n" +
                    "    - schema: tiger\n" +
                    "    - schema: tigress\n" +
                    "    - schema: stroke\n"
                );
            } catch (Exception ignored) {}
        }
        //if (BuildConfig.DEBUG || Config.getThemes().length==0) {
        try {
            LuaApplication.getInstance().unApk("assets/themes", Config.getThemeDir());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            LuaApplication.getInstance().unApk("assets/scripts", Config.getScriptsDir());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
