package com.osfans.trime.core;

import com.osfans.trime.core.RimeLifecycle.State;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 工具类,包含静态方法以替换 Kotlin 的挂起函数
 * (whenAtState, whenReady)用于状态依赖执行。
 * 注意:这些方法是阻塞的,如果在需要响应性的上下文中调用,
 * 应该在后台线程上执行。
 */
public final class RimeLifecycleUtils {

    /**
     * 私有构造函数,防止实例化。
     */
    private RimeLifecycleUtils() {
        // Utility class
    }

    /**
     * 当生命周期达到特定状态时执行任务,阻塞调用线程直到那时。
     * 任务本身在 RimeLifecycle 的专用执行器上执行。
     *
     * @param lifecycle RimeLifecycle 实例。
     * @param state 要等待的目标状态。
     * @param block 要执行的任务。
     * @param <T> 任务的返回类型。
     * @return 执行任务的结果。
     * @throws InterruptedException 如果等待线程被中断。
     * @throws ExecutionException 如果执行的任务抛出异常。
     * @throws Exception 如果内部状态等待逻辑超时。
     */
    public static <T> T whenAtState(
            RimeLifecycle lifecycle,
            State state,
            Callable<T> block
    ) throws InterruptedException, ExecutionException, Exception {

        // 1. 如果已经处于目标状态,立即在执行器上执行
        if (lifecycle.getCurrentState() == state) {
            FutureTask<T> future = new FutureTask<>(block);
            lifecycle.getLifecycleExecutor().execute(future);
            return future.get();
        }

        // 2. 使用标准 Java 并发等待状态

        final Object lock = new Object();
        final AtomicReference<State> currentStateRef = new AtomicReference<>(lifecycle.getCurrentState());

        RimeLifecycle.StateObserver waiter = newState -> {
            synchronized (lock) {
                currentStateRef.set(newState);
                if (newState == state) {
                    lock.notifyAll(); // Signal the waiting thread
                }
            }
        };

        // 注册观察者
        lifecycle.addObserver(waiter);
        try {
            synchronized (lock) {
                // 等待直到状态匹配目标
                while (currentStateRef.get() != state) {
                    // 设置超时以防止在错误情况下无限阻塞
                    lock.wait(5000);
                    if (currentStateRef.get() != state) {
                        // 再次检查,如果超时则抛出异常
                        throw new Exception("Timeout waiting for RimeLifecycle state to reach " + state);
                    }
                }
            }

            // 3. 状态已达到:在指定执行器上执行块
            FutureTask<T> future = new FutureTask<>(block);
            lifecycle.getLifecycleExecutor().execute(future);
            return future.get();

        } finally {
            // 4. 清理观察者
            lifecycle.removeObserver(waiter);
        }
    }

    /**
     * 当 Rime 生命周期处于 READY 状态时执行任务,阻塞调用线程直到那时。
     *
     * @param lifecycle RimeLifecycle 实例。
     * @param block 要执行的任务。
     * @param <T> 任务的返回类型。
     * @return 执行任务的结果。
     */
    public static <T> T whenReady(
            RimeLifecycle lifecycle,
            Callable<T> block
    ) throws InterruptedException, ExecutionException, Exception {
        return whenAtState(lifecycle, State.READY, block);
    }
}
