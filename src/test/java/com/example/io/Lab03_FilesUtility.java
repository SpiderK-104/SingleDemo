package test.java.com.example.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Lab03: Files 工具类实战 —— 现代文件操作的核心
 * -------------------------------------------------
 * 对应文档：第1.3节 Files 工具类
 * <p>
 * 学习目标：
 * 1. Files.readAllBytes / Files.write —— 小文件的便捷读写
 * 2. Files.newBufferedWriter —— 带缓冲的字符写入
 * 3. Files.deleteIfExists —— 安全删除
 * 4. Files.exists —— 存在性检查 + TOCTOU 问题说明
 * 5. Files.createDirectories —— 递归创建目录
 * 6. Files.lines —— 流式读取（大文件友好）
 * 7. Files.copy —— 文件复制
 * 8. StandardOpenOption —— 写模式控制
 */
public class Lab03_FilesUtility {

    private static final String LAB_DIR = "io-lab-temp/lab03";

    public static void main(String[] args) throws IOException {
        // 准备目录
        Path dir = Paths.get(LAB_DIR);
        System.out.println("准备目录: " + dir);  // io-lab-temp/lab03
        Files.createDirectories(dir);

        /*
         场景一：Files.write + Files.readAllBytes
         */

        // ★ Files.write 默认覆盖已有文件
        String content = "Hello Java I/O!";
        Path txtFile = Paths.get(LAB_DIR, "demo.txt");
        // 将字符串按照 UTF-8 编码写入文件
        Files.write(txtFile, content.getBytes(StandardCharsets.UTF_8));
        System.out.println("已写入: " + txtFile);

        // ★ readAllBytes 适合小文件（几 MB），大文件会 OOM
        byte[] readBack = Files.readAllBytes(txtFile);
        // ★ UTF-8 编码解码为字符串,解析内容
        String readRes = new String(readBack, StandardCharsets.UTF_8);


        // 场景二：Files.write + StandardOpenOption 追加模式
        // APPEND 追加模式
        Files.writeString(txtFile, "【追加内容】\n", StandardOpenOption.APPEND);


        System.out.println("追加后读取: " + Files.readString(txtFile));

        // CREATE_NEW —— 文件已存在时抛异常（原子化检查+创建，避免竞态）
        Path newFile = Paths.get(LAB_DIR, "new.txt");
        try {
            // csq 表示写入文件的字符串内容
            Files.writeString(newFile, "第一次写入", StandardOpenOption.CREATE_NEW);
            Files.writeString(newFile, "第二次写入", StandardOpenOption.CREATE_NEW);
        } catch (FileAlreadyExistsException e) {
            System.out.println("CREATE_NEW 第二次失败 → FileAlreadyExistsException (这正是我们期望的保护行为)");
        }

        System.out.println("\n======= 场景3: Files.newBufferedWriter 写出带 BOM 的 CSV =======");
        Path csvFile = Paths.get(LAB_DIR, "data.csv");


        try (BufferedWriter writer = Files.newBufferedWriter(csvFile, StandardCharsets.UTF_8)) {
            // ★ UTF-8 BOM: 让 Excel 正确识别 UTF-8 编码
            writer.write('﻿');  // BOM
            writer.write("id,姓名,年龄");
            writer.newLine();
            writer.write("1,张三,30");
            writer.newLine();
            writer.write("2,李四,25");
            writer.newLine();
        } // ★ 自动 close，自动 flush
        System.out.println("CSV 文件已创建（含 BOM）:");

        // ★ Files.readAllLines 一次读取所有行（小文件用）
        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        lines.forEach(line -> System.out.println("  | " + line));

        System.out.println("\n======= 场景4: Files.lines 流式读取（大文件场景） =======");
        // ★ 返回 Stream<String>，内部按需读取，不一次加载全文件
        // ★ 必须放在 try-with-resources 中，因为 Stream 持有文件句柄
        System.out.println("  (过滤出包含 '李' 的行):");
        try (Stream<String> stream = Files.lines(csvFile, StandardCharsets.UTF_8)) {
            stream.filter(line -> line.contains("李"))
                    .forEach(line -> System.out.println("  → " + line));
        }

        System.out.println("\n======= 场景5: Files.exists + Files.deleteIfExists =======");
        Path existPath = txtFile;
        System.out.println("  exists? " + Files.exists(existPath));

        boolean deleted = Files.deleteIfExists(existPath);
        System.out.println("  deleteIfExists: " + deleted);
        System.out.println("  deleteIfExists(再次): " + Files.deleteIfExists(existPath)
                + " (第二次返回 false，不抛异常)");

        // ★ Files.delete 在文件不存在时抛异常
        System.out.println("  Files.delete 效果对比:");
        try {
            Files.delete(existPath);
        } catch (java.nio.file.NoSuchFileException e) {
            System.out.println("  Files.delete → NoSuchFileException (不存在就抛异常)");
        }

        System.out.println("\n======= 场景6: Files.createDirectories 递归创建目录 =======");
        Path deepDir = Paths.get(LAB_DIR, "a", "b", "c");
        Files.createDirectories(deepDir);  // 等价于 mkdirs()
        System.out.println("  多级目录已创建: " + deepDir);
        System.out.println("  exists? " + Files.exists(deepDir));

        System.out.println("\n======= 场景7: Files.copy 文件复制 =======");
        Path source = csvFile;
        Path target = Paths.get(LAB_DIR, "data_backup.csv");
        long size = Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING).getNameCount();
        System.out.println("  已复制 " + size + " 字节，覆盖已有文件");

        // ★ Files.copy(InputStream, Path) 从网络流保存到本地
        System.out.println("  Files.copy 还支持 InputStream → Path 和 Path → OutputStream");

        System.out.println("\n======= 场景8: Files.list 非递归遍历目录 =======");
        // ★ list 只遍历当前目录一层，返回的 Stream 需要 close
        try (Stream<Path> dirStream = Files.list(dir)) {
            List<Path> files = dirStream.collect(Collectors.toList());
            System.out.println("  " + LAB_DIR + " 目录下的文件(" + files.size() + "个):");
            files.forEach(p -> System.out.println("    " + p.getFileName()));
        }

        System.out.println("\n======= 清理 =======");
        deleteDirectory(dir);
        System.out.println("  临时目录已清理");

        System.out.println("\n✓ Lab03 完成！Files 工具类是 NIO.2 的核心，绝大部分日常操作都可以用它完成。");
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (Stream<Path> walk = Files.walk(dir)) {
                walk.sorted((a, b) -> b.toString().length() - a.toString().length())  // 先删子再删父
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException e) {
                            }
                        });
            }
        }
    }
}
