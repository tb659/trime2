package com.osfans.trime.core;

import java.util.concurrent.Executor;

/**
 * Rime 生命周期所有者接口。
 * 用于拥有 RimeLifecycle 实例的组件,提供生命周期管理和执行器访问。
 */
public interface RimeLifecycleOwner {

    /**
     * 获取 Rime 生命周期实例。
     *
     * @return RimeLifecycle 实例。
     */
    RimeLifecycle getLifecycle();

    /**
     * 获取生命周期执行器。
     * 等价于 Kotlin 扩展属性: val RimeLifecycleOwner.lifecycleScope
     * 所有依赖生命周期的任务都应该在此执行器上运行。
     *
     * @return 生命周期绑定的执行器。
     */
    default Executor getLifecycleExecutor() {
        return getLifecycle().getLifecycleExecutor();
    }
}
