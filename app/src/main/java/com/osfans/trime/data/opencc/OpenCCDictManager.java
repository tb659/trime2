// SPDX-FileCopyrightText: 2015 - 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.data.opencc;

import com.androlua.LuaApplication;
import com.osfans.trime.core.DataManager;
import com.osfans.trime.data.opencc.dict.Dictionary;
import com.osfans.trime.data.opencc.dict.OpenCCDictionary;
import com.osfans.trime.data.opencc.dict.TextDictionary;
import timber.log.Timber;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OpenCCDictManager {

    /** 二进制到文本的转换模式(OCD/OCD2 -> TXT) */
    public static final boolean MODE_BIN_TO_TXT = true;  // OCD(2) to TXT
    /** 文本到二进制的转换模式(TXT -> OCD2) */
    public static final boolean MODE_TXT_TO_BIN = false; // TXT to OCD2

    /** 共享词典目录 */
    private static final File sharedDir;

    static {
        System.loadLibrary("rime_jni");
        sharedDir = new File(DataManager.getSharedDataDir(), "opencc");
        if (!sharedDir.exists()) {
            sharedDir.mkdirs();
        }
    }


    // 私有构造函数，防止实例化
    /**
     * OpenCC 词典管理器。
     * 管理共享和用户目录下的 OpenCC 词典,支持导入、转换和单行文本转换功能。
     */
    private OpenCCDictManager() {}

    /**
     * 获取用户词典目录。
     * @return 用户词典目录。
     */
    private static File getUserDir() {
        File userDir = new File(DataManager.getUserDataDir(), "opencc");
        if (!userDir.exists()) {
            userDir.mkdirs();
        }
        return userDir;
    }

    /**
     * 获取共享词典列表。
     *
     * @return 共享目录下的所有词典。
     */
    public static List<Dictionary> sharedDictionaries() {
        File[] files = sharedDir.listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        List<Dictionary> dictionaries = new ArrayList<>();
        for (File file : files) {
            Dictionary dict = Dictionary.newDictionary(file);
            if (dict != null) {
                dictionaries.add(dict);
            }
        }
        return dictionaries;
    }

    /**
     * 获取用户词典列表。
     *
     * @return 用户目录下的所有词典。
     */
    public static List<Dictionary> userDictionaries() {
        File[] files = getUserDir().listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        List<Dictionary> dictionaries = new ArrayList<>();
        for (File file : files) {
            Dictionary dict = Dictionary.newDictionary(file);
            if (dict != null) {
                dictionaries.add(dict);
            }
        }
        return dictionaries;
    }

    /**
     * 获取所有词典(共享 + 用户)。
     *
     * @return 所有词典列表。
     */
    public static List<Dictionary> getAllDictionaries() {
        List<Dictionary> all = new ArrayList<>(sharedDictionaries());
        all.addAll(userDictionaries());
        return all;
    }

    /**
     * 从文件导入词典。
     * 将文本或二进制格式的词典转换为 OpenCC 格式(.ocd2)并保存到用户目录。
     *
     * @param file 源词典文件。
     * @return 导入后的 OpenCC 词典实例。
     */
    public static OpenCCDictionary importFromFile(File file) {
        Dictionary raw = Dictionary.newDictionary(file);
        if (raw == null) {
            throw new IllegalArgumentException(file.getPath() + " is not a opencc/text dictionary");
        }

        // 获取不带后缀的文件名
        String nameWithoutExtension = file.getName();
        int dotIndex = nameWithoutExtension.lastIndexOf('.');
        if (dotIndex > 0) {
            nameWithoutExtension = nameWithoutExtension.substring(0, dotIndex);
        }

        // convert to opencc format in dictionaries dir
        // preserve original file name
        File destFile = new File(getUserDir(), nameWithoutExtension + "." + Dictionary.Type.OCD2.getExt());
        OpenCCDictionary newDict = raw.toOpenCCDictionary(destFile);
        Timber.d("Converted %s to %s", raw, newDict);
        return newDict;
    }

    /**
     * 构建 OpenCC 词典。
     * 遍历所有词典,将文本格式的词典转换为二进制格式(.ocd2)。
     */
    public static void buildOpenCCDict() {
        for (Dictionary d : getAllDictionaries()) {
            if (d instanceof TextDictionary) {
                long startTime = System.currentTimeMillis();
                try {
                    OpenCCDictionary r = ((TextDictionary) d).toOpenCCDictionary();
                    long duration = System.currentTimeMillis() - startTime;
                    Timber.d("Took %d ms to convert to %s", duration, r);
                } catch (Exception e) {
                    Timber.e(e, "Failed to convert %s", d);
                }
            }
        }
    }

    /**
     * 从输入流导入词典。
     * 先将流内容写入临时文件,然后调用 importFromFile 导入,最后删除临时文件。
     *
     * @param stream 输入流。
     * @param name 文件名。
     * @return 导入后的 OpenCC 词典实例。
     */
    public static OpenCCDictionary importFromInputStream(InputStream stream, String name) throws IOException {
        File tempFile = new File(LuaApplication.getInstance().getCacheDir(), name);

        // 使用 Java 的 try-with-resources 自动关闭流
        try (OutputStream os = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = stream.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }

        try {
            return importFromFile(tempFile);
        } finally {
            tempFile.delete();
        }
    }

    /**
     * 转换单行文本。
     * 根据指定的配置文件进行简繁转换或其他文本转换。
     * 优先查找用户目录,其次查找共享目录。
     *
     * @param input 输入文本。
     * @param configFileName 配置文件名。
     * @return 转换后的文本,如果配置文件不存在则返回原文本。
     */
    public static String convertLine(String input, String configFileName) {
        if (configFileName == null || configFileName.isEmpty()) {
            return input;
        }

        File userFile = new File(getUserDir(), configFileName);
        if (userFile.exists()) {
            return openCCLineConv(input, userFile.getPath());
        }

        File sharedFile = new File(sharedDir, configFileName);
        if (sharedFile.exists()) {
            return openCCLineConv(input, sharedFile.getPath());
        }

        Timber.w("Specified config %s doesn't exist, returning raw input ...", configFileName);
        return input;
    }

    /**
     * 转换词典文件格式(本地方法)。
     *
     * @param src 源文件路径。
     * @param dest 目标文件路径。
     * @param mode 转换模式(true: 二进制到文本, false: 文本到二进制)。
     */
    public static native void openCCDictConv(String src, String dest, boolean mode);

    /**
     * 转换单行文本(本地方法)。
     *
     * @param input 输入文本。
     * @param configFileName 配置文件名。
     * @return 转换后的文本。
     */
    public static native String openCCLineConv(String input, String configFileName);
}
