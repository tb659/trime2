package com.osfans.trime.core;

import java.util.Arrays;
import java.util.Objects;

/**
 * Rime 协议数据类,替换 Kotlin 数据类。
 * 此类作为所有协议相关数据结构的容器,
 * 用于与 Rime 引擎通信。
 */
public final class RimeProto {

    // --- Commit 数据类 ---

    /**
     * 提交数据类。
     */
    public static final class Commit {
        /** 提交文本 */
        private final String text;

        /**
         * 构造函数。
         *
         * @param text 提交文本。
         */
        public Commit(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Commit commit = (Commit) o;
            return Objects.equals(text, commit.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text);
        }

        @Override
        public String toString() {
            return "Commit(text='" + text + "')";
        }
    }

    // --- Candidate 数据类 ---

    /**
     * 候选词数据类。
     */
    public static final class Candidate {
        /** 候选词文本 */
        private final String text;
        /** 候选词注释 */
        private final String comment;
        /** 候选词标签 */
        private final String label;

        /**
         * 构造函数。
         *
         * @param text 候选词文本。
         * @param comment 候选词注释。
         * @param label 候选词标签。
         */
        public Candidate(String text, String comment, String label) {
            this.text = text;
            this.comment = comment;
            this.label = label;
        }

        public String getText() {
            return text;
        }

        public String getComment() {
            return comment;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Candidate candidate = (Candidate) o;
            return Objects.equals(text, candidate.text) &&
                    Objects.equals(comment, candidate.comment) &&
                    Objects.equals(label, candidate.label);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, comment, label);
        }

