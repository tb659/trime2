package com.osfans.trime.core;

import android.view.KeyEvent;

/**
 * Rime 键值类。
 * Java 标准表示形式,对应 Kotlin 的 @JvmInline value class KeyValue。
 * 保存 Rime 使用的内部整数值。
 */
public final class KeyValue {

    // ==================== 成员变量 ====================
    /** Rime 键值的内部整数表示 */
    private final int value;

    /**
     * 构造函数。
     *
     * @param value Rime 键值的原始整数值。
     */
    public KeyValue(int value) {
        this.value = value;
    }

    /**
     * 获取键值的原始内部整数值。
     *
     * @return 键值。
     */
    public int getValue() {
        return value;
    }

    /**
     * 计算与 Rime 值对应的 Android KeyEvent 代码。
     * 假设 RimeKeyMap 是项目中可用的工具类。
     *
     * @return Android KeyEvent 代码。
     */
    public int getKeyCode() {
        return RimeKeyMap.valToKeyCode(this.value);
    }

    /**
     * 返回 KeyValue 的十六进制字符串表示。
     * 零填充到4个字符(例如 "0x0020")。
     *
     * @return 十六进制字符串表示。
     */
    @Override
    public String toString() {
        // Equivalent to Kotlin's "0x" + value.toString(16).padStart(4, '0')
        String hex = Integer.toHexString(this.value);
        StringBuilder paddedHex = new StringBuilder(hex);
        while (paddedHex.length() < 4) {
            paddedHex.insert(0, '0');
        }
        return "0x" + paddedHex;
    }

    /**
     * 比较两个 KeyValue 是否相等。
     * 基于内部值进行比较。
     *
     * @param o 要比较的对象。
     * @return true 表示相等。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyValue keyValue = (KeyValue) o;
        return value == keyValue.value;
    }

    /**
     * 计算哈希码。
     * 基于内部值计算。
     *
     * @return 哈希码值。
     */
    @Override
    public int hashCode() {
        return value;
    }

    /**
     * 伴生对象功能(静态方法)。
     */
    public static class Companion {

        /**
         * 从 Android KeyEvent 创建 KeyValue 实例。
         * 假设 RimeKeyMap 是项目中可用的工具类。
         *
         * @param event Android KeyEvent。
         * @return 新的 KeyValue 实例,如果 event 为 null 则返回 null。
         */
        public static KeyValue fromKeyEvent(KeyEvent event) {
            if (event == null) {
                return null; // Or throw IllegalArgumentException
            }
            int rimeValue = RimeKeyMap.keyCodeToVal(event.getKeyCode());
            return new KeyValue(rimeValue);
        }
    }
}
