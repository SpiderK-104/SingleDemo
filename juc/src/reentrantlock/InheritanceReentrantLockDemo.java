package reentrantlock;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 可重入锁应用场景三：继承 + 子类重写父类同步方法
 *
 * 核心点：子类在重写父类加锁方法时，可以先获取锁做自己的逻辑，
 * 再调用 super.父类方法 复用父类已有的加锁逻辑——同一线程再次获取同一把锁不会阻塞。
 *
 * 本 Demo 演示：
 *   父类 AbstractCounter 定义 increment() 模板方法（获取锁后调用 doIncrement）
 *   子类 LoggingCounter 重写 doIncrement，在父类逻辑之外增加日志记录
 *   子类 SafeCounter 重写 increment() 自身，先做范围校验再调用 super.increment()
 *
 * 两种重写方式都依赖可重入性：子类持有锁的前提下调用 super.method() 能顺利通过。
 */
public class InheritanceReentrantLockDemo {

    public static void main(String[] args) {
        System.out.println("=== 场景1：重写钩子方法 doIncrement ===");
        LoggingCounter loggingCounter = new LoggingCounter();
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 3; i++) loggingCounter.increment();
        }, "T1");
        t1.start();
        try { t1.join(); } catch (InterruptedException ignored) {}

        System.out.println("\n=== 场景2：重写模板方法 increment 并调用 super ===");
        SafeCounter safeCounter = new SafeCounter(5);
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 8; i++) safeCounter.increment();
        }, "T2");
        t2.start();
        try { t2.join(); } catch (InterruptedException ignored) {}

        System.out.println("\n最终 safeCounter 计数: " + safeCounter.getCount());
    }

    /**
     * 父类：定义加锁模板 + 钩子方法
     */
    static abstract class AbstractCounter {
        protected final ReentrantLock lock = new ReentrantLock();
        protected int count = 0;

        public void increment() {
            lock.lock();
            try {
                count++;
                doIncrement();
            } finally {
                lock.unlock();
            }
        }

        /**
         * 钩子方法：子类可以重写扩展行为（此时已持有锁，再次获取同一锁安全）
         */
        protected void doIncrement() {
        }

        public int getCount() {
            lock.lock();
            try {
                return count;
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 场景1：重写钩子方法 doIncrement —— 依赖可重入性再次获取锁
     */
    static class LoggingCounter extends AbstractCounter {
        @Override
        protected void doIncrement() {
            lock.lock();
            try {
                System.out.println(Thread.currentThread().getName()
                        + " [LoggingCounter] 当前 count=" + count
                        + ", 锁持有计数=" + lock.getHoldCount());
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * 场景2：重写模板方法 increment —— 先获取锁做校验，再调用 super.increment()
     */
    static class SafeCounter extends AbstractCounter {
        private final int max;

        SafeCounter(int max) {
            this.max = max;
        }

        @Override
        public void increment() {
            lock.lock();
            try {
                System.out.println(Thread.currentThread().getName()
                        + " [SafeCounter] 进入 increment, 锁持有计数=" + lock.getHoldCount());
                if (count >= max) {
                    System.out.println(Thread.currentThread().getName() + " 达到上限 " + max + ", 不再增加");
                    return;
                }
                super.increment();
            } finally {
                lock.unlock();
            }
        }
    }
}