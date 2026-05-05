package com.osfans.trime.core;

/**
 * Rime 键位修饰符枚举。
 * 定义 Rime 键位修饰符标志,对应于 librime/key_table.h。
 * 使用 Java 'int' (32位有符号整数) 来存储位掩码。
 */
public enum KeyModifier {
    /** 无修饰符 */
    None(0),
    /** Shift 键 */
    Shift(1 << 0),
    /** CapsLock 锁定键 */
    Lock(1 << 1),
    /** Control 键 */
    Control(1 << 2),
    /** Mod1 修饰符 */
    Mod1(1 << 3),
    /** Alt 键(等同于 Mod1) */
    Alt(Mod1.modifier),
    /** Mod2 修饰符(NumLock) */
    Mod2(1 << 4),
    /** Mod3 修饰符 */
    Mod3(1 << 5),
    /** Mod4 修饰符 */
    Mod4(1 << 6),
    /** Mod5 修饰符 */
    Mod5(1 << 7),
    /** 鼠标按钮1 */
    Button1(1 << 8),
    /** 鼠标按钮2 */
    Button2(1 << 9),
    /** 鼠标按钮3 */
    Button3(1 << 10),
    /** 鼠标按钮4 */
    Button4(1 << 11),
    /** 鼠标按钮5 */
    Button5(1 << 12),
    /** 已处理标志 */
    Handled(1 << 24),
    /** 转发标志 */
    Forward(1 << 25),
    /** 忽略标志(等同于 Forward) */
    Ignored(Forward.modifier),
    /** Super 键 */
    Super(1 << 26),
    /** Hyper 键 */
    Hyper(1 << 27),
    /** Meta 键 */
    Meta(1 << 28),
    /** 释放标志 */
    Release(1 << 30),
    /** 所有修饰符的组合掩码(0x5F001FFF) */
    Modifier(1593853951);

    // ==================== 成员变量 ====================
    /** 修饰符位掩码值 */
    private final int modifier;

    /**
     * 构造函数(整数修饰符值)。
     *
     * @param modifier 修饰符位掩码值。
     */
    KeyModifier(int modifier) {
        this.modifier = modifier;
    }

    /**
     * 构造函数(用于 Alt(Mod1) 和 Ignored(Forward) 这种重载)。
     *
     * @param value 修饰符值。
     * @param isValue 标记参数,用于区分构造函数。
     */
    KeyModifier(int value, boolean isValue) {
        this.modifier = value;
    }

    /**
     * 复制构造函数。
     *
     * @param other 要复制的 KeyModifier 实例。
     */
    KeyModifier(KeyModifier other) {
        this.modifier = other.modifier;
    }

    /**
     * 获取修饰符位掩码值。
     *
     * @return 修饰符值。
     */
    public int getModifier() {
        return modifier;
    }

    /**
     * 按位或运算(与另一个 KeyModifier)。
     * 模拟 Kotlin 的 infix fun or(other: KeyModifier)。
     *
     * @param other 另一个修饰符。
     * @return 两个修饰符的按位或结果。
     */
    public int or(KeyModifier other) {
        return this.modifier | other.modifier;
    }

    /**
     * 按位或运算(与整数)。
     * 模拟 Kotlin 的 infix fun or(other: UInt)。
     *
     * @param other 整数值。
     * @return 修饰符与整数的按位或结果。
     */
    public int or(int other) {
        return this.modifier | other;
    }

    /**
     * 添加修饰符到当前修饰符集合。
     * 模拟 Kotlin 的操作符重载 (UInt.plus(KeyModifier))。
     *
     * @param currentModifiers 当前修饰符集合。
     * @param modifier 要添加的修饰符。
     * @return 添加后的修饰符集合。
     */
    public static int add(int currentModifiers, KeyModifier modifier) {
        return currentModifiers | modifier.modifier;
    }

    /**
     * 从当前修饰符集合中移除修饰符。
     * 模拟 Kotlin 的操作符重载 (UInt.minus(KeyModifier))。
     *
     * @param currentModifiers 当前修饰符集合。
     * @param modifier 要移除的修饰符。
     * @return 移除后的修饰符集合。
     */
    public static int remove(int currentModifiers, KeyModifier modifier) {
        return currentModifiers & (~modifier.modifier);
    }
}
