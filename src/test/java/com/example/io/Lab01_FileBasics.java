package test.java.com.example.io;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;

/**
 * Lab01: File 类实战 —— 文件系统的入口
 * -------------------------------------------------
 * 对应文档：第1节 File
 *
 * 学习目标：
 * 1. 理解 File 是路径名的抽象，不操作文件内容
 * 2. 掌握 mkdirs() 与 mkdir() 的区别
 * 3. 掌握 getAbsolutePath vs getCanonicalPath 的区别
 * 4. 掌握 listFiles() 的 null 判断
 * 5. 理解 delete() 返回 boolean 而非抛异常的设计
 *
 * 运行前无需任何准备，所有文件自动在临时目录创建清理。
 */
public class Lab01_FileBasics {

    /** 临时目录：会在项目根目录下的 io-lab-temp 中操作 */
    private static final String LAB_BASE = "io-lab-temp/lab01";

    public static void main(String[] args) throws IOException {
        // ── 准备工作：确保目录干净 ──
        File baseDir = new File(LAB_BASE);
        deleteRecursively(baseDir);          // 先删旧的
        System.out.println("======= 场景1: mkdirs() 递归创建目录 =======");

        // 创建一个多级目录结构: io-lab-temp/lab01/a/b/c
        File dirC = new File(LAB_BASE + "/a/b/c");
        boolean created = dirC.mkdirs();     // 递归创建所有不存在的父目录
        System.out.println("mkdirs() 创建 " + dirC + " → " + created);
        System.out.println("  父目录存在? " + new File(LAB_BASE + "/a").exists());

        // 对比 mkdir(): 父目录不存在时失败
        File dirFail = new File(LAB_BASE + "/x/y/z");
        boolean fail = dirFail.mkdir();      // 父目录 x/y 不存在 → 失败
        System.out.println("\nmkdir() 创建 " + dirFail + " → " + fail + " (父目录不存在，失败)");

        // ── 创建文件 ──
        System.out.println("\n======= 场景2: createNewFile + exists + delete =======");
        File testFile = new File(LAB_BASE + "/a/b/test.txt");
        boolean fileCreated = testFile.createNewFile();
        System.out.println("createNewFile: " + fileCreated);
        System.out.println("  exists? " + testFile.exists());
        System.out.println("  isFile? " + testFile.isFile());
        System.out.println("  isDirectory? " + testFile.isDirectory());

        // ── getAbsolutePath vs getCanonicalPath ──
        System.out.println("\n======= 场景3: 路径解析 =======");
        File weirdPath = new File(LAB_BASE + "/a/b/.././test.txt");
        System.out.println("  getPath():          " + weirdPath.getPath());
        System.out.println("  getAbsolutePath():  " + weirdPath.getAbsolutePath());
        System.out.println("  getCanonicalPath(): " + weirdPath.getCanonicalPath());
        // ↑ getCanonicalPath 解析了 .. 和 .，返回规范路径

        // ── 文件属性 ──
        System.out.println("\n======= 场景4: 文件属性 =======");
        System.out.println("  length():       " + testFile.length() + " (刚创建，长度为 0)");
        System.out.println("  lastModified(): " + testFile.lastModified());

        // ── listFiles 遍历目录 ──
        System.out.println("\n======= 场景5: listFiles() 遍历目录 =======");
        File aDir = new File(LAB_BASE + "/a");
        File[] subItems = aDir.listFiles();
        // ★ 重要：listFiles 在空目录返回空数组，在 IO 错误时返回 null
        System.out.println("  子文件/目录数: " + (subItems == null ? "null(IO错误)" : subItems.length));
        if (subItems != null) {
            Arrays.stream(subItems).forEach(f ->
                System.out.println("    " + (f.isDirectory() ? "[DIR] " : "[FILE]") + f.getName()));
        }

        // ── FileFilter 过滤 ──
        System.out.println("\n======= 场景6: FileFilter =======");
        File[] dirsOnly = aDir.listFiles(File::isDirectory);  // 只取目录
        System.out.println("  目录数量: " + (dirsOnly == null ? 0 : dirsOnly.length));

        // ── 跨平台分隔符 ──
        System.out.println("\n======= 场景7: 跨平台分隔符 =======");
        System.out.println("  File.separator:       \"" + File.separator + "\"");
        System.out.println("  File.pathSeparator:   \"" + File.pathSeparator + "\"");

        // ── 使用 File.separator 安全拼接路径 ──
        String safePath = "base" + File.separator + "sub" + File.separator + "file.txt";
        System.out.println("  安全拼接的路径: " + safePath);

        // ── deleteOnExit 演示 ──
        System.out.println("\n======= 场景8: deleteOnExit =======");
        File tmp = File.createTempFile("lab01-demo-", ".main.java.com.example.tmp");
        System.out.println("  创建临时文件: " + tmp.getAbsolutePath());
        tmp.deleteOnExit();  // JVM 退出时自动删除
        System.out.println("  deleteOnExit 已注册，JVM 正常退出时会自动删除");

        // ── 清理 ──
        System.out.println("\n======= 清理 =======");
        boolean cleaned = deleteRecursively(baseDir);
        System.out.println("  递归删除 lab01 目录: " + cleaned);
        System.out.println("  exists? " + baseDir.exists());
        System.out.println("\n✓ Lab01 完成！请查阅上方输出理解 File 类的关键行为。");
    }

    /** 递归删除文件或目录（演示 File 的 delete 用法） */
    private static boolean deleteRecursively(File file) {
        if (file == null || !file.exists()) return true;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        return file.delete();
    }
}
