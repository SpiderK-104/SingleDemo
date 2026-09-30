package main.java.com.example.debug;

/**
 * Debug 实验室入口
 * <p>
 * 用法：
 *   直接 Debug 运行本类（Shift+F9），会依次跑完全部 6 个模块。
 *   每个模块里都有断点练习，用 Shift+F9 单独 Debug 某一个类更方便。
 * <p>
 * ---------------------------------------------------------------
 *  IDEA Debug 快捷键速查（Windows / Linux，Mac 把 Alt 换成 Option）
 * ---------------------------------------------------------------
 *  Shift+F9    Debug 启动（Run 是 Shift+F10，跑 Debug 断点才生效）
 *  F9          Resume：跑到下一个断点
 *  F7          Step Into：进入当前行的方法
 *  F8          Step Over：执行当前行但不进方法
 *  Shift+F8    Step Out：从当前方法跳出
 *  Shift+F7    Smart Step Into：一行多个调用时选择进哪个
 *  Alt+F9      Run to Cursor：直接跑到光标所在行
 *  Alt+F8      Evaluate Expression：临时求值任意表达式
 *  Ctrl+F8     Toggle Line Breakpoint：行断点开关
 *  Ctrl+Shift+F8   View Breakpoints（也可 Alt+R 菜单进入）
 *  Alt+S       Settings
 *
 *  Alt+R 菜单里还有这些高频项：
 *    Temporary Breakpoint        临时断点，命中一次自动消失
 *    Drop Frame                  把当前栈帧退回方法开头重跑
 *    Get Thread Dump             抓所有线程栈（死锁检测）
 *    Force Return                强制从当前方法返回指定值
 *
 * ---------------------------------------------------------------
 *  6 个模块的分工
 * ---------------------------------------------------------------
 *  1. EvaluateExpressionDemo   断点 + Alt+F8 求值（原有用例，已修正）
 *  2. BreakpointTypesDemo      7 种断点类型 + 断点不生效排查
 *  3. SteppingDemo             F7/F8/Shift+F8、Smart Step Into、Drop Frame、递归
 *  4. ExceptionDebugDemo       异常断点、栈展开顺序、finally 陷阱
 *  5. ThreadDebugDemo          线程身份、挂起策略、死锁 Thread Dump、中断
 *  6. WatchAndVariablesDemo    toString、Watches、集合 Map、静态字段
 */
public class DebugLab {

    public static void main(String[] args) {
        printBanner();
        printShortcuts();

        System.out.println("\n########## 1. EvaluateExpressionDemo ##########");
        EvaluateExpressionDemo.main(new String[0]);

        System.out.println("\n########## 2. BreakpointTypesDemo ##########");
        BreakpointTypesDemo.main(new String[0]);

        System.out.println("\n########## 3. SteppingDemo ##########");
        SteppingDemo.main(new String[0]);

        System.out.println("\n########## 4. ExceptionDebugDemo ##########");
        ExceptionDebugDemo.main(new String[0]);

        System.out.println("\n########## 5. ThreadDebugDemo ##########");
        ThreadDebugDemo.main(new String[0]);

        System.out.println("\n########## 6. WatchAndVariablesDemo ##########");
        WatchAndVariablesDemo.main(new String[0]);

        System.out.println("\n全部模块执行完毕。");
    }

    private static void printBanner() {
        System.out.println("=======================================================");
        System.out.println("  Debug 实验室 —— 6 个模块，按需单独 Debug 某个类");
        System.out.println("=======================================================");
    }

    private static void printShortcuts() {
        System.out.println("\n---- Debug 快捷键 ----");
        System.out.println("  Shift+F9   Debug 启动       F9  Resume");
        System.out.println("  F7 Step Into                 F8  Step Over");
        System.out.println("  Shift+F8 Step Out            Shift+F7 Smart Step Into");
        System.out.println("  Alt+F9 Run to Cursor          Alt+F8 Evaluate Expression");
        System.out.println("  Ctrl+F8 Toggle Breakpoint    Alt+R Run 菜单");
        System.out.println("-------------------------------");
    }
}