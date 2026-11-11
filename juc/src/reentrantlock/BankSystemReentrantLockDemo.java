package reentrantlock;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 可重入锁应用场景二：一个类中多个 public 方法互相调用（银行系统）
 * 核心点：在同一个类中，方法 A 获取锁后调用方法 B，B 再次获取同一把锁不会阻塞。
 * 这在"门面方法 → 校验方法 → 扣款方法"这种调用链中非常常见。
 * 如果锁不可重入，外层方法拿到锁后调用内层方法会被自己卡死。
 * <p>
 * 本 Demo 模拟银行账户操作：
 * transfer() 转账门面方法 ——> validate() 校验方法 ——> debit() / credit() 扣款/入账方法
 * 有两个账户 A 和 B，各 10000 元。
 * 开两个线程，每个线程都从 A 转 10 次、每次 100 元给 B。
 * 两个线程同时跑，一共转 2000 元。
 * 最后期望：A = 8000，B = 12000，A+B 还是 20000（钱不会凭空多或少的）。
 */
public class BankSystemReentrantLockDemo {

    // 程序入口：main 方法，JVM 从这里开始执行
    public static void main(String[] args) {

        // 创建一个账户 A，名字叫 "A"，初始余额 10000
        Account accountA = new Account("A", 10000);
        // 创建一个账户 B，名字叫 "B"，初始余额 10000
        Account accountB = new Account("B", 10000);

        // 定义一个任务（Runnable）：循环 10 次，每次从 A 转 100 到 B
        Runnable task = () -> {
            // 循环 10 次
            for (int i = 0; i < 10; i++) {
                // 调用 A 的转账方法，把 100 元转给 B
                accountA.transfer(accountB, 100);
            }
        };

        // 创建线程 t1，名字叫 "转账线程-1"，执行上面的任务
        Thread t1 = new Thread(task, "转账线程-1");
        // 创建线程 t2，名字叫 "转账线程-2"，执行同一个任务
        Thread t2 = new Thread(task, "转账线程-2");

        // 启动线程 t1
        t1.start();
        // 启动线程 t2
        t2.start();

        /**
         * 补充1：为什么 join 要包在 try-catch 里？
         * join() 声明了 throws InterruptedException。因为主线程在"等待"期间是可以被其他线程 interrupt() 打断的，
         * 一旦被打断就抛出这个异常，所以 Java 强制你处理它（这里是简单 printStackTrace）。
         *
         * 补充2：为什么这里要join？
         * 因为join() 是为了让 main 线程等两个转账线程都跑完，再去读取并打印余额；
         * 否则主线程会在转账还没结束时就去读余额，打印出来的是中间状态，结果不可信。
         */
        try {
            // 主线程(main)等待 t1 执行结束，也即main 卡在这里，直到 t1 跑完
            t1.join();
            // 主线程(main)等待 t2 执行结束，也即main 卡在这里，直到 t2 跑完
            t2.join();
        } catch (InterruptedException e) {
            // 如果等待过程中被中断，打印异常堆栈
            e.printStackTrace();
        }

        // 打印 A 的最终余额（getBalance 内部也加了锁，保证读到的是最新值）
        System.out.println("最终 A 余额: " + accountA.getBalance());
        // 打印 B 的最终余额
        System.out.println("最终 B 余额: " + accountB.getBalance());
        // 打印 A+B 的总和，正常情况下应该还是 20000（钱只是转移，没有凭空消失或产生）
        System.out.println("A + B = " + (accountA.getBalance() + accountB.getBalance()) + " (应为 20000)");
    }

    // 静态内部类 Account：表示一个银行账户
    static class Account {
        // 每个账户各自持有一把可重入锁（ReentrantLock）
        // 注意：这是实例(非static)字段，所以 A 有一把锁，B 有另一把锁
        private final ReentrantLock lock = new ReentrantLock();
        // 账户名字（A 或 B），final 表示创建后不能改
        private final String name;
        // 账户余额，会被多个线程修改，所以需要加锁保护
        private int balance;

