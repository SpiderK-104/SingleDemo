package interrupt;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AtomicBoolean 软中断 Demo（极简版）
 *
 * 和 volatile 版唯一区别：AtomicBoolean 底层 = volatile + CAS，
 * 可以用 compareAndSet 做"一次性中断"（防止多线程重复触发）。
 */
public class AtomicSoftInterruptDemo {

    // AtomicBoolean 底层 value 本身就是 volatile 修饰的，同样保证可见性
    private static final AtomicBoolean stop = new AtomicBoolean(false);

    public static void main(String[] args) throws Exception {
        Thread t = new Thread(() -> {
            int count = 0;
            while (!stop.get()) {
                count++;
                System.out.println("[工作线程] 运行中 # " + count);
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
            System.out.println("[工作线程] 收到软中断，安全退出");
        }, "工作线程");

        t.start();
        Thread.sleep(3000);

        // 和 volatile 版的本质区别：compareAndSet 保证"只触发一次"
        boolean firstTime = stop.compareAndSet(false, true);
        System.out.println("\n[主线程] stop = true → 发出软中断, 首次触发=" + firstTime + "\n");

        // 模拟另一个线程也想发中断，会被 CAS 拦住
        new Thread(() -> {
            boolean first = stop.compareAndSet(false, true);
            System.out.println("[其他线程] 尝试再次中断, 首次触发=" + first + " (应为 false)");
        }).start();
        t.join();
        System.out.println("[主线程] 程序结束");
    }
}