package main.java.com.example.debug;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 步进执行实操类（Step Over / Into / Out / Smart Step Into / Run to Cursor / Drop Frame）
 * <p>
 * 断点只是让你"停下来"，步进才是让你"看清执行过程"的核心能力。
 * 核心键位：
 *   F7        Step Into        进入当前行调用的方法
 *   F8        Step Over        执行当前行但不进入方法
 *   Shift+F8  Step Out         从当前方法跳出
 *   Shift+F7  Smart Step Into  一行有多个调用时，选择进哪一个
 *   Alt+F9    Run to Cursor    直接跑到光标所在行
 *   Alt+F8    Evaluate Expression
 *   F9        Resume           继续跑到下一个断点
 */
public class SteppingDemo {

    // ============================================================
    //  场景 1 —— Step Over / Step Into / Step Out 的基本分工
    // ============================================================
    public static void scenario1BasicStepping() {
        int base = 10;
        int result = level1(base);   // <-- 断点放这里，然后开始按 F8 / F7
        System.out.println("result = " + result);
    }

    static int level1(int n) {
        int a = n + 1;              // F8：执行这一行，不进任何方法
        int b = level2(a);          // F7：进入 level2；F8：跳过
        return a + b;               // Shift+F8：从 level2 里跳出到这一行
    }

    static int level2(int n) {
        int c = n * 3;
        return level3(c);
    }

    static int level3(int n) {
        return n - 1;
    }

    /*
     * 【练习 1.1】三种步进的标准动作
     *   在 scenario1BasicStepping 的 `int result = level1(base);` 设断点，Debug 启动：
     *   1) F8 一次  → 停在 System.out.println 那行（level1 整体被"跑掉"了）
     *   2) 重新来，F7 一次 → 进入 level1，停在 int a = n + 1;
     *   3) F8 两次   → a=11、b=level2(11)，当前行停在 `return a + b;`
     *   4) 再来一次，在 int b = level2(a); 按 F7 → 进入 level2
     *   5) 在 level2 里按 Shift+F8 → 弹回 level1
     *
     * 【练习 1.2】练习 1.1 的坑
     *   level1(a) 已经执行完后再按 F8，b 的值不会重新计算 —— 步进是真实执行，
     *   不是"回放"。想重新算就必须回到那行（F9 回到断点，或用 Drop Frame）。
     *
     * 【练习 1.3】F8 会跳过 getter / 简单 setter
     *   调试时 F8 一路按到底，方法调用的黑盒部分永远不会展开。
     */


    // ============================================================
    //  场景 2 —— Smart Step Into：一行多个调用时选进哪个
    // ============================================================
    public static void scenario2SmartStepInto() {
        int value = combine(combine(1, 2), combine(3, 4));   // <-- 这一行有 3 个调用
        System.out.println("combine = " + value);
    }

    static int combine(int a, int b) {
        return a * 10 + b;
    }

    /*
     * 【练习 2.1】在场景 2 那一行按 Shift+F7
     *   IDEA 会弹出候选列表：combine(a,b) / combine(a,b) / combine(a,b)
     *   每一项带 ★ 的是本行执行次数最多的，★ 后面的数字告诉你这行调用了几次。
     *   选第 2 个 combine → 直接进到外层 combine 的第一行。
     *
     *   ★ 这比"F8 进去第一个、F9 出去、再 F8"快得多，
     *     是分析嵌套表达式最常用的操作。
     *
     * 【练习 2.2】进方法体后
     *   IDEA 会问你要不要"一直步入"某个循环/方法，用弹窗里的 Yes / No 回答即可。
     */


    // ============================================================
    //  场景 3 —— 多态：F7 跟着实际类型走
    // ============================================================
    interface Shape {
        double area();
    }

    static class Circle implements Shape {
        final double r;

        Circle(double r) {
            this.r = r;
        }

        @Override
        public double area() {
            return Math.PI * r * r;
        }
    }

    static class Rect implements Shape {
        final double w;
        final double h;

        Rect(double w, double h) {
            this.w = w;
            this.h = h;
        }

        @Override
        public double area() {
            return w * h;
        }
    }

    public static void scenario3Polymorphism() {
        List<Shape> shapes = Arrays.asList(new Circle(2), new Rect(3, 4), new Circle(1));
        double total = 0;

        for (Shape s : shapes) {
            total += s.area();   // <-- 断点放这里，按 F8 跳过；F7 进入实际实现
        }

        System.out.println("total area = " + total);
    }

