package com.example.io;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Lab02: Path / Paths 实战 —— NIO.2 的路径抽象
 * -------------------------------------------------
 * 对应文档：第1.2节 Path 和 Paths
 *
 * 学习目标：
 * 1. Paths.get() 是推荐的路径拼接方式
 * 2. Path.resolve() / relativize() / normalize() 路径运算
 * 3. Path ↔ File 互转
 * 4. Paths.get("") 和 Paths.get(".") 的特殊情况
 */
public class Lab02_PathBasics {

    private static final String LAB_BASE = "io-lab-temp/lab02";

    public static void main(String[] args) throws IOException {
        System.out.println("======= 场景1: 创建 Path 的多种方式 =======");

        // 方式1: 字符串直接转
        Path p1 = Paths.get("io-lab-temp/lab02/file.txt");
        System.out.println("Paths.get(单字符串):     " + p1);

        // 方式2: 变参拼接（自动处理分隔符）
        Path p2 = Paths.get("io-lab-temp", "lab02", "file.txt");
        System.out.println("Paths.get(变参拼接):     " + p2);

        // 方式3: 从 File 转 Path
        File f = new File("io-lab-temp/lab02/file.txt");
        Path p3 = f.toPath();
        System.out.println("File → Path:            " + p3);

        // Path → File
        Path p4 = Paths.get("io-lab-temp/lab02/file.txt");
        File f4 = p4.toFile();
        System.out.println("Path → File:            " + f4);

        System.out.println("\n======= 场景2: 路径运算 =======");

        Path base = Paths.get("io-lab-temp/lab02");
        System.out.println("base: " + base);

        // resolve: 追加子路径
        // ★ 如果传入的是绝对路径，直接返回传入的路径
        // ★ 如果传入的是相对路径，追加到当前路径后面
        Path sub = base.resolve("subdir/data.csv");
        System.out.println("resolve(\"subdir/data.csv\"): " + sub);

        // resolve 传入绝对路径
        Path abs = base.resolve("D:/data/file.txt");
        System.out.println("resolve(绝对路径):      " + abs + " (直接返回传入值)");

        // relativize: 计算相对路径
        Path from = Paths.get("io-lab-temp/lab02/a");
        Path to = Paths.get("io-lab-temp/lab02/b/file.txt");
        Path relative = from.relativize(to);
        System.out.println("\nrelativize 示例:");
        System.out.println("  from: " + from);
        System.out.println("  to:   " + to);
        System.out.println("  相对: " + relative + " (从 a 到 b/file.txt 的相对路径)");

        // normalize: 去除 .. 和 .
        System.out.println("\nnormalize 示例:");
        Path messy = Paths.get("io-lab-temp/./lab02/../lab02/a/../b/file.txt");
        System.out.println("  原始:       " + messy);
        System.out.println("  normalize:  " + messy.normalize());

        System.out.println("\n======= 场景3: 路径的组成部分 =======");
        Path fullPath = Paths.get("io-lab-temp/lab02/subdir/data.csv").toAbsolutePath();
        System.out.println("  完整路径: " + fullPath);
        System.out.println("  getFileName(): " + fullPath.getFileName());
        System.out.println("  getParent():   " + fullPath.getParent());
        System.out.println("  getRoot():     " + fullPath.getRoot());
        System.out.println("  getName(0):    " + fullPath.getName(0) + " (最顶层目录)");
        System.out.println("  getNameCount():" + fullPath.getNameCount());

        System.out.println("\n======= 场景4: Paths.get(\"\") 和 Paths.get(\".\") =======");
        Path empty = Paths.get("");
        System.out.println("Paths.get(\"\"):        \"" + empty + "\"");
        System.out.println("  空路径.toAbsolutePath(): " + empty.toAbsolutePath());
        System.out.println("  空路径.resolve(\"child\"): " + empty.resolve("child"));

        Path dot = Paths.get(".");
        System.out.println("\nPaths.get(\".\"):       \"" + dot + "\"");
        System.out.println("  dot.toAbsolutePath(): " + dot.toAbsolutePath());
        System.out.println("  dot.normalize():      " + dot.normalize());

        System.out.println("\n======= 场景5: toUri() 和路径比较 =======");
        Path pa = Paths.get("io-lab-temp/lab02/a.txt");
        Path pb = Paths.get("io-lab-temp/lab02/b.txt");
        System.out.println("  toUri(): " + pa.toUri());
        System.out.println("  startsWith(io-lab-temp): " + pa.startsWith("io-lab-temp"));
        System.out.println("  endsWith(a.txt):         " + pa.endsWith("a.txt"));

        System.out.println("\n======= 最佳实践: 安全的路径拼接 =======");
        // 推荐：用 Paths.get(变参) 不需要手动拼接分隔符
        // 不推荐：targetPath + File.separator + finalFileName
        Path safeTarget = Paths.get("io-lab-temp", "lab02", "archive", "2026", "07", "data.csv");
        System.out.println("  Paths.get 安全拼接: " + safeTarget);

        System.out.println("\n✓ Lab02 完成！重点记住 Paths.get 多参拼接、resolve、normalize。");
    }
}
