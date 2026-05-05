/*
 * SPDX-FileCopyrightText: 2015 - 2025 Rime community
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.osfans.trime.core;

import java.util.Objects;

/**
 * 候选词项。
 * 表示输入法候选区中的一个候选词,包含文本和注释信息。
 */
public class CandidateItem {
    // ==================== 成员变量 ====================
    /** 候选词文本 */
    private final String text;
    /** 候选词注释(如拼音、释义等) */
    private final String comment;
    /** 候选词索引位置 */
    private int mIndex=-1;

    /**
     * 构造函数(仅文本)。
     *
     * @param text 候选词文本。
     */
    public CandidateItem(String text) {
        this(text, "");
    }

    /**
     * 构造函数(文本和注释)。
     *
     * @param text 候选词文本。
     * @param comment 候选词注释,如果为 null 则使用空字符串。
     */
    public CandidateItem(String text, String comment) {
        this.text = text;
        this.comment = (comment != null) ? comment : "";
    }

    /**
     * 获取候选词文本。
     *
     * @return 候选词文本。
     */
    public String getText() {
        return text;
    }

    /**
     * 获取候选词注释。
     *
     * @return 候选词注释。
     */
    public String getComment() {
        return comment;
    }

    /**
     * 设置候选词索引。
     *
     * @param idx 索引位置。
     */
    public void setIndex(int idx){
        mIndex=idx;
    }

    /**
     * 获取候选词索引。
     *
     * @return 索引位置。
     */
    public int getIndex(){
        return mIndex;
    }

    /**
     * 比较两个候选词项是否相等。
     * 基于文本和注释进行比较,不考虑索引。
     *
     * @param o 要比较的对象。
     * @return true 表示相等。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CandidateItem that = (CandidateItem) o;
        return Objects.equals(text, that.text) && Objects.equals(comment, that.comment);
    }

    /**
     * 计算哈希码。
     * 基于文本和注释计算。
     *
     * @return 哈希码值。
     */
    @Override
    public int hashCode() {
        return Objects.hash(text, comment);
    }

    /**
     * 返回候选词的字符串表示。
     * 仅返回文本内容。
     *
     * @return 候选词文本。
     */
    @Override
    public String toString() {
        return text;
    }
}
