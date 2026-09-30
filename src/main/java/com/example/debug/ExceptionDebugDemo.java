package main.java.com.example.debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

/**
 * 异常调试实操类
 * <p>
 * 异常调试的三个核心问题：
 *   1. 为什么没在预期的地方断住？  → 异常断点配置（Caught / Uncaught）
 *   2. 抛出时的现场是什么？        → 异常断点停在 throw 瞬间的完整栈帧
 *   3. 为什么栈信息和我想的不一样？ → 栈展开顺序、finally 陷阱
 */
public class ExceptionDebugDemo {

    // ============================================================
    //  场景 1 —— 异常断点：Caught vs Uncaught
    // ============================================================
    static class OrderNotFoundException extends RuntimeException {
        final long orderId;

        OrderNotFoundException(long orderId, String message) {
            super(message);
            this.orderId = orderId;
        }
    }

    public static void scenario1CaughtVsUncaught() {
        // (a) 被 catch 住 → 只有勾了 Caught Exceptions 才断得住
        try {
            throw new OrderNotFoundException(1001L, "订单不存在: 1001");
        } catch (OrderNotFoundException e) {
            System.out.println("[a] 被捕获: " + e.getMessage() + ", orderId=" + e.orderId);
        }

        // (b) 没被 catch → Uncaught Exceptions 能断住
        unsafeThrow(2002L);
    }

    private static void unsafeThrow(long orderId) {
        throw new OrderNotFoundException(orderId, "订单不存在: " + orderId);
    }

    /*
     * 【练习 1.1】配置
     *   Alt+R → View Breakpoints → Java Exception Breakpoints → +
     *   Exception class 填：main.java.com.example.debug.ExceptionDebugDemo$OrderNotFoundException
     *   ☑ Caught Exceptions
     *   ☑ Uncaught Exceptions
     *
     * 【练习 1.2】两种勾选的差别
     *   只勾 Caught    → 场景 (a) 断住，(b) 也断住（因为它一路传到 main 也算被 JVM 捕获）
     *   只勾 Uncaught  → (a) 不断，(b) 断
     *
     *   ★ 关键结论：勾上 Caught 后，异常一被 new 出来调试器就停住，
     *     比在 catch 块里下断点强得多 —— 你能拿到"抛出瞬间"的
     *     局部变量、调用栈，而不是 catch 之后的残局。
     *
     * 【练习 1.3】现场能看什么
     *   停住后 Alt+F8 输入：
     *     e.getMessage()
     *     e.orderId                    ← 自定义异常携带的业务字段
     *     e.getStackTrace().length
     *     Arrays.toString(e.getStackTrace())   ← 注意要 import，用全限定名
     *     e.getStackTrace()[0].getMethodName()
     *   完整类名写法（Evaluate 里可以省 import）：
     *     java.util.Arrays.toString(e.getStackTrace())
     *
     * 【练习 1.4】条件化异常断点
     *   Exception class 填 java.lang.* ，Condition 填
     *     e instanceof NullPointerException
     *   只在 NPE 时停，噪音小很多。
     */


    // ============================================================
    //  场景 2 —— 栈展开顺序：throw → catch → finally → 调用方
    // ============================================================
    public static void scenario2StackUnwind() {
        try {
            levelA();
        } catch (IllegalStateException e) {
            System.out.println("[catch] " + e.getMessage());
            printTopFrames(e, 3);            // 此刻栈里已经看不到 levelA 了
        } finally {
            System.out.println("[finally] 资源清理");
        }
    }

    private static void levelA() {
        levelB();
    }

    private static void levelB() {
        levelC();
    }

    private static void levelC() {
        throw new IllegalStateException("levelC 内部出错");
    }

    private static void printTopFrames(Throwable t, int n) {
        System.out.println("  栈帧数量 = " + t.getStackTrace().length);
        StackTraceElement[] top = t.getStackTrace();
        for (int i = 0; i < Math.min(n, top.length); i++) {
            System.out.println("    #" + i + " " + top[i]);
        }
    }

    /*
     * 【练习 2.1】栈展开的三个观察点
     *   在 levelC 的 throw 行、catch 的 println 行、finally 的 println 行
     *   各下一个断点，按 F9 走一遍，注意 Frames 面板的变化：
     *
     *   停在 throw 行：levelC → levelB → levelA → scenario2StackUnwind  （4 层）
     *   停在 catch 行：scenario2StackUnwind                              （只剩 1 层）
     *   停在 finally 行：同上
     *
     *   ★ 核心结论：Throwable 里存的 stackTrace 是"抛出那一瞬间"拍下的快照，
     *     不会随着栈展开而更新。所以 catch 里 getStackTrace() 仍然能看到
     *     levelC/levelB/levelA —— 但 debug 面板的 Frames 已经只剩当前层。
     *     这两个"调用栈"不是一回事，别混淆。
     *
     * 【练习 2.2】catch 里还能拿到什么
     *   Alt+F8：`e` 的字段、异常对象的 toString()（默认是类名 + message）
     *   `e.getCause()` ← 异常链，包装异常时必看
     *   `Arrays.stream(e.getStackTrace()).map(StackTraceElement::getMethodName).collect(...)`
     */


