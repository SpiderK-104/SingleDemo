package test.java.com.example.io;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Lab09: DataOutputStream/DataInputStream 实战 —— 二进制数据的类型化读写
 * -------------------------------------------------
 * 对应文档：第2.5节 DataOutputStream：二进制数据写入
 *
 * 学习目标：
 * 1. DataOutputStream 提供 writeInt/writeLong/writeUTF 等类型化写入
 * 2. DataInputStream 对应的类型化读取
 * 3. 对比"手动转 byte[]"和"DataOutputStream"的差异
 * 4. 紧凑二进制文件格式的设计
 */
public class Lab09_DataStream {

    private static final String LAB_DIR = "io-lab-temp/lab09";

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Paths.get(LAB_DIR));

        System.out.println("======= 场景1: DataOutputStream 类型化写入 =======");
        System.out.println("  ★ 直接写入 Java 基本类型，省去手动转 byte[] 的步骤\n");

        Path dataFile = Paths.get(LAB_DIR, "binary.dat");

        try (DataOutputStream dos = new DataOutputStream(
                new FileOutputStream(dataFile.toFile()))) {
            dos.writeInt(42);                       // 写入 int（4 字节）
            dos.writeLong(System.currentTimeMillis()); // 写入 long（8 字节）
            dos.writeUTF("操作日志归档");            // 写入 UTF 字符串（2 字节长度 + 内容）
            dos.writeDouble(3.14159);                // 写入 double（8 字节）
            dos.writeBoolean(true);                  // 写入 boolean（1 字节）
        }
        System.out.println("  二进制文件已创建，大小: " + Files.size(dataFile) + " 字节");
        System.out.println("  ★ DataOutputStream 自动处理了各类型的字节编码\n");

        /* ============================================================
           场景2: DataInputStream 读取
           ============================================================ */
        System.out.println("======= 场景2: DataInputStream 类型化读取 =======");
        System.out.println("  ★ 读取顺序必须与写入顺序完全一致\n");

        try (DataInputStream dis = new DataInputStream(
                new FileInputStream(dataFile.toFile()))) {
            int i = dis.readInt();
            long l = dis.readLong();
            String s = dis.readUTF();
            double d = dis.readDouble();
            boolean b = dis.readBoolean();

            System.out.println("  readInt():     " + i);
            System.out.println("  readLong():    " + l + " (时间戳)");
            System.out.println("  readUTF():     " + s);
            System.out.println("  readDouble():  " + d);
            System.out.println("  readBoolean(): " + b);
        }

        System.out.println("\n======= 场景3: 对比 DataOutputStream 与 手动 byte[] =======");
        System.out.println("  ★ 两者结果完全一致，但 DataOutputStream 代码更简洁\n");

        // 方式 A: DataOutputStream
        ByteArrayOutputStream baosA = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baosA)) {
            dos.writeInt(100);
            dos.writeUTF("Hello");
        }
        byte[] bytesA = baosA.toByteArray();

        // 方式 B: 手动构造
        ByteArrayOutputStream baosB = new ByteArrayOutputStream();
        try {
            // writeInt(100) 手动
            int val = 100;
            baosB.write((val >>> 24) & 0xFF);
            baosB.write((val >>> 16) & 0xFF);
            baosB.write((val >>> 8) & 0xFF);
            baosB.write(val & 0xFF);
            // writeUTF("Hello") 手动
            byte[] strBytes = "Hello".getBytes(StandardCharsets.UTF_8);
            baosB.write((strBytes.length >>> 8) & 0xFF);  // 2 字节长度
            baosB.write(strBytes.length & 0xFF);
            baosB.write(strBytes);                         // 内容
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        byte[] bytesB = baosB.toByteArray();

        System.out.println("  DataOutputStream:  " + bytesToHex(bytesA) + " (" + bytesA.length + " 字节)");
        System.out.println("  手动构造:          " + bytesToHex(bytesB) + " (" + bytesB.length + " 字节)");
        System.out.println("  两者相等: " + java.util.Arrays.equals(bytesA, bytesB));

        System.out.println("\n======= 场景4: 实际应用场景 =======");
        System.out.println("  ★ 你项目中的 DnsConfig 使用了 DataOutputStream，但只当普通 OutputStream 用");
        System.out.println("  ★ DataOutputStream 的真正用途——紧凑二进制文件格式设计:");
        System.out.println("    记录头（int 类型） + 时间戳（long） + 内容（UTF 字符串）");
        System.out.println("  ★ 在项目中的潜在场景:");
        System.out.println("    - 自定义日志二进制格式");
        System.out.println("    - 文件头元信息存储");
        System.out.println("    - 与硬件设备通信的二进制协议");

        System.out.println("\n======= 清理 =======");
        deleteDirectory(Paths.get(LAB_DIR));
        System.out.println("  已清理");

        System.out.println("\n✓ Lab09 完成！DataOutputStream 适用于需要将 Java 基本类型紧凑写入文件或网络流的场景。");
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
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
