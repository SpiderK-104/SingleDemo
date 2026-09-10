package com.example.io;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Lab06: try-with-resources 实战 —— 资源管理的最佳实践
 * -------------------------------------------------
 * 对应文档：第1.6节 try-with-resources 与资源管理
 *
 * 学习目标：
 * 1. 对比旧式 try-catch-finally 与 try-with-resources
 * 2. 多个资源的声明和关闭顺序
 * 3. 抑制异常（Suppressed Exception）机制
 * 4. 自定义 AutoCloseable 资源
 * 5. try-with-resources + catch + finally 的组合
 */
public class Lab06_TryWithResources {

    private static final String LAB_DIR = "io-lab-temp/lab06";

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Paths.get(LAB_DIR));

        System.out.println("======= 场景1: 旧式资源释放的痛点 =======");

        // Java 7 之前的写法 —— 冗长、容易遗漏
        Path file1 = Paths.get(LAB_DIR, "old-style.txt");
        BufferedWriter oldWriter = null;
        try {
            oldWriter = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(file1.toFile()), StandardCharsets.UTF_8));
            oldWriter.write("旧式写法");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // ★ 要判空、要嵌套 try-catch —— 非常繁琐
            if (oldWriter != null) {
                try {
                    oldWriter.close();
                } catch (IOException e) {
                    // 通常只能忽略
                }
            }
        }
        System.out.println("  旧式写法完成（代码冗长，容易忘记 close）");

        System.out.println("\n======= 场景2: try-with-resources 单资源 =======");

        Path file2 = Paths.get(LAB_DIR, "try-with-resources.txt");
        try (BufferedWriter writer = Files.newBufferedWriter(file2, StandardCharsets.UTF_8)) {
            writer.write("try-with-resources 写法");
            // ★ 不需要 finally 来 close，编译器自动插入
        }  // 自动调用 writer.close()
        System.out.println("  try-with-resources 完成（简洁且安全）");

        System.out.println("\n======= 场景3: 多资源管理 =======");

        Path src = Paths.get(LAB_DIR, "source.txt");
        Path dst = Paths.get(LAB_DIR, "target.txt");

        // ★ 多个资源用分号分隔
        // ★ 关闭顺序与声明顺序相反：先关 is, 再关 os
        try (InputStream is = Files.newInputStream(src);
             OutputStream os = Files.newOutputStream(dst)) {
            // 创建 source 文件（用于演示）
            Files.writeString(src, "多资源演示", StandardCharsets.UTF_8);

            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
        }
        System.out.println("  多资源复制完成，自动关闭 InputStream 和 OutputStream");
        System.out.println("  目标文件内容: " + Files.readString(dst, StandardCharsets.UTF_8));

        System.out.println("\n======= 场景4: 抑制异常机制 =======");
        System.out.println("  (此场景演示 try 块抛异常时，close 的异常被抑制)");

        try (ResourceBad res = new ResourceBad()) {
            res.work();   // 这个会抛异常
        } catch (Exception e) {
            System.out.println("  捕获到异常: " + e.getMessage());
            System.out.println("  抑制异常数: " + e.getSuppressed().length);
            if (e.getSuppressed().length > 0) {
                System.out.println("  抑制异常: " + e.getSuppressed()[0].getMessage());
            }
            System.out.println("  ★ 如果不用 try-with-resources，close 抛的异常会覆盖 work 抛的异常");
        }

        System.out.println("\n======= 场景5: try-with-resources + catch + finally =======");

        Path missingFile = Paths.get(LAB_DIR, "不存在的目录/文件.txt");
        try (BufferedWriter w = Files.newBufferedWriter(missingFile, StandardCharsets.UTF_8)) {
            w.write("这行不会执行");
        } catch (IOException e) {
            // ★ catch 在资源关闭之后执行
            System.out.println("  catch 块: 目录不存在 → " + e.getClass().getSimpleName());
        } finally {
            // ★ finally 在 catch 之后执行
            System.out.println("  finally 块: 无论是否异常都会执行（清理其他资源）");
        }

        System.out.println("\n======= 场景6: 不需要 close 的资源不需要 try-with-resources =======");

        Path existFile = Paths.get(LAB_DIR, "check.txt");
        Files.writeString(existFile, "测试内容", StandardCharsets.UTF_8);
        // ★ Files.deleteIfExists 不涉及需要 close 的资源（Path 不是 Closeable）
        boolean deleted = Files.deleteIfExists(existFile);
        System.out.println("  Files.deleteIfExists 无需 try-with-resources，返回: " + deleted);

        System.out.println("\n======= 关键总结 =======");
        System.out.println("  ✔ 只要打开了 InputStream/OutputStream/Reader/Writer，就用 try-with-resources");
        System.out.println("  ✔ 多个资源用分号隔开，关闭顺序逆序");
        System.out.println("  ✔ 可以同时有 catch 和 finally（在资源关闭之后执行）");
        System.out.println("  ✔ 不需要 close 的资源（如 Path）无需 try-with-resources");

        System.out.println("\n======= 清理 =======");
        deleteDirectory(Paths.get(LAB_DIR));
        System.out.println("  已清理");

        System.out.println("\n✓ Lab06 完成！try-with-resources 是管理 I/O 资源的标准方式。");
    }

    /** 自定义资源：演示抑制异常机制 */
    static class ResourceBad implements AutoCloseable {
        void work() {
            throw new RuntimeException("work() 抛出的异常");
        }

        @Override
        public void close() {
            throw new RuntimeException("close() 抛出的异常");
        }
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