    // ============================================================
    //  场景 3 —— finally 的三个坑（调试时最容易怀疑异常的地方）
    // ============================================================
    public static void scenario3FinallyPitfalls() {
        System.out.println("(1) finally 正常执行，返回值 = " + returnFromTry());

        try {
            throw new RuntimeException("原始异常");
        } catch (RuntimeException e) {
            System.out.println("(2) 原始异常 = " + e.getMessage());
        } finally {
            System.out.println("    finally 依然执行");
        }

        System.out.println("(3) 嵌套 finally，return 会覆盖异常");
        try {
            nestedFinallyReturn();
        } catch (Exception e) {
            System.out.println("    外层没抓到异常? " + e);   // 实际不会打印
        }
    }

    private static int returnFromTry() {
        try {
            return 1;
        } finally {
            System.out.println("    finally 执行了，但 return 1 已经定了");
        }
    }

    private static void nestedFinallyReturn() {
        try {
            throw new IllegalArgumentException("抛出");
        } finally {
            return;     // ★ 坑：吞掉所有异常
        }
    }

    /*
     * 【练习 3.1】坑一：finally 执行 ≠ 能改变结果
     *   在 returnFromTry 的 finally println 处断点，
     *   Frames 面板里的 return value 已经能预判（IDEA 会显示"return value: ..."）。
     *   ★ 教程：IDEA 在 return 语句之后的断点会被标成"不可达"，而且 Frames
     *     面板直接告诉你 "not available"。这就是 finally 的 return 为什么无效的证据。
     *
     * 【练习 3.2】坑二：finally 里的 return 会吞掉异常
     *   nestedFinallyReturn 抛了异常但被 finally 的 return 覆盖，
     *   异常断点（Caught + Uncaught）会告诉你：异常被创建过，但从未传播。
     *   ★ 排查线上"异常莫名其妙消失"时，第一件事就是搜代码里的 finally { return ... }
     *
     * 【练习 3.3】坑三：try-with-resources 里关闭顺序是声明的逆序
     *   见场景 4。
     */


    // ============================================================
    //  场景 4 —— try-with-resources 的异常覆盖问题
    // ============================================================
    public static void scenario4TryWithResources() {
        try (BufferedReader r1 = new BufferedReader(new StringReader("first line\nsecond line"))) {
            System.out.println("[r1] " + r1.readLine());
        } catch (IOException e) {
            System.out.println("不会到这里: " + e.getMessage());
        }

        // 两个资源：close 顺序是逆序 (r2 先关，再关 r1)
        try (BufferedReader r1 = new BufferedReader(new StringReader("A"))) {
            System.out.println("[outer] 打开 r1");
        } catch (IOException e) {
            // ignore
        }

        // ★ try-with-resources 里若 close() 抛异常，会覆盖掉 try 块里的异常，
        //   原始异常被塞进 getSuppressed()。
        //   断点停在 catch 行，Alt+F8：
        //     e.getSuppressed().length
        //     java.util.Arrays.toString(e.getSuppressed())
        //   ★ 线上"Unknown error"排查必备技能。
    }


    // ============================================================
    //  场景 5 —— 栈溢出（StackOverflowError）：无限递归
    // ============================================================
    static int infiniteRecursion(int depth) {
        return infiniteRecursion(depth + 1);   // 故意没有出口
    }

    public static void scenario5StackOverflow() {
        int depth = 0;
        try {
            infiniteRecursion(depth);
        } catch (StackOverflowError err) {
            System.out.println("栈溢出了，栈深度约 " + err.getStackTrace().length);
        }
    }

    /*
     * 【练习 5.1】异常断点 + StackOverflowError
     *   Java Exception Breakpoints 里勾选 Throwable + Caught，
     *   在 infiniteRecursion 的递归行断点，
     *   可以看到 Frames 一层层加深直到栈耗尽。
     *
     * 【练习 5.2】IDEA 的栈溢出诊断
     *   一旦 StackOverflowError 发生，IDEA 会把 Variables 面板折叠，
     *   只显示 100 层左右的栈帧（防卡死）。
     *   想看更深的：Settings → Debugger → General → "Stack frames" 上限调整，
     *   或直接 Alt+F8 求值 err.getStackTrace().length。
     *
     * 【练习 5.3】递归深度经验值
     *   Alt+F8: `TThread.currentThread().getStackTrace().length`
     *   （Thread 起始的 stack trace 会过滤掉自己的几帧）
     */


    public static void main(String[] args) {
        System.out.println("===== 场景1: Caught vs Uncaught =====");
        try {
            scenario1CaughtVsUncaught();
        } catch (OrderNotFoundException e) {
            System.out.println("[b] 冒泡到 main: " + e.getMessage());
        }

        System.out.println("\n===== 场景2: 栈展开顺序 =====");
        scenario2StackUnwind();

        System.out.println("\n===== 场景3: finally 陷阱 =====");
        scenario3FinallyPitfalls();

        System.out.println("\n===== 场景4: try-with-resources =====");
        scenario4TryWithResources();

        System.out.println("\n===== 场景5: 栈溢出 =====");
        scenario5StackOverflow();

        System.out.println("\n全部执行完毕。");
    }
}