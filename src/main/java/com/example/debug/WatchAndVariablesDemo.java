package main.java.com.example.debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 变量视图与数据展示实操类
 * <p>
 * 调试时 80% 的信息来自 Variables 面板而不是控制台。
 * 这个类练三件事：
 *   1. toString 如何决定调试可读性（写好能省一半断点）
 *   2. Watches / 内联值 / 分组展示怎么配
 *   3. 集合、Map、数组、静态字段分别怎么看
 */
public class WatchAndVariablesDemo {

    // ============================================================
    //  场景 1 —— toString：有和没有的对比
    // ============================================================
    static class NoToStringUser {
        Long id;
        String name;
        int age;

        NoToStringUser(Long id, String name, int age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }
        // 故意不写 toString
    }

    static class WithToStringUser {
        Long id;
        String name;
        int age;

        WithToStringUser(Long id, String name, int age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }

        @Override
        public String toString() {
            return String.format("User#%d[%s, %d岁]", id, name, age);
        }
    }

    public static void scenario1ToString() {
        NoToStringUser a = new NoToStringUser(1L, "张三", 30);
        WithToStringUser b = new WithToStringUser(2L, "李四", 25);

        System.out.println("a = " + a);   // <-- 断点放这里，看 Variables 面板对比
        System.out.println("b = " + b);
    }

    /*
     * 【练习 1.1】在那一行断点，打开 Variables 面板
     *   a 显示：NoToStringUser@1b6d3586   ← 完全看不出内容
     *   b 显示：User#2[李四, 25岁]        ← 一眼看清
     *
     * ★ 这就是"生产代码必须写 toString"的真实理由：
     *   不是为了 log，是为了调试效率。没有 toString 时
     *   你必须展开树看每个字段，列表里一屏只能看 3 条数据。
     *
     * 【练习 1.2】没有 toString 时的三种补救
     *   1) Alt+F8 写 a.name
     *   2) Alt+F8 写 String.format("%s/%s/%s", a.id, a.name, a.age)
     *   3) 临时给类加 toString（比打断点快）
     *
     * 【练习 1.3】IDEA 自带的 render 技巧
     *   Settings → Debugger → Data Views → Java → 
     *     <数组> → "java.util.Arrays.toString(new Object[]{...})"
     *     <集合> → "java.util.Arrays.toString(new Object[]{...})"
     *   这是全局生效的默认渲染，不改任何业务代码。
     */


    // ============================================================
    //  场景 2 —— Watches：把常用表达式钉在面板上
    // ============================================================
    public static void scenario2Watches() {
        List<Order> orders = loadOrders();

        // 在这一行断点，然后打开 Debug 窗口的 Watches 标签（或 Alt+F8 旁的 +）
        // 依次添加下面这些表达式，然后按 F9 逐行观察变化：
        //   orders.size()
        //   orders.stream().filter(o -> "PAID".equals(o.status)).count()
        //   orders.stream().mapToDouble(o -> o.amount).sum()
        //   orders.stream().mapToDouble(o -> o.amount).max().getAsDouble()
        System.out.println("订单数 = " + orders.size());

        double total = 0;
        for (Order o : orders) {
            total += o.amount;
        }
        // Watch: total   → 每轮 F9 都能看到累加过程
        System.out.println("总额 = " + total);
    }

    /*
     * 【练习 2.1】Watches 面板在哪
     *   Debug 工具窗口 → 左侧的 Watches 标签（或 Alt+F8 里的下拉）
     *   快捷键：Debug 状态下 Ctrl+Shift+F10 切换 Watches 焦点？
     *   实际布局因版本不同，主路径是 Debug 窗口左侧的 "Watches" 标签页。
     *
     * 【练习 2.2】Watches 的核心价值
     *   - 表达式随每一行执行自动重新求值，不用反复 Alt+F8
     *   - 可以同时盯多个指标（比如同时盯 size 和 sum）
     *   - 表达式抛异常时不会中断程序，只在面板里显示异常信息
     *     ★ 这比在代码里 try-catch 一堆 System.out 优雅得多
     *
     * 【练习 2.3】Watches 与 Alt+F8 的区别
     *   Alt+F8  = 一次性求值，你手动按才执行
     *   Watches = 持续求值，每次断点停下自动重算
     *     用来盯"这个值随循环怎么变"时用 Watches
     *
     * 【练习 2.4】快速上手
     *   在 Variables 面板右键变量 → Add to Watches
     *   或者在 Variables 里选中一行按 Ctrl+Shift+F8？
     *   常用操作：拖拽表达式到 Watches 面板，双击编辑，F2 重命名，Alt+Delete 删除。
     */


