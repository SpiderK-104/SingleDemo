package main.java.com.example.thread;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 基于 CompletableFuture 的通用多线程批处理工具类
 */
public class BatchTaskFutureUtils {

    /**
     * 使用 CompletableFuture 并行处理一批任务，等待所有任务完成后返回。
     *
     * @param taskList 任务列表，不能为 null 或空
     * @param consumer 每个任务的处理逻辑
     * @param executor 线程池
     * @param <T>      任务元素的类型
     */
    public static <T> void disposeWithFuture(List<T> taskList,
                                             Consumer<? super T> consumer,
                                             Executor executor) {

        Objects.requireNonNull(taskList, "taskList must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");
        Objects.requireNonNull(executor, "executor must not be null");

        if (taskList.isEmpty()) {
            return;
        }

        // ---- 关键代码开始 ----
        // 为每个任务创建一个 CompletableFuture
        List<CompletableFuture<Void>> futures = taskList.stream()
                .map(task -> CompletableFuture.runAsync(() -> consumer.accept(task), executor))
                .collect(Collectors.toList());

        // 等待所有 Future 完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .join();
        // ---- 关键代码结束 ----
    }

    /**
     * 带超时和异常收集的增强版
     *
     * @param taskList  任务列表
     * @param consumer  处理逻辑
     * @param executor  线程池
     * @param timeoutMs 超时时间（毫秒），null 表示不超时
     * @param <T>       任务类型
     * @return 失败的任务列表，每个元素包含原始任务和异常原因
     */
    public static <T> List<FailedTask<T>> disposeWithFutureAdvanced(
            List<T> taskList,
            Consumer<? super T> consumer,
            Executor executor,
            Long timeoutMs) {

        Objects.requireNonNull(taskList, "taskList must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");
        Objects.requireNonNull(executor, "executor must not be null");

        if (taskList.isEmpty()) {
            return Collections.emptyList();
        }

        // 线程安全的结果收集
        List<FailedTask<T>> failures = Collections.synchronizedList(new ArrayList<>());

        List<CompletableFuture<Void>> futures = taskList.stream()
                .map(task -> CompletableFuture.runAsync(() -> {
                    try {
                        consumer.accept(task);
                    } catch (Throwable t) {
                        failures.add(new FailedTask<>(task, t));
                    }
                }, executor))
                .toList();

        CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        if (timeoutMs != null) {
            try {
                all.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                // 超时后记录尚未完成的任务
                for (int i = 0; i < taskList.size(); i++) {
                    if (!futures.get(i).isDone()) {
                        failures.add(new FailedTask<>(taskList.get(i),
                                new TimeoutException("任务未在 " + timeoutMs + " ms 内完成")));
                    }
                }
            } catch (Exception e) {
                // get 可能抛 ExecutionException / InterruptedException，这里忽略或记录
            }
        } else {
            all.join();
        }

        return failures;
    }


    public static void main(String[] args) {
        List<String> tasks = List.of("task-A", "task-B", "task-C", "task-D");

        ExecutorService executor = new ThreadPoolExecutor(
                4, 4,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(100),
                Thread.ofVirtual().factory()
        );

        long start = System.currentTimeMillis();

        // 基础版
        disposeWithFuture(tasks, BatchTaskFutureUtils::processTask, executor);

        long end = System.currentTimeMillis();
        System.out.println("Future 版全部完成，耗时：" + (end - start) + " ms");

        // 增强版（带超时和失败收集）
        List<FailedTask<String>> failed = disposeWithFutureAdvanced(
                tasks, BatchTaskFutureUtils::processTask, executor, 3000L);

        if (failed.isEmpty()) {
            System.out.println("增强版：所有任务成功");
        } else {
            System.out.println("增强版：失败任务数 = " + failed.size());
            failed.forEach(ft -> System.out.println("  失败: " + ft.getTask() + ", 原因: " + ft.getCause()));
        }

        executor.shutdown();
    }

    private static void processTask(String task) {
        System.out.printf("[%s] Future 处理: %s%n",
                Thread.currentThread().getName(), task);
        try {
            Thread.sleep(800);
            // 模拟偶发异常
            if ("task-C".equals(task)) {
                throw new RuntimeException("模拟 task-C 处理失败");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================== 辅助类 ====================

    public static class FailedTask<T> {
        private final T task;
        private final Throwable cause;

        public FailedTask(T task, Throwable cause) {
            this.task = task;
            this.cause = cause;
        }

        public T getTask() {
            return task;
        }

        public Throwable getCause() {
            return cause;
        }

        @Override
        public String toString() {
            return "FailedTask{task=" + task + ", cause=" + cause + '}';
        }
    }
}
