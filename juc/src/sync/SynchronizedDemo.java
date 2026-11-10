package sync;

/**
 *
 * 本 Demo 讲解知识点：
 * 1. 普通 synchronized 实例方法锁的是当前对象（this）
 * 2. 同一个对象的所有 synchronized 实例方法共享同一把锁
 * 3. 同一时刻只能有一个线程进入该对象的任意 synchronized 方法
 * 4. 非 synchronized 方法不受锁限制（可对比验证）
 * ============================================================
 */
public class SynchronizedDemo {

    /**
     * synchronized 实例方法 —— 锁的是 this（当前 SynchronizedDemo 实例）
     */
    public synchronized void methodA() {
        System.out.println(Thread.currentThread().getName() + " 进入同步方法 methodA，开始执行，休眠3秒...");

        try {
            Thread.sleep(3000); // 模拟业务耗时
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(Thread.currentThread().getName() + " 执行完毕，释放锁。");
    }

    /**
     * 另一个 synchronized 实例方法 —— 锁的也是 this（同一把锁）
     */
    public synchronized void methodB() {
        System.out.println(Thread.currentThread().getName() + " 进入同步方法 methodB，开始执行，休眠3秒...");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(Thread.currentThread().getName() + " 执行完毕，释放锁。");
    }

    /**
     * 非 synchronized 方法 —— 不受锁限制，任何线程随时可进入
     */
    public void normalMethod() {
        System.out.println(Thread.currentThread().getName() + " 进入普通方法（无锁），无需等待！");
    }

    public static void main(String[] args) {
        SynchronizedDemo demo0 = new SynchronizedDemo();

        // 线程A：调用 methodA
        Thread threadA = new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + " 准备获取锁...");
            demo0.methodA();
        }, "线程A");

        // 线程B：调用 methodB（同一个对象的不同同步方法）
        Thread threadB = new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + " 准备获取锁...");
            demo0.methodB();
        }, "线程B");

        // 线程C：调用普通方法（验证不受锁影响）
        Thread threadC = new Thread(() -> {
            demo0.normalMethod();
        }, "线程C");
        // threadA和threadB操作的都是同一个对象：demo0
        threadA.start();
        threadB.start();
        threadC.start();
    }
}