    /*
     * 【练习 3.1】在 `total += s.area();` 设断点，F8 跳到下一行；
     *   改按 F7，会进入 Circle.area 或 Rect.area —— 跟着运行期真实类型走。
     *
     * 【练习 3.2】关键：在接口 Shape.area() 上打断点
     *   三个 shape 全都会停。停下来后 Alt+F8 输入 `this.getClass().getName()`
     *   或看 Variables 面板，会发现 this 有时是 Circle 有时是 Rect。
     *
     * 【练习 3.3】VS Debug 插件的"重载方法断点"
     *   如果装了 Visual Studio Debugger 之类的插件，可以按重载签名分别下断点，
     *   只进 Circle.area 而不进 Rect.area。原生 IDEA 没有这个功能，
     *   用方法断点 + 条件 `this instanceof Circle` 替代。
     *
     * ★ 实战：接口里埋一行断点，是"实现类太多、不知道哪个被调"的万能解法。
     */


    // ============================================================
    //  场景 4 —— 继承与构造顺序
    // ============================================================
    static class BaseEntity {
        String id;

        BaseEntity(String id) {
            this.id = id;                    // <-- 在父类构造里断点
            System.out.println("BaseEntity 构造: " + id);
        }

        void describe() {
            System.out.println("BaseEntity.describe, id=" + id);
        }
    }

    static class UserEntity extends BaseEntity {
        String name;

        UserEntity(String id, String name) {
            super(id);                        // <-- 必须先调 super，断点观察执行顺序
            this.name = name;
            System.out.println("UserEntity 构造: " + name);
        }

        @Override
        void describe() {
            super.describe();                 // <-- F7 看它跳到父类实现
            System.out.println("UserEntity.describe, name=" + name);
        }
    }

    public static void scenario4Inheritance() {
        UserEntity u = new UserEntity("U001", "张三");
        u.describe();
    }

    /*
     * 【练习 4.1】在 BaseEntity 构造里打断点
     *   new UserEntity(...) 一执行就停在父类构造 —— 证明父类先构造。
     *   ★ 常见结论："字段初始化的实际顺序"，不是源码书写顺序。
     *
     * 【练习 4.2】在 super.describe() 按 F7
     *   会进入 BaseEntity.describe()，此时 this 是 UserEntity
     *   Alt+F8 输入 `this.getClass().getSimpleName()` 验证。
     *
     * 【练习 4.3】在 UserEntity.describe() 按 F8 会跳过 super.describe()
     *   直接执行下一行。想看父类逻辑必须 F7 —— 这是 F8 最容易踩的坑。
     */


    // ============================================================
    //  场景 5 —— Lambda 与 Stream：断点调试的高频战场
    // ============================================================
    public static void scenario5Lambda() {
        List<String> names = Arrays.asList("alice", "bob", "charlie", "dave");

        // 断点打在 lambda 体内部
        names.forEach(n -> System.out.println("lambda: " + n));   // <-- 实验断点落点
    }

    public static void scenario6Stream() {
        List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5, 6);

        List<Integer> result = nums.stream()
                .filter(n -> n % 2 == 0)                    // <-- 断点放这里
                .map(n -> n * 10)                           // <-- 断点放这里
                .collect(Collectors.toList());