        // 构造方法：创建账户时传入名字和初始余额
        Account(String name, int balance) {
            // 把传入的名字赋给成员变量
            this.name = name;
            // 把传入的余额赋给成员变量
            this.balance = balance;
        }

        /**
         * 门面方法：对外暴露的转账接口，内部依次调用 validate → debit → credit
         */
        public void transfer(Account target, int amount) {
            // 先获取"当前账户"（this）的锁；如果别的线程正持有这把锁，就在这里等待
            lock.lock();
            // try-finally 保证无论中间发生什么，最后一定会解锁
            try {
                // 打印日志：哪个线程开始转账、从谁转给谁、转多少
                System.out.println(Thread.currentThread().getName() + " 开始转账 " + name + " -> " + target.name + ", amount=" + amount);
                // 调用 validate 校验：金额是否合法、余额是否足够
                // 注意：validate 内部会再次 lock.lock()，但因为锁可重入，所以不会卡死
                if (!validate(amount)) {
                    // 如果校验失败（余额不足），打印提示
                    System.out.println("校验失败: " + name + " 余额不足");
                    // 直接返回，finally 里会解锁
                    return;
                }
                // 校验通过，从当前账户扣钱
                // debit 内部也会再次 lock.lock()，同样靠可重入性不会卡死
                debit(amount);
                // 给目标账户加钱；这里加的是 target 自己的锁，不是当前账户的锁
                target.credit(amount);
                // 打印转账完成日志，显示当前账户剩余余额
                System.out.println(Thread.currentThread().getName() + " 转账完成, " + name + " 余额=" + balance);
            } finally {
                // 无论成功、失败还是异常，都释放当前账户的锁
                lock.unlock();
            }
        }

        /**
         * 校验方法：检查金额是否合法，可被 transfer 内部调用，也可独立对外
         */
        public boolean validate(int amount) {
            // 获取当前账户的锁（如果 transfer 已经持有，这里重入计数 +1）
            lock.lock();
            // 保证最后一定解锁
            try {
                // 打印日志：当前线程、方法名、账户名、当前锁的重入次数
                // getHoldCount() 返回当前线程持有这把锁的次数
                System.out.println(Thread.currentThread().getName() + " [validate] " + name + " 持有锁计数: " + lock.getHoldCount());
                // 返回校验结果：金额必须大于 0，且余额 >= 金额
                return amount > 0 && balance >= amount;
            } finally {
                // 解锁（重入计数 -1）
                lock.unlock();
            }
        }

        /**
         * 扣款方法
         */
        public void debit(int amount) {
            // 获取当前账户的锁（重入）
            lock.lock();
            // 保证最后解锁
            try {
                // 打印日志：当前线程、debit 方法、账户名、锁重入计数
                System.out.println(Thread.currentThread().getName() + " [debit] " + name + " 持有锁计数: " + lock.getHoldCount());
                // 余额减去转账金额
                balance -= amount;
            } finally {
                // 解锁
                lock.unlock();
            }
        }

        /**
         * 入账方法（target 对象自己的锁）
         */
        public void credit(int amount) {
            // 获取"目标账户"自己的锁（注意：这里 this 是 target，不是转出账户）
            lock.lock();
            // 保证最后解锁
            try {
                // 打印日志：当前线程、credit 方法、账户名、锁重入计数
                System.out.println(Thread.currentThread().getName() + " [credit] " + name + " 持有锁计数: " + lock.getHoldCount());
                // 余额加上转账金额
                balance += amount;
            } finally {
                // 解锁
                lock.unlock();
            }
        }

        // 查询余额的方法
        public int getBalance() {
            // 加锁，保证读到的是其他线程修改后的最新值（保证可见性和一致性）
            lock.lock();
            // 保证最后解锁
            try {
                // 返回当前余额
                return balance;
            } finally {
                // 解锁
                lock.unlock();
            }
        }
    }
}