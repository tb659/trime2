package com.osfans.trime.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import timber.log.Timber;

/**
 * Rime 配置访问类。
 * 提供通过 JNI 读写 Rime 配置文件的方法,模拟原始 Kotlin 结构。
 */
public final class RimeConfig implements AutoCloseable {

    // ==================== 成员变量 ====================
    /** JNI 对等指针 */
    private final long peer;

    // --- getConfigList 的功能接口 ---

    /**
     * 功能接口,定义从 RimeConfig 列表检索项的操作。
     * 替换 Kotlin 扩展函数 lambda (RimeConfig.(String) -> E?)。
     *
     * @param <E> 配置项的预期类型。
     */
    @FunctionalInterface
    public interface RimeConfigAction<E> {
        /**
         * 在 RimeConfig 实例上执行操作以获取值。
         *
         * @param config RimeConfig 实例。
         * @param path 配置项的路径/键。
         * @return 检索到的值,如果检索失败则返回 null。
         */
        E get(RimeConfig config, String path);
    }

    // --- 构造函数 ---

    /**
     * 私有构造函数。
     *
     * @param peer JNI 对等指针,不能为0。
     * @throws IllegalArgumentException 如果 peer 为0。
     */
    private RimeConfig(long peer) {
        if (peer == 0) {
            throw new IllegalArgumentException("RimeConfig peer must not be 0.");
        }
        this.peer = peer;
    }

    // --- 公共 Getter 方法 ---

    /**
     * 从配置中获取整数值。
     *
     * @param key 配置键。
     * @return 整数值,如果键未找到或不是整数则返回 null。
     */
    public Integer getInt(String key) {
        // JNI 函数返回 Integer(可空),匹配 Kotlin 的返回类型。
        return getRimeConfigInt(peer, key);
    }

    /**
     * 从配置中获取字符串值。
     *
     * @param key 配置键。
     * @return 字符串值,如果键未找到或不是字符串则返回 null。
     */
    public String getString(String key) {
        // JNI function returns String (which is nullable), matching Kotlin's return type.
        return getRimeConfigString(peer, key);
    }

    /**
     * 通过遍历列表项路径获取配置项列表。
     *
     * @param key 指向列表结构的配置键。
     * @param getAction 从列表路径检索特定类型 {@code E} 的操作。
     * @param <E> 列表元素的预期类型。
     * @return 检索到的 {@code E} 类型值列表。
     */
    public <E> List<E> getList(String key, RimeConfigAction<E> getAction) {
        // JNI returns Array<String>
        String[] paths = getRimeConfigListItemPath(peer, key);

        // Pre-allocate list size
        List<E> values = new ArrayList<>(paths.length);

        for (String path : paths) {
            // Replaces the Kotlin extension call: val value = getAction(this, path)
            E value = getAction.get(this, path);

            if (value == null) {
                // Log the failure to retrieve the expected item
                String stringValue = getString(path);
                Timber.w("Failed to get value '%s' as expected on path '%s'", stringValue, path);
                continue;
            }
            values.add(value);
        }
        return values;
    }

    // --- 公共 Setter 方法 ---

    /**
     * 在配置中设置布尔值。
     *
     * @param key 配置键。
     * @param value 要设置的布尔值。
     */
    public void setBool(String key, boolean value) {
        setRimeConfigBool(peer, key, value);
    }

    // --- AutoCloseable 实现 ---

    /**
     * 关闭底层的 Rime 配置句柄。
     */
    @Override
    public void close() {
        closeRimeConfig(peer);
    }

    // --- 静态工厂方法 ---

    /**
     * 打开 Rime 配置文件进行读取。
     *
     * @param configId 配置文件 ID(如 "default")。
     * @return 新的 RimeConfig 实例。
     * @throws IllegalArgumentException 如果无法打开配置。
     */
    public static RimeConfig openConfig(String configId) {
        long peer = openRimeConfig(configId);
        if (peer == 0) {
            throw new IllegalArgumentException("Failed to open Rime config: " + configId);
        }
        return new RimeConfig(peer);
    }

    /**
     * 打开 Rime 用户配置文件。
     *
     * @param configId 用户配置文件 ID。
     * @return 新的 RimeConfig 实例。
     * @throws IllegalArgumentException 如果无法打开用户配置。
     */
    public static RimeConfig openUserConfig(String configId) {
        long peer = openRimeUserConfig(configId);
        if (peer == 0) {
            throw new IllegalArgumentException("Failed to open Rime user config: " + configId);
        }
        return new RimeConfig(peer);
    }

    /**
     * 打开 Rime 方案配置文件。
     *
     * @param schemaId 方案 ID。
     * @return 新的 RimeConfig 实例。
     * @throws IllegalArgumentException 如果无法打开方案。
     */
    public static RimeConfig openSchema(String schemaId) {
        long peer = openRimeSchema(schemaId);
        if (peer == 0) {
            throw new IllegalArgumentException("Failed to open Rime schema: " + schemaId);
        }
        return new RimeConfig(peer);
    }

    // --- JNI 声明(伴生对象) ---

    // 注意:这些方法是静态和私有的,镜像 Kotlin 伴生对象结构。

    /**
     * 打开 Rime 配置文件(JNI)。
     *
     * @param configId 配置文件 ID。
     * @return JNI 对等指针,失败返回0。
     */
    private static native long openRimeConfig(String configId);

    /**
     * 打开 Rime 用户配置文件(JNI)。
     *
     * @param configId 用户配置文件 ID。
     * @return JNI 对等指针,失败返回0。
     */
    private static native long openRimeUserConfig(String configId);

    /**
     * 打开 Rime 方案配置文件(JNI)。
     *
     * @param schemaId 方案 ID。
     * @return JNI 对等指针,失败返回0。
     */
    private static native long openRimeSchema(String schemaId);

    /**
     * 从配置中获取整数值(JNI)。
     *
     * @param peer JNI 对等指针。
     * @param key 配置键。
     * @return 整数值,失败返回 null。
     */
    private static native Integer getRimeConfigInt(long peer, String key);

    /**
     * 从配置中获取字符串值(JNI)。
     *
     * @param peer JNI 对等指针。
     * @param key 配置键。
     * @return 字符串值,失败返回 null。
     */
    private static native String getRimeConfigString(long peer, String key);

    /**
     * 获取配置列表项路径数组(JNI)。
     *
     * @param peer JNI 对等指针。
     * @param key 配置键。
     * @return 路径字符串数组。
     */
    private static native String[] getRimeConfigListItemPath(long peer, String key);

    /**
     * 在配置中设置布尔值(JNI)。
     *
     * @param peer JNI 对等指针。
     * @param key 配置键。
     * @param value 布尔值。
     */
    private static native void setRimeConfigBool(long peer, String key, boolean value);

    /**
     * 关闭 Rime 配置(JNI)。
     *
     * @param peer JNI 对等指针。
     */
    private static native void closeRimeConfig(long peer);
}
