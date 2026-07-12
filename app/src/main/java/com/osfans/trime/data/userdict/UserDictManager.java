/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.data.userdict;

import com.androlua.LuaApplication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class UserDictManager {

    static {
        // 确保你的 rime_jni 在某处被加载了，如果原先在别处加载了可以移除这块
        // System.loadLibrary("rime_jni");
    }

    // 私有构造函数防止实例化（对应 Kotlin 的 object 单例）
    private UserDictManager() {}

    /**
     * 简单的轻量级泛型包装类，用于替代 Kotlin 的 Result
     */
    public static final class DictResult<T> {
        private final T value;
        private final Throwable exception;

        private DictResult(T value, Throwable exception) {
            this.value = value;
            this.exception = exception;
        }

        public static <T> DictResult<T> success(T value) {
            return new DictResult<>(value, null);
        }

        public static <T> DictResult<T> failure(Throwable exception) {
            return new DictResult<>(null, exception);
        }

        public boolean isSuccess() {
            return exception == null;
        }

        public T getValue() {
            return value;
        }

        public Throwable getException() {
            return exception;
        }
    }

    /**
     * 恢复用户词典。
     * 从输入流读取快照文件并恢复到用户词典。
     *
     * @param stream 输入流。
     * @param snapshotFile 快照文件名。
     * @return 操作结果,成功返回 Result.success,失败返回 Result.failure。
     */
    public static DictResult<Void> restoreUserDict(InputStream stream, String snapshotFile) {
        File tempFile = new File(LuaApplication.getInstance().getCacheDir(), snapshotFile);
        try {
            // Android 7+ 兼容的 try-with-resources 流拷贝
            try (OutputStream os = new FileOutputStream(tempFile)) {
                copyStream(stream, os);
            }

            boolean success = restoreUserDict(tempFile.getAbsolutePath());
            if (success) {
                return DictResult.success(null); // Java 中用 Void 和 null 表达 Kotlin 的 Unit
            } else {
                return DictResult.failure(new Exception("Failed to restore"));
            }
        } catch (IOException e) {
            return DictResult.failure(e);
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * 导入用户词典。
     * 从输入流读取文本文件并导入到指定的用户词典中。
     *
     * @param stream 输入流。
     * @param dictName 词典名称。
     * @param textFile 文本文件名。
     * @return 操作结果,成功返回导入的词条数量,失败返回 Result.failure。
     */

    public static DictResult<Integer> importUserDict(InputStream stream, String dictName, String textFile) {
        File tempFile = new File(LuaApplication.getInstance().getCacheDir(), textFile);
        try {
            try (OutputStream os = new FileOutputStream(tempFile)) {
                copyStream(stream, os);
            }

            int count = importUserDict(dictName, tempFile.getAbsolutePath());
            if (count >= 0) {
                return DictResult.success(count);
            } else {
                return DictResult.failure(new Exception("Failed to import from '" + textFile + "' to '" + dictName + "'"));
            }
        } catch (IOException e) {
            return DictResult.failure(e);
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    /**
     * 导出用户词典。
     * 将指定的用户词典导出为文本文件并写入输出流。
     *
     * @param dest 输出流。
     * @param dictName 词典名称。
     * @param textFile 文本文件名。
     * @return 操作结果,成功返回导出的词条数量,失败返回 Result.failure。
     */

    public static DictResult<Integer> exportUserDict(OutputStream dest, String dictName, String textFile) {
        File tempFile = new File(LuaApplication.getInstance().getCacheDir(), textFile);
        try {
            int count = exportUserDict(dictName, tempFile.getAbsolutePath());
            if (count >= 0) {
                try (InputStream is = new FileInputStream(tempFile)) {
                    copyStream(is, dest);
                }
                return DictResult.success(count);
            } else {
                return DictResult.failure(new Exception("Failed to export '" + dictName + "' to '" + textFile + "'"));
            }
        } catch (IOException e) {
            return DictResult.failure(e);
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    // 基础流拷贝辅助方法，完全兼容全版本 Android
    private static void copyStream(InputStream source, OutputStream target) throws IOException {
        byte[] buf = new byte[8192];
        int length;
        while ((length = source.read(buf)) > 0) {
            target.write(buf, 0, length);
        }
        target.flush();
    }

    // --- Native JNI Methods ---


    /**
     * 获取用户词典列表(本地方法)。
     *
     * @return 所有用户词典名称数组。
     */
    public static native String[] getUserDictList();

    /**
     * 备份用户词典(本地方法)。
     *
     * @param dictName 词典名称。
     * @return true 表示备份成功。
     */
    public static native boolean backupUserDict(String dictName);

    /**
     * 恢复用户词典(本地方法)。
     *
     * @param snapshotFile 快照文件路径。
     * @return true 表示恢复成功。
     */
    public static native boolean restoreUserDict(String snapshotFile);

    /**
     * 导出用户词典(本地方法)。
     *
     * @param dictName 词典名称。
     * @param textFile 文本文件路径。
     * @return 导出的词条数量,失败返回负数。
     */
    public static native int exportUserDict(String dictName, String textFile);

    /**
     * 导入用户词典(本地方法)。
     *
     * @param dictName 词典名称。
     * @param textFile 文本文件路径。
     * @return 导入的词条数量,失败返回负数。
     */
    public static native int importUserDict(String dictName, String textFile);
}
