package com.osfans.trime.core;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

/**
 * 所有 Rime 消息的抽象基类,替换 Kotlin 的 sealed class。
 *
 * @param <T> 此消息携带的数据类型。
 */
public abstract class RimeMessage<T> {

    // ==================== 成员变量 ====================
    // 抽象字段用于数据(替换 Kotlin 的 open val data)
    /** 消息携带的数据 */
    public final T data;

    /** 消息类型(抽象方法) */
    public abstract MessageType getMessageType();

    /**
     * 构造函数。
     *
     * @param data 消息数据。
     */
    public RimeMessage(T data) {
        this.data = data;
    }

    /**
     * 获取消息数据。
     *
     * @return 消息数据。
     */
    public T getData(){
        return this.data;
    }

    // --- 消息类型枚举 ---

    /**
     * Rime 消息类型枚举。
     */
    public enum MessageType {
        /** 未知消息 */
        Unknown,
        /** 方案消息 */
        Schema,
        /** 选项消息 */
        Option,
        /** 部署消息 */
        Deploy,
        /** 提交消息 */
        Commit,
        /** 编码区消息 */
        Composition,
        /** 候选词菜单消息 */
        Menu,
        /** 状态消息 */
        Status,
        /** 候选词消息 */
        Candidate,
        /** 按键消息 */
        Key,
    }

    // --- 嵌套消息类(替换 Kotlin 数据类) ---

    /**
     * 未知消息类。
     */
    public static final class UnknownMessage extends RimeMessage<Object[]> {
        /**
         * 构造函数。
         *
         * @param data 消息数据数组。
         */
        public UnknownMessage(Object[] data) {
            super(data);
        }

        @Override
        public MessageType getMessageType() {
            return MessageType.Unknown;
        }

        /**
         * 比较两个未知消息是否相等。
         * 使用 Arrays.equals 进行内容比较。
         *
         * @param o 要比较的对象。
         * @return true 表示相等。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UnknownMessage that = (UnknownMessage) o;
            // Use Arrays.equals for content comparison
            return Arrays.equals(data, that.data);
        }

        /**
         * 计算哈希码。
         * 使用 Arrays.hashCode 进行内容哈希。
         *
         * @return 哈希码值。
         */
        @Override
        public int hashCode() {
            // 使用 Arrays.hashCode 进行内容哈希
            return Arrays.hashCode(data);
        }
    }

    /**
     * 方案消息类。
     */
    public static final class SchemaMessage extends RimeMessage<SchemaItem> {
        /**
         * 构造函数。
         *
         * @param data 方案项数据。
         */
        public SchemaMessage(SchemaItem data) {
            super(data);
        }

        @Override
        public MessageType getMessageType() {
            return MessageType.Schema;
        }

        @Override
        public String toString() {
            return String.format("SchemaMessage(id=%s, name=%s)", data.getId(), data.getName());
        }
    }

    /**
     * 选项消息类。
     */
    public static final class OptionMessage extends RimeMessage<OptionMessage.Data> {
        /**
         * 构造函数。
         *
         * @param data 选项数据。
         */
        public OptionMessage(OptionMessage.Data data) {
            super(data);
        }

        @Override
        public MessageType getMessageType() {
            return MessageType.Option;
        }

        /**
         * 选项数据类。
         */
        public static final class Data {
            /** 选项名称 */
            private final String option;
            /** 选项值 */
            private final boolean value;

            /**
             * 构造函数。
             *
             * @param option 选项名称。
             * @param value 选项值。
             */
            public Data(String option, boolean value) {
                this.option = option;
                this.value = value;
            }

            public String getOption() {
                return option;
            }

            public boolean isValue() {
                return value;
            }

            // Generated equals/hashCode for Data class
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Data data = (Data) o;
                return value == data.value && Objects.equals(option, data.option);
            }

            @Override
            public int hashCode() {
                return Objects.hash(option, value);
            }
            @Override
            public String toString() {
                return String.format("OptionMessage.Data(option=%s, value=%b)", getOption(), isValue());
            }
        }

