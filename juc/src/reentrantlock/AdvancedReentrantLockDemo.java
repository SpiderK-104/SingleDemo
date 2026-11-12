package reentrantlock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 可重入锁应用场景四：tryLock / 超时 / 公平锁 的高级并发控制
 *
 * 核心点：相比 synchronized 的"盲目等待直到拿到锁或被中断"，ReentrantLock 提供三种增强能力：
 *   1. tryLock()          —— 非阻塞式尝试，立即返回 true/false
 *   2. tryLock(time, unit) —— 带超时的尝试，超时后放弃，避免永久等待
 *   3. ReentrantLock(true) —— 公平锁，按请求顺序分配，避免线程饥饿
 *
 * 本 Demo 包含三个独立子场景：
 *   Scenario 1 (tryLock):      多个线程竞争锁，获取失败的线程做降级处理
 *   Scenario 2 (tryLock 超时): 模拟数据库连接池等待，超时后抛出业务异常
 *   Scenario 3 (公平锁):       对比公平锁与非公平锁的线程唤醒顺序
 */
public class AdvancedReentrantLockDemo {

    public static void main(String[] args) throws InterruptedException {
        scenarioTryLock();
        System.out.println("\n============================================================\n");
        scenarioTryLockTimeout();
        System.out.println("\n============================================================\n");
        scenarioFairLock();
    }

    /**
     * Scenario 1: tryLock() 非阻塞尝试
     * 适用于：不想因为等锁而卡住流程，拿不到锁就走降级逻辑
     */
    static void scenarioTryLock() {
        System.out.println("【Scenario 1】tryLock() 非阻塞尝试");
        ReentrantLock lock = new ReentrantLock();

        Thread worker1 = new Thread(() -> {
            lock.lock();
            try {
                System.out.println(Thread.currentThread().getName() + " 持有锁，模拟耗时操作...");
                sleep(3000);
            } finally {
                lock.unlock();
            }
        }, "Worker-1");

        Thread worker2 = new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + " 尝试获取锁...");
            if (lock.tryLock()) {
                try {
                    System.out.println(Thread.currentThread().getName() + " 拿到锁，执行任务");
                } finally {
                    lock.unlock();
                }
            } else {
                System.out.println(Thread.currentThread().getName() + " 拿不到锁，走降级逻辑：返回缓存数据");
            }
        }, "Worker-2");

        worker1.start();
        sleep(500);
        worker2.start();

        try {
            worker1.join();
            worker2.join();
        } catch (InterruptedException ignored) {}
    }

    /**
     * Scenario 2: tryLock(time, unit) 带超时
     * 适用于：比如数据库连接池——等一会儿还拿不到连接就放弃，别让请求一直挂着
     */
    static void scenarioTryLockTimeout() {
        System.out.println("【Scenario 2】tryLock(超时) 带超时尝试");
        ReentrantLock connectionLock = new ReentrantLock();

        Thread dbHolder = new Thread(() -> {
            connectionLock.lock();
            try {
                System.out.println(Thread.currentThread().getName() + " 占用数据库连接，执行慢查询 5 秒...");
                sleep(5000);
            } finally {
                connectionLock.unlock();
            }
        }, "DB-Holder");

        Thread requester = new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + " 请求数据库连接，最多等 2 秒...");
            try {
                boolean acquired = connectionLock.tryLock(2, TimeUnit.SECONDS);
                if (acquired) {
                    try {
                        System.out.println(Thread.currentThread().getName() + " 拿到连接，执行查询");
                    } finally {
                        connectionLock.unlock();
                    }
                } else {
                    System.out.println(Thread.currentThread().getName() + " 等了 2 秒还拿不到，抛出业务异常：服务暂不可用");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Requester");

        dbHolder.start();
        sleep(500);
        requester.start();

        try {
            dbHolder.join();
            requester.join();
        } catch (InterruptedException ignored) {}
    }

    /**
     * Scenario 3: 公平锁 vs 非公平锁
     * 非公平锁：允许插队，吞吐更高但可能线程饥饿
     * 公平锁：严格按先来后到，保证每个线程都能拿到
     */
    static void scenarioFairLock() {
        System.out.println("【Scenario 3】公平锁 vs 非公平锁");

        System.out.println("\n--- 非公平锁 (默认) ---");
        runFairnessTest(false);

        System.out.println("\n--- 公平锁 ---");
        runFairnessTest(true);
    }

    static void runFairnessTest(boolean fair) {
        ReentrantLock lock = new ReentrantLock(fair);
        int threadCount = 5;
        long[] acquireOrder = new long[threadCount];

        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            threads[i] = new Thread(() -> {
                lock.lock();
                try {
                    System.out.println("  线程-" + idx + " 获得锁");
                    acquireOrder[idx] = System.nanoTime();
                    sleep(200);
                } finally {
                    lock.unlock();
                }
            }, "Thread-" + idx);
        }

        for (Thread t : threads) {
            t.start();
            sleep(100);
        }
        for (Thread t : threads) {
            try { t.join(); } catch (InterruptedException ignored) {}
        }

        System.out.println("  唤醒顺序(时间从小到大):");
        Integer[] order = new Integer[threadCount];
        for (int i = 0; i < threadCount; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> Long.compare(acquireOrder[a], acquireOrder[b]));
        System.out.println("  -> " + java.util.Arrays.toString(order));
        if (fair) {
            System.out.println("  公平锁: " + (isSequential(order) ? "符合 FIFO 顺序 ✓" : "有插队发生"));
        }
    }

    static boolean isSequential(Integer[] order) {
        for (int i = 0; i < order.length; i++) {
            if (order[i] != i) return false;
        }
        return true;
    }

    static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}