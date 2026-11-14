package interrupt;

/**
 * volatile 软中断 Demo（极简版）
 *
 * 核心：纯轮询线程没有阻塞操作，Thread.interrupt() 无效，
 * 用 volatile 标志让主线程的修改立即被工作线程感知。
 */
public class VolatileSoftInterruptDemo {

    // 软中断标志：volatile 保证主线程写的值立刻穿透到主内存，工作线程下一次循环就能读到
    private static volatile boolean stop = false;

    public static void main(String[] args) throws Exception {
        Thread t = new Thread(() -> {
            int count = 0;
            while (!stop) {
                count++;
                System.out.println("[工作线程] 运行中 # " + count);
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
            System.out.println("[工作线程] 收到软中断，安全退出");
        }, "工作线程");

        t.start();
        Thread.sleep(3000);

        System.out.println("\n[主线程] stop = true → 发出软中断\n");
        stop = true;

        t.join();
        System.out.println("[主线程] 程序结束");
    }
}