    // ============================================================
    //  场景 3 —— 集合与 Map 的调试视图
    // ============================================================
    public static void scenario3Collections() {
        Map<String, List<Integer>> inventory = new LinkedHashMap<>();
        inventory.put("键盘", Arrays.asList(1, 2, 3));
        inventory.put("显示器", new ArrayList<>(Arrays.asList(4, 5)));
        inventory.put("鼠标", Arrays.asList(6));

        List<Order> orders = loadOrders();

        // 断点放在这里，展开 Variables 面板里的 inventory
        System.out.println("inventory = " + inventory);
        System.out.println("orders = " + orders);
    }

    /*
     * 【练习 3.1】在 Variables 面板展开 LinkedHashMap
     *   - 看到 entries 节点，key/value 分两列，能看到插入顺序（对比 HashMap 无序）
     *   - value 是 List 时可以继续展开看到每个 Integer
     *
     * 【练习 3.2】集合操作直接修改集合（Evaluate 的杀手锏）
     *   Alt+F8 输入：
     *     inventory.get("鼠标").add(99)     ← 修改真实集合
     *     inventory.put("新键", new ArrayList<>())
     *     inventory.remove("键盘")
     *     inventory.size()
     *   ★ 这是"快速验证假设"的顶级手法：想验证过滤逻辑对不对，
     *     直接在断点处改集合，再 F9 看后续代码走哪条分支。
     *
     * 【练习 3.3】大数据量的集合
     *   集合有 10000 个元素时展开 Variables 会卡住 IDEA。
     *   Alt+F8 改用摘要表达式：
     *     orders.stream().limit(10).collect(java.util.stream.Collectors.toList())
     *     orders.stream().map(o -> o.customer).distinct().collect(java.util.stream.Collectors.toList())
     *
     * 【练习 3.4】集合的 Show as
     *   Variables 面板里右键集合 → Show as → Array / Hash Table / Ascii String
     *   可以把 List 临时当数组看，索引直观。
     */


    // ============================================================
    //  场景 4 —— 数组与静态字段
    // ============================================================
    static int[] staticCounter = new int[3];   // 静态字段：调试时的隐形变量
    static final String APP_NAME = "debug-lab";

    public static void scenario4ArrayAndStatic() {
        int[] numbers = new int[]{10, 20, 30, 40, 50};
        String[] names = {"alice", "bob"};
        int[][] matrix = {{1, 2}, {3, 4}, {5, 6}};

        System.out.println("staticCounter = " + Arrays.toString(staticCounter));
        System.out.println("numbers = " + Arrays.toString(numbers));

        // 在这一行断点，Alt+F8 练习：
        //   numbers.length
        //   numbers[2]
        //   numbers[0] = 999        ← 改数组元素，观察下一行输出
        //   numbers
        //   java.util.Arrays.toString(numbers)
        //   matrix[1][0]
        //   matrix.length
        //   matrix[0]
        System.out.println("APP_NAME = " + APP_NAME);
    }

    /*
     * 【练习 4.1】数组在 Variables 面板里默认显示为 ArrayList 风格
     *   右键 → Show as → Array 可以看到 [0]=10, [1]=20 ... 带下标。
     *   二维数组要两层展开（matrix → row → element）。
     *
     * 【练习 4.2】数组越界的调试现场
     *   Alt+F8 输入 `numbers[10]` → 会看到
     *   ArrayIndexOutOfBoundsException: Index 10 out of bounds for length 5
     *   ★ Evaluate 里抛的异常不会中断程序，这是"安全试错"的关键。
     *
     * 【练习 4.3】静态字段：最容易漏看的变量
     *   在 Scenario 里断点时，staticCounter 的值会跨方法保留。
     *   Alt+F8 直接写 `staticCounter[0]++` 可以看到自增效果。
     *
     *   ★ 实战：单例 / 计数器 / 缓存被错误复用时，
     *     静态字段是第一个要看的地方。IDEA 的 Variables 面板
     *     会把 static 字段单独归类（展开类名下的 "static" 节点）。
     *
     * 【练习 4.4】eval 里访问静态字段要注意作用域
     *   在实例方法里写 staticCounter（不带类名）可能解析不到，
     *   用全限定：WatchAndVariablesDemo.staticCounter
     */


