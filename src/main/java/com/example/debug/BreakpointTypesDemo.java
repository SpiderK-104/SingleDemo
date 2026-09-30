package main.java.com.example.debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 断点类型全景实操类
 * <p>
 * 你的 EvaluateExpressionDemo 只练了"行断点 + Alt+F8"。
 * 这个类把 IDEA 全部 7 种断点类型都做成可运行的最小场景，外加"断点为什么不停"的排查。
 * <p>
 * 快捷键：Alt+R 打开 Run 菜单；Shift+F9 Debug；F9 Resume；Alt+F8 Evaluate。
 */
public class BreakpointTypesDemo {

    // ============================================================
    //  场景 1 —— 行断点 + 挂起策略（Suspend: Thread / All）
    // ============================================================
    public static void scenario1LineBreakpoint() {
        for (int i = 1; i <= 5; i++) {
            System.out.println("行断点命中第 " + i + " 次");   // <-- 在此行设行断点
        }
    }

    /*
     * 【练习 1.1】普通行断点
     *   在 println 行右键 → 断点变实心红点，F9 单步 / 再按 F9 直接跑到下一个断点。
     *
     * 【练习 1.2】挂起策略（重点，最容易被忽略）
     *   断点右键 → Suspend → Thread / All
     *     Thread：只挂起命中的那个线程，其它线程继续跑
     *     All   ：挂起所有线程（IDEA 默认是 All）
     *   观察方法：断点停住后，看 Debug 窗口的 Threads 面板，
     *             切换 Thread 挂起时其它线程状态仍是 "running"，
     *             All 时全部变成 "suspended"。
     *   ★ 多线程程序里如果选了 Thread，你可能永远看不到"其他线程"的状态。
     *
     * 【练习 1.3】临时断点
     *   Alt+R（Run 菜单）→ Temporary Breakpoint，命中一次后自动消失。
     *   用来在不想污染断点列表的情况下快速探一下。
     */


    // ============================================================
    //  场景 2 —— 条件断点（Conditional Breakpoint）
    // ============================================================
    public static void scenario2ConditionalBreakpoint() {
        List<BreakpointTypesDemo.Order> orders = buildOrders();

        for (BreakpointTypesDemo.Order o : orders) {
            System.out.println("候选: " + o);   // <-- 在此行设断点，再右键加 Condition
        }
    }

    /*
     * 【操作】断点右键 → Condition...，可以填以下任意一条，然后 Resume（F9）
     *
     *   o.amount > 1000
     *   "PAID".equals(o.status)
     *   o.amount > 1000 && "PAID".equals(o.status)
     *   o.customer.startsWith("张")
     *   o.items.size() > 2
     *   o.id % 2 == 0
     *   true                    ← 相当于没有条件
     *
     * 【高级】Condition 对话框里的 "Evaluate once"
     *   勾上 = 条件只在第一次到达该行时求值一次，之后不再判断（更快）
     *   不勾 = 每次命中都求值（能配合条件里的副作用，但慢）
     *
     * ★ 条件里可以写任意合法 Java 表达式，但不要写有副作用的代码
     *   （比如 orders.remove(0)，会真的删元素）。
     */


    // ============================================================
    //  场景 3 —— 命中次数断点（Hit Count）
    // ============================================================
    public static void scenario3HitCount() {
        for (int i = 1; i <= 20; i++) {
            System.out.println("第 " + i + " 次调用");   // <-- 断点右键 → Hit Count...
        }
    }

    /*
     * 【操作】断点右键 → Hit Count...
     *   Invocation count is equal to 5      → 第 5 次命中才挂起
     *   Invocation count is greater than 5  → 第 6 次起每次都挂起
     *   Invocation count is multiple of 4   → 第 4、8、12... 次挂起
     *
     * ★ 实战价值：线上偶发问题常见做法是"跑 1000 次，第 837 次才炸"，
     *   用命中次数断点可以直接把程序停在那一次，省掉逐次 F8。
     *
     * ★ 另一个用法：把命中率调成 equal to 1，就是"只看第一次进入"。
     */


    // ============================================================
    //  场景 4 —— 日志断点（Logpoint：不中断，只打日志）
    // ============================================================
    public static void scenario4Logpoint() {
        for (int i = 1; i <= 5; i++) {
            // 故意什么都不打印，用来演示"不打断程序也能出日志"
            consume(i);
        }
    }

    private static void consume(int i) {
        if (i % 2 == 0) {
            System.out.println("consume: " + i);   // <-- 在此行设日志断点
        }
    }

    /*
     * 【操作】断点右键 → Suspend 选 Never，Log message 填：
     *
     *   entered consume, i = {i}
     *   i={i}, doubled={i * 2}, sqrt={Math.sqrt(i)}
     *
     *   然后把代码里的 println 全删掉、按 F9 跑完，
     *   Console 里照样能看到日志 —— 这就是日志断点。
     *
     * ★ 和"临时加一行 System.out.println 再删掉"的区别：
     *   不改一行代码、不重新编译、不影响业务逻辑，还能算任意表达式。
     *
     * ★ 注意：Suspend = Never 之后，断点图标会变成"小眼睛/水滴"样式，不再是实心红点。
     */


