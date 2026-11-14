package interrupt;

/**
 * Thread 原生中断 Demo（极简版）
 * <p>
 * 核心：Thread.interrupt() + InterruptedException / isInterrupted()
 * <p>
 * 和软中断的区别：
 * 软中断（volatile/Atomic）：纯轮询线程，自己手动 while(!stop) 检查
 * 原生中断：               有阻塞操作（sleep/wait/lock）时，JVM 自动抛 InterruptedException
 */
public class NativeInterruptDemo {

    public static void main(String[] args) throws Exception {
        Thread t = new Thread(() -> {
            try {
                int count = 0;
                // 方式一：阻塞操作（sleep/wait/lock）被 interrupt 时会直接抛 InterruptedException
                while (!Thread.currentThread().isInterrupted()) {
                    count++;
                    System.out.println("[工作线程] 运行中 # " + count);
                    Thread.sleep(1000); // 阻塞时被 interrupt → 直接跳到 catch
                }
            } catch (InterruptedException e) {
                // 方式二：捕获 InterruptedException 后退出
                Thread.currentThread().interrupt(); // 【关键】恢复中断标志，防止吞掉信号
            }
            System.out.println("[工作线程] 收到原生中断，安全退出");
        }, "工作线程");

        t.start();
        Thread.sleep(3000);

        System.out.println("\n[主线程] t.interrupt() → 发出原生中断\n");
        t.interrupt(); // JVM 自动处理：如果 t 在 sleep 里 → 立刻抛 InterruptedException

        t.join();
        System.out.println("[主线程] 程序结束");
    }
}