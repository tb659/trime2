// SPDX-FileCopyrightText: 2015 - 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.data.opencc


import com.androlua.LuaApplication
import com.osfans.trime.TrimeApplication
import com.osfans.trime.core.DataManager
import com.osfans.trime.data.opencc.dict.Dictionary
import com.osfans.trime.data.opencc.dict.OpenCCDictionary
import com.osfans.trime.data.opencc.dict.TextDictionary
import timber.log.Timber
import java.io.File
import java.io.InputStream
import kotlin.system.measureTimeMillis

/**
 * OpenCC 词典管理器。
 * 管理共享和用户目录下的 OpenCC 词典,支持导入、转换和单行文本转换功能。
 */
object OpenCCDictManager {
    init {
        System.loadLibrary("rime_jni")
    }

    /** 共享词典目录 */
    private val sharedDir = File(DataManager.getSharedDataDir(), "opencc").also { it.mkdirs() }
    /** 用户词典目录 */
    private val userDir get() = File(DataManager.getUserDataDir(), "opencc").also { it.mkdirs() }

    /**
     * 获取共享词典列表。
     *
     * @return 共享目录下的所有词典。
     */
    fun sharedDictionaries(): List<Dictionary> = sharedDir
        .listFiles()
        ?.mapNotNull { Dictionary.new(it) } ?: listOf()

    /**
     * 获取用户词典列表。
     *
     * @return 用户目录下的所有词典。
     */
    fun userDictionaries(): List<Dictionary> = userDir
        .listFiles()
        ?.mapNotNull { Dictionary.new(it) } ?: listOf()

    /**
     * 获取所有词典(共享 + 用户)。
     *
     * @return 所有词典列表。
     */
    fun getAllDictionaries(): List<Dictionary> = sharedDictionaries() + userDictionaries()

    /**
     * 从文件导入词典。
     * 将文本或二进制格式的词典转换为 OpenCC 格式(.ocd2)并保存到用户目录。
     *
     * @param file 源词典文件。
     * @return 导入后的 OpenCC 词典实例。
     */
    fun importFromFile(file: File): OpenCCDictionary {
        val raw =
            Dictionary.new(file)
                ?: throw IllegalArgumentException("${file.path} is not a opencc/text dictionary")
        // convert to opencc format in dictionaries dir
        // preserve original file name
        val new =
            raw.toOpenCCDictionary(
                File(
                    userDir,
                    file.nameWithoutExtension + ".${Dictionary.Type.OCD2.ext}",
                ),
            )
        Timber.d("Converted $raw to $new")
        return new
    }

    /**
     * 构建 OpenCC 词典。
     * 遍历所有词典,将文本格式的词典转换为二进制格式(.ocd2)。
     */
    @JvmStatic
    fun buildOpenCCDict() {
        for (d in getAllDictionaries()) {
            if (d is TextDictionary) {
                val result: Result<OpenCCDictionary>
                measureTimeMillis {
                    result = runCatching { d.toOpenCCDictionary() }
                }.also {
                    result
                        .onSuccess { r ->
                            Timber.d("Took $it to convert to $r")
                        }.onFailure {
                            Timber.e(it, "Failed to convert $d")
                        }
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
    fun importFromInputStream(
        stream: InputStream,
        name: String,
    ): OpenCCDictionary {
        val tempFile = File(LuaApplication.getInstance().cacheDir, name)
        tempFile.outputStream().use {
            stream.copyTo(it)
        }
        val new = importFromFile(tempFile)
        tempFile.delete()
        return new
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
    @JvmStatic
    fun convertLine(
        input: String,
        configFileName: String,
    ): String {
        if (configFileName.isEmpty()) return input
        with(File(userDir, configFileName)) {
            if (exists()) return openCCLineConv(input, path)
        }
        with(File(sharedDir, configFileName)) {
            if (exists()) return openCCLineConv(input, path)
        }
        Timber.w("Specified config $configFileName doesn't exist, returning raw input ...")
        return input
    }

    /**
     * 转换词典文件格式(本地方法)。
     *
     * @param src 源文件路径。
     * @param dest 目标文件路径。
     * @param mode 转换模式(true: 二进制到文本, false: 文本到二进制)。
     */
    @JvmStatic
    external fun openCCDictConv(
        src: String,
        dest: String,
        mode: Boolean,
    )

    /**
     * 转换单行文本(本地方法)。
     *
     * @param input 输入文本。
     * @param configFileName 配置文件名。
     * @return 转换后的文本。
     */
    @JvmStatic
    external fun openCCLineConv(
        input: String,
        configFileName: String,
    ): String

    /** 二进制到文本的转换模式(OCD/OCD2 -> TXT) */
    const val MODE_BIN_TO_TXT = true
    /** 文本到二进制的转换模式(TXT -> OCD2) */
    const val MODE_TXT_TO_BIN = false
}