        System.out.println("stream result = " + result);
    }

    /*
     * 【练习 5.1】Lambda 断点落在哪
     *   在 `names.forEach(...)` 那一行打断点，F8 一次。
     *   发现停在 forEach 调用行，F7 一次才进 lambda 体。
     *   ★ 原因：lambda 体被编译成了独立方法（名字形如 lambda$scenario5Lambda$0），
     *     所以它是"一个方法调用"，有自己的栈帧。
     *
     * 【练习 5.2】在 lambda 体内 Alt+F8
     *   `n` 可用；外层变量 names 也可见（捕获的变量）。
     *   外层方法的局部变量若不是 effectively final，编译期就被拒了 ——
     *     这正是断点调试时最想知道的"为什么编译不过"。
     *
     * 【练习 6.1】Stream 的执行顺序（重点，容易想错）
     *   Stream 是惰性的，断点停下时你看到的是"管道定义"，不是执行过程。
     *   ★ 想看真正的逐元素执行，把断点打在 filter/map 的 lambda 体内，
     *     然后 Alt+F9（Run to Cursor）不会跨 lambda，得 F7 逐层进。
     *
     * 【练习 6.2】最有效的手段：Smart Step Into + 变量视图
     *   在 `.collect(...)` 行按 Shift+F7，选 collect，
     *     会进入 JDK 的 Collectors.toList()，在 Stream 源码里看整个管道。
     *     JDK 源码要能进去：Project Structure → SDK → Add Source → 选 src.zip。
     *
     * 【练习 6.3】并行 stream 的坑
     *   nums.parallelStream() 时，断点会在多个线程上分别命中，
     *     同一个 n 会被多个线程处理。要看清顺序就别用 parallel。
     *
     * ★ 真实项目里更实用的等价手法：用 for 循环 + 断点复现，
     *   或者把 stream 拆成中间变量一行行看（stream = nums.stream()），
     *   这样每个阶段的结果都能在 Variables 面板里直接比对。
     */


    // ============================================================
    //  场景 7 —— Alt+F9 Run to Cursor：跳过不想看的代码
    // ============================================================
    public static void scenario7RunToCursor() {
        for (int i = 1; i <= 100; i++) {
            System.out.println("噪声日志: 第 " + i + " 次");   // 这里按 F8 要按 100 次
        }

        int target = -1;   // <-- 把光标放这行，然后 Alt+F9
        for (int i = 1; i <= 100; i++) {
            if (i * i > 3000) {
                target = i;
                break;
            }
        }

        System.out.println("target = " + target);
    }

    /*
     * 【练习 7.1】在第一个循环内打断点，光标移到 `int target = -1;`，按 Alt+F9
     *   一口气跳过 100 次 println 直接到位。F9 只跳到"下一个断点"，
     *   没有断点时会跑到底；Alt+F9 是精确到"光标行"。
     *
     * ★ Run to Cursor 会真的执行中间所有代码，所以副作用（写库、发请求）照常发生。
     */


    // ============================================================
    //  场景 8 —— Drop Frame：把栈帧"倒回去"重跑
    // ============================================================
    public static void scenario8DropFrame() {
        long result = computeFactorial(5);   // <-- 断点在这里，然后 Alt+R → Drop Frame
        System.out.println("5! = " + result);
    }

    static long computeFactorial(int n) {
        long result = 1;
        for (int i = 2; i <= n; i++) {
            result *= i;                   // <-- 观察 n、i、result 逐轮变化
        }
        return result;
    }

    /*
     * 【练习 8.1】在 computeFactorial(n) 调用处断点，Alt+R → Drop Frame
     *   栈帧回到 computeFactorial 方法入口，result = 1 被重置，
     *   然后你可以重新 F8 一遍，逐轮看 result 的累乘过程。
     *
     * 【练习 8.2】适合什么场景
     *   - 中间状态算错了，想知道是哪一步开始错的
     *   - 循环跑了 1000 次只想看第 3 次：Drop Frame 后改循环变量再跑
     *   - 想重复进入同一个方法（Watch 会在方法入口求值，Drop Frame 可触发重入）
     *
     * ★ 限制：不能往栈帧更上层跳；Drop Frame 会丢弃该帧之后的局部变量修改，
     *   也不适合在有 try-finally 的方法里做（finally 状态可能不一致）。
     */


    // ============================================================
    //  场景 9 —— 递归：栈帧就是递归深度
    // ============================================================
    static int fib(int n) {
        if (n <= 1) {
            return n;                       // 递归出口
        }
        return fib(n - 1) + fib(n - 2);     // <-- 断点放这里，每调用一次停一次
    }

    public static void scenario9Recursion() {
        int result = fib(8);
        System.out.println("fib(8) = " + result);
    }

    /*
     * 【练习 9.1】在递归调用行断点，按 F9 连续执行
     *   Frames 面板会越来越深（fib(8) → fib(7) → fib(6) ...），
     *   点任意一帧可以"跳帧"查看那层的 n，直接看清递归的展开与收敛。
     *
     * 【练习 9.2】JIT 优化后的现象
     *   Debug 模式下不会做激进优化（禁用 Tiered 默认行为差异），
     *   但热点方法仍可能出现"变量显示为已优化不可读"（Value is optimized out）。
     *   解决：Settings → Build, Execution, Deployment → Debugger → Advanced →
     *   勾 "Disable instrumenting agent" 旁边相关选项，或用 Alt+F8 读字段。
     *
     * 【练习 9.3】栈溢出的观察
     *   把 n 改成 -1（n <= 1 永不成立）会 StackOverflowError，
     *   配合 ExceptionDebugDemo 的异常断点一起看。
     */


    public static void main(String[] args) {
        System.out.println("===== 场景1: 三种步进 =====");
        scenario1BasicStepping();

        System.out.println("\n===== 场景2: Smart Step Into =====");
        scenario2SmartStepInto();

        System.out.println("\n===== 场景3: 多态 =====");
        scenario3Polymorphism();

        System.out.println("\n===== 场景4: 继承与构造顺序 =====");
        scenario4Inheritance();

        System.out.println("\n===== 场景5: Lambda =====");
        scenario5Lambda();

        System.out.println("\n===== 场景6: Stream =====");
        scenario6Stream();

        System.out.println("\n===== 场景7: Run to Cursor =====");
        scenario7RunToCursor();

        System.out.println("\n===== 场景8: Drop Frame =====");
        scenario8DropFrame();

        System.out.println("\n===== 场景9: 递归 =====");
        scenario9Recursion();

        System.out.println("\n全部执行完毕。每个场景的注释里写了按键步骤。");
    }
}