/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.core;

import java.util.Objects;

/**
 * 方案项。
 * 表示 Rime 输入法的一个输入方案,包含方案 ID 和名称。
 */
public class SchemaItem {
    // ==================== 成员变量 ====================
    /** 方案 ID(唯一标识符) */
    private final String id;
    /** 方案名称(显示给用户) */
    private final String name;

    /**
     * 构造函数(仅 ID)。
     *
     * @param id 方案 ID。
     */
    public SchemaItem(String id) {
        this(id, "");
    }

    /**
     * 构造函数(ID 和名称)。
     *
     * @param id 方案 ID。
     * @param name 方案名称,如果为 null 则使用空字符串。
     */
    public SchemaItem(String id, String name) {
        this.id = id;
        this.name = (name != null) ? name : "";
    }

    /**
     * 获取方案 ID。
     *
     * @return 方案 ID。
     */
    public String getId() {
        return id;
    }

    /**
     * 获取方案名称。
     *
     * @return 方案名称。
     */
    public String getName() {
        return name;
    }

    /**
     * 比较两个方案项是否相等。
     * 基于 ID 和名称进行比较。
     *
     * @param o 要比较的对象。
     * @return true 表示相等。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SchemaItem that = (SchemaItem) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name);
    }

    /**
     * 计算哈希码。
     * 基于 ID 和名称计算。
     *
     * @return 哈希码值。
     */
    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    /**
     * 返回方案项的字符串表示。
     * 格式: "SchemaItem(id='...', name='...')"。
     *
     * @return 方案项的字符串表示。
     */
    @Override
    public String toString() {
        return "SchemaItem(id='" + id + "', name='" + name + "')";
    }
}

