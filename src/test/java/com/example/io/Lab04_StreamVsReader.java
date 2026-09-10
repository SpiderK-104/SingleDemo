package com.example.io;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * Lab04: 字节流 vs 字符流 —— 选型决定乱码还是正确
 * -------------------------------------------------
 * 对应文档：第1.4节 字节流与字符流
 *
 * 学习目标：
 * 1. 字节流(InputStream/OutputStream)处理二进制数据
 * 2. 字符流(Reader/Writer)处理文本数据 -> 指定编码
 * 3. 选错会导致乱码或数据损坏
 * 4. InputStreamReader/OutputStreamWriter 桥梁
 * 5. 模拟加密场景：全程操作 byte[]
 * 6. 模拟 CSV 写入场景：操作 char
 */
public class Lab04_StreamVsReader {

    private static final String LAB_DIR = "io-lab-temp/lab04";

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Paths.get(LAB_DIR));

        System.out.println("======= 核心原则 =======");
        System.out.println("字节流(Stream): 处理二进制 → 图片、加密数据、压缩文件");
        System.out.println("字符流(Reader/Writer): 处理文本 → CSV、JSON、XML");
        System.out.println("选错后果：乱码 或 数据损坏\n");

        /* ============================================================
           场景1: 用字节流处理二进制（加密场景模拟）
           对应文档中: LogArchiveService 加密流程 byte[] 操作
           ============================================================ */
        System.out.println("======= 场景1: 字节流处理二进制（模拟加密归档） =======");

        // Step 1: 原始文本 → byte[] (字符→字节)
        String originalText = "这是一条操作日志：用户 admin 登录系统";
        byte[] textBytes = originalText.getBytes(StandardCharsets.UTF_8);

        // Step 2: 模拟 SM4 加密（用 Base64 编码模拟加密）
        // ★ 加密处理的是字节而非字符
        String encryptedBase64 = Base64.getEncoder().encodeToString(textBytes);
        byte[] encryptedBytes = encryptedBase64.getBytes(StandardCharsets.UTF_8);

        // Step 3: 用 FileOutputStream 写加密数据到文件（字节流）
        Path encFile = Paths.get(LAB_DIR, "data.enc");
        try (FileOutputStream fos = new FileOutputStream(encFile.toFile())) {
            fos.write(encryptedBytes);
            // fos.flush();  // close 会自动 flush
        }
        System.out.println("加密数据已写入: " + encFile + " (" + encryptedBytes.length + " 字节)");

        // Step 4: 用 FileInputStream 读取回来（字节流）
        try (FileInputStream fis = new FileInputStream(encFile.toFile())) {
            byte[] buffer = new byte[(int) Files.size(encFile)];
            int bytesRead = fis.read(buffer);  // 一次读取全部（小文件）
            System.out.println("读取加密文件: " + bytesRead + " 字节");

            // 解密
            String base64Str = new String(buffer, StandardCharsets.UTF_8);
            byte[] decryptedBytes = Base64.getDecoder().decode(base64Str);
            String decryptedText = new String(decryptedBytes, StandardCharsets.UTF_8);
            System.out.println("解密后内容: " + decryptedText);
        }

        /* ============================================================
           场景2: 用字符流处理文本（CSV 写入场景）
           对应文档中: LogArchiveService CSV 写入
           ============================================================ */
        System.out.println("\n======= 场景2: 字符流处理文本（CSV 写入） =======");

        Path csvFile = Paths.get(LAB_DIR, "users.csv");
        // ★ 字符流必须指定编码！不指定默认是平台编码（Win GBK / Linux UTF-8）
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(csvFile.toFile()), StandardCharsets.UTF_8))) {
            writer.write('﻿');  // BOM
            writer.write("id,名称,角色");
            writer.newLine();
            writer.write("1,张三,管理员");
            writer.newLine();
            writer.write("2,李四,操作员");
            writer.newLine();
        }
        System.out.println("CSV 已写入（字符流 + UTF-8）");

        // 用 BufferedReader 读取
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(csvFile.toFile()), StandardCharsets.UTF_8))) {
            System.out.println("读取 CSV:");
            String line;
            while ((line = reader.readLine()) != null) {
                // ★ readLine() 返回的字符串不含换行符
                System.out.println("  | " + line);
            }
        }

        /* ============================================================
           场景3: 选错流的后果 —— 字节流读文本（演示）
           ============================================================ */
        System.out.println("\n======= 场景3: 选错流 → 问题展示 =======");

        // 用 FileInputStream 读取文本文件（没有指定编码，手动转换）
        Path textFile = Paths.get(LAB_DIR, "hello.txt");
        Files.writeString(textFile, "你好，世界！Hello World!", StandardCharsets.UTF_8);

        // 正确方式：Files.readString
        String correct = Files.readString(textFile, StandardCharsets.UTF_8);
        System.out.println("  正确(字符流+UTF-8): " + correct);

        // ★ 错误方式：不指定编码的转换 = 使用平台默认编码
        byte[] rawBytes = Files.readAllBytes(textFile);
        String wrongDefault = new String(rawBytes);  // 依赖平台编码！
        System.out.println("  错误(未指定编码):   " + wrongDefault
                + "  ← 在 Windows 上可能乱码");

        // ★ 再演示编码不一致导致的乱码
        byte[] utf8Bytes = "你好".getBytes(StandardCharsets.UTF_8);
        String gbkDecoded = new String(utf8Bytes, "GBK");  // UTF-8 的字节用 GBK 解码
        System.out.println("\n编码不一致的乱码演示:");
        System.out.println("  UTF-8 编码 → GBK 解码: " + gbkDecoded + " (乱码!)");

        /* ============================================================
           场景4: InputStream 分块读取大文件（避免 OOM 的关键模式）
           ============================================================ */
        System.out.println("\n======= 场景4: 分块读取（大文件模式） =======");

        // 先创建一个稍大的测试文件
        Path bigFile = Paths.get(LAB_DIR, "sample.log");
        try (BufferedWriter w = Files.newBufferedWriter(bigFile, StandardCharsets.UTF_8)) {
            for (int i = 1; i <= 100; i++) {
                w.write("第" + i + "条日志记录 - 用户操作详情");
                w.newLine();
            }
        }

        // ★ 分块读取：用 byte[8192] 缓冲区，避免一次性加载全部
        try (InputStream in = Files.newInputStream(bigFile)) {
            byte[] buffer = new byte[8192];  // 8KB 缓冲区
            int totalBytes = 0;
            int len;
            while ((len = in.read(buffer)) != -1) {
                totalBytes += len;
                // 模拟处理：这里只累计，实际业务中处理 buffer 中的内容
            }
            System.out.println("  分块读取完成，总字节数: " + totalBytes
                    + " (缓冲区 8KB，每次最多读 8192 字节)");
        }

        System.out.println("\n======= 清理 =======");
        deleteDirectory(Paths.get(LAB_DIR));
        System.out.println("  已清理");

        System.out.println("\n✓ Lab04 完成！记住根本原则:");
        System.out.println("  二进制 → 字节流 (InputStream/OutputStream)");
        System.out.println("  文本   → 字符流 (Reader/Writer) + 指定编码");
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
