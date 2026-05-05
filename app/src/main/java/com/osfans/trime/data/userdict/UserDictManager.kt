/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.data.userdict

import com.androlua.LuaApplication
import com.osfans.trime.TrimeApplication
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * 用户词典管理器。
 * 管理 Rime 输入法的用户词典,支持备份、恢复、导入和导出功能。
 */
object UserDictManager {
    /**
     * 恢复用户词典。
     * 从输入流读取快照文件并恢复到用户词典。
     *
     * @param stream 输入流。
     * @param snapshotFile 快照文件名。
     * @return 操作结果,成功返回 Result.success,失败返回 Result.failure。
     */
    fun restoreUserDict(stream: InputStream, snapshotFile: String): Result<Unit> {
        val tempFile = File(LuaApplication.getInstance().cacheDir, snapshotFile)
        try {
            tempFile.outputStream().use {
                stream.copyTo(it)
            }
            val success = restoreUserDict(tempFile.absolutePath)
            return if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to restore"))
            }
        } finally {
            tempFile.delete()
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
    fun importUserDict(stream: InputStream, dictName: String, textFile: String): Result<Int> {
        val tempFile = File(LuaApplication.getInstance().cacheDir, textFile)
        try {
            tempFile.outputStream().use {
                stream.copyTo(it)
            }
            val count = importUserDict(dictName, tempFile.absolutePath)
            return if (count >= 0) {
                Result.success(count)
            } else {
                Result.failure(
                    Exception("Failed to import from '$textFile' to '$dictName'"),
                )
            }
        } finally {
            tempFile.delete()
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
    fun exportUserDict(dest: OutputStream, dictName: String, textFile: String): Result<Int> {
        val tempFile = File(LuaApplication.getInstance().cacheDir, textFile)
        try {
            val count = exportUserDict(dictName, tempFile.absolutePath)
            tempFile.inputStream().use {
                it.copyTo(dest)
            }
            return if (count >= 0) {
                Result.success(count)
            } else {
                Result.failure(
                    Exception("Failed to export '$dictName' to '$textFile'"),
                )
            }
        } finally {
            tempFile.delete()
        }
    }

    /**
     * 获取用户词典列表(本地方法)。
     *
     * @return 所有用户词典名称数组。
     */
    @JvmStatic
    external fun getUserDictList(): Array<String>

    /**
     * 备份用户词典(本地方法)。
     *
     * @param dictName 词典名称。
     * @return true 表示备份成功。
     */
    @JvmStatic
    external fun backupUserDict(dictName: String): Boolean

    /**
     * 恢复用户词典(本地方法)。
     *
     * @param snapshotFile 快照文件路径。
     * @return true 表示恢复成功。
     */
    @JvmStatic
    external fun restoreUserDict(snapshotFile: String): Boolean

    /**
     * 导出用户词典(本地方法)。
     *
     * @param dictName 词典名称。
     * @param textFile 文本文件路径。
     * @return 导出的词条数量,失败返回负数。
     */
    @JvmStatic
    external fun exportUserDict(dictName: String, textFile: String): Int

    /**
     * 导入用户词典(本地方法)。
     *
     * @param dictName 词典名称。
     * @param textFile 文本文件路径。
     * @return 导入的词条数量,失败返回负数。
     */
    @JvmStatic
    external fun importUserDict(dictName: String, textFile: String): Int
}
