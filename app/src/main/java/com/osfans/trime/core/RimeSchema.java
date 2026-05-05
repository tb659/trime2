package com.osfans.trime.core;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * 表示 Rime 输入方案的配置和结构。
 * 此类从 Rime 配置文件加载其属性。
 */
public final class RimeSchema {

    // ==================== 成员变量 ====================
    /** 方案 ID */
    private final String schemaId;
    /** 开关列表 */
    private  List<Switch> switches;
    /** 字母表 */
    private  String alphabet;

    // --- Switch 数据类 ---

    /**
     * 开关数据类。
     */
    public static final class Switch {
        /** 开关名称 */
        private final String name;
        /** 选项列表 */
        private final List<String> options;
        /** 重置值 */
        private int reset;
        /** 状态列表 */
        private final List<String> states;

        /**
         * 默认构造函数(匹配 Kotlin 的默认参数)。
         */
        public Switch() {
            this("", Collections.emptyList(), 0, Collections.emptyList());
        }

        public Switch(String name, List<String> options, int reset, List<String> states) {
            this.name = name;
            this.options = (options != null) ? options : Collections.emptyList();
            this.reset = reset;
            this.states = (states != null) ? states : Collections.emptyList();
        }

        public String getName() {
            return name;
        }

        public List<String> getOptions() {
            return options;
        }

        public int getReset() {
            return reset;
        }

        public List<String> getStates() {
            return states;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Switch aSwitch = (Switch) o;
            return reset == aSwitch.reset &&
                    Objects.equals(name, aSwitch.name) &&
                    Objects.equals(options, aSwitch.options) &&
                    Objects.equals(states, aSwitch.states);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, options, reset, states);
        }

        @Override
        public String toString() {
            return "Switch(name='" + name + "', options=" + options + ", reset=" + reset + ", states=" + states + ")";
        }

        /**
         * 获取当前状态。
         *
         * @return 状态字符串。
         */
        public String getState() {
            if (!options.isEmpty()) {
                return getStates().get(reset);
            } else {
                return getStates().get(Rime.getRimeOption(getName()) ? 1 : 0);
            }
        }

        /**
         * 获取未选中状态。
         *
         * @return 未选中状态字符串。
         */
        public String getUnState() {
            if (!options.isEmpty()) {
                return getStates().get((reset + 1) % options.size());
            } else {
                return getStates().get(Rime.getRimeOption(getName()) ? 0 : 1);
            }
        }

        /**
         * 切换选项状态。
         */
        public void toggleOption() {
            if (!options.isEmpty()) {
                Rime.setRimeOption(options.get(reset), false);
                reset = (reset + 1) % options.size();
                Rime.setRimeOption(options.get(reset), true);
            } else {
                Log.w("TAG", "toggleOption:1 "+reset );
                reset = 1 - reset;
                Log.w("TAG", "toggleOption:2 "+reset );
                Rime.setRimeOption(getName(), reset == 1);
            }
        }
    }

    // --- 构造函数(替换 Kotlin 的主构造函数和 init 块) ---

    /**
     * 构造函数。
     * 从 Rime 配置文件加载方案属性。
     *
     * @param schemaId 方案 ID。
     */
    public RimeSchema(String schemaId) {
        this.schemaId = schemaId;
        RimeConfig schemaConfig;

        // 等价于 Kotlin 的 'when' 表达式,用于打开配置
        if (schemaId == null || schemaId.isEmpty()) {
            schemaConfig = RimeConfig.openConfig("default");
        } else if (schemaId.startsWith(".")) {
            schemaConfig = RimeConfig.openSchema(schemaId.substring(1));
        } else {
            schemaConfig = RimeConfig.openSchema(schemaId);
        }

        // 等价于 Kotlin 的 'use' 块(try-with-resources)
        try (RimeConfig config = schemaConfig) {

            // 1. 加载开关列表
            // 定义用于加载单个 Switch 对象的 RimeConfigAction
            RimeConfig.RimeConfigAction<Switch> switchLoader = (rc, path) -> {
                // 获取嵌套属性。Null 检查转换 Kotlin 的 ? : 默认值。
                String switchName = rc.getString(path + "/name");
                if (switchName == null) switchName = "";

                // 对于嵌套列表(options, states),我们需要另一个操作来获取字符串
                RimeConfig.RimeConfigAction<String> stringAction = (innerRc, innerPath) -> innerRc.getString(innerPath);

                List<String> options = rc.getList(path + "/options", stringAction);
                Integer resetInt = rc.getInt(path + "/reset");
                int reset = (resetInt != null) ? resetInt : 0;
                List<String> states = rc.getList(path + "/states", stringAction);

                return new Switch(switchName, options, reset, states);
            };

            this.switches = config.getList("switches", switchLoader);

            // 2. 加载字母表字符串
            String alpha = config.getString("speller/alphabet");
            this.alphabet = (alpha != null) ? alpha : "";

        } catch (Exception e) {
            // 处理 AutoCloseable 异常,如果 RimeConfig.close() 失败或在构造/加载期间发生异常
            // 为简单起见,我们在致命失败时初始化为默认值。
            // 在实际应用中,这应该更积极地抛出/记录日志。
            System.err.println("Error loading RimeSchema for ID: " + schemaId + ". " + e.getMessage());
            this.switches = Collections.emptyList();
            this.alphabet = "";
        }
    }

    // --- 公共 Getter 方法 ---

    /**
     * 获取方案 ID。
     *
     * @return 方案 ID。
     */
    public String getSchemaId() {
        return schemaId;
    }

    /**
     * 获取开关列表。
     *
     * @return 开关列表。
     */
    public List<Switch> getSwitches() {
        return switches;
    }

    /**
     * 获取字母表。
     *
     * @return 字母表字符串。
     */
    public String getAlphabet() {
        return alphabet;
    }
}
