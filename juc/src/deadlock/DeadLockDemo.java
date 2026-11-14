package deadlock;

/**
 * 死锁竞争案例:
 * 线程A 获取A锁之后，期望获取B锁
 * 线程B 获取B锁之后，期望获取A锁
 * 死锁: 线程A和线程B都获取到了对方的锁，导致无法继续执行
 * 最终结果：程序进入死锁状态，无法继续执行
 */
public class DeadLockDemo {
    public static void main(String[] args) {
        final Object objA = new Object();
        final Object objB = new Object();

        new Thread(() -> {
            //线程A 获取objA锁
            synchronized (objA) {
                System.out.printf(Thread.currentThread().getName() + "\t获取objA锁成功，期望获取objB锁");
                try {
                    Thread.sleep(1000);
                    //线程A 获取objB锁
                    synchronized (objB) {
                        System.out.printf(Thread.currentThread().getName() + "\t成功获得B锁\n");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "线程A").start();


        new Thread(() -> {
            //线程B 获取objB锁
            synchronized (objB) {
                try {
                    System.out.printf(Thread.currentThread().getName() + "\t获取objB锁成功，期望获取objA锁\n");
                    Thread.sleep(1000);
                    //线程B 获取objA锁
                    synchronized (objA) {
                        System.out.printf(Thread.currentThread().getName() + "\t成功获得A锁\n");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "线程B").start();
    }
}
