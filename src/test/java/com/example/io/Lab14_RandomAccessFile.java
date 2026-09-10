package com.example.io;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Lab14: RandomAccessFile 实战 —— 随机读写大文件
 * -------------------------------------------------
 * 对应文档：第2.3节 RandomAccessFile：随机读写大文件
 *
 * 学习目标：
 * 1. 与流式（顺序）读写的区别：seek 跳转任意位置
 * 2. 模式参数："r" 只读、"rw" 读写
 * 3. getFilePointer() 获取当前位置
 * 4. 分段读取（模拟多线程并行加密）
 * 5. 在指定位置修改文件内容
 * 6. 注意：不是 InputStream/OutputStream 的子类
 */
public class Lab14_RandomAccessFile {

    private static final String LAB_DIR = "io-lab-temp/lab14";

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Paths.get(LAB_DIR));

        System.out.println("======= 核心区别 =======");
        System.out.println("  FileInputStream/OutputStream: 顺序读写，不能跳转");
        System.out.println("  RandomAccessFile:           可跳转到任意位置，同时支持读写\n");

        /* ============================================================
           场景1: 创建文件 & 顺序写入
           ============================================================ */
        System.out.println("======= 场景1: 创建并写入数据 =======");

        Path dataFile = Paths.get(LAB_DIR, "data.bin");
        // "rw" 模式：读写，文件不存在自动创建
        try (RandomAccessFile raf = new RandomAccessFile(dataFile.toFile(), "rw")) {
            raf.writeInt(100);           // 位置 0-3
            raf.writeLong(123456789L);    // 位置 4-11
            raf.writeUTF("Hello RAF");    // 位置 12+ (writeUTF 先写 2 字节长度)
            System.out.println("  数据写入完成，文件大小: " + raf.length() + " 字节");
            System.out.println("  文件指针位置: " + raf.getFilePointer());
        }

        System.out.println("\n======= 场景2: seek 跳转 + 随机读取 =======");
        System.out.println("  已知数据结构: [0-3]int:100  [4-11]long:123456789  [12+...]String\n");

        try (RandomAccessFile raf = new RandomAccessFile(dataFile.toFile(), "r")) {
            // 直接跳转到 long 字段的起始位置（跳过 int 的 4 字节）
            raf.seek(4);  // 跳过开头的 int（4 字节）
            long longVal = raf.readLong();
            System.out.println("  seek(4) → readLong(): " + longVal + " (跳过 int，直接读 long)");

            // 回到开头读 int
            raf.seek(0);
            int intVal = raf.readInt();
            System.out.println("  seek(0) → readInt():  " + intVal + " (回到开头读 int)");

            // 读取字符串
            long pos = raf.getFilePointer();  // 当前在 4
            System.out.println("  字符串起始位置: " + pos);
            String strVal = raf.readUTF();
            System.out.println("  readUTF(): \"" + strVal + "\"");

            System.out.println("\n  ★ RandomAccessFile 可以跳转到任意位置读取，这是 FileInputStream 做不到的");
        }

        System.out.println("\n======= 场景3: 在指定位置修改内容 =======");
        System.out.println("  ★ 使用 \"rw\" 模式，跳转到指定位置覆写\n");

        try (RandomAccessFile raf = new RandomAccessFile(dataFile.toFile(), "rw")) {
            // 修改第二个字段（long）：从 123456789 改为 999999999
            raf.seek(4);  // 跳转到 long 的起始位置
            raf.writeLong(999999999L);
            System.out.println("  已将位置 4 的 long 值改为 999999999");

            // 验证
            raf.seek(4);
            long newVal = raf.readLong();
            System.out.println("  验证读取: " + newVal);
        }

        System.out.println("\n======= 场景4: 模拟 FileProtector 并行分段加密 =======");
        System.out.println("  ★ 对应文档中的 FileProtector 并行加密模式\n");

        // 创建一个测试文件（模拟大文件）
        Path bigFile = Paths.get(LAB_DIR, "bigfile.dat");
        try (RandomAccessFile raf = new RandomAccessFile(bigFile.toFile(), "rw")) {
            raf.setLength(1024 * 1024 * 10);  // 设为 10MB 的空文件（稀疏文件）
        }
        long fileSize = Files.size(bigFile);
        System.out.println("  创建 10MB 测试文件，实际大小: " + fileSize / 1024 / 1024 + "MB");

        // 模拟分块：每块 3MB
        int blockSize = 3 * 1024 * 1024;  // 3MB
        long totalBlocks = (fileSize + blockSize - 1) / blockSize;
        System.out.println("  分 " + totalBlocks + " 块，每块 " + (blockSize / 1024 / 1024) + "MB");

        for (int i = 1; i <= totalBlocks; i++) {
            long startPos = (long) (i - 1) * blockSize;
            long endPos = Math.min(startPos + blockSize, fileSize);
            long actualBlockSize = endPos - startPos;

            System.out.printf("    线程 %d: 处理字节范围 %d ~ %d (%d 字节)%n",
                    i, startPos, endPos - 1, actualBlockSize);
            // ★ 实际多线程处理时，每个线程有自己的 RandomAccessFile 实例
            // ★ 每个线程调用 raf.seek(startPos) 跳转到自己的块起始位置
        }
        System.out.println("  ★ 每个线程处理一个块，理论上速度提升 = min(CPU核数, 块数)");

        /* ============================================================
           场景5: getFilePointer 的跟踪
           ============================================================ */
        System.out.println("\n======= 场景5: 文件指针跟踪 =======");

        Path trackFile = Paths.get(LAB_DIR, "track.bin");
        try (RandomAccessFile raf = new RandomAccessFile(trackFile.toFile(), "rw")) {
            System.out.printf("  初始位置: %d%n", raf.getFilePointer());
            raf.writeBoolean(true);
            System.out.printf("  writeBoolean 后: %d (1 字节)%n", raf.getFilePointer());
            raf.writeInt(42);
            System.out.printf("  writeInt 后:     %d (4 字节)%n", raf.getFilePointer());
            raf.writeDouble(3.14);
            System.out.printf("  writeDouble 后:  %d (8 字节)%n", raf.getFilePointer());
            raf.seek(0);
            System.out.printf("  seek(0) 后:      %d%n", raf.getFilePointer());
            raf.skipBytes(5);
            System.out.printf("  skipBytes(5) 后: %d (跳过 5 字节)%n", raf.getFilePointer());
        }

        System.out.println("\n======= RandomAccessFile 的注意事项 =======");
        System.out.println("  □ 不是 InputStream/OutputStream 的子类，不能用于需要流参数的方法");
        System.out.println("  □ 不支持装饰器模式，不能包 BufferedInputStream 等");
        System.out.println("  □ 多线程共享一个实例不安全——每个线程应有自己的实例");
        System.out.println("  □ 顺序读性能不如 FileInputStream（后者有预读优化）");
        System.out.println("  □ 构造函数路径不存在时抛 FileNotFoundException");

        System.out.println("\n======= 清理 =======");
        deleteDirectory(Paths.get(LAB_DIR));
        System.out.println("  已清理");

        System.out.println("\n✓ Lab14 完成！RandomAccessFile 适合分段处理、修改指定位置、断点续传等场景。");
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
