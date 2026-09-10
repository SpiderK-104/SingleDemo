package main.java.com.example.thread;

import java.util.*;
import java.util.stream.*;

/**
 * Evaluate Expression 调试实操类
 *
 * 断点设在 main 方法每一段的打印语句行，
 * 在断点暂停时按 Alt+F8（Win/Linux）或 Option+F8（Mac）打开 Evaluate Expression 窗口，
 * 然后输入右侧注释中建议的表达式，观察结果。
 *
 * 目标：学会在调试时即时计算任意表达式，而不是用 System.out.println 猜变量。
 */
public class EvaluateExpressionDemo {

    // ============================================================
    //  辅助类 —— 模拟开发接口中的数据传输对象
    // ============================================================
    static class Order {
        long id;
        String customer;
        double amount;
        String status;      // PENDING, PAID, CANCELLED
        List<String> items;

        Order(long id, String customer, double amount, String status, List<String> items) {
            this.id = id; this.customer = customer; this.amount = amount;
            this.status = status; this.items = items;
        }

        boolean isHighValue() { return amount > 1000; }

        @Override
        public String toString() {
            return String.format("Order#%d[%s %.2f %s %s]",
                    id, customer, amount, status, items);
        }
    }

    static class ApiResponse<T> {
        int code;
        String message;
        T data;

        ApiResponse(int code, String message, T data) {
            this.code = code; this.message = message; this.data = data;
        }

        boolean isSuccess() { return code == 200; }

        @Override
        public String toString() {
            return String.format("ApiResponse{code=%d, msg='%s', data=%s}", code, message, data);
        }
    }


    // ============================================================
    //  场景 1 —— 基础表达式：变量运算、方法调用、类型转换
    // ============================================================
    public static void demoBasicExpressions() {
        String name = "alice";
        int score = 85;
        double rate = 0.085;
        List<Integer> numbers = Arrays.asList(10, 20, 30, 40, 50);

        // ===== 在下面这行设断点（第 66 行附近） =====
        System.out.println("===== 场景1: 基础表达式 =====");   // <-- 断点停在此行

        /*
         * 断点暂停后，按 Alt+F8 打开 Evaluate 窗口，逐一输入以下表达式查看结果：
         *
         * 【练习 1.1】字符串操作
         *   name.toUpperCase()
         *   name.substring(0, 3)
         *   String.format("Hello, %s! your score is %d", name, score)
         *   name.isEmpty()
         *
         * 【练习 1.2】数字运算
         *   score + 15
         *   Math.max(score, 90)
         *   rate * 100 + "%"
         *   score > 80 ? "优秀" : "继续努力"
         *
         * 【练习 1.3】集合取值 + 类型转换
         *   numbers.get(2)
         *   numbers.stream().mapToInt(Integer::intValue).sum()
         *   numbers.contains(30)
         *   (double) score / 3          ← 注意带括号强转
         */
    }


    // ============================================================
    //  场景 2 —— 对象字段访问、调用 getter / isXxx 方法
    // ============================================================
    public static void demoObjectFieldAccess() {
        Order order = new Order(1001L, "张三", 2599.00, "PAID",
                Arrays.asList("iPhone 15", "充电器", "手机壳"));

        System.out.println("===== 场景2: 对象字段与方法 =====");   // <-- 在此行设断点

        /*
         * 【练习 2.1】直接访问字段
         *   order.id
         *   order.customer
         *   order.amount
         *   order.status
         *
         * 【练习 2.2】调用对象方法
         *   order.toString()
         *   order.isHighValue()
         *   order.items.size()
         *   order.items.get(1)
         *
         * 【练习 2.3】条件逻辑 —— 模拟接口中经常出现的判断
         *   order.isHighValue() && "PAID".equals(order.status)
         *   "PAID".equals(order.status) ? "已支付" : "未支付"
         *   order.amount >= 1000 && order.amount < 5000 ? "中等金额" : "其他"
         *
         * 【练习 2.4】在 Evaluate 中修改对象状态（重点）
         *   输入:   order.status = "CANCELLED"   然后回车
         *   输入:   order.status                  查看是否已改变
         *   输入:   order.amount = 9999
         *   观察下一行 println 输出内容的变化
         */
    }


