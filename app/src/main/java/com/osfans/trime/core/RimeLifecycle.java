package com.osfans.trime.core;

import java.util.concurrent.Executor;

/**
 * Rime 生命周期接口。
 * 定义 Rime 生命周期状态和生命周期管理的基本契约,替代 Kotlin 的 RimeLifecycle 接口和 StateFlow 功能。
 */
public interface RimeLifecycle {

    /**
     * Rime 生命周期状态枚举。
     * 定义生命周期的四个阶段: STARTING(启动中)、READY(就绪)、STOPPING(停止中)、STOPPED(已停止)。
     */
    enum State {
        /** 启动中 */
        STARTING,
        /** 就绪状态,可以正常使用 */
        READY,
        /** 停止中 */
        STOPPING,
        /** 已停止 */
        STOPPED,
    }

    /**
     * 状态观察者接口。
     * 用于监听生命周期状态变化,替代 StateFlow。
     */
    interface StateObserver {
        /**
         * 状态变化回调。
         *
         * @param newState 新的生命周期状态。
         */
        void onStateChange(State newState);
    }

    /**
     * 获取当前生命周期状态。
     *
     * @return 当前的生命周期状态。
     */
    State getCurrentState();

    /**
     * 注册状态变化监听器。
     *
     * @param observer 状态观察者。
     */
    void addObserver(StateObserver observer);

    /**
     * 移除已注册的监听器。
     *
     * @param observer 要移除的状态观察者。
     */
    void removeObserver(StateObserver observer);

    /**
     * 获取生命周期执行器。
     * 提供生命周期绑定任务的执行上下文(替换 lifecycleScope)。
     * 所有依赖生命周期状态的任务都应该在此 Executor 上运行。
     *
     * @return 可以运行任务的 Executor。
     */
    Executor getLifecycleExecutor();
}
