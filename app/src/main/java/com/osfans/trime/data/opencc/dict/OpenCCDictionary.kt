// SPDX-FileCopyrightText: 2015 - 2024 Rime community
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.osfans.trime.data.opencc.dict

import com.osfans.trime.data.opencc.OpenCCDictManager
import java.io.File

/**
 * OpenCC 二进制格式词典。
 * 支持 .ocd(旧版)和 .ocd2(新版)两种格式,可以转换为文本格式(.txt)。
 */
class OpenCCDictionary(
    file: File,
) : Dictionary() {
    /** 词典文件 */
    override var file: File = file
        private set

    /**
     * 词典类型。
     * 根据文件扩展名自动判断是 OCD 还是 OCD2 格式。
     */
    override val type: Type =
        if (file.extension == NEW_FORMAT) {
            Type.OCD2
        } else {
            Type.OCD
        }

    init {
        ensureFileExists()
        if (file.extension != type.ext) {
            throw IllegalArgumentException("Not a OpenCC dict ${file.name}")
        }
    }

    /**
     * 转换为文本词典。
     * 使用 OpenCCDictManager 将二进制格式转换为 .txt 格式。
     *
     * @param dest 目标文件。
     * @return 新的文本词典实例。
     */
    override fun toTextDictionary(dest: File): TextDictionary {
        ensureTxt(dest)
        OpenCCDictManager.openCCDictConv(
            file.absolutePath,
            dest.absolutePath,
            OpenCCDictManager.MODE_BIN_TO_TXT,
        )
        return TextDictionary(dest)
    }

    /**
     * 转换为 OpenCC 二进制词典(直接复制文件)。
     *
     * @param dest 目标文件。
     * @return 新的 OpenCC 词典实例。
     */
    override fun toOpenCCDictionary(dest: File): OpenCCDictionary {
        ensureBin(dest)
        file.copyTo(dest)
        return OpenCCDictionary(dest)
    }

    companion object {
        /** OpenCC 新版二进制格式扩展名 */
        const val NEW_FORMAT = "ocd2"
        /** OpenCC 旧版二进制格式扩展名 */
        const val OLD_FORMAT = "ocd"
    }
}
