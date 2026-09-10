package main.java.com.example.stream;

import java.util.*;
import java.util.stream.*;

/**
 * Java Stream 精简入门
 *
 * 把集合想成流水线，Stream操作就是工序：
 *   list.stream().filter(...).map(...).collect(...)
 *
 * 本文覆盖：filter / map / flatMap / sorted / distinct /
 *          limit / skip / forEach / collect / reduce / groupingBy
 */
public class StreamDemo {

    public static void main(String[] args) {

        // ─── 数据准备 ───
        List<String> names = Arrays.asList("Alice", "Bob", "Charlie", "David", "Eve", "Frank");
        List<Integer> nums = Arrays.asList(1, 2, 2, 3, 3, 3, 4, 5, 6);
        List<List<String>> nested = Arrays.asList(
            Arrays.asList("a", "b"), Arrays.asList("c", "d", "e"), Arrays.asList("f")
        );

        // ==========================================================
        //  1. filter —— 筛选（留下符合条件的）
        // ==========================================================
        System.out.println("=== 1. filter ===");
        // 名字长度>=4的
        names.stream()
            .filter(name -> name.length() >= 4)
            .forEach(System.out::println);  // Alice, Charlie, David, Frank

        System.out.println();

        // ==========================================================
        //  2. map —— 转换（A→B，1对1）
        // ==========================================================
        System.out.println("=== 2. map ===");
        // 名字→大写
        names.stream()
            .map(String::toUpperCase)
            .forEach(System.out::println);
        System.out.println();

        // map链式：名字→长度
        names.stream()
            .map(name -> name + "(" + name.length() + ")")
            .forEach(System.out::println);
        System.out.println();

        // ==========================================================
        //  3. flatMap —— 摊平（集合的集合→单个集合）
        // ==========================================================
        System.out.println("=== 3. flatMap ===");
        // nested：[[a,b], [c,d,e], [f]] → [a,b,c,d,e,f]
        nested.stream()
            .flatMap(list -> list.stream())
            .forEach(System.out::print);  // abcdef
        System.out.println("\n");

        // flatMap实用场景：多个词条拆成单词
        Arrays.asList("hello world", "java stream", "flat map")
            .stream()
            .flatMap(s -> Arrays.stream(s.split(" ")))
            .forEach(s -> System.out.print(s + " "));
        System.out.println("\n");

        // ==========================================================
        //  4. sorted —— 排序
        // ==========================================================
        System.out.println("=== 4. sorted ===");
        names.stream()
            .sorted()
            .forEach(s -> System.out.print(s + " "));
        System.out.println("\n");

        // 按长度排序
        names.stream()
            .sorted(Comparator.comparingInt(String::length))
            .forEach(s -> System.out.print(s + " "));
        System.out.println("\n");

        // ==========================================================
        //  5. distinct / limit / skip
        // ==========================================================
        System.out.println("=== 5. distinct / limit / skip ===");
        nums.stream().distinct().forEach(s -> System.out.print(s + " "));  // 1 2 3 4 5 6
        System.out.println("  <- distinct去重");

        nums.stream().limit(3).forEach(s -> System.out.print(s + " "));    // 1 2 2
        System.out.println("  <- limit(3)取前3个");

        nums.stream().skip(3).forEach(s -> System.out.print(s + " "));     // 3 3 4 5 6
        System.out.println("  <- skip(3)跳过前3个");
        System.out.println();

        // ==========================================================
        //  6. collect —— 收集（把结果装到容器里）
        // ==========================================================
        System.out.println("=== 6. collect ===");
        // toList
        List<String> list = names.stream().filter(n -> n.length() > 3).collect(Collectors.toList());
        System.out.println("toList: " + list);

        // toSet（自动去重）
        Set<Integer> set = nums.stream().collect(Collectors.toSet());
        System.out.println("toSet: " + set);

        // toMap（key->value）
        Map<String, Integer> map = names.stream()
            .collect(Collectors.toMap(n -> n, String::length));
        System.out.println("toMap: " + map);

        // joining（拼接字符串）
        String joined = names.stream().collect(Collectors.joining(", "));
        System.out.println("joining: " + joined);

        // groupingBy（分组！很实用）
        Map<Integer, List<String>> byLen = names.stream()
            .collect(Collectors.groupingBy(String::length));
        System.out.println("groupingBy按长度分组: " + byLen);

        // partitioningBy（true/false分区）
        Map<Boolean, List<String>> part = names.stream()
            .collect(Collectors.partitioningBy(n -> n.length() >= 4));
        System.out.println("partitioningBy(>=4): " + part);
        System.out.println();

        // ==========================================================
        //  7. reduce —— 聚合（把一串合成一个）
        // ==========================================================
        System.out.println("=== 7. reduce ===");
        // 求和：0是初始值
        int sum = nums.stream().reduce(0, (a, b) -> a + b);
        System.out.println("求和: " + sum);

        // 找最大值
        int max = nums.stream().reduce(Integer.MIN_VALUE, (a, b) -> a > b ? a : b);
        System.out.println("最大值: " + max);
        System.out.println();

        // ==========================================================
        //  8. 常用快捷方法
        // ==========================================================
        System.out.println("=== 8. 快捷方法 ===");
        System.out.println("count: " + names.stream().filter(n -> n.length() > 3).count());
        System.out.println("anyMatch(有长度>5的吗): " + names.stream().anyMatch(n -> n.length() > 5));
        System.out.println("allMatch(全都>2吗): " + names.stream().allMatch(n -> n.length() > 2));
        System.out.println("findFirst: " + names.stream().filter(n -> n.startsWith("C")).findFirst().orElse("无"));
        System.out.println("min(最短名字): " + names.stream().min(Comparator.comparingInt(String::length)).orElse("无"));
        System.out.println("max(最长名字): " + names.stream().max(Comparator.comparingInt(String::length)).orElse("无"));

        // mapToInt + sum（数值流专用）
        int totalLen = names.stream().mapToInt(String::length).sum();
        System.out.println("名字总长度: " + totalLen);
    }
}