    // ============================================================
    //  场景 5 —— 方法断点（Method Breakpoint）
    // ============================================================
    interface PayHandler {
        void pay(BreakpointTypesDemo.Order order);
    }

    static class WechatPayHandler implements PayHandler {
        @Override
        public void pay(BreakpointTypesDemo.Order order) {
            order.status = "PAID";   // 方法断点会停在这行的上一条语句位置
            System.out.println("微信支付成功: " + order.id);
        }
    }

    static class AlipayHandler implements PayHandler {
        @Override
        public void pay(BreakpointTypesDemo.Order order) {
            order.status = "PAID";
            System.out.println("支付宝成功: " + order.id);
        }
    }

    public static void scenario5MethodBreakpoint() {
        List<PayHandler> handlers = Arrays.asList(new WechatPayHandler(), new AlipayHandler());
        List<BreakpointTypesDemo.Order> orders = buildOrders();

        for (BreakpointTypesDemo.Order o : orders) {
            PayHandler handler = handlers.get((int) (o.id % 2));   // 交替使用不同实现
            handler.pay(o);
        }
    }

    /*
     * 【操作】光标放在方法名 pay 上 → Ctrl+F8 / 右键 → Add Method Breakpoint
     *   Invocation（默认）：方法被调用时停
     *   Entry：真正进入方法体第一行时停
     *   Exit：返回时停
     *
     * ★ 实战价值：这是"回答类问题"的利器
     *   - 这个方法到底被调用了几次？→ Invocation + hit count 配合
     *   - 是谁调用了它？          → 停住后看 Frames 面板最上面一帧
     *   - 为什么多次调用？        → 看栈帧参数每次是否相同
     *
     * ★ 方法断点加在接口方法上，对所有实现类同时生效（这里会命中
     *   WechatPayHandler.pay 和 AlipayPayHandler.pay 两次）。
     */


    // ============================================================
    //  场景 6 —— 字段断点（Field Breakpoint）—— 排查"谁改了这个变量"
    // ============================================================
    static class Account {
        String status = "INIT";     // <-- 光标放这里 → Add Field Breakpoint
        long balance;

        void setStatus(String next) {
            this.status = next;
        }

        void deduct(long amount) {
            balance -= amount;
        }
    }

    public static void scenario6FieldBreakpoint() {
        Account acc = new Account();

        acc.setStatus("VERIFIED");
        acc.deduct(100);
        acc.setStatus("FROZEN");
        acc.deduct(50);
        acc.setStatus("CLOSED");

        System.out.println("最终状态: " + acc.status);
    }

    /*
     * 【操作】光标放在字段声明 status 上 → 右键 → Add Field Breakpoint
     *   Field access       → 每次读或写都停（非常吵，一般不用）
     *   Field modification → 只有写的时候停
     *
     * ★ 这是最强的一招：代码里有 5 个地方给 status 赋值，
     *   手动逐个下断点要改 5 次代码；字段断点一次搞定，
     *   而且任何地方（包括第三方代码）改它都会停。
     *
     * ★ 实战价值：定位"这个值怎么变成 null / -1 / 2026-01-01 的"
     *   —— 有字段断点就不用再猜了。
     *
     * ★ 补充：balance 也试一次字段断点，观察 deduct 里的 balance -= amount
     *   是否会命中（复合赋值也写字段，会命中）。
     */


    // ============================================================
    //  场景 7 —— 异常断点（Exception Breakpoint）
    // ============================================================
    static class PaymentException extends RuntimeException {
        PaymentException(String message) {
            super(message);
        }
    }

    public static void scenario7ExceptionBreakpoint() {
        try {
            charge(8000);
        } catch (PaymentException e) {
            System.out.println("被捕获: " + e.getMessage());   // <-- 异常断点会在进入这里之前停住
        }

        try {
            charge(50);
        } catch (PaymentException e) {
            System.out.println("被捕获: " + e.getMessage());
        }
    }

    private static void charge(int amount) {
        if (amount > 5000) {
            throw new PaymentException("超出风控限额: " + amount);   // <-- 抛出点
        }
        System.out.println("扣款成功: " + amount);
    }

    /*
     * 【操作】Alt+R → View Breakpoints... → 左侧选 "Java Exception Breakpoints" → 绿色 +
     *   Exception class: main.java.com.example.debug.BreakpointTypesDemo$PaymentException
     *   勾选 Caught Exceptions（被 catch 住的）
     *   勾选 Uncaught Exceptions（没人 catch、最终抛到 main 的）
     *   Cancel 选 "Don't break" / "Break when exception in class" 精确限定抛异常的类
     *
     * ★ 最关键的认知：勾上 Caught Exceptions 后，
     *   即使业务代码 catch 住不让它扩散，调试器也会在异常被创建的那一刻停住。
     *   你可以在这里看完整的调用栈 + 现场变量，比在 catch 里猜强得多。
     *
     * ★ 也可以选 "Any Exception" + Suspend: Never 配合 Log message，
     *   变成"全量异常日志断点"，很多线上疑难问题就是这么抓的。
     *
     * ★ Exception class 支持通配：java.lang.* 能一次性盯住所有 JDK 异常（会很吵）。
     */


