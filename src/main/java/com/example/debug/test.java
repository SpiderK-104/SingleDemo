package main.java.com.example.debug;

import java.util.Arrays;

public class test {
    public static void main(String[] args) {
        char[] arr = {'a', 'b', 'c', 'd'};

        String cp = Arrays.toString(Arrays.copyOfRange(arr, 0, 2));
        // ★ 注释写错了：Arrays.toString 输出的是 [a, b]，不是 "a b"
        //   想要 "ab" 应该用 new String(Arrays.copyOfRange(arr, 0, 2))
        //   这正是 EvaluateExpressionDemo 场景 9.2 要对比的点
        System.out.println(cp);

        System.out.println(new String(Arrays.copyOfRange(arr, 0, 2)));   // ab
        System.out.println(new String(arr, 1, 3));                        // bc

        // 断点放在这里，Alt+F8 观察：
        //   arr                       → IDEA 直接显示为 {a, b, c, d}
        //   Arrays.copyOfRange(arr, 0, 2)   → char[] 视图，可继续 getClass()
        //   new String(arr)          → "abcd"，这才是 char[] → String 的正解
        System.out.println("done");
    }
}
