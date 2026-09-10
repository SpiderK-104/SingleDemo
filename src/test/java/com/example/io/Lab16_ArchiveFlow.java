package com.example.io;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Lab16: 完整归档流程模拟 —— 把你项目的 I/O 全链路串起来
 * -------------------------------------------------
 * 对应文档：第1.10节 项目完整 I/O 流程串讲
 *
 * 模拟 LogArchiveService.doArchive 的完整流程：
 *
 * 1. 创建归档目录（Files.createDirectories）
 * 2. 分页读取数据库（模拟数据源）
 * 3. 写入 CSV 文件（含 BOM）→ Files.newBufferedWriter
 * 4. 关闭写入器（自动 flush）
 * 5. 读取 CSV 全部字节 → Files.readAllBytes
 * 6. 计算摘要（模拟 SM3）→ SHA-256
 * 7. "加密"（模拟 SM4）→ Base64 编码
 * 8. 写入 .enc 文件 → Files.write
 * 9. 删除明文 CSV → Files.deleteIfExists
 * 10. 异常处理：清理临时文件
 *
 * 运行前无需任何准备！自动在 io-lab-temp/lab16 下操作。
 */
public class Lab16_ArchiveFlow {

    private static final String ARCHIVE_DIR = "io-lab-temp/lab16/archive";
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public static void main(String[] args) {
        System.out.println("══════════════════════════════════════════════");
        System.out.println("  LogArchiveService 归档流程模拟");
        System.out.println("  文档对应: 第1.10节 项目完整 I/O 流程串讲");
        System.out.println("══════════════════════════════════════════════\n");

        String timeStamp = LocalDateTime.now().format(DTF);
        String csvFilePath = ARCHIVE_DIR + "/oplog_" + timeStamp + ".csv";
        String encFilePath = ARCHIVE_DIR + "/oplog_" + timeStamp + ".enc";

        try {
            // ═══════════════════════════════════════════════════
            // Step 1: 创建归档目录
            // ═══════════════════════════════════════════════════
            System.out.println("[Step 1] 创建归档目录");
            Path dir = Paths.get(ARCHIVE_DIR);
            Files.createDirectories(dir);
            System.out.println("  ✓ 目录已创建: " + dir.toAbsolutePath().normalize());
            System.out.println();

            // ═══════════════════════════════════════════════════
            // Step 2: 模拟分页读取数据库
            // ═══════════════════════════════════════════════════
            System.out.println("[Step 2] 分页读取数据库（模拟数据源）");
            int totalRecords = 53;     // 模拟总记录数
            int pageSize = 20;         // 每页 20 条（文档中 STREAM_PAGE_SIZE）
            int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
            System.out.printf("  总记录: %d, 每页: %d, 总页数: %d%n", totalRecords, pageSize, totalPages);

            // ═══════════════════════════════════════════════════
            // Step 3: 写入 CSV 文件（含 BOM）
            // ═══════════════════════════════════════════════════
            System.out.println("\n[Step 3] 写入 CSV 文件（含 BOM）");

            try (BufferedWriter writer = Files.newBufferedWriter(
                    Paths.get(csvFilePath), StandardCharsets.UTF_8)) {

                // ★ 写 BOM，确保 Excel 打开不乱码
                writer.write('﻿');  // U+FEFF
                System.out.println("  ✓ BOM 已写入 (0xEF 0xBB 0xBF)");

                // ★ 写 CSV 表头
                writer.write("id,username,operation,detail,create_time");
                writer.newLine();

                // ★ 模拟分页处理
                int writtenCount = 0;
                for (int page = 1; page <= totalPages; page++) {
                    List<String[]> pageData = simulateDbQuery(page, pageSize, totalRecords);

                    for (String[] record : pageData) {
                        // 将一行数据拼接为 CSV 格式
                        String csvLine = String.join(",", record);
                        writer.write(csvLine);
                        writer.newLine();
                        writtenCount++;
                    }

                    System.out.printf("  第 %d 页完成: 已写入 %d 条记录%n", page, writtenCount);
                }

                // ★ 手动 flush，确保最后一批数据落盘
                writer.flush();
                System.out.println("  ✓ flush 完成，数据已从缓冲区写入磁盘");

            } // ★ try-with-resources 自动 close，自动再次 flush
            System.out.println("  ✓ CSV 文件已关闭");

            long csvSize = Files.size(Paths.get(csvFilePath));
            System.out.printf("  CSV 文件大小: %d 字节%n", csvSize);

            // ═══════════════════════════════════════════════════
            // Step 4: 读取 CSV 全部字节
            // ═══════════════════════════════════════════════════
            System.out.println("\n[Step 4] 读取 CSV 全部字节");
            byte[] csvBytes = Files.readAllBytes(Paths.get(csvFilePath));
            System.out.printf("  ✓ 读取完成: %d 字节%n", csvBytes.length);

            // ═══════════════════════════════════════════════════
            // Step 5: 计算 SM3 摘要（这里用 SHA-256 模拟）
            // ═══════════════════════════════════════════════════
            System.out.println("\n[Step 5] 计算数据摘要（模拟 SM3 = SHA-256）");
            String digest = computeSha256(csvBytes);
            System.out.printf("  ✓ 摘要值: %s%n", digest);
            System.out.println("  ★ 摘要用于完整性校验：比对数据库中存储的 digest 字段");

            // ═══════════════════════════════════════════════════
            // Step 6: SM4 加密（用 Base64 模拟）
            // ═══════════════════════════════════════════════════
            System.out.println("\n[Step 6] 加密数据（模拟 SM4 = Base64 编码）");
            String encryptedBase64 = Base64.getEncoder().encodeToString(csvBytes);
            byte[] encBytes = Base64.getDecoder().decode(encryptedBase64);
            System.out.printf("  ✓ 加密完成: %d 字节 → Base64 → %d 字节%n",
                    csvBytes.length, encBytes.length);
            System.out.println("  ★ 文档说明: KeyProtectionManager.encrypt 返回 Base64 字符串");

            // ═══════════════════════════════════════════════════
            // Step 7: 写入 .enc 加密文件
            // ═══════════════════════════════════════════════════
            System.out.println("\n[Step 7] 写入加密文件");
            Files.write(Paths.get(encFilePath), encBytes);
            long encSize = Files.size(Paths.get(encFilePath));
            System.out.printf("  ✓ 已写入: %s (%d 字节)%n",
                    Paths.get(encFilePath).getFileName(), encSize);

            // 模拟：将 digest 保存到"数据库"
            String simulatedDigestInDb = digest;

            // ═══════════════════════════════════════════════════
            // Step 8: 删除明文 CSV
            // ═══════════════════════════════════════════════════
            System.out.println("\n[Step 8] 删除明文 CSV 文件");
            boolean deleted = Files.deleteIfExists(Paths.get(csvFilePath));
            System.out.printf("  ✓ 明文 CSV 已删除: %s%n", deleted);
            System.out.println("  ★ 生产环境只保留密文文件，确保数据安全");

            // ═══════════════════════════════════════════════════
            // Step 9: 验证归档文件的完整性
            // ═══════════════════════════════════════════════════
            System.out.println("\n[Step 9] 验证归档文件完整性（读取 → 解密 → 摘要对比）");

            // 读加密文件
            byte[] encContent = Files.readAllBytes(Paths.get(encFilePath));

            // Base64 解码（模拟解密）
            String base64Str = new String(encContent, StandardCharsets.UTF_8);
            byte[] decryptedBytes = Base64.getDecoder().decode(base64Str);

            // 重新计算摘要
            String recalculatedDigest = computeSha256(decryptedBytes);

            System.out.printf("  数据库中的摘要: %s%n", simulatedDigestInDb);
            System.out.printf("  重新计算的摘要: %s%n", recalculatedDigest);

            boolean integrityOk = simulatedDigestInDb.equals(recalculatedDigest);
            System.out.printf("  ✓ 完整性校验: %s%n", integrityOk ? "通过 (文件未被篡改)" : "失败 (文件已损坏)");

            // 将解密后的字节转为文本
            String readableContent = new String(decryptedBytes, StandardCharsets.UTF_8);
            System.out.println("\n[Step 10] 解密后的文本内容预览:");
            String[] contentLines = readableContent.split("\n");
            for (int i = 0; i < Math.min(5, contentLines.length); i++) {
                System.out.println("  " + contentLines[i]);
            }
            if (contentLines.length > 5) {
                System.out.printf("  ... 共 %d 行%n", contentLines.length);
            }

            // ═══════════════════════════════════════════════════
            // 输出总结
            // ═══════════════════════════════════════════════════
            System.out.println("\n══════════════════════════════════════════════");
            System.out.println("  归档完成！流程总结:");
            System.out.println("  ┌─────────────────────────────────────────┐");
            System.out.println("  │ ① 创建目录                              │");
            System.out.println("  │ ② 分页读取 DB → 写入 CSV (BOM + UTF-8)  │");
            System.out.println("  │ ③ 读取 CSV byte[]                       │");
            System.out.println("  │ ④ 计算 SHA-256 摘要                     │");
            System.out.println("  │ ⑤ Base64 编码（模拟加密）                │");
            System.out.println("  │ ⑥ 写入 .enc 文件                        │");
            System.out.println("  │ ⑦ 删除明文 CSV                          │");
            System.out.println("  │ ⑧ 读取 .enc → Base64 解码 → 摘要对比    │");
            System.out.println("  └─────────────────────────────────────────┘");
            System.out.println("══════════════════════════════════════════════\n");

        } catch (Exception e) {
            // ★ 异常处理：清理临时文件，避免残存文件堆积磁盘
            System.err.println("  ✗ 归档过程异常: " + e.getMessage());
            System.err.println("  开始清理临时文件...");
            cleanupQuietly(csvFilePath);
            cleanupQuietly(encFilePath);
            System.err.println("  临时文件已清理");
            e.printStackTrace();
        }

        // 清理整个测试目录
        try {
            deleteDirectory(Paths.get("io-lab-temp/lab16"));
        } catch (IOException e) {
            System.err.println("清理目录失败: " + e.getMessage());
        }

        System.out.println("\n✓ Lab16 完成！这个流程完整复现了你项目中的 LogArchiveService.doArchive 归档全链路。");
    }

