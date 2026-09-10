package test.java.com.example.io;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Lab11: 常用 I/O 场景代码模板实战
 * -------------------------------------------------
 * 对应文档：第2.1节 常用 I/O 场景的代码模板
 *
 * 学习目标：8 种常用模式的代码记牢
 * 1. 逐行读取文本文件
 * 2. Stream API 读取文本文件
 * 3. 写入多行到文本文件
 * 4. 复制文件
 * 5. 文件下载到本地（模拟）
 * 6. 分块读取大文件
 * 7. 压缩与解压
 * 8. 分块写入（InputStream → OutputStream 复制）
 */
public class Lab11_FileReadWriteTemplates {

    private static final String LAB_DIR = "io-lab-temp/lab11";

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Paths.get(LAB_DIR));

        /* ============================================================
           模板1: 逐行读取文本文件（适合大文件）
           ============================================================ */
        System.out.println("======= 模板1: 逐行读取文本文件 =======");
        // 准备测试文件
        Path logFile = Paths.get(LAB_DIR, "access.log");
        try (BufferedWriter w = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8)) {
            for (int i = 0; i < 20000; i++) {
                w.write("2026-07-09 10:00:0" + (i % 10) + " [INFO] 请求处理完成,耗时" + (i % 100) + "ms");
                w.newLine();
            }
        }

        // ★ 逐行读取，不会把整个文件加载到内存
        int lineCount = 0;
        int slowCount = 0;
        try (BufferedReader reader = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineCount++;
                if (line.contains("耗时>50ms") || line.contains("耗时5") || line.contains("耗时6")) {
                    slowCount++;
                }
            }
        }
        System.out.printf("  逐行读取完成: %d 行，其中慢查询 %d 条（内存只保留一行的数据）%n", lineCount, slowCount);

        /* ============================================================
           模板2: Stream API 读取文本文件
           ============================================================ */
        System.out.println("\n======= 模板2: Stream API 读取（函数式风格） =======");

        // ★ Stream 必须放在 try-with-resources，因为持有文件句柄
        try (Stream<String> lines = Files.lines(logFile, StandardCharsets.UTF_8)) {
            List<String> errorLines = lines
                    .skip(0)                          // 跳过表头
                    .filter(l -> l.contains("耗时>50ms") || l.contains("耗时5") || l.contains("耗时6"))
                    .limit(5)                          // 只取前 5 条
                    .collect(Collectors.toList());

            System.out.printf("  Stream API 过滤出 %d 条慢查询（前 5 条）:%n", errorLines.size());
            errorLines.forEach(l -> System.out.println("    → " + l));
        }

        /* ============================================================
           模板3: 写入多行到文本文件
           ============================================================ */
        System.out.println("\n======= 模板3: 写入多行到文本文件 =======");

        List<String> lines = Arrays.asList("id,name,age", "1,张三,30", "2,李四,25", "3,王五,28");
        // ★ Files.write 的 Collection 重载自动用行分隔符连接
        Path csvFile = Paths.get(LAB_DIR, "users.csv");
        Files.write(csvFile, lines, StandardCharsets.UTF_8);
        System.out.println("  多行写入完成:");
        Files.readAllLines(csvFile, StandardCharsets.UTF_8)
             .forEach(l -> System.out.println("    " + l));

        /* ============================================================
           模板4: 复制文件
           ============================================================ */
        System.out.println("\n======= 模板4: 复制文件 =======");

        Path backupFile = Paths.get(LAB_DIR, "users_backup.csv");
        long copySize = Files.copy(csvFile, backupFile, StandardCopyOption.REPLACE_EXISTING).getNameCount();
        System.out.println("  已复制 " + copySize + " 字节 (REPLACE_EXISTING 覆盖已有)");

        /* ============================================================
           模板5: 文件下载到本地（模拟）
           ============================================================ */
        System.out.println("\n======= 模板5: 模拟文件下载到本地 =======");

        // 模拟从 HTTP 下载数据（用本地数据代替远程）
        String simulatedRemoteData = "报告内容: 2026年7月运营数据\n总收入: ¥1,234,567\n支出: ¥890,123";
        byte[] downloadedData = simulatedRemoteData.getBytes(StandardCharsets.UTF_8);

        Path downloadTarget = Paths.get(LAB_DIR, "downloads", "report.csv");
        Files.createDirectories(downloadTarget.getParent());  // ★ 确保父目录存在
        Files.write(downloadTarget, downloadedData);
        System.out.println("  下载保存完成: " + downloadTarget + " (" + downloadedData.length + " 字节)");

        /* ============================================================
           模板6: 需要分块处理的大文件
           ============================================================ */
        System.out.println("\n======= 模板6: 分块处理 =======");

        // ★ 对应文档中的 STREAM_PAGE_SIZE 策略
        int totalEntries = 10007;  // 总记录数
        int pageSize = 2000;       // 每页 2000 条
        int totalPages = (int) Math.ceil((double) totalEntries / pageSize);

        System.out.printf("  总记录: %d, 每页: %d, 总页数: %d%n", totalEntries, pageSize, totalPages);
        int processed = 0;
        for (int page = 1; page <= totalPages; page++) {
            int start = (page - 1) * pageSize;
            int end = Math.min(start + pageSize, totalEntries);
            int pageRecords = end - start;
            processed += pageRecords;
            System.out.printf("    第 %d 页: 记录 %d ~ %d (%d 条)%n", page, start, end - 1, pageRecords);
        }
        System.out.println("  共处理 " + processed + " 条（避免 OOM，每次只加载 2000 条到内存）");

        /* ============================================================
           模板7: InputStream → OutputStream 分块复制
           ============================================================ */
        System.out.println("\n======= 模板7: 分块复制（大文件流式处理的核心模式） =======");

        Path srcFile = Paths.get(LAB_DIR, "large-source.dat");
        Path dstFile = Paths.get(LAB_DIR, "large-copy.dat");

        // 创建测试数据
        try (OutputStream out = Files.newOutputStream(srcFile)) {
            for (int i = 0; i < 1000; i++) {
                out.write("这是一行测试数据，模拟大文件的分块处理流程。\n".getBytes(StandardCharsets.UTF_8));
            }
        }

        // ★ 核心模式：byte[8192] 缓冲区 + while 循环
        try (InputStream in = Files.newInputStream(srcFile);
             OutputStream out = Files.newOutputStream(dstFile)) {
            byte[] buffer = new byte[8192];  // 8KB 缓冲区
            int len;
            int total = 0;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
                total += len;
            }
            System.out.printf("  分块复制完成: %d 字节（缓冲区 8KB，循环写入）%n", total);
        }

        /* ============================================================
           模板8: 压缩与解压（模拟）
           ============================================================ */
        System.out.println("\n======= 模板8: 压缩与解压 =======");
        System.out.println("  ★ 文档中的 ZipUtils 基于 java.util.zip 包");
        System.out.println("  压缩: ZipOutputStream(FileOutputStream)");
        System.out.println("  解压: ZipInputStream(FileInputStream)");
        System.out.println("  缓冲区: byte[1024] 是常见选择，平衡内存和 IO");

        System.out.println("\n======= 清理 =======");
        deleteDirectory(Paths.get(LAB_DIR));
        System.out.println("  已清理");

        System.out.println("\n✓ Lab11 完成！这 8 个模板覆盖了日常 90% 的 I/O 场景，建议熟记。");
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            try (Stream<Path> walk = Files.walk(dir)) {
                walk.sorted((a, b) -> b.toString().length() - a.toString().length())
                    .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException e) {} });
            }
        }
    }
}
