package com.example.io;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Lab10: 装饰器模式在 Java I/O 中的体现
 * -------------------------------------------------
 * 对应文档：第1.8节 装饰器模式在 Java I/O 中的体现
 *
 * 学习目标：
 * 1. 理解"核心组件 + 增强组件"的组合方式
 * 2. 演示装饰链的构建：FileInputStream ← BufferedInputStream ← GzipInputStream
 * 3. 理解装饰链的关闭顺序
 * 4. Files.newBufferedWriter 底层实际上也是装饰器组合
 * 5. CSVPrinter 是装饰器模式的业务变体
 */
public class Lab10_DecoratorPattern {

    private static final String LAB_DIR = "io-lab-temp/lab10";

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Paths.get(LAB_DIR));

        System.out.println("======= 核心思想 =======");
        System.out.println("  不修改核心类，用一层层包装来增强功能");
        System.out.println("  例: GzipInputStream(BufferedInputStream(FileInputStream))\n");

        /* ============================================================
           场景1: 装饰链构建（需要压缩的场景）
           ============================================================ */
        System.out.println("======= 场景1: 压缩写入（装饰链） =======");

        Path gzFile = Paths.get(LAB_DIR, "data.txt.gz");

        // ★ 从内到外：核心文件输出 → 缓冲 → GZIP 压缩
        // 写数据时：先压缩成 gzip 格式，再缓冲，再写入文件
        try (OutputStream out = new GZIPOutputStream(          // 最外层：压缩
                new BufferedOutputStream(                       // 中间层：缓冲
                        new FileOutputStream(gzFile.toFile())))) {  // 核心层：文件写入
            out.write("这是一段需要压缩的内容，重复内容能压缩得很好！".getBytes(StandardCharsets.UTF_8));
            out.write("这是一段需要压缩的内容，重复内容能压缩得很好！".getBytes(StandardCharsets.UTF_8));
        }
        System.out.println("  写入完成，文件大小: " + Files.size(gzFile) + " 字节");
        System.out.println("  ★ 由于内容重复，gzip 压缩效果明显，大小会比原始小很多");

        // 读取解压
        try (InputStream in = new GZIPInputStream(
                new BufferedInputStream(
                        new FileInputStream(gzFile.toFile())))) {
            String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("  解压读取: " + content);
        }

        /* ============================================================
           场景2: Files.newBufferedWriter 底层的装饰链
           ============================================================ */
        System.out.println("\n======= 场景2: newBufferedWriter 底层等价于 =======");
        System.out.println("  ★ Files.newBufferedWriter(path, charset) 内部等价于:");
        System.out.println("    new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(path), charset))");
        System.out.println("  这就是装饰器模式——Files 工具类帮你隐藏了装饰链的构建\n");

        Path csvFile = Paths.get(LAB_DIR, "decorator.csv");
        // 等价于 Files.newBufferedWriter 的手动版本
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(csvFile.toFile()), StandardCharsets.UTF_8))) {
            writer.write("id,name");
            writer.newLine();
            writer.write("1,test");
            writer.newLine();
        }
        System.out.println("  CSV 写入完成");

        /* ============================================================
           场景3: CSVPrinter —— 业务层面的装饰
           ============================================================ */
        System.out.println("\n======= 场景3: 模拟 CSVPrinter 装饰模式 =======");

        Path csvPrinterFile = Paths.get(LAB_DIR, "printers.csv");
        try (BufferedWriter writer = Files.newBufferedWriter(csvPrinterFile, StandardCharsets.UTF_8)) {
            // ★ CSVPrinter 装饰了 BufferedWriter，增加了 CSV 格式化能力
            // 这里模拟 CSVPrinter 的行为
            CsvWriter csvWriter = new CsvWriter(writer);
            csvWriter.writeRecord("id", "姓名", "角色");
            csvWriter.writeRecord("1", "张三", "管理员");
            csvWriter.writeRecord("2", "李四", "操作员");
        }
        System.out.println("  CSV 内容:");
        Files.readAllLines(csvPrinterFile, StandardCharsets.UTF_8)
             .forEach(line -> System.out.println("    | " + line));

        /* ============================================================
           场景4: 关闭顺序演示——从外到内
           ============================================================ */
        System.out.println("\n======= 场景4: 关闭顺序 =======");
        System.out.println("  ★ 关闭顺序与构造顺序相反：先关最外层，逐层向内");
        System.out.println("  最外层 close() → 刷缓冲 → 关闭内层 → ... → 关闭文件句柄");
        System.out.println("  用 try-with-resources 声明所有装饰器时，自动处理关闭顺序");
        System.out.println("  ★ 不要在装饰链中间层持有引用并继续使用——一旦外层关闭，内层流也被关闭了");

        System.out.println("\n======= 场景5: 自定义装饰器模式 =======");
        // 自己实现一个简单的装饰器
        Path decoratorFile = Paths.get(LAB_DIR, "custom-decorator.txt");
        try (OutputStream out = new UpperCaseOutputStream(
                new BufferedOutputStream(
                        new FileOutputStream(decoratorFile.toFile())))) {
            out.write("hello world".getBytes(StandardCharsets.UTF_8));
        }
        String decoratorOutput = Files.readString(decoratorFile, StandardCharsets.UTF_8);
        System.out.println("  自定义装饰器（转大写）输出: " + decoratorOutput);
        System.out.println("  ★ 核心 FileOutputStream 不变，UpperOutputStream 增强了转换功能");

        System.out.println("\n======= 关键总结 =======");
        System.out.println("  装饰器模式让 I/O 类库具备了无限的组合能力：");
        System.out.println("  读文件 + 缓冲 + 解压 + 解密 = 多层包装，每一层只管一件事");

        System.out.println("\n======= 清理 =======");
        deleteDirectory(Paths.get(LAB_DIR));
        System.out.println("  已清理");

        System.out.println("\n✓ Lab10 完成！装饰器模式是 Java I/O 的架构精髓——组合优于继承。");
    }

    /**
     * 自定义 OutputStream 装饰器：将写入的字节转为大写
     * 演示装饰器模式的"增强但不修改核心"特性
     */
    static class UpperCaseOutputStream extends OutputStream {
        private final OutputStream out;

        public UpperCaseOutputStream(OutputStream out) {
            this.out = out;
        }

        @Override
        public void write(int b) throws IOException {
            // 将单个字节转大写（只处理 ASCII 字母）
            if (b >= 'a' && b <= 'z') {
                out.write(b - 32);
            } else {
                out.write(b);
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            // 批量转换更高效
            byte[] upper = new byte[len];
            for (int i = 0; i < len; i++) {
                byte bb = b[off + i];
                if (bb >= 'a' && bb <= 'z') {
                    upper[i] = (byte) (bb - 32);
                } else {
                    upper[i] = bb;
                }
            }
            out.write(upper, 0, len);
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }

        @Override
        public void close() throws IOException {
            out.close();
        }
    }

    /** 模拟 CSVPrinter 的简单实现 */
    static class CsvWriter implements Closeable {
        private final BufferedWriter writer;

        CsvWriter(BufferedWriter writer) {
            this.writer = writer;
        }

        void writeRecord(String... fields) throws IOException {
            for (int i = 0; i < fields.length; i++) {
                if (i > 0) writer.write(',');
                writer.write(fields[i]);
            }
            writer.newLine();
        }

        @Override
        public void close() throws IOException {
            writer.close();
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
