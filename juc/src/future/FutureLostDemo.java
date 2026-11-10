package future;

import java.util.concurrent.*;

public class FutureLostDemo {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(2);

        // ========== 场景1：把 Future 丢掉 ==========
        System.out.println("=== 场景1：丢了 Future ===");
        pool.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Thread.sleep(1000);
                return "任务A的结果";
            }
        });
        // ⚠️ submit() 返回的 Future 没人接！直接丢掉了！

        Thread.sleep(1500);
        System.out.println("任务A早跑完了，但我没地方拿它的结果 😢");

        // ========== 场景2：攥着 Future ==========
        // pool.submit的结果必须用Future来获取结果，否则会丢失结果
        System.out.println("\n=== 场景2：攥着 Future ===");
        Future<String> future = pool.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Thread.sleep(1000);
                return "任务B的结果";
            }
        });

        Thread.sleep(1500);
        System.out.println("我有 Future，能拿到结果：" + future.get());

        pool.shutdown();
    }
}