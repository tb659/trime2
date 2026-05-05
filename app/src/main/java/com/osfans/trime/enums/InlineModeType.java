/*
 * Copyright (C) 2015-present, osfans
 * waxaca@163.com https://github.com/osfans
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.osfans.trime.enums;

/**
 * 嵌入模式枚举。
 * 定义编码区文本的显示方式:
 * - INLINE_NONE: 不嵌入显示
 * - INLINE_PREVIEW: 嵌入显示预览文本
 * - INLINE_COMPOSITION: 嵌入显示编码文本
 * - INLINE_INPUT: 嵌入显示输入文本
 */
public enum InlineModeType {
  /** 不嵌入显示 */
  INLINE_NONE,
  /** 嵌入显示预览文本(候选词) */
  INLINE_PREVIEW,
  /** 嵌入显示编码文本(拼音等) */
  INLINE_COMPOSITION,
  /** 嵌入显示输入文本(原始输入) */
  INLINE_INPUT
}
