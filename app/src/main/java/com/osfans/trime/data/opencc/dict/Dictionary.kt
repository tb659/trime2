// SPDX-FileCopyrightText: 2015 - 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.data.opencc.dict

import java.io.File

/**
 * OpenCC 词典抽象基类。
 * 支持三种词典格式: OCD、OCD2(二进制)和 Text(文本),提供格式转换功能。
 */
abstract class Dictionary {
    /**
     * 词典类型枚举。
     * 定义支持的词典文件格式及其扩展名。
     */
    enum class Type(
        val ext: String,
    ) {
        /** OpenCC 旧版二进制格式 */
        OCD("ocd"),
        /** OpenCC 新版二进制格式 */
        OCD2("ocd2"),
        /** 文本格式 */
        Text("txt"),
        ;

        companion object {
            /**
             * 根据文件名推断词典类型。
             *
             * @param name 文件名。
             * @return 对应的词典类型,无法识别则返回 null。
             */
            fun fromFileName(name: String): Type? = when {
                name.endsWith(".ocd2") -> OCD2
                name.endsWith(".ocd") -> OCD
                name.endsWith(".txt") -> Text
                else -> null
            }
        }
    }

    /** 词典文件 */
    abstract val file: File

    /** 词典类型 */
    abstract val type: Type

    /**
     * 转换为文本词典。
     *
     * @param dest 目标文件。
     * @return 转换后的文本词典实例。
     */
    abstract fun toTextDictionary(dest: File): TextDictionary

    /**
     * 转换为 OpenCC 二进制词典。
     *
     * @param dest 目标文件。
     * @return 转换后的 OpenCC 词典实例。
     */
    abstract fun toOpenCCDictionary(dest: File): OpenCCDictionary

    /**
     * 词典名称(不含扩展名)。
     */
    open val name: String
        get() = file.nameWithoutExtension

    /**
     * 转换为文本词典(自动生成目标文件名)。
     * 目标文件与源文件在同一目录,扩展名为 .txt。
     *
     * @return 转换后的文本词典实例。
     */
    fun toTextDictionary(): TextDictionary {
        val dest = file.resolveSibling("$name.${Type.Text.ext}")
        return toTextDictionary(dest)
    }

    /**
     * 转换为 OpenCC 二进制词典(自动生成目标文件名)。
     * 目标文件与源文件在同一目录,扩展名为 .ocd2。
     *
     * @return 转换后的 OpenCC 词典实例。
     */
    fun toOpenCCDictionary(): OpenCCDictionary {
        val dest = file.resolveSibling("$name.${Type.OCD2.ext}")
        return toOpenCCDictionary(dest)
    }

    /**
     * 确保文件存在,不存在则抛出异常。
     */
    protected fun ensureFileExists() {
        if (!file.exists()) {
            throw IllegalStateException("File ${file.absolutePath} does not exist")
        }
    }

    /**
     * 确保目标文件是文本格式(.txt),并删除已存在的文件。
     *
     * @param dest 目标文件。
     */
    protected fun ensureTxt(dest: File) {
        if (dest.extension != Type.Text.ext) {
            throw IllegalArgumentException("Dest file name must end with .${Type.Text.ext}")
        }
        dest.delete()
    }

    /**
     * 确保目标文件是二进制格式(.ocd 或 .ocd2),并删除已存在的文件。
     *
     * @param dest 目标文件。
     */
    protected fun ensureBin(dest: File) {
        if (dest.extension != Type.OCD.ext && dest.extension != Type.OCD2.ext) {
            throw IllegalArgumentException("Dest file name must end with .${Type.OCD.ext} or .${Type.OCD2.ext}")
        }
        dest.delete()
    }

    /**
     * 返回词典的字符串表示。
     * 格式: "类名[名称 -> 文件路径]"。
     */
    override fun toString(): String = "${javaClass.simpleName}[$name -> ${file.path}]"

    companion object {
        /**
         * 根据文件创建对应的词典实例。
         * 根据文件扩展名自动判断是 OpenCCDictionary 还是 TextDictionary。
         *
         * @param it 词典文件。
         * @return 词典实例,无法识别则返回 null。
         */
        fun new(it: File): Dictionary? = when (Type.fromFileName(it.name)) {
            Type.OCD, Type.OCD2 -> OpenCCDictionary(it)
            Type.Text -> TextDictionary(it)
            null -> null
        }
    }
}
