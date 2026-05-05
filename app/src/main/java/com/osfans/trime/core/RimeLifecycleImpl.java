package com.osfans.trime.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import timber.log.Timber;

/**
 * RimeLifecycle 的实现类,处理状态转换和观察者通知。
 * 此类替换 Kotlin 的 MutableStateFlow。
 */
public class RimeLifecycleImpl implements RimeLifecycle {

    // ==================== 成员变量 ====================
    // 当前状态的线程安全持有者。
    /** 当前状态(原子引用,线程安全) */
    private final AtomicReference<State> currentState = new AtomicReference<>(State.STOPPED);

    // 已注册观察者的线程安全集合。
    /** 状态观察者集合(同步 HashSet,线程安全) */
    private final Set<StateObserver> observers = Collections.synchronizedSet(new HashSet<>());

    // 专用于生命周期绑定任务的单线程执行器(替换 CoroutineScope)。
    /** 生命周期执行器(单线程,用于执行生命周期绑定的任务) */
    private final ExecutorService lifecycleExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setName("rime-lifecycle");
        return t;
    });

    /**
     * 获取当前状态。
     *
     * @return 当前生命周期状态。
     */
    @Override
    public State getCurrentState() {
        return currentState.get();
    }

    /**
     * 添加状态观察者。
     *
     * @param observer 要添加的观察者。
     */
    @Override
    public void addObserver(StateObserver observer) {
        observers.add(observer);
        // 订阅时立即通知当前状态,模仿 StateFlow 行为。
        observer.onStateChange(currentState.get());
    }

    /**
     * 移除状态观察者。
     *
     * @param observer 要移除的观察者。
     */
    @Override
    public void removeObserver(StateObserver observer) {
        observers.remove(observer);
    }

    /**
     * 获取生命周期执行器。
     *
     * @return 生命周期执行器。
     */
    @Override
    public Executor getLifecycleExecutor() {
        return lifecycleExecutor;
    }

    /**
     * 执行状态转换,验证转换并通知观察者。
     *
     * @param newState 目标状态。
     */
    public void emitState(State newState) {
        State oldState = currentState.get();

        // 1. 检查有效转换(替换 Kotlin 的 checkAtState 逻辑)
        checkTransition(oldState, newState);

        // 2. 执行状态更新
        if (currentState.compareAndSet(oldState, newState)) {
            Timber.d("RimeLifecycle transition: %s -> %s", oldState, newState);

            // 3. 在 STOPPED 时处理清理
            if (newState == State.STOPPED) {
                // 通过取消所有任务来复制 CoroutineScope.cancelChildren() 逻辑。
                lifecycleExecutor.shutdownNow();
            }

            // 4. 通知所有观察者
            for (StateObserver observer : observers) {
                try {
                    observer.onStateChange(newState);
                } catch (Exception e) {
                    Timber.e(e, "Observer failed to handle state change to %s", newState);
                }
            }
        }
    }

    /**
     * 检查状态转换是否有效。
     *
     * @param oldState 旧状态。
     * @param newState 新状态。
     * @throws IllegalStateException 如果状态转换无效。
     */
    private void checkTransition(State oldState, State newState) {
        State expectedOldState = null;
        switch (newState) {
            case STARTING:
                expectedOldState = State.STOPPED;
                break;
            case READY:
                expectedOldState = State.STARTING;
                break;
            case STOPPING:
                expectedOldState = State.READY;
                break;
            case STOPPED:
                expectedOldState = State.STOPPING;
                break;
        }

        if (expectedOldState != null && oldState != expectedOldState) {
            throw new IllegalStateException("Invalid RimeLifecycle transition. Expected " + expectedOldState + " but found " + oldState + " when trying to move to " + newState);
        }
    }
}
