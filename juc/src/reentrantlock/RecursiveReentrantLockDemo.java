package reentrantlock;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 可重入锁应用场景一：递归方法
 * <p>
 * 核心点：同一个线程在持有锁的情况下，可以再次获取同一把锁而不会被阻塞。
 * 如果使用 synchronized，同样支持可重入，但 ReentrantLock 能更显式地控制加锁/解锁，
 * 并且可以通过 getHoldCount() 观察锁的持有计数，便于理解可重入的本质。
 * <p>
 * 本 Demo 演示：阶乘计算中，递归调用反复获取同一把锁。
 * 若非可重入锁，线程在第 n 层递归再次 lock() 时将永远阻塞自己（死锁）。
 */
public class RecursiveReentrantLockDemo {

    public static void main(String[] args) {
        Factorial factorial = new Factorial();
        Thread t1 = new Thread(() -> {
            long result = factorial.compute(10);
            System.out.println(Thread.currentThread().getName() + " 计算 10! = " + result);
        }, "Thread-A");

        Thread t2 = new Thread(() -> {
            long result = factorial.compute(7);
            System.out.println(Thread.currentThread().getName() + " 计算 7! = " + result);
        }, "Thread-B");


        t1.start();
        t2.start();
    }

    static class Factorial {
        private final ReentrantLock lock = new ReentrantLock();
        public long compute(int n) {
            lock.lock();
            try {
                System.out.println(Thread.currentThread().getName()
                        + " 进入 compute(" + n + "), 当前锁持有计数: " + lock.getHoldCount());
                if (n <= 1) {
                    return 1;
                }
                return n * compute(n - 1);
            } finally {
                lock.unlock();
            }
        }
    }
}