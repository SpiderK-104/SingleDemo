package lock;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 非公平锁演示：以多个线程抢票为例
 */
public class NonFairLock {


    static class Ticket {
        private int num = 50;
        ReentrantLock lock = new ReentrantLock(true);

        public void sale() {
            lock.lock();
            try {
                if (num > 0) {
                    num--;
                    /**
                     * Thread.currentThread() 是一个 public static 方法，它的作用是：
                     * 返回当前正在执行的线程对象.它不需要你传入任何参数，JVM 内部维护着一个"当前线程上下文"
                     * ——CPU 此刻正在跑哪个线程，它直接从这个上下文里取，就像你不用知道自己在哪个房间，也能抬头看到当前讲话的人是谁。
                     */
                    System.out.println(Thread.currentThread().getName() + "当前票数为：" + num );
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public static void main(String[] args) {
        Ticket ticket = new Ticket();
        new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                ticket.sale();
            }
        }, "线程A").start();

        new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                ticket.sale();
            }
        }, "线程B").start();

        new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                ticket.sale();
            }
        }, "线程C").start();
    }
}
