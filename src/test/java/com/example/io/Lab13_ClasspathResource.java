package com.example.io;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Lab13: Classpath 资源加载实战
 * -------------------------------------------------
 * 对应文档：第2.2节 资源加载：从 classpath 读取文件
 *
 * 学习目标：
 * 1. getClass().getClassLoader().getResourceAsStream() — 从 classpath 根加载
 * 2. getClass().getResourceAsStream() — 相对路径 vs 绝对路径
 * 3. classpath 资源不一定是磁盘文件（可能是 JAR 内部的条目）
 * 4. 从 JAR 提取 native 库到临时目录的模式
 * 5. 推荐使用不带 / 前缀的类加载器方式
 *
 * 运行说明：
 *   本 Lab 会在 target/test-classes/ 下创建测试资源文件，
 *   然后演示从 classpath 读取它。
 */
public class Lab13_ClasspathResource {

    private static final String LAB_DIR = "io-lab-temp/lab13";
    private static final String RESOURCE_NAME = "io-lab-config.properties";

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Paths.get(LAB_DIR));

        System.out.println("======= 准备工作：创建测试资源文件 =======");
        System.out.println("  classpath 上不一定有我们的资源文件，这里先创建一个\n");

        // 在项目的 src/test/resources/ 下创建测试资源
        Path resourceDir = Paths.get("src", "test", "resources");
        Files.createDirectories(resourceDir);
        Path resourceFile = resourceDir.resolve(RESOURCE_NAME);

        // 写入测试配置
        String configContent = "# I/O 学习配置\napp.name=DebugDemo\napp.version=1.0\ndb.url=jdbc:h2:mem:test\n";
        Files.writeString(resourceFile, configContent, StandardCharsets.UTF_8);
        System.out.println("  已创建: " + resourceFile.toAbsolutePath().normalize());

        /* ============================================================
           场景1: ClassLoader.getResourceAsStream() — 推荐方式
           ============================================================ */
        System.out.println("\n======= 场景1: 类加载器方式（推荐） =======");
        System.out.println("  ★ 路径不以 / 开头，相对于 classpath 根\n");

        try (InputStream in = Lab13_ClasspathResource.class
                .getClassLoader()
                .getResourceAsStream(RESOURCE_NAME)) {

            if (in == null) {
                System.out.println("  × 资源未找到！检查是否在 classpath 上");
                System.out.println("  ★ 在 IDE 中运行：资源在 target/test-classes/ 下");
                System.out.println("  请先编译项目，或者手动复制文件到 target/test-classes/");
            } else {
                String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                System.out.println("  √ 读取成功:");
                System.out.println("  " + content.replace("\n", "\\n\n  "));
            }
        }

        /* ============================================================
           场景2: Class.getResourceAsStream() — 注意 / 前缀
           ============================================================ */
        System.out.println("\n======= 场景2: Class.getResourceAsStream() =======");
        System.out.println("  ★ 带 / 前缀 → 从 classpath 根开始查找");
        System.out.println("  ★ 不带 / 前缀 → 相对当前类所在的包\n");

        // 方式A：带 / 前缀，从 classpath 根开始
        try (InputStream in = Lab13_ClasspathResource.class
                .getResourceAsStream("/" + RESOURCE_NAME)) {
            if (in != null) {
                System.out.println("  getResourceAsStream(\"/" + RESOURCE_NAME + "\"): √ 找到");
            } else {
                System.out.println("  getResourceAsStream(\"/" + RESOURCE_NAME + "\"): × 未找到");
            }
        }

        // 方式B：不带 / 前缀，相对包路径
        try (InputStream in = Lab13_ClasspathResource.class
                .getResourceAsStream(RESOURCE_NAME)) {
            if (in != null) {
                System.out.println("  getResourceAsStream(\"" + RESOURCE_NAME + "\"): √ 找到 (相对于当前包)");
            } else {
                System.out.println("  getResourceAsStream(\"" + RESOURCE_NAME + "\"): × 未找到");
            }
        }

        /* ============================================================
           场景3: classpath vs 文件系统的区别
           ============================================================ */
        System.out.println("\n======= 场景3: classpath vs 文件系统 =======");
        System.out.println("  关键区别:");
        System.out.println("  □ classpath 资源不一定是磁盘文件，可能是 JAR 包里的条目");
        System.out.println("  □ 错误写法：new File(\"classpath:config.properties\") → 路径非法");
        System.out.println("  □ 正确写法：getClassLoader().getResourceAsStream(\"config.properties\")");
        System.out.println("  □ 在 JAR 包中运行时，不能用 File 访问 classpath 资源");
        System.out.println("  □ 必须通过 InputStream 读取");

        System.out.println("\n======= 场景4: 从 JAR 提取 native 库的模式 =======");
        System.out.println("  ★ 对应文档中 LibraryLoader 的模式:\n");
        System.out.println("  InputStream in = getClass().getResourceAsStream(\"/lib/\" + libName);");
        System.out.println("  File tmpFile = File.createTempFile(\"native-\", \".lib\");");
        System.out.println("  tmpFile.deleteOnExit();");
        System.out.println("  try (FileOutputStream out = new FileOutputStream(tmpFile)) {");
        System.out.println("      byte[] buffer = new byte[4096];");
        System.out.println("      int len;");
        System.out.println("      while ((len = in.read(buffer)) != -1) {");
        System.out.println("          out.write(buffer, 0, len);");
        System.out.println("      }");
        System.out.println("  }");
        System.out.println("  System.load(tmpFile.getAbsolutePath());");
        System.out.println("  ★ 注意点：createTempFile 在 /main.java.com.example.tmp 创建，deleteOnExit 在 JVM 退出时删除");

        // 清理测试资源
        System.out.println("\n======= 清理 =======");
        Files.deleteIfExists(resourceFile);
        System.out.println("  resources 目录下的测试资源已删除");
        deleteDirectory(Paths.get(LAB_DIR));
        System.out.println("  临时目录已清理");

        System.out.println("\n✓ Lab13 完成！记住 classpath 资源必须通过 InputStream 读取，不能用 File 直接访问。");
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
