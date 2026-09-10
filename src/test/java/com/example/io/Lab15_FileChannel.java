package com.example.io;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Lab15: FileChannel 实战 —— NIO 的核心通道
 * -------------------------------------------------
 * 对应文档：第2.6节 FileChannel 的潜在应用场景
 *
 * 学习目标：
 * 1. FileChannel 的基本读写
 * 2. ByteBuffer 的 position/limit/flip 操作
 * 3. MappedByteBuffer 内存映射文件（零拷贝到虚拟内存）
 * 4. transferTo 零拷贝（文件→Socket）
 * 5. FileLock 跨进程文件锁
 * 6. 对比传统流式 I/O 和 FileChannel 的差异
 */
public class Lab15_FileChannel {

    private static final String LAB_DIR = "io-lab-temp/lab15";

    public static void main(String[] args) throws IOException {
        Files.createDirectories(Paths.get(LAB_DIR));

        System.out.println("======= 场景1: FileChannel 基本读写 =======");
        System.out.println("  ★ FileChannel + ByteBuffer 是 NIO 的基础模式\n");

        Path dataFile = Paths.get(LAB_DIR, "channel-data.txt");

        // 写文件：FileChannel + ByteBuffer
        try (FileChannel channel = FileChannel.open(dataFile,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {

            // ★ ByteBuffer 分配：堆内分配 vs 直接内存
            ByteBuffer buffer = ByteBuffer.allocate(1024);  // 堆内缓冲区

            // 1. 往 buffer 写入数据
            buffer.put("Hello FileChannel!\n第二行内容。\n".getBytes(StandardCharsets.UTF_8));

            // 2. flip: 从写模式切换到读模式
            // ★ 必须 flip! 否则 position 在末尾，channel 读不到数据
            buffer.flip();

            // 3. 将 buffer 内容写入 channel
            int bytesWritten = channel.write(buffer);
            System.out.println("  写入 " + bytesWritten + " 字节");
        }

        // 读文件
        try (FileChannel channel = FileChannel.open(dataFile, StandardOpenOption.READ)) {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead = channel.read(buffer);  // 从 channel 读到 buffer

            // ★ flip 切换为读模式
            buffer.flip();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);  // 从 buffer 读取到 byte[]

            String content = new String(bytes, StandardCharsets.UTF_8);
            System.out.println("  读取 " + bytesRead + " 字节:");
            System.out.println("  " + content);
        }

        System.out.println("\n======= 场景2: ByteBuffer 位置详解 =======");

        ByteBuffer buf = ByteBuffer.allocate(16);
        System.out.printf("  初始: position=%d, limit=%d, capacity=%d%n",
                buf.position(), buf.limit(), buf.capacity());

        buf.put("Hello".getBytes(StandardCharsets.UTF_8));
        System.out.printf("  put(\"Hello\")后: position=%d (已写 5 字节)%n", buf.position());

        buf.flip();
        System.out.printf("  flip()后: position=%d, limit=%d (切换到读模式)%n",
                buf.position(), buf.limit());

        byte first = buf.get();
        System.out.printf("  读取 1 字节后: position=%d (已读 1 字节), 值='%c'%n",
                buf.position(), (char) first);

        buf.compact();  // 切换到写模式，保留未读数据
        System.out.printf("  compact()后: position=%d, limit=%d (未读数据移到头部，准备继续写)%n",
                buf.position(), buf.limit());

        System.out.println("\n======= 场景3: MappedByteBuffer 内存映射文件 =======");
        System.out.println("  ★ 适合大文件（几百 MB），使用操作系统虚拟内存，不占 JVM 堆\n");

        Path mappedFile = Paths.get(LAB_DIR, "mapped.dat");
        try (RandomAccessFile raf = new RandomAccessFile(mappedFile.toFile(), "rw")) {
            raf.setLength(1024 * 1024);  // 1MB
        }

        try (FileChannel channel = FileChannel.open(mappedFile,
                StandardOpenOption.READ, StandardOpenOption.WRITE)) {

            // ★ map 方法将文件区域映射到虚拟内存
            // ★ READ_ONLY: 只读映射；READ_WRITE: 读写映射；PRIVATE: 写时复制
            MappedByteBuffer mapped = channel.map(
                    FileChannel.MapMode.READ_WRITE, 0, 1024 * 1024);

            // 可以直接对 MappedByteBuffer 进行读写，就像操作内存一样
            mapped.putInt(0, 42);           // 在偏移 0 处写 int
            mapped.putLong(4, 123456789L);  // 在偏移 4 处写 long

            System.out.println("  内存映射写入完成 (1MB 文件映射到虚拟内存)");
            System.out.println("  ★ 对 MappedByteBuffer 的修改会自动同步到文件");
            System.out.println("  ★ 不占用 JVM 堆内存，由操作系统管理");

            // 验证
            int val = mapped.getInt(0);
            long val2 = mapped.getLong(4);
            System.out.println("  mapped.getInt(0):  " + val);
            System.out.println("  mapped.getLong(4): " + val2);
        }

        System.out.println("\n======= 场景4: transferTo 零拷贝 =======");
        System.out.println("  ★ 利用操作系统的 DMA 机制，直接从磁盘→网卡，跳过 JVM 内存\n");

        Path zeroCopySrc = Paths.get(LAB_DIR, "zerocopy-source.txt");
        Files.writeString(zeroCopySrc, "零拷贝传输测试数据", StandardCharsets.UTF_8);

        // ★ 模拟 transferTo 到另一个文件（相当于复制）
        // 实际生产中是 transferTo(SocketChannel) 实现磁盘→网络直接传输
        try (FileChannel srcChannel = FileChannel.open(zeroCopySrc, StandardOpenOption.READ);
             FileChannel dstChannel = FileChannel.open(
                     Paths.get(LAB_DIR, "zerocopy-target.txt"),
                     StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {

            long transferred = srcChannel.transferTo(0, srcChannel.size(), dstChannel);
            System.out.println("  transferTo 零拷贝传输: " + transferred + " 字节");
            System.out.println("  ★ 适合大文件传输场景（如数据库备份下载）");
        }

        System.out.println("\n======= 场景5: FileLock 跨进程文件锁 =======");
        System.out.println("  ★ 对应文档中：防止多个实例同时归档\n");

        Path lockFile = Paths.get(LAB_DIR, "archive.lock");
        try (FileChannel channel = FileChannel.open(lockFile,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {

            // ★ tryLock 非阻塞尝试获取锁
            // ★ lock() 阻塞等待锁——建议用 tryLock
            FileLock lock = channel.tryLock();
            if (lock != null) {
                System.out.println("  ✓ 获取文件锁成功！开始执行归档...");
                System.out.println("  ★ 锁类型: " + (lock.isShared() ? "共享锁" : "排他锁"));
                System.out.println("  ★ 锁范围: " + lock.position() + " ~ " + (lock.position() + lock.size()));

                // 模拟执行归档操作
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                lock.release();
                System.out.println("  锁已释放，归档完成");
            } else {
                System.out.println("  × 其他进程正在归档，跳过本次执行");
            }
        }

        System.out.println("\n======= 场景对比 =======");
        System.out.println("  ┌─────────────────┬──────────────┬──────────────────┐");
        System.out.println("  │     场景        │  Files API   │   FileChannel    │");
        System.out.println("  ├─────────────────┼──────────────┼──────────────────┤");
        System.out.println("  │ 小文件读写      │  ✓ 推荐      │  可用但没必要    │");
        System.out.println("  │ 大文件(>100MB)  │  可能OOM     │  ✓ MappedByteBuf │");
        System.out.println("  │ 零拷贝传输      │  ✗ 不支持    │  ✓ transferTo   │");
        System.out.println("  │ 跨进程锁        │  ✗ 不支持    │  ✓ FileLock      │");
        System.out.println("  │ 代码简洁性      │  ✓ 高        │  低(Buffer管理)  │");
        System.out.println("  └─────────────────┴──────────────┴──────────────────┘");

        System.out.println("\n======= 清理 =======");
        deleteDirectory(Paths.get(LAB_DIR));
        System.out.println("  已清理");

        System.out.println("\n✓ Lab15 完成！FileChannel 适合大文件、零拷贝、文件锁等高级场景。");
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
