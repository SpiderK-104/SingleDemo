package com.example.io;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Lab12: 原子写入实战 —— 先写临时文件再 rename
 * -------------------------------------------------
 * 对应文档：第2.1.8节 原子写入
 *
 * 学习目标：
 * 1. 理解"临时文件 + rename"模式的必要性
 * 2. 演示：直接写入 vs 原子写入（模拟崩溃场景）
 * 3. renameTo 在同一个文件系统内是原子操作
 * 4. Files.move 替代 File.renameTo
 * 5. 该模式在 DnsConfig.java 中的应用
 */
public class Lab12_AtomicWrite {

    private static final String LAB_DIR = "io-lab-temp/lab12";

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Paths.get(LAB_DIR));

        System.out.println("======= 问题场景 =======");
        System.out.println("  直接写入文件时程序崩溃 → 文件内容只写了一半 → 数据损坏");
        System.out.println("  原子写入（main.java.com.example.tmp + rename）: 要么全写成功，要么保持旧文件\n");

        /* ============================================================
           场景1: 直接写入的问题（模拟）
           ============================================================ */
        System.out.println("======= 场景1: 直接写入的脆弱性（模拟） =======");

        Path directFile = Paths.get(LAB_DIR, "config.properties");

        // 假设这是旧配置
        Files.writeString(directFile, "db.url=old-url\ndb.user=admin\n", StandardCharsets.UTF_8);
        System.out.println("  旧配置写入完成");

        // ★ 模拟直接写入时程序崩溃（写入了一半）
        // 实际中可能是写 100 行时写到第 50 行断电了
        try (OutputStream out = Files.newOutputStream(directFile)) {
            out.write("db.url=new-url\n".getBytes(StandardCharsets.UTF_8));
            out.write("db.user=new-admin\n".getBytes(StandardCharsets.UTF_8));
            out.write("db.password=".getBytes(StandardCharsets.UTF_8));
            // ★ 假设这里程序崩溃！配置文件只有前三行
            // out.write("secret\n".getBytes());
        }
        System.out.println("  直接写入（模拟崩溃后）:");
        System.out.println("  " + Files.readString(directFile, StandardCharsets.UTF_8).replace("\n", "\\n\n  "));
        System.out.println("  ↑ 配置不完整！密码字段缺失，应用可能启动失败\n");

        /* ============================================================
           场景2: 原子写入（临时文件 + rename）
           ============================================================ */
        System.out.println("======= 场景2: 原子写入（推荐模式） =======");

        Path targetFile = Paths.get(LAB_DIR, "important.conf");
        Files.writeString(targetFile, "旧配置内容", StandardCharsets.UTF_8);
        System.out.println("  旧配置已存在");

        // ★ 原子写入的四步曲：
        // 1. 写临时文件（同一目录／同一文件系统）
        // 2. flush + close
        // 3. 删除目标文件
        // 4. rename 临时文件为目标文件

        Path tmpFile = Paths.get(LAB_DIR, "important.conf.main.java.com.example.tmp");

        // 第一步：写入临时文件
        try (BufferedWriter writer = Files.newBufferedWriter(tmpFile, StandardCharsets.UTF_8)) {
            writer.write("db.url=new-url\n");
            writer.write("db.user=new-admin\n");
            writer.write("db.password=secret123\n");
            writer.write("db.pool=10\n");
            // ★ 如果这里崩溃，只影响 .main.java.com.example.tmp 文件，important.conf 还是完整的旧配置
        }  // 自动 flush + close

        // 第二步：原子替换
        // ★ File.renameTo 在同一个文件系统内是原子操作
        // ★ 要么目标文件保持旧内容，要么被新内容完整替换，不会出现半写状态
        Files.move(tmpFile, targetFile, StandardCopyOption.REPLACE_EXISTING);

        String finalContent = Files.readString(targetFile, StandardCharsets.UTF_8);
        System.out.println("  原子写入后的配置:");
        System.out.println("  " + finalContent.replace("\n", "\\n\n  "));
        System.out.println("  ↑ 配置完整！要么全成功（如上），要么保持旧配置\n");

        /* ============================================================
           场景3: Files.move 与 File.renameTo 的对比
           ============================================================ */
        System.out.println("======= 场景3: move vs renameTo =======");

        Path src = Paths.get(LAB_DIR, "temp.txt");
        Path dst = Paths.get(LAB_DIR, "final.txt");
        Files.writeString(src, "测试内容", StandardCharsets.UTF_8);

        // ★ Files.move: 支持 StandardCopyOption，跨文件系统时自动 copy+delete
        Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("  Files.move: 成功! (支持 REPLACE_EXISTING, ATOMIC_MOVE 选项)");
        System.out.println("  源文件存在? " + Files.exists(src));
        System.out.println("  目标文件存在? " + Files.exists(dst));

        // 对比 File.renameTo
        File oldWay = new File(LAB_DIR, "old-way.txt");
        File newWay = new File(LAB_DIR, "new-way.txt");
        oldWay.createNewFile();
        boolean renamed = oldWay.renameTo(newWay);
        System.out.println("\n  File.renameTo: " + renamed + " (返回 boolean，不抛异常)");
        System.out.println("  ★ renameTo 在 Windows 上有一些限制（目标存在时可能失败）");

        System.out.println("\n======= 场景4: DnsConfig.java 的模式复现 =======");
        System.out.println("  你的 DnsConfig.java 第 47~70 行:");
        System.out.println("    1. File tmpFile = new File(\"/etc/resolv.conf.main.java.com.example.tmp\")");
        System.out.println("    2. try (DataOutputStream dout = ...) → 写入临时文件");
        System.out.println("    3. dst.delete()");
        System.out.println("    4. tmpFile.renameTo(dst)");
        System.out.println("  ★ 这个模式确保 DNS 配置写入要么成功要么不破坏旧配置\n");

        System.out.println("======= 关键要点 =======");
        System.out.println("  □ 临时文件和目标文件必须在同一文件系统（同一分区）");
        System.out.println("  □ 临时文件通常加 .main.java.com.example.tmp 后缀，同一目录");
        System.out.println("  □ 写入后必须先 close 再 rename（确保数据落盘）");
        System.out.println("  □ 若写入过程崩溃，.main.java.com.example.tmp 文件可能残留，需定期清理");

        System.out.println("\n======= 清理 =======");
        deleteDirectory(Paths.get(LAB_DIR));
        System.out.println("  已清理");

        System.out.println("\n✓ Lab12 完成！原子写入是保护配置文件、重要数据的关键模式。");
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