    // ============================================================
    //  场景 8 —— 断点为什么不停？（实战排查清单）
    // ============================================================
    public static void scenario8WhyNotHit() {
        // (1) 分支根本没走到
        if (System.getProperty("never.set") != null) {
            System.out.println("永远不会执行");   // <-- 断点放这儿，Release 模式下空跑
        }

        // (2) 断点在被 continue 跳过的行：只会命中一半
        for (int i = 0; i < 5; i++) {
            if (i % 2 == 1) {
                continue;
            }
            System.out.println("只在偶数 i 命中: " + i);   // <-- 断点，只会停 0/2/4 三次
        }

        // (3) 断点打在 lambda 体上，可能只停到外层这一行
        List<String> list = Arrays.asList("a", "b", "c");
        list.forEach(item -> System.out.println("lambda: " + item));   // <-- 断点位置实验
    }

    /*
     * 断点不生效的排查顺序（照着查）：
     *
     *   1. 断点是不是实心的？灰色 = 被 disabled（Mute Breaks 或点了 Mute 按钮）
     *      Alt+R → View Breakpoints 看左侧有没有勾选框，勾掉 muted 的即可。
     *
     *   2. 是不是用 Run（Shift+F10）跑的而不是 Debug（Shift+F9）？
     *      普通 Run 根本不挂调试器，断点当然不会停。
     *
     *   3. 用的是 Debug 还是 Attach？
     *      项目改了代码后，如果 Attach 的是旧进程 / 容器里的老 jar，断点对不上。
     *
     *   4. 行号对不上（最常见的玄学问题）：
     *      改了代码但没重新编译（Build → Build Project / Ctrl+F9），
     *      调试器用的还是旧 class 的行号表 LineNumberTable。
     *      典型症状：断点自动跳到几行之外，或者根本不亮。
     *
     *   5. 代码在 lambda / 增强 for / switch 里，行号映射本来就特殊，
     *      试试把断点挪到外层语句。
     *
     *   6. 代码被优化 / 根本没执行（try (final var) 里的判断）。
     *
     *   7. 断点打在了方法体外（比如接口的 default 实现、抽象方法上）。
     *
     *   8. 多线程 + Suspend: Thread，而命中的不是你正在看的那个线程。
     *
     *   9. 该段代码被 AOP / 字节增强（如 Spring 事务、Mock 代理）重新生成过，
     *      行号可能对不上真实源码。
     */


    // ============================================================
    //  内部数据
    // ============================================================
    static class Order {
        long id;
        String customer;
        double amount;
        String status;
        List<String> items;

        Order(long id, String customer, double amount, String status, List<String> items) {
            this.id = id;
            this.customer = customer;
            this.amount = amount;
            this.status = status;
            this.items = items;
        }

        boolean isHighValue() {
            return amount > 1000;
        }

        @Override
        public String toString() {
            return String.format("Order#%d[%s %.2f %s %s]", id, customer, amount, status, items);
        }
    }

    static List<Order> buildOrders() {
        return new ArrayList<>(Arrays.asList(
                new Order(1L, "张三", 500.00, "PENDING", Arrays.asList("鼠标")),
                new Order(2L, "李四", 3200.00, "PAID", Arrays.asList("键盘", "显示器")),
                new Order(3L, "王五", 150.00, "CANCELLED", Arrays.asList("数据线")),
                new Order(4L, "张三", 2800.00, "PAID", Arrays.asList("笔记本", "鼠标垫")),
                new Order(5L, "赵六", 6000.00, "PENDING", Arrays.asList("主机"))
        ));
    }


    public static void main(String[] args) {
        System.out.println("===== 场景1: 行断点与挂起策略 =====");
        scenario1LineBreakpoint();

        System.out.println("\n===== 场景2: 条件断点 =====");
        scenario2ConditionalBreakpoint();

        System.out.println("\n===== 场景3: 命中次数断点 =====");
        scenario3HitCount();

        System.out.println("\n===== 场景4: 日志断点 =====");
        scenario4Logpoint();

        System.out.println("\n===== 场景5: 方法断点 =====");
        scenario5MethodBreakpoint();

        System.out.println("\n===== 场景6: 字段断点 =====");
        scenario6FieldBreakpoint();

        System.out.println("\n===== 场景7: 异常断点 =====");
        scenario7ExceptionBreakpoint();

        System.out.println("\n===== 场景8: 断点不生效排查 =====");
        scenario8WhyNotHit();

        System.out.println("\n全部执行完毕。每个场景的注释里写了断点该怎么设。");
    }
}