package cf;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CompletableFutureDemo {


    public static void main(String[] args) {
//        demo1();
//        System.out.println(demo2());

//        demo3();
//        demo4();
//        demo5();
//        demo6();
//        System.out.println(demo7());

        System.out.println(demo8());
    }

    /**
     * 异步执行 runAsync方法
     * Void类型，表明执行结束后 “我不需要返回值”
     * runAsync方法如果没有给定线程池，默认会使用ForkJoinPool.commonPool()
     *
     */
    private static void demo1() throws ExecutionException, InterruptedException {

        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            System.out.println(Thread.currentThread().getName());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("任务结束");

        });
        //Void类型只能返回Null.
        System.out.println(future.get());

    }

    /**
     * 异步执行 supplyAsync方法
     * supplyAsync方法返回的是一个CompletableFuture类型，可以使用get()方法获取结果
     * 可以指定线程池，也可以不指定，默认会使用ForkJoinPool.commonPool()
     *
     */
    private static String demo2() throws ExecutionException, InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(4);

        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            System.out.println(Thread.currentThread().getName());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("任务结束");
            return "任务A的结果";
        }, pool);
        return future.get();
    }


    /**
     * 异步执行 thenApply方法：转换结果
     */
    private static void demo3() {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        CompletableFuture<Integer> cf =
                CompletableFuture.supplyAsync(() -> "123", pool)
                        // 转换为整数
                        .thenApply(Integer::parseInt)
                        // 加1
                        .thenApply(x -> x + 1);

        System.out.println(cf.join()); // 124
    }

    /**
     * 异步执行：thenAccept方法：处理结果
     */
    private static void demo4() {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        CompletableFuture<Void> future = CompletableFuture.supplyAsync(() -> "hello", pool)
                .thenAccept(s -> System.out.println(s + "world"));
    }

    ;

    /**
     * 异步执行：thenCompose方法：组合任务:
     * 串行依赖（A的结果给B）
     */
    private static void demo5() {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> "hello", pool)
                .thenCompose(s -> CompletableFuture.completedFuture(s + "world"));
        System.out.println(future.join()); // hello world

    }

    ;


    /**
     * 独立任务执行：`thenCombine` 是异步、非阻塞的，用来组合两个 future 的结果；
     * 当需要拿到最终结果时，一般用 `join()`或 `get()`来阻塞等待它完成。
     */
    private static void demo6() {
        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> 10);
        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> 20);
        CompletableFuture<Integer> future3 = future1.thenCombine(future2, (x, y) -> x + y);
        System.out.println(future3.join()); // 30
    }

    ;

    /**
     * allOf方法：等待多个任务完成
     * 当所有任务都完成时，才会返回结果
     * 注意，join这里不是为了阻塞等待，而是为了获取所有任务的结果。这里的join一定不会阻塞等待。
     * 因为：allOf(future1, future2) 返回的 CompletableFuture<Void> 只有等 future1 和 future2 都完成后才会完成。
     */
    private static Integer demo7() {
        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> 10);
        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> 20);
        CompletableFuture<Integer> future3 = CompletableFuture.allOf(future1, future2).thenApply(v -> future1.join() + future2.join());
        return future3.join(); // 30
    }

    ;


    /**
     * anyOf方法：等待任意一个任务完成
     * anyOf返回的CompletableFuture<Object>会携带第一个完成的任务的结果
     * 因为泛型无法统一（future1是Integer，future2也是Integer，但anyOf是通用方法），所以返回Object，需要强转
     */
    private static Integer demo8() {
        CompletableFuture<Integer> future2 = CompletableFuture.supplyAsync(() -> 20);

        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> 10);
        CompletableFuture<Integer> future3 = CompletableFuture.anyOf(future1, future2).thenApply(v -> (Integer) v);
        return future3.join();
    }

    ;

    /**
     * 异步执行：exceptionally方法：处理异常
     * 当任务抛出异常时，会调用exceptionally方法处理异常
     */
    private static void demo9() {
        CompletableFuture.supplyAsync(() -> {
            throw new RuntimeException("error");
        }).exceptionally(ex -> {
            System.out.println(ex.getMessage());
            return "default";
        });
    }

    /**
     * 异步执行：handle方法：处理异常
     * 当任务抛出异常时，会调用handle方法处理异常
     */
    private static void demo10() {
        CompletableFuture.supplyAsync(() -> 1 / 0)
                .handle((result, ex) -> {
                    if (ex != null) {
                        return 0;
                    }
                    return result;
                }).join();
    }
}