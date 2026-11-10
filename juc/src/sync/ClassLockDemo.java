package sync;

/**
 * 本 Demo 讲解知识点：
 * 1. static synchronized 方法锁的是 Class 对象（类锁），不是 this
 * 2. JVM 中每个类只有一个 Class 对象，所以类锁全局唯一
 * 3. 不管 new 多少个实例，所有 static synchronized 方法共享同一把类锁
 * 4. 类锁和对象锁互不干扰（static synchronized vs synchronized 不互斥）
 * ============================================================
 */
public class ClassLockDemo {

    /**
     * 静态同步方法 —— 锁的是 ClassLockDemo.class（类对象）
     */
    public static synchronized void sendEmail() {
        System.out.println(Thread.currentThread().getName() + " 准备发邮件（静态同步方法，锁Class）...");
        System.out.println(Thread.currentThread().getName() + " 发邮件中，休眠3秒...");
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(Thread.currentThread().getName() + " 邮件发完，释放类锁。");
    }

    /**
     * 普通同步方法 —— 锁的是 this（对象锁），用来对比验证"类锁和对象锁不互斥"
     */
    public synchronized void sendSms() {
        System.out.println(Thread.currentThread().getName() + " 发短信（普通同步方法，锁this）...");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(Thread.currentThread().getName() + " 短信发完，释放对象锁。");
    }

    public static void main(String[] args) throws InterruptedException {
        ClassLockDemo phone1 = new ClassLockDemo();
        ClassLockDemo phone2 = new ClassLockDemo();

        // ===== 实验一：不同实例 + 静态同步方法 → 互斥（类锁）=====
        Thread threadA = new Thread(() -> {
            ClassLockDemo.sendEmail();  // 静态方法，用类名调用
        }, "线程A");

        Thread threadB = new Thread(() -> {
            ClassLockDemo.sendEmail();  // 同样调静态方法，不同实例也互斥
        }, "线程B");

        threadA.start();
        Thread.sleep(100); // 确保A先拿到锁
        threadB.start();

        // ===== 实验二（取消注释验证）：类锁 vs 对象锁 不互斥 =====
        // Thread threadC = new Thread(() -> {
        //     phone1.sendSms();  // 锁的是 phone1 的 this
        // }, "线程C");
        //
        // threadC.start();
        // → 线程C 和 线程A 会并发执行，因为锁的不是同一个东西
    }
}