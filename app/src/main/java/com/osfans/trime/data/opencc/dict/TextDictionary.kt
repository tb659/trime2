// SPDX-FileCopyrightText: 2015 - 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.data.opencc.dict

import com.osfans.trime.data.opencc.OpenCCDictManager
import java.io.File

/**
 * 文本格式 OpenCC 词典。
 * 扩展名为 .txt,可以转换为 OpenCC 二进制格式(.ocd2)。
 */
class TextDictionary(
    file: File,
) : Dictionary() {
    /** 词典文件 */
    override var file: File = file
        private set

    /** 词典类型(文本格式) */
    override val type: Type = Type.Text

    init {
        ensureFileExists()
        if (file.extension != type.ext) {
            throw IllegalArgumentException("Not a text dict ${file.name}")
        }
    }

    /**
     * 转换为文本词典(直接复制文件)。
     *
     * @param dest 目标文件。
     * @return 新的文本词典实例。
     */
    override fun toTextDictionary(dest: File): TextDictionary {
        ensureTxt(dest)
        file.copyTo(dest)
        return TextDictionary(dest)
    }

    /**
     * 转换为 OpenCC 二进制词典。
     * 使用 OpenCCDictManager 将文本格式转换为 .ocd2 格式。
     *
     * @param dest 目标文件。
     * @return 新的 OpenCC 词典实例。
     */
    override fun toOpenCCDictionary(dest: File): OpenCCDictionary {
        ensureBin(dest)
        OpenCCDictManager.openCCDictConv(
            file.absolutePath,
            dest.absolutePath,
            OpenCCDictManager.MODE_TXT_TO_BIN,
        )
        return OpenCCDictionary(dest)
    }
}
