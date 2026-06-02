package com.osfans.trime.core;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import timber.log.Timber;

/**
 * RimeDispatcher 是单线程执行器的包装类,用于运行 RimeController。
 * 它提供向执行器内部队列分发任务的方法。
 * 还提供 stop() 方法以优雅地停止执行器并返回剩余任务。
 *
 * 改编自 [fcitx5-android/FcitxDispatcher.kt]。
 */
public final class RimeDispatcher {

    // --- 静态常量(来自伴生对象) ---

    /** 任务等待限制(毫秒) — 超过该时间未运行则记录警告 */
    private static final long JOB_WAITING_LIMIT = 2000L; // ms

    /**
     * submit 任务等待结果的超时(毫秒)。
     * 该值需要覆盖冷启动/方案组切换/部署等场景下 librime 的最坏耗时(解压资源、编译码表等),
     * 过短会导致任务被截断返回 null,从而让上层 UI 误以为 Rime 已就绪而出现空白/异常。
     */
    private static final long SUBMIT_TIMEOUT_MS = 30_000L; // ms

    /**
     * 提交任务并等待结果(最多 SUBMIT_TIMEOUT_MS 毫秒)。
     *
     * 任务会投递到 rime-main 单线程的内部队列中串行执行,与 nativeStartup() 共享同一线程,
     * 从根本上避免 librime JNI 跨线程并发调用导致的竞态。
     *
     * @param block 要执行的任务。
     * @param <T> 返回类型。
     * @return 任务结果,如果异常或超时则返回 null。
     */
    public <T> T submit(Callable<T> block) {
        if (!isRunning.get()) {
            // 调度器未运行(尚未 start() 或已 stop()),直接返回 null
            return null;
        }
        FutureTask<T> future = new FutureTask<>(block);
        WrappedRunnable wrapped = new WrappedRunnable(future, "submit");
        queue.offer(wrapped);
        try {
            return future.get(SUBMIT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            Timber.e(e, "RimeDispatcher submit timed out after %d ms", SUBMIT_TIMEOUT_MS);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Timber.e(e, "RimeDispatcher submit interrupted");
            return null;
        } catch (ExecutionException e) {
            Timber.e(e, "RimeDispatcher submit execution failed");
            return null;
        }
    }

    // --- 接口 ---

    /**
     * Rime 控制器接口。
     * 定义原生启动和 finalize 方法。
     */
    public interface RimeController {
        /** 原生启动 */
        void nativeStartup();
        /** 原生清理 */
        void nativeFinalize();
    }

    // --- WrappedRunnable 类 ---

    /**
     * 包装标准 Runnable 以跟踪执行时间并提供调试名称。
     */
    public static final class WrappedRunnable implements Runnable {
        /** 底层 Runnable */
        private final Runnable runnable;
        /** 任务名称 */
        private final String name;
        /** 创建时间 */
        private final long time;
        /** 是否已开始执行 */
        private boolean started = false;

        /**
         * 构造函数(带名称)。
         *
         * @param runnable 底层 Runnable。
         * @param name 任务名称。
         */
        public WrappedRunnable(Runnable runnable, String name) {
            this.runnable = runnable;
            this.name = name;
            this.time = System.currentTimeMillis();
        }

        /**
         * 构造函数(不带名称)。
         *
         * @param runnable 底层 Runnable。
         */
        public WrappedRunnable(Runnable runnable) {
            this(runnable, null);
        }

        /**
         * 检查是否已开始执行。
         *
         * @return true 表示已开始。
         */
        public boolean isStarted() {
            return started;
        }

        /**
         * 获取从创建到现在的时间差(毫秒)。
         *
         * @return 时间差。
         */
        private long getDelta() {
            return System.currentTimeMillis() - time;
        }

        /**
         * 执行任务,如果等待时间超过限制则记录警告。
         * 任何异常都会被捕获并记录,避免单个任务失败导致主循环退出。
         */
        @Override
        public void run() {
            long delta = getDelta();
            if (delta > JOB_WAITING_LIMIT) {
                Timber.w("%s has waited %d ms to get run since created!", toString(), delta);
            }
            started = true;
            try {
                runnable.run();
            } catch (Throwable t) {
                Timber.e(t, "WrappedRunnable %s threw an exception", toString());
            }
        }

        /**
         * 返回字符串表示。
         *
         * @return WrappedRunnable 的字符串表示。
         */
        @Override
        public String toString() {
            return "WrappedRunnable[" + (name != null ? name : String.valueOf(hashCode())) + "]";
        }

        /** 空任务常量 */
        public static final WrappedRunnable EMPTY = new WrappedRunnable(() -> {}, "Empty");

        /**
         * 获取底层 Runnable(用于 stop() 返回值)。
         *
         * @return 底层 Runnable。
         */
        public Runnable getUnderlyingRunnable() {
            return runnable;
        }
    }

    // --- RimeDispatcher 成员变量 ---

    /** Rime 控制器 */
    private final RimeController controller;
    /** 内部执行器 */
    private final ExecutorService internalExecutor;
    /** 任务队列 */
    private final LinkedBlockingQueue<WrappedRunnable> queue = new LinkedBlockingQueue<>();
    /** 是否正在运行(原子布尔值) */
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    /** 生命周期锁(替换 Kotlin 的 Mutex) */
    private final Object lifecycleLock = new Object(); // Replaces Kotlin's Mutex

    // --- 构造函数 ---

    /**
     * 构造函数。
     *
     * @param controller Rime 控制器。
     */
    public RimeDispatcher(RimeController controller) {
        this.controller = controller;
        // Simulates the Executors.newSingleThreadExecutor { Thread(it, "rime-main") } setup
        this.internalExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("rime-main");
            return t;
        });
    }

    // --- 公共方法 ---

    /**
     * 启动调度器。
     * 此函数立即在单线程上执行原生启动过程,并开始处理任务队列。
     */
    public void start() {
        Timber.d("RimeDispatcher start()");

        // 在单线程上启动主循环
        internalExecutor.execute(() -> {
            synchronized (lifecycleLock) {
                if (isRunning.compareAndSet(false, true)) {
                    try {
                        Timber.d("nativeStartup()");
                        controller.nativeStartup();

                        // 主消息循环:运行直到 'isRunning' 设置为 false
                        while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
                            // 阻塞直到有可用任务(类似于 Kotlin 的 queue.take())
                            WrappedRunnable block = queue.take();

                            // 'EMPTY' 哨兵用于在 stop() 时中断循环
                            if (block == WrappedRunnable.EMPTY) {
                                break;
                            }
                            block.run();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // 恢复中断状态
                    } finally {
                        Timber.i("nativeFinalize()");
                        controller.nativeFinalize();
                        // 执行器在此处不关闭,只是循环中断。
                    }
                }
            }
        });
    }

    /**
     * 优雅地停止调度器。
     * 此函数阻塞直到调度器的主循环和原生 finalize 完成。
     *
     * @return 未执行的底层 Runnable 列表(剩余任务)。
     */
    public List<Runnable> stop() {
        Timber.i("RimeDispatcher stop()");
        if (isRunning.compareAndSet(true, false)) {
            // 1. 提供哨兵以中断主循环中的阻塞 'queue.take()'
            queue.offer(WrappedRunnable.EMPTY);

            // 2. 阻塞直到主循环完成其同步部分(nativeFinalize 完成)
            //    我们提交一个阻塞任务并等待其完成。
            Future<List<Runnable>> future = internalExecutor.submit((Callable<List<Runnable>>) () -> {
                // 此代码在主循环完成其 lifecycleLock 部分后运行。
                synchronized (lifecycleLock) {
                    List<WrappedRunnable> rest = new ArrayList<>();
                    // 3. 排空队列中的所有剩余任务(包括哨兵,如果未被消费)
                    queue.drainTo(rest);

                    // 将 WrappedRunnable 列表转换为 Runnable 列表以返回
                    List<Runnable> result = new ArrayList<>(rest.size());
                    for (WrappedRunnable wrapped : rest) {
                        if (wrapped != WrappedRunnable.EMPTY) {
                            result.add(wrapped.getUnderlyingRunnable());
                        }
                    }
                    return result;
                }
            });

            // 阻塞并等待最终任务完成(即循环已停止且队列已排空)
            try {
                // 主循环完成工作后关闭执行器
                internalExecutor.shutdown();
                // 我们使用 future.get() 阻塞直到关闭清理任务完成。
                return future.get();
            } catch (InterruptedException | ExecutionException e) {
                Timber.e(e, "Error during RimeDispatcher stop()");
                // 如果等待失败,尝试强制关闭
                internalExecutor.shutdownNow();
                // 返回我们能得到的,由于异常可能为空列表
                return new ArrayList<>();
            }
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * 将 Runnable 任务分发到 Rime 处理队列。
     *
     * @param block 要执行的 Runnable 任务。
     */
    // 替换 CoroutineDispatcher 的 dispatch 方法
    public void dispatch(Runnable block) {
        if (!isRunning.get()) {
            throw new IllegalStateException("Dispatcher is not in running state!");
        }
        queue.offer(new WrappedRunnable(block));
    }
}
