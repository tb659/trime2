/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.core;

/**
 * 数据流接口。
 * 支持发射值和获取当前值,用于响应式数据传递。
 *
 * @param <T> 数据类型。
 */
public interface Flow<T> {
    /**
     * 尝试发射一个值。
     *
     * @param value 要发射的值。
     * @return true 表示发射成功。
     */
    boolean tryEmit(T value);

    /**
     * 获取当前值。
     *
     * @return 当前的值。
     */
    T getValue();
}
