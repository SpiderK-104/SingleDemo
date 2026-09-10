package com.example.io;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Lab07: 编码与乱码实战 —— Charset 的全局理解
 * -------------------------------------------------
 * 对应文档：第1.9节 编码与乱码
 *
 * 学习目标：
 * 1. 理解"字符→编码→字节→解码→字符"的全过程
 * 2. 编码/解码不一致导致乱码
 * 3. UTF-8 BOM 的作用
 * 4. Base64 编码（二进制→文本）
 * 5. StandardCharsets 常量与 Charset.forName
 * 6. 创建实验环境来观察乱码
 */
public class Lab07_CharsetEncoding {

    private static final String LAB_DIR = "io-lab-temp/lab07";

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Paths.get(LAB_DIR));

        System.out.println("======= 核心公式 =======");
        System.out.println("  字符 → 编码(Charset) → 字节序列 → 解码(相同Charset) → 字符");
        System.out.println("  乱码原因 = 编码用的字符集 ≠ 解码用的字符集\n");

        /* ============================================================
           场景1: 同一字符串在不同编码下的字节数
           ============================================================ */
        System.out.println("======= 场景1: 不同编码的字节数差异 =======");

        String text = "A你好";
        System.out.println("字符串: \"" + text + "\"");

        byte[] asciiBytes = text.getBytes(StandardCharsets.US_ASCII);
        byte[] utf8Bytes = text.getBytes(StandardCharsets.UTF_8);
        byte[] utf16Bytes = text.getBytes(StandardCharsets.UTF_16);
        byte[] gbkBytes = text.getBytes(Charset.forName("GBK"));

        System.out.println("  US-ASCII: " + Arrays.toString(asciiBytes)
                + " → " + asciiBytes.length + " 字节 (中文变成 63 即 '?')");
        System.out.println("  UTF-8:    " + Arrays.toString(utf8Bytes)
                + " → " + utf8Bytes.length + " 字节 (A=1, 你=3, 好=3)");
        System.out.println("  UTF-16:   " + Arrays.toString(utf16Bytes)
                + " → " + utf16Bytes.length + " 字节 (含 BOM)");
        System.out.println("  GBK:      " + Arrays.toString(gbkBytes)
                + " → " + gbkBytes.length + " 字节 (A=1, 你=2, 好=2)");

        /* ============================================================
           场景2: 编码不一致导致乱码
           ============================================================ */
        System.out.println("\n======= 场景2: 编码/解码不一致 → 乱码 =======");

        // 用 UTF-8 编码，用 GBK 解码
        String original = "你好世界";
        byte[] encoded = original.getBytes(StandardCharsets.UTF_8);
        String wrongDecoded = new String(encoded, Charset.forName("GBK"));
        System.out.println("  UTF-8编码 → GBK解码: \"" + wrongDecoded + "\" (乱码!)");

        // 用 GBK 编码，用 UTF-8 解码
        byte[] gbkEncoded = original.getBytes(Charset.forName("GBK"));
        String wrongDecoded2 = new String(gbkEncoded, StandardCharsets.UTF_8);
        System.out.println("  GBK编码 → UTF-8解码: \"" + wrongDecoded2 + "\" (乱码!)");

        // 正确的方式：编码和解码用相同的字符集
        String correct = new String(encoded, StandardCharsets.UTF_8);
        System.out.println("  UTF-8编码 → UTF-8解码: \"" + correct + "\" (正确!)");

        /* ============================================================
           场景3: BOM 的作用演示
           ============================================================ */
        System.out.println("\n======= 场景3: UTF-8 BOM 的作用 =======");

        Path csvWithBom = Paths.get(LAB_DIR, "with-bom.csv");
        Path csvWithoutBom = Paths.get(LAB_DIR, "without-bom.csv");

        // 写入带 BOM 的 CSV
        try (BufferedWriter w = Files.newBufferedWriter(csvWithBom, StandardCharsets.UTF_8)) {
            w.write('﻿');  // UTF-8 BOM (U+FEFF)
            w.write("姓名,年龄");
            w.newLine();
            w.write("张三,30");
            w.newLine();
        }
        System.out.println("  带 BOM 的 CSV: " + Files.size(csvWithBom) + " 字节 (第1字节=0xEF 0xBB 0xBF)");

        // 检查 BOM 字节
        byte[] bomBytes = Files.readAllBytes(csvWithBom);
        System.out.println("  前3字节 (BOM): "
                + String.format("%02X %02X %02X", bomBytes[0], bomBytes[1], bomBytes[2]));
        System.out.println("  文档中说明: Windows Excel 通过 BOM 判断 CSV 编码，无 BOM 则用系统编码打开 → 中文乱码");

        // 不带 BOM 写入对比
        try (BufferedWriter w = Files.newBufferedWriter(csvWithoutBom, StandardCharsets.UTF_8)) {
            w.write("姓名,年龄");
            w.newLine();
        }
        System.out.println("  无 BOM 的 CSV: " + Files.size(csvWithoutBom) + " 字节 (无 BOM 前缀)");

        /* ============================================================
           场景4: Base64 编码（二进制→文本转换）
           ============================================================ */
        System.out.println("\n======= 场景4: Base64 编码 =======");
        // ★ 对应文档中 KeyProtectionManager.encrypt → Base64 中转
        // ★ Base64 不是字符编码，而是二进制数据到文本的编码方案

        byte[] binaryData = {0x00, 0x01, (byte) 0xFF, (byte) 0xFE, 0x7F, 0x30};
        String base64Str = java.util.Base64.getEncoder().encodeToString(binaryData);
        byte[] decoded = java.util.Base64.getDecoder().decode(base64Str);

        System.out.println("  原始字节:    " + Arrays.toString(binaryData));
        System.out.println("  Base64 字符串: \"" + base64Str + "\"");
        System.out.println("  解码后字节: " + Arrays.toString(decoded));
        System.out.println("  是否一致: " + Arrays.equals(binaryData, decoded));

        // ★ 模拟文档中的加密中转场景
        String logData = "用户 admin 执行了删除操作";
        byte[] logBytes = logData.getBytes(StandardCharsets.UTF_8);
        // "加密" → Base64 字符串（模拟加密接口返回 Base64）
        String encryptedBase64 = java.util.Base64.getEncoder().encodeToString(logBytes);
        // 解码回字节再写入文件
        byte[] toWrite = java.util.Base64.getDecoder().decode(encryptedBase64);
        Path encFile = Paths.get(LAB_DIR, "encrypted.enc");
        Files.write(encFile, toWrite);
        System.out.println("\n  模拟归档加密流程:");
        System.out.println("    原始日志: " + logData);
        System.out.println("    加密Base64: " + encryptedBase64);
        System.out.println("    写入 " + toWrite.length + " 字节到 " + encFile.getFileName());

        /* ============================================================
           场景5: Charset.defaultCharset() 的陷阱
           ============================================================ */
        System.out.println("\n======= 场景5: 默认编码的陷阱 =======");
        System.out.println("  当前 JVM 默认编码: " + Charset.defaultCharset());
        System.out.println("  Windows 默认 GBK，Linux 默认 UTF-8");
        System.out.println("  同样的代码在不同平台运行结果不同 → 必须显式指定编码！");

        // ★ StandardCharsets 常量 vs Charset.forName
        System.out.println("\n  StandardCharsets.UTF_8: " + StandardCharsets.UTF_8);
        System.out.println("  Charset.forName(\"UTF-8\"): " + Charset.forName("UTF-8"));
        System.out.println("  ★ 用 StandardCharsets 常量比 Charset.forName 更高效（避免字符串查找）");

        /* ============================================================
           场景6: 读取文件时显式指定编码的重要性
           ============================================================ */
        System.out.println("\n======= 场景6: 读写文件指定编码 =======");

        Path encodingFile = Paths.get(LAB_DIR, "encoding-demo.txt");
        // 写入 UTF-8
        Files.writeString(encodingFile, "这是一段中文内容", StandardCharsets.UTF_8);

        // 正确读取
        String correctRead = Files.readString(encodingFile, StandardCharsets.UTF_8);
        System.out.println("  指定UTF-8读取: " + correctRead);

        // ★ 不安全的方式 —— 不指定编码，依赖平台默认编码
        // 在 Windows 上可能出现的问题：
        // Files.readAllBytes(path) → 然后用平台默认编码 new String(bytes)
        byte[] raw = Files.readAllBytes(encodingFile);
        String defaultRead = new String(raw);  // 未指定编码
        System.out.println("  未指定编码读取: " + defaultRead + " (跨平台行为不一致!)");

        System.out.println("\n======= 清理 =======");
        deleteDirectory(Paths.get(LAB_DIR));
        System.out.println("  已清理");

        System.out.println("\n✓ Lab07 完成！记住：在任何 I/O 操作中都显式指定 StandardCharsets.UTF_8。");
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
