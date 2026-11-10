package future;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 与之前的FutureTask+Thread 相比，FutureWithExecutable 使用了ExecutorService 来管理线程池，更方便地处理多个任务。
 * <p>
 * 之前是一个任务=一个Thread对象=一个操作系统线程：任务1->线程1->操作系统线程1; 每个任务独占一个系统线程
 * 现在是线程池复用有限的线程来跑无限的任务，5个任务让3个线程轮流干活。线程不够就排队，够用就复用
 * 不理解Executor.submit方法，参考{@link FutureLostDemo}
 */
public class FutureWithExecutor {
    public static void main(String[] args) throws ExecutionException, InterruptedException {

        // 创建一个固定线程池
        ExecutorService pool = Executors.newFixedThreadPool(3);
        List<Future<String>> futures = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            // 这跟之前手动包 FutureTask 不一样了！线程池帮你做了包装，submit() 直接返回一个 Future，你只管拿着就行。
            // Future 本质上就是一张提货单——线程池不会帮你存 Future！它只是执行任务，结果存在 Future 对象里，线程池提交的任务，必须用 Future 来取结果。
            // submit() 返回的 Future 是一次性的！你提交完任务，手里必须攥着这张"提货单"，丢了就没法取结果了。
            Future<String> future = pool.submit(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    Thread.sleep(1000);
                    return "任务-" + taskId + " 执行完毕，线程: " + Thread.currentThread().getName();
                }
            });
            // 把 Future 放到列表里，等等再取结果
            futures.add(future);
            System.out.println("提交任务-" + taskId);
        }

        System.out.println("所有任务已提交，主线程可以继续做其他事...");
        // 这是5个任务已经在并发跑了！
        for (int i = 0; i < futures.size(); i++) {
            Future<String> future = futures.get(i);
            //统一获取结果
            System.out.println("获取任务-" + (i + 1) + "结果: " + future.get());
        }
        pool.shutdown();
    }
}