    // ============================================================
    //  场景 3 —— 集合 & Stream 表达式（接口开发中最常用的调试手段）
    // ============================================================
    public static void demoCollectionStream() {
        List<Order> orders = Arrays.asList(
                new Order(1L, "张三", 500.00, "PENDING",  Arrays.asList("鼠标")),
                new Order(2L, "李四", 3200.00, "PAID",    Arrays.asList("键盘", "显示器")),
                new Order(3L, "王五", 150.00, "CANCELLED",Arrays.asList("数据线")),
                new Order(4L, "张三", 2800.00, "PAID",    Arrays.asList("笔记本", "鼠标垫")),
                new Order(5L, "赵六", 6000.00, "PENDING",  Arrays.asList("主机"))
        );

        System.out.println("===== 场景3: 集合与Stream =====");   // <-- 在此行设断点

        /*
         * ★★★ 场景3是接口开发最高频使用的场景 ★★★
         *
         * 【练习 3.1】Stream 过滤
         *   orders.stream().filter(o -> "PAID".equals(o.status)).collect(Collectors.toList())
         *   orders.stream().filter(Order::isHighValue).collect(Collectors.toList())
         *   orders.stream().filter(o -> o.amount > 500 && o.amount < 3000).collect(Collectors.toList())
         *
         * 【练习 3.2】Stream 映射（提取字段）
         *   orders.stream().map(o -> o.customer).collect(Collectors.toList())
         *   orders.stream().map(o -> o.customer).collect(Collectors.toSet())
         *   orders.stream().map(o -> o.items).collect(Collectors.toList())
         *   orders.stream().flatMap(o -> o.items.stream()).collect(Collectors.toList())
         *
         * 【练习 3.3】Stream 聚合
         *   orders.stream().mapToDouble(o -> o.amount).sum()
         *   orders.stream().mapToDouble(o -> o.amount).average()
         *   orders.stream().filter(o -> "PAID".equals(o.status)).count()
         *   orders.stream().collect(Collectors.groupingBy(o -> o.customer))
         *
         * 【练习 3.4】在 Evaluate 窗口中创建新集合
         *   Arrays.asList("新商品A", "新商品B")
         *   new ArrayList<>(Arrays.asList("临时项1", "临时项2"))
         *   Collections.singletonList("只有一个")
         */
    }


    // ============================================================
    //  场景 4 —— Optional 判空 + 链式调用（接口开发防 NPE 必备）
    // ============================================================
    public static void demoOptionalNullCheck() {
        Order maybeNull = null;
        Order normal = new Order(6L, "孙七", 888.00, "PENDING",
                Collections.singletonList("耳机"));

        // ===== mock 一个可能为空的查询结果 =====
        Order queryResult = Math.random() > 0.5 ? normal : null;

        System.out.println("===== 场景4: Optional 与空值 =====");   // <-- 在此行设断点

        /*
         * 【练习 4.1】Optional 包装与安全取值
         *   Optional.ofNullable(queryResult)
         *   Optional.ofNullable(queryResult).map(o -> o.customer)
         *   Optional.ofNullable(queryResult).map(o -> o.customer).orElse("默认用户")
         *   Optional.ofNullable(queryResult).map(o -> o.amount).orElse(0.0)
         *
         * 【练习 4.2】多层安全访问（ simulate 接口中 user -> order -> item 的场景 ）
         *   Optional.ofNullable(queryResult)
         *       .map(o -> o.items)
         *       .filter(list -> !list.isEmpty())
         *       .map(list -> list.get(0))
         *       .orElse("无商品")
         *
         * 【练习 4.3】直接操作可能为 null 的引用
         *   maybeNull == null
         *   maybeNull != null
         *   maybeNull instanceof Order
         *   注意：直接输入 maybeNull.customer 会抛 NullPointerException
         *            但在 Evaluate 中能看到精确的异常栈
         */
    }


    // ============================================================
    //  场景 5 —— API 响应包装类的调试（贴近实际开发）
    // ============================================================
    public static void demoApiResponseDebug() {
        Order orderData = new Order(7L, "周八", 4500.00, "PAID",
                Arrays.asList("MacBook Air", "电源适配器"));

        ApiResponse<Order> success = new ApiResponse<>(200, "OK", orderData);
        ApiResponse<Order> failure = new ApiResponse<>(500, "服务器繁忙", null);

        System.out.println("===== 场景5: API 响应调试 =====");   // <-- 在此行设断点

        /*
         * 【练习 5.1】检查响应状态
         *   success.isSuccess()
         *   failure.isSuccess()
         *   success.code == 200
         *
         * 【练习 5.2】安全提取嵌套数据
         *   Optional.ofNullable(success.data).map(d -> d.customer).orElse("未知")
         *   Optional.ofNullable(failure.data).map(d -> d.amount).orElse(0.0)
         *
         * 【练习 5.3】模拟下游处理逻辑
         *   success.data.amount > 1000 ? "大额订单" : "普通订单"
         *   failure.message.contains("繁忙") ? "重试" : "其他错误"
         *
         * 【练习 5.4】同时查看多个表达式的方法
         *   如果要对比 success 和 failure，可以一次看一个，也可以写复杂组合判断：
         *   success.isSuccess() && success.data.isHighValue() ? "成功-大额" : "需要关注"
         */
    }


