package main.java.com.example.fileIO;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

    private static final int BUFFER_SIZE = 8 * 1024; // 8KB

    /**
     * 压缩目录到 ZIP 文件
     * 压缩就是ZipOutputStream + ZipEntry的组合使用
     *
     * @param sourceDir 源目录路径
     * @param zipFile   目标 ZIP 文件路径
     */
    public static void zipDirectory(Path sourceDir, Path zipFile) throws IOException {
        // 创建目标 ZIP 文件的父目录，确保路径存在
        Files.createDirectories(zipFile.getParent());

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile), StandardCharsets.UTF_8)) {
            zos.setLevel(Deflater.BEST_COMPRESSION);
            Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
                @Override
                // preVisitDirectory 方法在访问目录之前调用，用于处理目录的压缩，保证空目录也会被压缩保留
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                        throws IOException {
                    // 如果目录不是源目录，则创建目录
                    if (!dir.equals(sourceDir)) {
                        // 创建目录的相对路径，并确保使用正斜杠作为分隔符：ZIP 规范：必须用 /，不论环境是什么样的
                        String relativePath =
                                sourceDir.relativize(dir).toString().replace('\\', '/') + "/";
                        // 创建目录的 ZIP 条目
                        ZipEntry entry = new ZipEntry(relativePath);
                        entry.setTime(attrs.lastModifiedTime().toMillis());
                        zos.putNextEntry(entry);
                        zos.closeEntry();
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                // visitFile 方法在访问文件时调用，用于处理文件的压缩
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String relativePath = sourceDir.relativize(file).toString().replace('\\', '/');
                    ZipEntry entry = new ZipEntry(relativePath);
                    entry.setTime(attrs.lastModifiedTime().toMillis());
                    zos.putNextEntry(entry);
                    // 把这个文件的内容，全部拷贝到当前这个 ZIP 条目里，写完就关掉这个条目，然后继续遍历下一个文件。”
                    Files.copy(file, zos);
                    // 关闭当前 ZIP 条目, 准备写入下一个条目
                    zos.closeEntry();
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
    /**
     * 解压 ZIP 文件到指定目录
     * 解压就是ZipInputStream + ZipEntry的组合使用
     *
     * @param zipFile ZIP 文件路径
     * @param destDir 目标目录路径
     */
    public static void unzip(Path zipFile, Path destDir) throws IOException {
        Files.createDirectories(destDir);
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile.toFile()), StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                //destDir是目标目录，entry.getName()是压缩包里的条目名称，resolve方法会把它们拼接成一个完整的路径，normalize方法会清理掉无效的 .. 或 /
                Path entryPath = destDir.resolve(entry.getName()).normalize();
                // ✅ Zip Slip 漏洞防护
                if (!entryPath.startsWith(destDir)) {
                    throw new IOException("非法 ZIP 条目: " + entry.getName());
                }
                // 如果是目录分支，创建目录（包括父目录）
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    // 如果是文件分支，创建父目录，然后写入文件内容
                    Files.createDirectories(entryPath.getParent());
                    try (OutputStream os = Files.newOutputStream(entryPath)) {
                        zis.transferTo(os);
                    }
                    // 设置文件的最后修改时间
                    Files.setLastModifiedTime(entryPath, FileTime.fromMillis(entry.getTime()));
                }
                zis.closeEntry();
            }
        }
    }

    /**
     * 测试入口（跨平台兼容）
     */
    public static void main(String[] args) throws IOException {
        Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "zip_test");
        Path sourceDir = tempDir.resolve("source");
        Path zipFile = tempDir.resolve("archive.zip");
        Path unzipDir = tempDir.resolve("restored");

        Files.createDirectories(sourceDir);
        Files.write(sourceDir.resolve("a.log"), "hello zip".getBytes());
        Files.write(sourceDir.resolve("b.log"), "hello world".getBytes());

        System.out.println("压缩: " + sourceDir + " -> " + zipFile);
        zipDirectory(sourceDir, zipFile);

        System.out.println("解压: " + zipFile + " -> " + unzipDir);
        unzip(zipFile, unzipDir);

        System.out.println("完成!");
    }
}