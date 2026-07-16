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
    /** 自造词标识文本。 */
    private static final String SELF_CREATED_MARKER = "☯";
    /** 造词入口候选显示文本。 */
    private static final String CREATE_WORD_ACTION_TEXT = "添加自造词";
    // ==================== 成员变量 ====================
    /** 候选词文本 */
    private final String text;
    /** 候选词注释(如拼音、释义等) */
    private final String comment;
    /** 候选词是否来自用户词典/自造词链路。 */
    private final boolean selfCreated;
    /** 候选是否是“打开造词弹窗”的动作项。 */
    private final boolean createWordAction;
    /** 造词动作项对应的默认编码。 */
    private final String createWordCode;
    /** 候选词索引位置 */
    private int mIndex=-1;

    /**
     * 构造函数(仅文本)。
     *
     * @param text 候选词文本。
     */
    public CandidateItem(String text) {
        this(text, "", false);
    }

    /**
     * 构造函数(文本和注释)。
     *
     * @param text 候选词文本。
     * @param comment 候选词注释,如果为 null 则使用空字符串。
     */
    public CandidateItem(String text, String comment) {
        this(text, comment, false);
    }

    /**
     * 构造函数(文本、注释和自造词标记)。
     *
     * @param text 候选词文本。
     * @param comment 候选词注释,如果为 null 则使用空字符串。
     * @param selfCreated true 表示该候选来自用户词典/自造词链路。
     */
    public CandidateItem(String text, String comment, boolean selfCreated) {
        this(text, comment, selfCreated, false, "");
    }

    /**
     * 构造函数(完整参数)。
     *
     * @param text 候选词文本。
     * @param comment 候选词注释。
     * @param selfCreated true 表示该候选来自用户词典/自造词链路。
     * @param createWordAction true 表示该候选点击后打开造词弹窗。
     * @param createWordCode 造词弹窗默认带入的编码。
     */
    private CandidateItem(
            String text,
            String comment,
            boolean selfCreated,
            boolean createWordAction,
            String createWordCode) {
        this.text = text;
        this.comment = (comment != null) ? comment : "";
        this.selfCreated = selfCreated;
        this.createWordAction = createWordAction;
        this.createWordCode = createWordCode != null ? createWordCode : "";
    }

    /**
     * 创建“添加自造词”动作候选。
     *
     * @param code 当前预输入编码。
     * @param comment 需要展示给用户的辅助说明。
     * @return 动作候选项。
     */
    public static CandidateItem createWordAction(String code, String comment) {
        return new CandidateItem(CREATE_WORD_ACTION_TEXT, comment, false, true, code);
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
        if (!selfCreated) {
            return comment;
        }
        if (comment.isEmpty()) {
            return SELF_CREATED_MARKER;
        }
        if (comment.contains(SELF_CREATED_MARKER)) {
            return comment;
        }
        return comment + " " + SELF_CREATED_MARKER;
    }

    /**
     * 候选是否为自造词。
     *
     * @return true 表示该候选来自用户词典或 Java 补出的 learned mixed completion。
     */
    public boolean isSelfCreated() {
        return selfCreated;
    }

    /**
     * 当前候选是否为“添加自造词”动作项。
     */
    public boolean isCreateWordAction() {
        return createWordAction;
    }

    /**
     * 获取造词动作项默认带入的编码。
     */
    public String getCreateWordCode() {
        return createWordCode;
    }

    /**
     * 获取原始注释内容（不附加小太极标识）。
     *
     * @return 原始注释文本。
     */
    public String getRawComment() {
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
        return selfCreated == that.selfCreated
                && createWordAction == that.createWordAction
                && Objects.equals(text, that.text)
                && Objects.equals(comment, that.comment)
                && Objects.equals(createWordCode, that.createWordCode);
    }

    /**
     * 计算哈希码。
     * 基于文本和注释计算。
     *
     * @return 哈希码值。
     */
    @Override
    public int hashCode() {
        return Objects.hash(text, comment, selfCreated, createWordAction, createWordCode);
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
