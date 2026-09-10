package com.example.io;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Lab08: 序列化与反序列化实战 —— 对象↔文件
 * -------------------------------------------------
 * 对应文档：第1.7节 序列化与反序列化
 *
 * 学习目标：
 * 1. 理解序列化：Java 对象 → 字节序列 → 文件
 * 2. Java 原生序列化 ObjectOutputStream（了解缺点）
 * 3. JSON 序列化到文件（推荐方式，即使没有 Jackson 也用模拟方式演示）
 * 4. transient 关键字的作用
 * 5. serialVersionUID 的作用演示
 * 6. 为什么 JSON/Thrift 比 Java 原生序列化更安全
 *
 * 注意：本 Lab 为了演示 JSON 序列化，用了手动拼接 JSON 的方式。
 * 实际项目中会用 fastjson2 / Jackson / Gson。
 */
public class Lab08_Serialization {

    private static final String LAB_DIR = "io-lab-temp/lab08";

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Paths.get(LAB_DIR));

        System.out.println("======= 场景1: Java 原生序列化（ObjectOutputStream） =======");
        System.out.println("  ★ 了解即可，生产环境不推荐（安全问题）\n");

        Path serFile = Paths.get(LAB_DIR, "user.ser");

        // 序列化
        User user = new User("admin", "管理员", "secret123");
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream(serFile.toFile()))) {
            oos.writeObject(user);
        }
        System.out.println("  序列化完成，大小: " + Files.size(serFile) + " 字节");
        System.out.println("  ★ Java 原生序列化包含了类名、字段名等元信息，文件较大");

        // 反序列化
        try (ObjectInputStream ois = new ObjectInputStream(
                new FileInputStream(serFile.toFile()))) {
            User deserializedUser = (User) ois.readObject();
            System.out.println("  反序列化结果:");
            System.out.println("    username: " + deserializedUser.getUsername());
            System.out.println("    displayName: " + deserializedUser.getDisplayName());
            System.out.println("    password: " + deserializedUser.getPassword()
                    + " ← ★ transient 字段被跳过，返回 null（不会序列化密码！）");
        } catch (ClassNotFoundException e) {
            System.out.println("  反序列化失败: " + e.getMessage());
        }

        System.out.println("\n======= 场景2: JSON 序列化（推荐方案） =======");
        System.out.println("  文档：\"采用 JSON 序列化而非 Java 原生序列化\"");
        System.out.println("  原因：JSON 只处理纯数据，不执行任意代码，更安全\n");

        // ★ 模拟 JSON 序列化（实际项目用 fastjson2 / Jackson）
        List<User> userList = new ArrayList<>();
        userList.add(new User("admin", "管理员", "secret123"));
        userList.add(new User("zhangsan", "张三", "123456"));

        String jsonStr = toJsonArray(userList);
        Path jsonFile = Paths.get(LAB_DIR, "users.json");
        Files.writeString(jsonFile, jsonStr, StandardCharsets.UTF_8);

        System.out.println("  JSON 字符串:");
        System.out.println("    " + jsonStr);
        System.out.println("  JSON 文件大小: " + Files.size(jsonFile) + " 字节");
        System.out.println("  ★ JSON 比原生序列化更紧凑、可读、跨语言");

        // ★ 模拟 JSON 解析（反序列化）
        String jsonContent = Files.readString(jsonFile, StandardCharsets.UTF_8);
        System.out.println("\n  从文件读取 JSON 并解析:");
        System.out.println("    文件内容: " + jsonContent);
        System.out.println("    ★ 实际项目中用 JSON.parseArray(jsonStr, User.class)");

        System.out.println("\n======= 场景3: transient 关键字演示 =======");
        System.out.println("  transient 字段：不参与序列化。密码、密钥等敏感信息应标记为 transient。");
        System.out.println("  ★ 演示见上方：password 字段为 transient，反序列化后为 null");
        System.out.println("  另外 serialVersionUID 的作用:");
        System.out.println("    显式声明 private static final long serialVersionUID = 1L;");
        System.out.println("    避免类增加字段后反序列化旧数据报 InvalidClassException");

        System.out.println("\n======= 场景4: 为什么不用 Java 原生序列化 =======");
        System.out.println("  1. 安全问题：反序列化时自动执行对象的 readObject 方法");
        System.out.println("     攻击者构造恶意字节流 → RCE（远程代码执行）");
        System.out.println("  2. 性能问题：Java 序列化/反序列化速度慢");
        System.out.println("  3. 跨语言问题：Java 原生序列化只能被 Java 读取");
        System.out.println("  4. 文件大：包含大量类元信息");
        System.out.println("  ★ JSON 和 Thrift 序列化只处理纯数据，不执行任意代码");

        System.out.println("\n======= 清理 =======");
        deleteDirectory(Paths.get(LAB_DIR));
        System.out.println("  已清理");

        System.out.println("\n✓ Lab08 完成！记住：生产环境用 JSON 序列化代替 Java 原生序列化。");
    }

    /** 手动拼接 JSON 数组（演示用，无 JSON 依赖） */
    private static String toJsonArray(List<User> users) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < users.size(); i++) {
            User u = users.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"username\":\"").append(u.getUsername())
              .append("\",\"displayName\":\"").append(u.getDisplayName())
              .append("\"}");  // 不包含 password（transient）
        }
        sb.append("]");
        return sb.toString();
    }

    /** 用户类：演示序列化 */
    static class User implements Serializable {
        private static final long serialVersionUID = 1L;  // ★ 显式声明

        private String username;
        private String displayName;

        /** ★ transient 字段不会被序列化 */
        private transient String password;

        public User(String username, String displayName, String password) {
            this.username = username;
            this.displayName = displayName;
            this.password = password;
        }

        public String getUsername() { return username; }
        public String getDisplayName() { return displayName; }
        public String getPassword() { return password; }
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (java.util.stream.Stream<Path> walk = Files.walk(dir)) {
                walk.sorted((a, b) -> b.toString().length() - a.toString().length())
                    .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException e) {} });
            }
        }
    }
}