        @Override
        public String toString() {
            return String.format("OptionMessage(option=%s, value=%b)", data.getOption(), data.isValue());
        }
    }

    /**
     * 部署消息类。
     */
    public static final class DeployMessage extends RimeMessage<DeployMessage.State> {
        /**
         * 构造函数。
         *
         * @param data 部署状态数据。
         */
        public DeployMessage(DeployMessage.State data) {
            super(data);
        }

        @Override
        public MessageType getMessageType() {
            return MessageType.Deploy;
        }

        public enum State {
            Start,
            Success,
            Failure,
        }

        @Override
        public String toString() {
            return String.format("DeployMessage(state=%s)", data.name());
        }
    }

    /**
     * 提交文本消息类。
     */
    public static final class CommitTextMessage extends RimeMessage<RimeProto.Commit> {
        /**
         * 构造函数。
         *
         * @param data 提交数据。
         */
        public CommitTextMessage(RimeProto.Commit data) {
            super(data);
        }

        @Override
        public MessageType getMessageType() {
            return MessageType.Commit;
        }
    }

    /**
     * 编码区消息类。
     */
    public static final class CompositionMessage extends RimeMessage<RimeProto.Context.Composition> {
        /**
         * 构造函数。
         *
         * @param data 编码区数据。
         */
        public CompositionMessage(RimeProto.Context.Composition data) {
            super(data);
        }

        @Override
        public MessageType getMessageType() {
            return MessageType.Composition;
        }
    }

    /**
     * 候选词菜单消息类。
     */
    public static final class CandidateMenuMessage extends RimeMessage<RimeProto.Context.Menu> {
        /**
         * 构造函数。
         *
         * @param data 菜单数据。
         */
        public CandidateMenuMessage(RimeProto.Context.Menu data) {
            super(data);
        }

        @Override
        public MessageType getMessageType() {
            return MessageType.Menu;
        }
    }

    /**
     * 状态消息类。
     */
    public static final class StatusMessage extends RimeMessage<RimeProto.Status> {
        /**
         * 构造函数。
         *
         * @param data 状态数据。
         */
        public StatusMessage(RimeProto.Status data) {
            super(data);
        }

        @Override
        public MessageType getMessageType() {
            return MessageType.Status;
        }
    }

    /**
     * 候选词列表消息类。
     */
    public static final class CandidateListMessage extends RimeMessage<CandidateListMessage.Data> {
        /**
         * 构造函数。
         *
         * @param data 候选词列表数据。
         */
        public CandidateListMessage(CandidateListMessage.Data data) {
            super(data);
        }

        @Override
        public MessageType getMessageType() {
            return MessageType.Candidate;
        }

        /**
         * 候选词列表数据类。
         */
        public static final class Data {
            /** 候选词总数 */
            private final int total;
            /** 候选词数组 */
            private final CandidateItem[] candidates;

            /**
             * 构造函数。
             *
             * @param total 候选词总数。
             * @param candidates 候选词数组。
             */
            public Data(int total, CandidateItem[] candidates) {
                this.total = total;
                this.candidates = candidates;
            }

            public int getTotal() {
                return total;
            }

            public CandidateItem[] getCandidates() {
                return candidates;
            }

            @Override
            public String toString() {
                String candidatesStr;
                if (candidates.length > 5) {
                    candidatesStr = Arrays.toString(Arrays.copyOf(candidates, 5)) + ", ...]";
                } else {
                    candidatesStr = Arrays.toString(candidates);
                }
                return String.format("total=%d, candidates=%s", total, candidatesStr);
            }

            // Generated equals/hashCode for Data class using array content
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Data data = (Data) o;
                return total == data.total && Arrays.equals(candidates, data.candidates);
            }

            @Override
            public int hashCode() {
                int result = total;
                result = 31 * result + Arrays.hashCode(candidates);
                return result;
            }
        }
    }

    /**
     * 按键消息类。
     */
    public static final class KeyMessage extends RimeMessage<KeyMessage.Data> {
        /**
         * 构造函数。
         *
         * @param data 按键数据。
         */
        public KeyMessage(KeyMessage.Data data) {
            super(data);
        }

        @Override
        public MessageType getMessageType() {
            return MessageType.Key;
        }

        /**
         * 按键数据类。
         */
        public static final class Data {
            /** 键值 */
            private final KeyValue value;
            /** 键修饰符 */
            private final KeyModifiers modifiers;
            /** 是否为虚拟键 */
            private final boolean isVirtual;

            /**
             * 构造函数。
             *
             * @param value 键值。
             * @param modifiers 键修饰符。
             * @param isVirtual 是否为虚拟键。
             */
            public Data(KeyValue value, KeyModifiers modifiers, boolean isVirtual) {
                this.value = value;
                this.modifiers = modifiers;
                this.isVirtual = isVirtual;
            }

            public KeyValue getValue() {
                return value;
            }

            public KeyModifiers getModifiers() {
                return modifiers;
            }

            public boolean isVirtual() {
                return isVirtual;
            }

            // Generated equals/hashCode for Data class
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Data data = (Data) o;
                return isVirtual == data.isVirtual &&
                        Objects.equals(value, data.value) &&
                        Objects.equals(modifiers, data.modifiers);
            }

            @Override
            public int hashCode() {
                return Objects.hash(value, modifiers, isVirtual);
            }
        }
    }

    // --- 静态工厂方法(替换 Kotlin 伴生对象) ---

    /** 消息类型数组 */
    private static final MessageType[] TYPES = MessageType.values();

    /**
     * 工厂方法,从原生参数创建 RimeMessage。
     * 此方法通常从 JNI 调用。
     *
     * @param type MessageType 的序数(int)。
     * @param params 来自原生端的参数数组。
     * @return 构造的 RimeMessage 实例。
     * @throws IllegalArgumentException 如果类型序数无效。
     */
    public static RimeMessage<?> nativeCreate(
            int type,
            Object[] params
    ) {
        if (type < 0 || type >= TYPES.length) {
            return new UnknownMessage(params);
        }

        switch (TYPES[type]) {
            case Schema:
                String schemaString = (String) params[0];
                String[] parts = schemaString.split("/", 2);
                String id = parts[0];
                String name = parts.length > 1 ? parts[1] : id;
                return new SchemaMessage(new SchemaItem(id, name));

            case Option:
                String value = (String) params[0];
                boolean isSet = !value.startsWith("!");
                String optionName = value.substring(isSet ? 0 : 1); // remove '!' if present
                return new OptionMessage(
                        new OptionMessage.Data(optionName, isSet)
                );

            case Deploy:
                String stateStr = (String) params[0];
                // Java equivalent of Kotlin's replaceFirstChar { it.titlecase() }
                String capitalizedState = stateStr.substring(0, 1).toUpperCase(Locale.ROOT) + stateStr.substring(1);
                return new DeployMessage(DeployMessage.State.valueOf(capitalizedState));

            case Commit:
                return new CommitTextMessage((RimeProto.Commit) params[0]);

            case Composition:
                return new CompositionMessage((RimeProto.Context.Composition) params[0]);

            case Menu:
                return new CandidateMenuMessage((RimeProto.Context.Menu) params[0]);

            case Status:
                return new StatusMessage((RimeProto.Status) params[0]);

            case Candidate:
                return new CandidateListMessage(
                        new CandidateListMessage.Data(
                                (Integer) params[0],
                                (CandidateItem[]) params[1]
                        )
                );

            case Key:
                return new KeyMessage(
                        new KeyMessage.Data(
                                new KeyValue((Integer) params[0]),
                                KeyModifiers.of((Integer) params[1]),
                                (Boolean) params[2]
                        )
                );

            case Unknown:
            default:
                return new UnknownMessage(params);
        }
    }

    /**
     * 工厂方法,从已知的 MessageType 和参数创建 RimeMessage。
     *
     * @param type 特定的消息类型。
     * @param params 匹配所需消息结构的参数数组。
     * @return 构造的 RimeMessage 实例。
     */
    public static RimeMessage<?> create(
            MessageType type,
            Object[] params
    ) {
        return nativeCreate(type.ordinal(), params);
    }
}
