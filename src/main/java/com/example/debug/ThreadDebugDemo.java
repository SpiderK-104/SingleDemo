package main.java.com.example.debug;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多线程调试实操类
 * <p>
 * 多线程调试最大的坑：
 *   断点停了，但你看的是另一个线程的栈；Suspend 选 Thread 导致观察不到全局。
 * <p>
 * 快捷键：Alt+R → Get Thread Dump（或调试工具栏的地球图标）
 */
public class ThreadDebugDemo {

    // ============================================================
    //  场景 1 —— 线程名：断点停下后你必须知道"我是谁"
    // ============================================================
    public static void scenario1ThreadIdentity() {
        System.out.println("主线程名: " + Thread.currentThread().getName());

        Thread t = new Thread(() -> {
            workerBody("custom-worker");
        }, "my-worker-1");

        t.start();
        try {
            t.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static void workerBody(String arg) {
        System.out.println("  工作线程名: " + Thread.currentThread().getName() + ", arg=" + arg);
    }

    /*
     * 【练习 1.1】在线程体第一行打断点，F9 启动
     *   Debug 工具栏会有一个线程选择下拉框（显示 main / my-worker-1）。
     *   停住后 Alt+F8 输入 `Thread.currentThread().getName()` 确认身份。
     *
     * 【练习 1.2】看不出我在哪个线程？两个办法
     *   1) Debug 窗口的 Threads 面板，被挂起的线程高亮显示
     *   2) Alt+F8: Thread.currentThread().getName()
     *   3) Alt+F8: Thread.currentThread().getStackTrace()
     *      → 直接拿到当前线程的完整栈，比切 Frames 面板快
     *
     * ★ 实战：把断点加到 workerBody 第一行，如果断点"时有时无"，
     *   先确认 Suspend 策略和线程名，再怀疑代码。
     */


    // ============================================================
    //  场景 2 —— Suspend: Thread vs All 的关键差别
    // ============================================================
    public static void scenario2SuspendPolicy() {
        AtomicInteger counter = new AtomicInteger();

        Thread worker = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                counter.incrementAndGet();
                sleepQuietly(50);
            }
        }, "counter-worker");
        worker.start();

        for (int i = 0; i < 5; i++) {
            counter.incrementAndGet();
            sleepQuietly(50);
        }

        try {
            worker.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("counter = " + counter.get() + " (期望 10)");
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /*
     * 【练习 2.1】在 counter.incrementAndGet(); 下断点（两处都下）
     *   第一次做法（Suspend: All，IDEA 默认）：
     *     停住 → 看 Threads 面板，main 和 counter-worker 都 suspended，
     *     counter 值固定不变，可以从容观察。这就是为什么多线程调试强烈建议 All。
     *
     *   第二次做法（Suspend: Thread）：
     *     停住 → 只有当前线程挂起，另一个线程照跑，
     *     counter 每按一次 F9 都可能跳变。
     *
     * ★ 结论：Suspend: Thread 是"不断言其他线程状态"的表现，
     *   调试共享状态时必须切回 All。
     *
     * 【练习 2.2】在 All 模式下看竞态
     *   停住时 Alt+F8 输入 `counter.get()`，按 F9 几次，
     *   会发现每次停住的值不同 —— 因为另一个线程已经被挂起了，
     *   所以这个值是"某个瞬间的快照"，不是最终值。
     */


    // ============================================================
    //  场景 3 —— 死锁：Get Thread Dump
    // ============================================================
    private static final Object LOCK_A = new Object();
    private static final Object LOCK_B = new Object();

    public static void scenario3Deadlock() {
        CountDownLatch ready = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            ready.countDown();
            awaitQuietly(ready);
            synchronized (LOCK_A) {
                System.out.println("t1 拿到了 LOCK_A");
                sleepQuietly(100);
                synchronized (LOCK_B) {          // t1 等 B
                    System.out.println("t1 拿到了 LOCK_B");
                }
            }
        }, "deadlock-t1");

        Thread t2 = new Thread(() -> {
            ready.countDown();
            awaitQuietly(ready);
            synchronized (LOCK_B) {
                System.out.println("t2 拿到了 LOCK_B");
                sleepQuietly(100);
                synchronized (LOCK_A) {          // t2 等 A
                    System.out.println("t2 拿到了 LOCK_A");
                }
            }
        }, "deadlock-t2");

        // ★ 必须设成守护线程：它们会死锁卡住，不设成 daemon 的话 main 结束后 JVM 不会退出
        t1.setDaemon(true);
        t2.setDaemon(true);
        t1.start();
        t2.start();

        // 主线程等着不死，好让 t1/t2 卡在死锁里
        sleepQuietly(1500);

        // ★ 此时按 Alt+R → Get Thread Dump（或调试工具栏地球图标）
        //   IDEA 会把死锁线程的持锁/等锁信息标红，直接告诉你"t1 等 B、t2 等 A"。
        //
        // ★ 也可以不用 Thread Dump：在 t1 等 B 的那一行和 t2 等 A 的那一行
        //   各下断点，Suspend: All，两个断点都会命中，
        //   然后在 Threads 面板分别看两个线程的栈 —— 栈底都是 synchronized 阻塞。
        System.out.println("（若两个线程都没走到最后一行，说明死锁了）");

        // 不 join 死锁的线程，让程序正常退出
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /*
     * 【练习 3.1】正确姿势
     *   1) Debug 启动（Shift+F9），不要用 Run
     *   2) 断点不用设，直接等线程卡住
     *   3) Alt+R → Get Thread Dump → IDEA 自动检测死锁并高亮
     *
     * 【练习 3.2】为什么 Run 就看不出来
     *   普通 Run 没有 thread dump 视图，程序就是"卡住不动"。
     *   Thread Dump 是纯读操作，不影响程序运行。
     *
     * 【练习 3.3】jstack 命令行等价物
     *   jps -l  找到 pid，再 jstack <pid>，输出文本里
     *   "Found one Java-level deadlock" 就是死锁报告。
     */


