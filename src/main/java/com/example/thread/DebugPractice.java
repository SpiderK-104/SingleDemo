package main.java.com.example.thread;

import java.util.*;

public class DebugPractice {

    // ===== 场景1：循环 + 方法调用（练 F8/F7/Watches/条件断点）=====
    public static int multiply(int x) {
        return x * 2;   // 👉 Step Into 会进来这里
    }

    // ===== 场景2：对象判空 & 业务分支（练 Evaluate / 修改变量）=====
    static class User {
        String name;
        boolean active;
        User(String n, boolean a) { this.name = n; this.active = a; }
    }

    public static String greetUser(User u) {
        if (u == null || !u.active) {
            return "INACTIVE";  // 👉 调试时可 Alt+F8 改 u.active=true
        }
        return "Hello, " + u.name;
    }

    // ===== 场景3：会抛异常的代码（练异常断点 NullPointerException）=====
    public static void riskyMethod(List<String> list) {
        System.out.println(list.get(3)); // 👉 越界或 NPE
    }

    public static void main(String[] args) {
        /* ---- ① 循环 + 方法调用 ---- */
        int sum = 0;
        for (int i = 1; i <= 5; i++) {
            sum += multiply(i);   // 👉 在此行设断点，右键 → Condition: i == 3（条件断点）
        }
        System.out.println("sum = " + sum);

        /* ---- ② 对象分支逻辑 ---- */
        User user = new User("Tom", false);
        String result = greetUser(user);
        System.out.println(result);

        /* ---- ③ 异常场景 ---- */
        riskyMethod(new ArrayList<>(Arrays.asList("a", "b")));
    }
}