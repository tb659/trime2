package com.osfans.trime.core;

import com.osfans.trime.core.KeyModifiers;

import java.util.Objects;

/**
 * Rime 按键事件数据类。
 * 表示 Rime 中的按键按下事件,包括键值、修饰符和字符串表示。
 */
public final class RimeKeyEvent {
    static {
        System.loadLibrary("rime_jni");
    }

    // ==================== 成员变量 ====================
    /** 键值 */
    private final int value;
    /** 修饰符位掩码 */
    private final int modifiers;
    /** 字符串表示(如 "Control_L+q") */
    private final String repr;

    // 懒加载字段
    /** 懒加载的 KeyValue 对象 */
    private volatile KeyValue keyVal = null;
    /** 懒加载的 KeyModifiers 对象 */
    private volatile KeyModifiers keyModifiers = null;

    // --- 构造函数(匹配 Kotlin 主构造函数) ---

    /**
     * 构造函数。
     *
     * @param value 键值。
     * @param modifiers 修饰符位掩码。
     * @param repr 字符串表示。
     */
    public RimeKeyEvent(int value, int modifiers, String repr) {
        this.value = value;
        this.modifiers = modifiers;
        this.repr = repr;
    }

    // --- Getter 方法 ---

    /**
     * 获取键值。
     *
     * @return 键值。
     */
    public int getValue() {
        return value;
    }

    /**
     * 获取修饰符位掩码。
     *
     * @return 修饰符位掩码。
     */
    public int getModifiers() {
        return modifiers;
    }

    /**
     * 获取按键事件的字符串表示(如 "Control_L+q")。
     *
     * @return 字符串表示。
     */
    public String getRepr() {
        return repr;
    }

    // --- 懒加载 Getter(等价于 'by lazy') ---

    /**
     * 懒加载并返回键值的 KeyValue 对象。
     * 使用双重检查锁定确保线程安全。
     *
     * @return KeyValue 对象。
     */
    public KeyValue getKeyVal() {
        if (keyVal == null) {
            synchronized (this) {
                if (keyVal == null) {
                    keyVal = new KeyValue(value);
                }
            }
        }
        return keyVal;
    }

    /**
     * 懒加载并返回修饰符的 KeyModifiers 对象。
     * 使用双重检查锁定确保线程安全。
     *
     * @return KeyModifiers 对象。
     */
    public KeyModifiers getKeyModifiers() {
        if (keyModifiers == null) {
            synchronized (this) {
                if (keyModifiers == null) {
                    // Assumes KeyModifiers.of(int) is available in Java
                    keyModifiers = KeyModifiers.of(modifiers);
                }
            }
        }
        return keyModifiers;
    }

    // --- 数据类方法 ---

    /**
     * 使用字符串表示(repr)作为主要字符串输出。
     *
     * @return 按键事件的字符串表示。
     */
    @Override
    public String toString() {
        return repr;
    }

    /**
     * 比较两个按键事件是否相等。
     * 基于 value、modifiers 和 repr 进行比较。
     *
     * @param o 要比较的对象。
     * @return true 表示相等。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RimeKeyEvent that = (RimeKeyEvent) o;
        return value == that.value &&
                modifiers == that.modifiers &&
                Objects.equals(repr, that.repr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, modifiers, repr);
    }

    // --- 伴生对象(静态字段和 JNI 方法) ---

    /**
     * 表示无按键事件的静态实例(RimeKeyEvent(0, 0, "0x0000"))。
     */
    public static final RimeKeyEvent None = new RimeKeyEvent(0, 0, "0x0000");

    /**
     * 通过 JNI 将按键事件的字符串表示解析为 RimeKeyEvent 对象。
     *
     * @param repr 字符串表示(如 "Control_L+q")。
     * @return 解析后的 RimeKeyEvent 对象。
     */
    public static native RimeKeyEvent parse(String repr);

    /**
     * 通过 JNI 根据名称获取 Rime 键码整数值。
     *
     * @param name 键名(如 "Control_L")。
     * @return 整数键码。
     */
    public static native int getKeycodeByName(String name);

    /**
     * 通过 JNI 根据名称获取 Rime 修饰符掩码整数值。
     *
     * @param name 修饰符名(如 "Control")。
     * @return 整数修饰符掩码。
     */
    public static native int getModifierByName(String name);
}