    // ============================================================
    //  场景 4 —— 线程池 + 并发流：断点会被多个线程命中
    // ============================================================
    public static void scenario4ThreadPool() {
        java.util.concurrent.ExecutorService pool =
                java.util.concurrent.Executors.newFixedThreadPool(2);

        for (int i = 0; i < 4; i++) {
            int taskId = i;
            pool.execute(() -> {
                processTask(taskId);
            });
        }

        pool.shutdown();
    }

    static void processTask(int taskId) {
        String result = transform(taskId);   // <-- 断点放这里
        System.out.println("task " + taskId + " → " + result);
    }

    static String transform(int x) {
        sleepQuietly(100);
        return "#" + (x * 10);
    }

    /*
     * 【练习 4.1】在 transform 返回那行断点，Suspend: All
     *   F9 几次，会看到每次停在不同的线程（pool-1-thread-1 / pool-1-thread-2）。
     *   Alt+F8 输入 `Thread.currentThread().getName()` 确认。
     *   ★ 这就是线程池场景下"同一个断点时有时无"的真相 ——
     *     不是没执行，是停在了另一个线程上。
     *
     * 【练习 4.2】并行 stream 同理
     *   把 processTask 换成 list.parallelStream().forEach(this::processTask)，
     *   断点会被 ForkJoinPool 的多个 worker 命中，命中顺序不保证。
     *
     * 【练习 4.3】Alt+F8 求值线程池状态
     *   在 processTask 内 Alt+F8：
     *     Thread.currentThread().getName()
     *     Thread.currentThread().isAlive()
     *     Thread.currentThread().getPriority()
     *     Thread.currentThread().getThreadGroup().getName()
     */


    // ============================================================
    //  场景 5 —— 中断与中断标志（InterruptedException 调试）
    // ============================================================
    public static void scenario5Interrupt() {
        Thread worker = new Thread(() -> {
            try {
                System.out.println("[worker] 准备睡眠");
                Thread.sleep(10_000);          // <-- 在这行断点
                System.out.println("[worker] 睡眠正常结束");
            } catch (InterruptedException e) {
                // ★ 陷阱：catch 到之后中断标志已被清零，必须手动恢复
                System.out.println("[worker] 被中断: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }, "interruptible-worker");

        worker.start();
        sleepQuietly(200);
        worker.interrupt();

        try {
            worker.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /*
     * 【练习 5.1】在 sleep 那行断点，然后在 Threads 面板右键 worker 线程
     *   → 选 "Interrupt"（或用 Alt+R 菜单里对应项）。
     *   程序会直接跳进 catch 块。
     *
     * 【练习 5.2】中断标志是状态，不是异常
     *   在 catch 行 Alt+F8：
     *     Thread.interrupted()       ← 静态，查询并清零
     *     Thread.currentThread().isInterrupted()  ← 实例，只查询
     *     e.getMessage()
     *   ★ 中断后若不调用 Thread.currentThread().interrupt() 恢复标志，
     *     传给下游的线程池任务会以为没人中断过，导致 shutdownNow 失效。
     *
     * 【练习 5.3】catch 里不恢复标志的经典 bug
     *   删掉 catch 里的 interrupt() 再跑一次，对比行为差异。
     */


    // ============================================================
    //  场景 6 —— 非 volatile 变量的可见性问题
    // ============================================================
    static boolean stopRequested = false;    // 故意不加 volatile

    public static void scenario6Visibility() {
        Thread worker = new Thread(() -> {
            while (!stopRequested) {          // <-- 断点放这里
                Thread.onSpinWait();
            }
            System.out.println("[worker] 看到停止信号，退出循环");
        }, "spin-worker");
        worker.setDaemon(true);      // 防止空转导致 JVM 不退出

        worker.start();
        sleepQuietly(300);
        stopRequested = true;
        System.out.println("[main] 已设置 stopRequested = true");

        try {
            worker.join(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("[main] worker 是否已结束: " + !worker.isAlive()
                + "（若为 false 说明 worker 还在空转 —— 这就是可见性问题）");

        // 收尾，避免线程泄漏影响后续场景
        if (worker.isAlive()) {
            System.out.println("[main] 强制中断 worker");
            worker.interrupt();
        }
    }

    /*
     * 【练习 6.1】在 while 那行断点，注意观察什么
     *   Alt+F8: stopRequested            主线程改了值，但 worker 读到的还是 false
     *   Alt+F8: Thread.currentThread().getName()
     *   ★ JIT 可能把 stopRequested 提升到寄存器（loop invariant），
     *     即使加了 volatile 在高优化下也可能出现异常。
     *     排查这类问题的正确方式是 Thread Dump + 看字段值，
     *     而不是加断点反复试。
     *
     * 【练习 6.2】验证修复
     *   把 stopRequested 改成 `static volatile boolean`，
     *   再跑一次，观察 worker 是否立刻退出。
     */


    public static void main(String[] args) {
        System.out.println("===== 场景1: 线程身份 =====");
        scenario1ThreadIdentity();

        System.out.println("\n===== 场景2: Suspend 策略 =====");
        scenario2SuspendPolicy();

        System.out.println("\n===== 场景3: 死锁 =====");
        scenario3Deadlock();

        System.out.println("\n===== 场景4: 线程池 =====");
        scenario4ThreadPool();

        System.out.println("\n===== 场景5: 中断 =====");
        scenario5Interrupt();

        System.out.println("\n===== 场景6: 可见性 =====");
        scenario6Visibility();

        System.out.println("\n全部执行完毕。");
    }
}