    // ============================================================
    //  场景 6 —— 在 Evaluate 中执行代码块 + 创建临时变量（高阶技巧）
    // ============================================================
    public static void demoAdvancedEval() {
        List<String> rawData = Arrays.asList("a", "b", "c", "d", "e");

        System.out.println("===== 场景6: 高级技巧 =====");   // <-- 在此行设断点

        /*
         * 【练习 6.1】多语句执行（用 ; 分隔）
         *   rawData.add("f"); rawData.add("g"); rawData
         *   （这会永久修改 rawData，谨慎使用）
         *
         * 【练习 6.2】创建临时局部变量
         *   int temp = 100; String tempResult = "值=" + temp; tempResult
         *   （temp 只在 Evaluate 中存在，不影响实际代码）
         *
         * 【练习 6.3】用临时变量暂存复杂计算结果
         *   List<String> paidOrders = orders.stream()
         *       .filter(o -> "PAID".equals(o.status))
         *       .map(o -> o.customer)
         *       .collect(Collectors.toList());
         *   paidOrders.size() + " 位客户已支付"
         *
         *   注意：上面引用了 orders，所以需要在包含 orders 变量的上下文中求值
         *       （比如场景3的断点处）；在当前断点处 rawData 可用但 orders 不可用。
         *       可以切换断点位置再试。
         *
         * 【练习 6.4】模拟异常
         *   throw new RuntimeException("测试异常")
         *   （Evaluate 窗口会捕获这个异常并显示，不会让程序崩溃）
         *
         * 【练习 6.5】调用静态工具方法
         *   String.join(" -> ", rawData)
         *   Collections.reverse(Arrays.asList(1,2,3,4,5))
         */

        // 技巧：Evaluate 窗口可以留空按回车，重复上一次的表达式
    }


    // ============================================================
    //  场景 7 —— 真实 API 接口场景综合演练
    // ============================================================
    static class UserDto {
        Long userId;
        String loginName;
        String email;
        boolean deleted;

        UserDto(Long userId, String loginName, String email, boolean deleted) {
            this.userId = userId; this.loginName = loginName;
            this.email = email; this.deleted = deleted;
        }
    }

    static class ProductQuery {
        String keyword;
        int page;
        int size;
        String sortBy;
        boolean asc;

        ProductQuery(String keyword, int page, int size, String sortBy, boolean asc) {
            this.keyword = keyword; this.page = page; this.size = size;
            this.sortBy = sortBy; this.asc = asc;
        }
    }

    // ===== 模拟从数据库查询用户列表 =====
    static List<UserDto> queryUsers(String keyword) {
        // 模拟数据
        return Arrays.asList(
                new UserDto(1L, "alice", "alice@example.com", false),
                new UserDto(2L, "bob", "bob@test.com", true),
                new UserDto(3L, "charlie", "charlie@example.com", false),
                new UserDto(4L, "deleted_user", "old@test.com", true)
        );
    }

    // ===== 模拟业务：获取活跃用户邮箱列表 =====
    public static List<String> getActiveUserEmails(String keyword) {
        List<UserDto> all = queryUsers(keyword);

        // ---- 模拟业务逻辑 ----
        List<UserDto> active = new ArrayList<>();
        for (UserDto u : all) {
            if (!u.deleted && u.email != null) {
                active.add(u);
            }
        }

        List<String> emails = new ArrayList<>();
        for (UserDto u : active) {
            emails.add(u.email.toLowerCase());
        }

        System.out.println("===== 场景7: 接口业务调试 =====");   // <-- 在此行设断点

        // ★ 实际开发中，你可能想确认：
        //   1. 查询出了哪些用户
        //   2. 过滤掉了几个 deleted=true 的
        //   3. 最终邮箱列表是否正确
        // 用 Evaluate 而不是加 println

        return emails;
    }


    public static void main(String[] args) {
        // 按顺序执行各个场景
        demoBasicExpressions();
        demoObjectFieldAccess();
        demoCollectionStream();
        demoOptionalNullCheck();
        demoApiResponseDebug();
        demoAdvancedEval();
        getActiveUserEmails("test");

        System.out.println("\n全部执行完毕。按场景依次设断点练习就好。");
    }
}
