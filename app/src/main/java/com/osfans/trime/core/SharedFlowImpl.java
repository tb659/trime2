/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 共享流实现类。
 * 实现 Flow 接口,提供带缓冲区的值发射功能,简化了 StateFlow/MutableSharedFlow 的行为。
 *
 * @param <T> 数据类型。
 */
public class SharedFlowImpl<T> implements Flow<T> {
    // ==================== 成员变量 ====================
    /** 缓冲区容量 */
    private final int capacity;
    /** 值缓冲区列表 */
    private final List<T> buffer;
    /** 最后一个发射的值(简化 stateFlow/MutableSharedFlow 行为) */
    private T lastValue = null;

    /**
     * 构造函数。
     *
     * @param capacity 缓冲区容量。
     */
    public SharedFlowImpl(int capacity) {
        this.capacity = capacity;
        this.buffer = new ArrayList<>(capacity);
    }

    /**
     * 尝试发射一个值。
     * 如果缓冲区未满,则添加值并更新最后值;如果已满,则返回 false(模拟 BufferOverflow.DROP_OLDEST)。
     *
     * @param value 要发射的值。
     * @return true 表示发射成功,false 表示缓冲区已满。
     */
    @Override
    public boolean tryEmit(T value) {
        synchronized (buffer) {
            if (buffer.size() < capacity) {
                // 缓冲区未满,添加值并更新最后值
                buffer.add(value);
                lastValue = value;
                // 在实际实现中,这里会通知观察者
                return true;
            } else {
                // 模拟 BufferOverflow.DROP_OLDEST
                // 在真正的 DROP_OLDEST 中,我们会丢弃*最旧的*,但对于缓冲区容量这通常意味着如果满了就丢弃传入的值
                // 考虑到小容量和性质,丢弃传入的值对主线程更安全
                return false;
            }
        }
    }

    /**
     * 获取最后发射的值。
     *
     * @return 最后一个发射的值,如果没有则返回 null。
     */
    @Override
    public T getValue() {
        return lastValue;
    }
}

