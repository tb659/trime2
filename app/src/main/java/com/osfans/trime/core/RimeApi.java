/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.core;

import com.osfans.trime.core.KeyModifiers;
import com.osfans.trime.core.KeyValue;

/**
 * Rime API 接口。
 * Java 版本的 Rime API,将 Kotlin 的协程概念( suspend、SharedFlow)转换为同步方法和通用 Flow 接口。
 * 需要在具体 Java 类中处理线程和 Flow 实现。
 */
public interface RimeApi {

    /**
     * 获取消息流。
     *
     * @return 共享流实例。
     */
    SharedFlowImpl<?> getMessage();

    /**
     * 获取当前生命周期状态。
     *
     * @return Rime 生命周期状态。
     */
    RimeLifecycle.State getState();

    /**
     * 检查 Rime 是否就绪。
     *
     * @return true 表示就绪。
     */
    boolean isReady();

    /**
     * 获取缓存的方案信息。
     *
     * @return 当前方案。
     */
    RimeSchema getSchemaCached();

    /**
     * 获取缓存的状态信息。
     *
     * @return 当前状态。
     */
    RimeProto.Status getStatusCached();

    /**
     * 获取缓存的编码区信息。
     *
     * @return 当前编码区组合。
     */
    RimeProto.Context.Composition getCompositionCached();

    /**
     * 获取缓存的候选词菜单。
     *
     * @return 当前候选词菜单。
     */
    RimeProto.Context.Menu getMenuCached();

    // --- 挂起函数(转换为同步方法) ---

    /**
     * 检查编码区是否为空。
     *
     * @return true 表示为空。
     */
    boolean isEmpty();

    /**
     * 部署 Rime 配置。
     */
    void deploy();

    /**
     * 同步用户数据。
     *
     * @return true 表示同步成功。
     */
    boolean syncUserData();

    /**
     * 发送按键事件到 Rime。
     *
     * @param value 键码值。
     * @param modifiers 键修饰符(从 UInt 转换)。
     * @param isVirtual 是否为虚拟键。
     * @return true 表示按键事件已被 Rime 处理。
     */
    boolean processKey(
            int value,
            long modifiers, // Kotlin UInt 转换为 long 以确保安全
            boolean isVirtual
    );

    /**
     * 发送按键事件到 Rime(默认修饰符和虚拟键)。
     *
     * @param value 键码值。
     * @return true 表示按键事件已被 Rime 处理。
     */
    default boolean processKey(int value) {
        return processKey(value, 0L, true);
    }

    /**
     * 发送按键事件到 Rime(默认虚拟键)。
     *
     * @param value 键码值。
     * @param modifiers 键修饰符。
     * @return true 表示按键事件已被 Rime 处理。
     */
    default boolean processKey(int value, long modifiers) {
        return processKey(value, modifiers, true);
    }

    /**
     * 发送按键事件到 Rime(KeyValue 版本)。
     *
     * @param value 键值对象。
     * @param modifiers 键修饰符。
     * @param isVirtual 是否为虚拟键。
     * @return true 表示按键事件已被 Rime 处理。
     */
    boolean processKey(
            KeyValue value,
            KeyModifiers modifiers,
            boolean isVirtual
    );

    /**
     * 发送按键事件到 Rime(KeyValue 版本,默认虚拟键)。
     *
     * @param value 键值对象。
     * @param modifiers 键修饰符。
     * @return true 表示按键事件已被 Rime 处理。
     */
    default boolean processKey(KeyValue value, KeyModifiers modifiers) {
        return processKey(value, modifiers, true);
    }

    /**
     * 模拟按键序列。
     *
     * @param sequence 按键序列字符串。
     * @return true 表示序列已被处理。
     */
    boolean simulateKeySequence(String sequence);

    /**
     * 选择候选词。
     *
     * @param idx 候选词索引。
     * @return true 表示选择成功。
     */
    boolean selectCandidate(int idx);

    /**
     * 忘记候选词(从用户词典中删除)。
     *
     * @param idx 候选词索引。
     * @return true 表示删除成功。
     */
    boolean forgetCandidate(int idx);

    /**
     * 选择分页候选词。
     *
     * @param idx 候选词索引。
     * @return true 表示选择成功。
     */
    boolean selectPagedCandidate(int idx);

    /**
     * 删除分页候选词。
     *
     * @param idx 候选词索引。
     * @return true 表示删除成功。
     */
    boolean deletedPagedCandidate(int idx);

    /**
     * 切换候选词页。
     *
     * @param backward true 表示上一页,false 表示下一页。
     * @return true 表示切换成功。
     */
    boolean changeCandidatePage(boolean backward);

    /**
     * 移动光标位置。
     *
     * @param position 目标位置。
     */
    void moveCursorPos(int position);

    /**
     * 获取所有可用方案列表。
     *
     * @return 可用方案数组。
     */
    SchemaItem[] availableSchemata();

    /**
     * 获取已启用的方案列表。
     *
     * @return 已启用方案数组。
     */
    SchemaItem[] enabledSchemata();

    /**
     * 设置已启用的方案列表。
     *
     * @param schemaIds 方案 ID 数组。
     * @return true 表示设置成功。
     */
    boolean setEnabledSchemata(String[] schemaIds);

    /**
     * 获取已选择的方案列表。
     *
     * @return 已选择方案数组。
     */
    SchemaItem[] selectedSchemata();

    /**
     * 获取当前选择的方案 ID。
     *
     * @return 方案 ID。
     */
    String selectedSchemaId();

    /**
     * 选择方案。
     *
     * @param schemaId 方案 ID。
     * @return true 表示选择成功。
     */
    boolean selectSchema(String schemaId);

    /**
     * 获取当前方案。
     *
     * @return 当前方案对象。
     */
    RimeSchema currentSchema();

    /**
     * 提交编码区内容。
     *
     * @return true 表示提交成功。
     */
    boolean commitComposition();

    /**
     * 清空编码区。
     */
    void clearComposition();

    /**
     * 设置运行时选项。
     *
     * @param option 选项名称。
     * @param value 选项值。
     */
    void setRuntimeOption(
            String option,
            boolean value
    );

    /**
     * 获取运行时选项的值。
     *
     * @param option 选项名称。
     * @return 选项值。
     */
    boolean getRuntimeOption(String option);

    /**
     * 获取候选词列表。
     *
     * @param startIndex 起始索引。
     * @param limit 限制数量。
     * @return 候选词数组。
     */
    CandidateItem[] getCandidates(
            int startIndex,
            int limit
    );
}
