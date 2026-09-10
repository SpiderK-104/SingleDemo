package main.java.com.example.thread;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * 基于 CountDownLatch 的通用多线程批处理工具类
 */
public class BatchTaskUtils {

    /**
     * 使用线程池并行处理一批任务，主线程等待所有任务完成后返回。
     *
     * @param taskList 任务列表，不能为 null 或空
     * @param consumer 每个任务的处理逻辑
     * @param executor 线程池
     * @param <T>      任务元素的类型
     * @throws InterruptedException 如果等待过程中线程被中断
     */
    public static <T> void disposeWithLatch(List<T> taskList,
                                            Consumer<? super T> consumer,
                                            Executor executor) throws InterruptedException {
        Objects.requireNonNull(taskList, "taskList must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");
        Objects.requireNonNull(executor, "executor must not be null");
        if (taskList.isEmpty()) {
            return;
        }
        CountDownLatch latch = new CountDownLatch(taskList.size());
        for (T item : taskList) {
            T task = item; // 显式局部变量，避免 lambda 捕获循环变量的隐患
            executor.execute(() -> {
                try {
                    consumer.accept(task);
                } finally {
                    latch.countDown(); // 无论成功还是异常，都确保计数器减一
                }
            });
        }
        latch.await(); // 主线程阻塞，直到所有任务执行完毕
    }

    // ==================== 可执行的完整 Demo ====================

    public static void main(String[] args) throws InterruptedException {
        // 构造测试任务列表
        List<String> tasks = List.of("task-1", "task-2", "task-3", "task-4", "task-5");

        // 创建线程池（JDK 21 推荐用 ThreadPoolExecutor 显式配置）
        ExecutorService executor = new ThreadPoolExecutor(
                4, 4,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(100),
                Thread.ofVirtual().factory() // JDK 21 虚拟线程工厂，也可换成平台线程
        );
        long start = System.currentTimeMillis();
        // 调用通用批处理工具
        disposeWithLatch(tasks, BatchTaskUtils::processTask, executor);
        long end = System.currentTimeMillis();
        System.out.println("全部任务处理完成，耗时：" + (end - start) + " ms");
        executor.shutdown();
    }

    /**
     * 模拟单个任务的处理逻辑
     */
    private static void processTask(String task) {
        System.out.printf("[%s] 开始处理任务: %s%n",
                Thread.currentThread().getName(), task);
        try {
            Thread.sleep(1000); // 模拟耗时操作
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.printf("[%s] 完成任务: %s%n",
                Thread.currentThread().getName(), task);
    }
}