        @Override
        public String toString() {
            return "Candidate(text='" + text + "', comment='" + comment + "', label='" + label + "')";
        }
    }

    // --- Context 数据类 ---

    /**
     * 上下文数据类。
     */
    public static final class Context {
        /** 编码区组合 */
        private final Composition composition;
        /** 候选词菜单 */
        private final Menu menu;
        /** 输入文本 */
        private final String input;
        /** 光标位置 */
        private final int caretPos;

        /**
         * 默认构造函数(匹配 Kotlin 的默认参数)。
         */
        public Context() {
            this(new Composition(), new Menu(), "", 0);
        }

        public Context(Composition composition, Menu menu, String input, int caretPos) {
            this.composition = composition;
            this.menu = menu;
            this.input = input;
            this.caretPos = caretPos;
        }

        public Composition getComposition() {
            return composition;
        }

        public Menu getMenu() {
            return menu;
        }

        public String getInput() {
            return input;
        }

        public int getCaretPos() {
            return caretPos;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Context context = (Context) o;
            return caretPos == context.caretPos &&
                    Objects.equals(composition, context.composition) &&
                    Objects.equals(menu, context.menu) &&
                    Objects.equals(input, context.input);
        }

        @Override
        public int hashCode() {
            return Objects.hash(composition, menu, input, caretPos);
        }

        @Override
        public String toString() {
            return "Context(composition=" + composition + ", menu=" + menu + ", input='" + input + "', caretPos=" + caretPos + ")";
        }

        // --- Context.Composition 数据类 ---

        /**
         * 编码区组合数据类。
         */
        public static final class Composition {
            /** 长度 */
            private final int length;
            /** 光标位置 */
            private final int cursorPos;
            /** 选择起始位置 */
            private final int selStart;
            /** 选择结束位置 */
            private final int selEnd;
            /** 预编辑文本 */
            private final String preedit;
            /** 提交文本预览 */
            private final String commitTextPreview;

            /**
             * 默认构造函数(匹配 Kotlin 的主构造函数,带默认值)。
             */
            public Composition() {
                this(0, 0, 0, 0, null, null);
            }

            public Composition(int length, int cursorPos, int selStart, int selEnd, String preedit, String commitTextPreview) {
                this.length = length;
                this.cursorPos = cursorPos;
                this.selStart = selStart;
                this.selEnd = selEnd;
                this.preedit = preedit;
                this.commitTextPreview = commitTextPreview;
            }

            // Secondary constructor matching Kotlin's secondary constructor (text conversion)
            public Composition(String text) {
                this(
                        text.length(),
                        text.length(),
                        text.length(),
                        text.length(),
                        text,
                        null // commitTextPreview remains null
                );
            }

            public int getLength() {
                return length;
            }

            public int getCursorPos() {
                return cursorPos;
            }

            public int getSelStart() {
                return selStart;
            }

            public int getSelEnd() {
                return selEnd;
            }

            public String getPreedit() {
                return preedit;
            }

            public String getCommitTextPreview() {
                return commitTextPreview;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Composition that = (Composition) o;
                return length == that.length &&
                        cursorPos == that.cursorPos &&
                        selStart == that.selStart &&
                        selEnd == that.selEnd &&
                        Objects.equals(preedit, that.preedit) &&
                        Objects.equals(commitTextPreview, that.commitTextPreview);
            }

            @Override
            public int hashCode() {
                return Objects.hash(length, cursorPos, selStart, selEnd, preedit, commitTextPreview);
            }

            @Override
            public String toString() {
                return "Composition(length=" + length + ", cursorPos=" + cursorPos + ", selStart=" + selStart + ", selEnd=" + selEnd + ", preedit='" + preedit + "', commitTextPreview='" + commitTextPreview + "')";
            }
        }

        // --- Context.Menu 数据类 ---

        /**
         * 菜单数据类。
         */
        public static final class Menu {
            /** 每页大小 */
            private final int pageSize;
            /** 页码 */
            private final int pageNumber;
            /** 是否为最后一页 */
            private final boolean isLastPage;
            /** 高亮候选词索引 */
            private final int highlightedCandidateIndex;
            /** 候选词数组 */
            private final Candidate[] candidates;
            /** 选择键 */
            private final String selectKeys;
            /** 选择标签数组 */
            private final String[] selectLabels;

            /**
             * 默认构造函数(匹配 Kotlin 的主构造函数,带默认值)。
             */
            public Menu() {
                this(0, 0, false, 0, new Candidate[0], null, new String[0]);
            }

            public Menu(int pageSize, int pageNumber, boolean isLastPage, int highlightedCandidateIndex, Candidate[] candidates, String selectKeys, String[] selectLabels) {
                this.pageSize = pageSize;
                this.pageNumber = pageNumber;
                this.isLastPage = isLastPage;
                this.highlightedCandidateIndex = highlightedCandidateIndex;
                this.candidates = candidates;
                this.selectKeys = selectKeys;
                this.selectLabels = selectLabels;
            }

            public int getPageSize() {
                return pageSize;
            }

            public int getPageNumber() {
                return pageNumber;
            }

            public boolean isLastPage() {
                return isLastPage;
            }

            public int getHighlightedCandidateIndex() {
                return highlightedCandidateIndex;
            }

            public Candidate[] getCandidates() {
                return candidates;
            }

            public String getSelectKeys() {
                return selectKeys;
            }

            public String[] getSelectLabels() {
                return selectLabels;
            }

            // Custom equals implementation using Arrays.equals for array content comparison
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Menu menu = (Menu) o;
                return pageSize == menu.pageSize &&
                        pageNumber == menu.pageNumber &&
                        isLastPage == menu.isLastPage &&
                        highlightedCandidateIndex == menu.highlightedCandidateIndex &&
                        Objects.equals(selectKeys, menu.selectKeys) &&
                        Arrays.equals(candidates, menu.candidates) && // Array content check
                        Arrays.equals(selectLabels, menu.selectLabels); // Array content check
            }

            // Custom hashCode implementation using Arrays.hashCode for array content
            @Override
            public int hashCode() {
                int result = Objects.hash(pageSize, pageNumber, isLastPage, highlightedCandidateIndex, selectKeys);
                result = 31 * result + Arrays.hashCode(candidates);
                result = 31 * result + Arrays.hashCode(selectLabels);
                return result;
            }

            @Override
            public String toString() {
                return "Menu(pageSize=" + pageSize + ", pageNumber=" + pageNumber + ", isLastPage=" + isLastPage + ", highlightedCandidateIndex=" + highlightedCandidateIndex + ", candidates=" + Arrays.toString(candidates) + ", selectKeys='" + selectKeys + "', selectLabels=" + Arrays.toString(selectLabels) + ")";
            }
        }
    }

    // --- Status 数据类 ---

    /**
     * 状态数据类。
     */
    public static final class Status {
        /** 方案 ID */
        private final String schemaId;
        /** 方案名称 */
        private final String schemaName;
        /** 是否禁用 */
        private final boolean isDisabled;
        /** 是否正在编码 */
        private final boolean isComposing;
        /** 是否为 ASCII 模式 */
        private final boolean isAsciiMode;
        /** 是否为全角 */
        private final boolean isFullShape;
        /** 是否为简体 */
        private final boolean isSimplified;
        /** 是否为繁体 */
        private final boolean isTraditional;
        /** 是否为 ASCII 标点 */
        private final boolean isAsciiPunch;

        /**
         * 默认构造函数(匹配 Kotlin 的主构造函数,带默认值)。
         */
        public Status() {
            this("", "", true, false, true, false, false, false, true);
        }

        public Status(String schemaId, String schemaName, boolean isDisabled, boolean isComposing, boolean isAsciiMode, boolean isFullShape, boolean isSimplified, boolean isTraditional, boolean isAsciiPunch) {
            this.schemaId = schemaId;
            this.schemaName = schemaName;
            this.isDisabled = isDisabled;
            this.isComposing = isComposing;
            this.isAsciiMode = isAsciiMode;
            this.isFullShape = isFullShape;
            this.isSimplified = isSimplified;
            this.isTraditional = isTraditional;
            this.isAsciiPunch = isAsciiPunch;
        }

        public String getSchemaId() {
            return schemaId;
        }

        public String getSchemaName() {
            return schemaName;
        }

        public boolean isDisabled() {
            return isDisabled;
        }

        public boolean isComposing() {
            return isComposing;
        }

        public boolean isAsciiMode() {
            return isAsciiMode;
        }

        public boolean isFullShape() {
            return isFullShape;
        }

        public boolean isSimplified() {
            return isSimplified;
        }

        public boolean isTraditional() {
            return isTraditional;
        }

        public boolean isAsciiPunch() {
            return isAsciiPunch;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Status status = (Status) o;
            return isDisabled == status.isDisabled &&
                    isComposing == status.isComposing &&
                    isAsciiMode == status.isAsciiMode &&
                    isFullShape == status.isFullShape &&
                    isSimplified == status.isSimplified &&
                    isTraditional == status.isTraditional &&
                    isAsciiPunch == status.isAsciiPunch &&
                    Objects.equals(schemaId, status.schemaId) &&
                    Objects.equals(schemaName, status.schemaName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(schemaId, schemaName, isDisabled, isComposing, isAsciiMode, isFullShape, isSimplified, isTraditional, isAsciiPunch);
        }

        @Override
        public String toString() {
            return "Status(schemaId='" + schemaId + "', schemaName='" + schemaName + "', isDisabled=" + isDisabled + ", isComposing=" + isComposing + ", isAsciiMode=" + isAsciiMode + ", isFullShape=" + isFullShape + ", isSimplified=" + isSimplified + ", isTraditional=" + isTraditional + ", isAsciiPunch=" + isAsciiPunch + ")";
        }
    }
}
