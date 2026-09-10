package main.java.com.example.fileIO;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

public class FileDemo {

    public static void main(String[] args) throws IOException {

        // ===== 1. 准备数据 =====
        List<String> lines = Arrays.asList("id,name,age", "1,张三,30", "2,李四,25");

        // Windows 下的输出文件
        Path outputFile = Paths.get("src/main/output/output.txt");

        // ===== 2. 原子写入 =====
        writeAtomically(
                outputFile.resolveSibling(outputFile.getFileName() + ".tmp"),
                outputFile,
                String.join(System.lineSeparator(), lines)
        );

        // ===== 3. 读取并验证 =====
        String content = Files.readString(outputFile, StandardCharsets.UTF_8);
        System.out.println("文件内容：");
        System.out.println(content);
    }

    /**
     * 原子写入文本文件（Windows / Linux 通用）
     */
    public static void writeAtomically(Path tmpPath, Path dstPath, String content) throws IOException {
        // 确保父目录存在（Windows 第一次运行必加）
        Files.createDirectories(dstPath.getParent());
        // 2 写临时文件（不怕失败）
        try (BufferedWriter writer = Files.newBufferedWriter(
                tmpPath, StandardCharsets.UTF_8)) {
            writer.write(content);
        }
        // 2 原子替换目标文件，要么成功，要么源文件不变
        Files.move(tmpPath, dstPath,
                StandardCopyOption.REPLACE_EXISTING,    // 覆盖已存在的文件
                StandardCopyOption.ATOMIC_MOVE  // 原子替换
        );
    }
}