    // ============================================================
    //  场景 5 —— 内联值（Show Inline Values）
    // ============================================================
    public static void scenario5InlineValues() {
        Order order = loadOrders().get(0);

        // 开启 Show Inline Values 后，
        // 上面这行的行尾会直接显示：Order order = Order#1[张三 500.00 ...]
        // 不打断点也能看到，调试时非常省事。
        System.out.println("order = " + order);

        int x = 10;
        int y = 20;
        System.out.println("x + y = " + (x + y));
    }

    /*
     * 【练习 5.1】开启方式
     *   Settings → Build, Execution, Deployment → Debugger → Data Views
     *     → Show Inline Values（快捷键 Alt+S，勾上）
     *
     * 【练习 5.2】效果与代价
     *   好处：不用打断点就能看到赋值结果
     *   代价：代码看起来会很"吵"，长表达式会被截断
     *   建议：本地排查时开，提交代码前关掉（它是 IDE 设置，不进仓库，放心）。
     *
     * 【练习 5.3】IntelliJ 的 "Show toString" 也有快捷键
     *   在 Variables 里选中变量，按 Shift+F4 可以在
     *   "Show toString" 和 "Show as Hashcode/String" 之间切换视图。
     */


    // ============================================================
    //  场景 6 —— 循环引用的调试观察
    // ============================================================
    static class Node {
        String name;
        Node next;

        Node(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name + "->" + (next == null ? "null" : next.name);
        }
    }

    public static void scenario6CircularReference() {
        Node a = new Node("A");
        Node b = new Node("B");
        Node c = new Node("C");

        a.next = b;
        b.next = c;
        c.next = a;      // 环：A → B → C → A

        System.out.println("a = " + a);
        System.out.println("环长度 = " + ringLength(a, 0));
    }

    private static int ringLength(Node start, int steps) {
        return steps >= 3 ? steps : ringLength(start.next, steps + 1);
    }

    /*
     * 【练习 6.1】在 println 那行断点
     *   Variables 面板展开 a：会看到 next → b → next → c → next → a → ...
     *   IDEA 用缩进和循环标记防止无限展开，不会卡死。
     *
     * 【练习 6.2】LinkedList / 双向链表调试
     *   Alt+F8 求值避免在面板里无限展开：
     *     a.next.next.name
     *     a.next.next.next == a        ← true，说明成环了
     *
     * ★ toString 里遇到环必须做保护，否则打日志会直接 StackOverflowError。
     *   这是面试常问的"循环引用怎么打印"。
     */


    // ============================================================
    //  辅助数据
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

        @Override
        public String toString() {
            return String.format("Order#%d[%s %.2f %s %s]", id, customer, amount, status, items);
        }
    }

    static List<Order> loadOrders() {
        return new ArrayList<>(Arrays.asList(
                new Order(1L, "张三", 500.00, "PENDING", Arrays.asList("鼠标")),
                new Order(2L, "李四", 3200.00, "PAID", Arrays.asList("键盘", "显示器")),
                new Order(3L, "王五", 150.00, "CANCELLED", Arrays.asList("数据线")),
                new Order(4L, "张三", 2800.00, "PAID", Arrays.asList("笔记本", "鼠标垫"))
        ));
    }

    static Map<String, String> quickMap() {
        Map<String, String> m = new HashMap<>();
        m.put("k1", "v1");
        m.put("k2", "v2");
        return m;
    }


    public static void main(String[] args) {
        System.out.println("===== 场景1: toString 对比 =====");
        scenario1ToString();

        System.out.println("\n===== 场景2: Watches =====");
        scenario2Watches();

        System.out.println("\n===== 场景3: 集合与 Map =====");
        scenario3Collections();

        System.out.println("\n===== 场景4: 数组与静态字段 =====");
        scenario4ArrayAndStatic();

        System.out.println("\n===== 场景5: 内联值 =====");
        scenario5InlineValues();

        System.out.println("\n===== 场景6: 循环引用 =====");
        scenario6CircularReference();

        System.out.println("\n全部执行完毕。");
    }
}