    /** 模拟数据库分页查询 */
    private static List<String[]> simulateDbQuery(int page, int pageSize, int totalRecords) {
        int start = (page - 1) * pageSize + 1;
        int end = Math.min(start + pageSize - 1, totalRecords);

        List<String[]> pageData = new ArrayList<>();
        String[] users = {"admin", "张三", "李四", "王五", "赵六"};
        String[] operations = {"登录系统", "查询日志", "导出报表", "修改配置", "删除记录", "新增用户", "归档操作"};
        Random random = new Random(42);  // 固定种子，每次运行结果一致

        for (int id = start; id <= end; id++) {
            String username = users[random.nextInt(users.length)];
            String operation = operations[random.nextInt(operations.length)];
            String detail = "用户" + username + "执行了" + operation;
            String time = LocalDateTime.now().minusDays(random.nextInt(30))
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            pageData.add(new String[]{
                    String.valueOf(id),
                    username,
                    operation,
                    detail,
                    time
            });
        }
        return pageData;
    }

    /** 计算 SHA-256 摘要（模拟 SM3） */
    private static String computeSha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 不可用", e);
        }
    }

    /** 安静地删除文件（不抛异常） */
    private static void cleanupQuietly(String filePath) {
        if (filePath == null) return;
        try {
            Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            // 忽略
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
