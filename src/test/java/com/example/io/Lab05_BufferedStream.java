package com.example.io;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Lab05: 缓冲流实战 —— 为什么需要缓冲区
 * -------------------------------------------------
 * 对应文档：第1.5节 缓冲流
 *
 * 学习目标：
 * 1. 理解缓冲的原理：减少系统调用次数
 * 2. 对比缓冲与非缓冲的性能差异（通过计时感受）
 * 3. 了解缓冲区大小选择（8KB 默认值）
 * 4. 理解 flush 的必要性
 * 5. 演示 BufferedReader.readLine() 的便捷性
 */
public class Lab05_BufferedStream {

    private static final String LAB_DIR = "io-lab-temp/lab05";

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Paths.get(LAB_DIR));

        System.out.println("======= 核心原理 =======");
        System.out.println("每次 read()/write() → 一次系统调用 → 磁盘 IO（毫秒级延迟）");
        System.out.println("缓冲流：内部 8KB 数组，攒满才真正写磁盘，系统调用次数从 N 降到 N/8192\n");

        /* ============================================================
           场景1: 性能对比 —— 非缓冲 vs 缓冲写入
           ============================================================ */
        System.out.println("======= 场景1: 写入性能对比 =======");

        Path unbufFile = Paths.get(LAB_DIR, "unbuffered.txt");
        Path bufFile = Paths.get(LAB_DIR, "buffered.txt");

        int writeCount = 50000;  // 写 5 万次

        // 非缓冲写入（直接 FileOutputStream，不包 Buffer）
        long t1 = System.currentTimeMillis();
        try (FileOutputStream fos = new FileOutputStream(unbufFile.toFile())) {
            for (int i = 0; i < writeCount; i++) {
                fos.write("Hello\n".getBytes(StandardCharsets.UTF_8));
                // ★ 没有缓冲，每次 write 都触发系统调用！
            }
        }
        long t2 = System.currentTimeMillis();
        long unbufTime = t2 - t1;
        System.out.printf("  非缓冲 FileOutputStream: %d ms (每次 write 一次系统调用)%n", unbufTime);

        // 缓冲写入（FileOutputStream → BufferedOutputStream）
        long t3 = System.currentTimeMillis();
        try (BufferedOutputStream bos = new BufferedOutputStream(
                new FileOutputStream(bufFile.toFile()))) {
            for (int i = 0; i < writeCount; i++) {
                bos.write("Hello\n".getBytes(StandardCharsets.UTF_8));
                // ★ 数据积攒在 8KB 缓冲区，满了才真正写入
            }
        }  // ★ close 自动 flush
        long t4 = System.currentTimeMillis();
        long bufTime = t4 - t3;
        System.out.printf("  缓冲 BufferedOutputStream: %d ms (攒满 8KB 才写一次)%n", bufTime);

        if (unbufTime > 0) {
            System.out.printf("  性能差距: %.1f 倍 (非缓冲/缓冲)%n", (double) unbufTime / Math.max(bufTime, 1));
        }

        // 验证两个文件内容一致
        long size1 = Files.size(unbufFile);
        long size2 = Files.size(bufFile);
        System.out.printf("  文件大小: 非缓冲=%d, 缓冲=%d%n", size1, size2);

        /* ============================================================
           场景2: 演示 flush 的重要性
           ============================================================ */
        System.out.println("\n======= 场景2: flush 的重要性 =======");

        Path flushDemo = Paths.get(LAB_DIR, "flush-demo.txt");
        BufferedWriter writer = new BufferedWriter(
                new FileWriter(flushDemo.toFile(), StandardCharsets.UTF_8));
        writer.write("这条数据在缓冲区里");
        writer.newLine();

        // ★ 故意不 flush 和 close，此时数据还在缓冲区，没有落到磁盘
        // 如果此时程序崩溃，这行数据就丢了
        System.out.println("  数据已写入 writer，但还在缓冲区，尚未落到磁盘");
        System.out.println("  文件当前大小: " + Files.size(flushDemo) + " (可能为 0)");

        // flush 让缓冲区数据真正写入磁盘
        writer.flush();
        System.out.println("  flush() 后的文件大小: " + Files.size(flushDemo));

        // close 也会自动 flush
        writer.close();
        System.out.println("  close() 后文件大小: " + Files.size(flushDemo));
        System.out.println("  内容验证: " + Files.readString(flushDemo, StandardCharsets.UTF_8));

        /* ============================================================
           场景3: 缓冲读取 —— BufferedReader.readLine()
           ============================================================ */
        System.out.println("\n======= 场景3: 缓冲读取 =======");

        // 创建一个测试文件
        Path readTest = Paths.get(LAB_DIR, "read-test.log");
        try (BufferedWriter w = Files.newBufferedWriter(readTest, StandardCharsets.UTF_8)) {
            for (int i = 1; i <= 10; i++) {
                w.write("line-" + i + ": Java I/O 实战练习");
                w.newLine();
            }
        }

        // ★ BufferedReader.readLine() 返回 null 表示读取结束
        try (BufferedReader reader = Files.newBufferedReader(readTest, StandardCharsets.UTF_8)) {
            System.out.println("  逐行读取:");
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                // ★ readLine() 返回的字符串不含换行符
                lineNum++;
                System.out.printf("    [第%d行] %s%n", lineNum, line);
            }
        }

        /* ============================================================
           场景4: 缓冲区大小的选择 —— 理解为什么 8KB 是默认值
           ============================================================ */
        System.out.println("\n======= 场景4: 缓冲区的权衡 =======");
        System.out.println("  Java 默认缓冲区大小: 8192 字节 (8KB)");
        System.out.println("  为什么是 8KB？经过大量实测，平衡了内存占用和 I/O 效率");
        System.out.println("  经验法则:");
        System.out.println("    8KB  → 64KB: 有明显提升");
        System.out.println("    64KB → 1MB:  几乎没有提升（收益递减）");
        System.out.println("  适用场景: 写入大量小数据块时效果最明显");
        System.out.println("  不适用的场景: 一次写入几 MB（系统调用次数本来就少）");

        System.out.println("\n======= 清理 =======");
        deleteDirectory(Paths.get(LAB_DIR));
        System.out.println("  已清理");

        System.out.println("\n✓ Lab05 完成！记住：缓冲是用空间换 I/O 次数，8KB 默认值适合绝大多数场景。");
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
