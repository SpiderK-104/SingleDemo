package future;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class FirstFuture {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // FutureTask 包装 Callable，同时实现了 Runnable 和 Future 接口
        FutureTask<String> futureTask = new FutureTask<>(new MyThread());

        /**
         * 为什么这里还要用一个Thread包住呢？
         * FutureTask其实是可以直接通过futureTask.run直接运行，但是它在main方法
         * 上运行，和普通方法调用就没有区别了。
         * Thread才是真正的线程，它负责向OS申请一个新的执行单元。
         * Thread 构造函数里传了一个 Runnable（FutureTask 实现了 Runnable）。
         * 此时也什么都没执行，只是创建了一个 Thread 对象，还没向操作系统申请资源。
         */
        Thread t1 = new Thread(futureTask, "t1");
        //开始执行线程
        t1.start();
        //获取线程结果：FutureTask 特性
        System.out.println("futureTask执行结果：" + futureTask.get());
    }

    /**
     * 实现Callable:定义了一个有返回值的任务
     */
    static class MyThread implements Callable<String> {
        @Override
        public String call() throws Exception {
            System.out.println("开始执行线程调用..");
            return "hello~";
        }
    }
}
