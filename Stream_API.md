# 11 Stream API

Stream 是 Java 8 引入的一套处理集合的 API，核心思路是把数据操作组装成一条管道（pipeline），每个操作只管自己这一站的转换，不关心上下游。写出来的代码读起来像 SQL 一样：筛选、转换、归并，三步走完。

看一个最直观的例子：

```java
list.stream()
    .filter(item -> item.getPrice() > 100)
    .map(Item::getName)
    .collect(Collectors.toList());
```

filter 筛选出价格大于 100 的商品，map 把 Item 对象转换成名字字符串，collect 把结果收进 List。这条链路上每个方法都返回一个新的 Stream，数据从源头流到终点，中间不产生额外的集合对象。

为什么用 Stream 而不用 for 循环？对比例子最能说明问题。同样是从订单列表里取出金额超过 100 元的订单号，收集到 List 中。用 for 循环：
```java
List<String> orderIds = new ArrayList<>();
for (Order order : orders) {
    if (order.getAmount() > 100) {
        orderIds.add(order.getOrderId());
    }
}
```

用 Stream：

```java
List<String> orderIds = orders.stream()
    .filter(o -> o.getAmount() > 100)
    .map(Order::getOrderId)
    .collect(Collectors.toList());
```

两者的本质区别：

| 维度    | for 循环                 | Stream                       |
| ----- | ---------------------- | ---------------------------- |
| 意图表达  | 告诉计算机"怎么算"（遍历、判断、逐个添加） | 告诉阅读者"要什么"（筛选、转换、收集）         |
| 数据量敏感 | 同样的代码，1 万条和 1 亿条写法一样   | 改成 .parallel() 就能利用多核，不需要改逻辑 |
| 中间变量  | 需要显式声明 ArrayList 和循环变量 | 不产生中间集合，数据在管道内流动             |
| 可读性   | 逻辑散落在每一行赋值语句中          | 操作链从上至下，每个操作干一件事             |

Stream 并不追求比 for 循环跑得更快，它追求的是把表达意图和实现细节分开。当数据量变大需要并行时，加一个 .parallel() 就行，for 循环要改成并行需要重写整个逻辑。

---

## 11.1 创建 Stream

从哪里可以拿到一个 Stream，记牢这几种最常见的来源。

| 创建方式 | 示例                                     | 说明                                     |
| ---- | -------------------------------------- | -------------------------------------- |
| 从集合  | collection.stream()                    | List、Set、Queue 都有 stream() 方法，最常用      |
| 从数组  | Arrays.stream(array)                   | 数组本身没有 stream()，需要用 Arrays 工具类         |
| 从值序列 | Stream.of(a, b, c)                     | 少量已知元素直接组装                             |
| 从文件  | Files.lines(path)                      | 按行读取文本文件，返回 Stream\<String\>，JDK 7 NIO |
| 范围数值 | IntStream.range(0, 100)                | 生成连续整数，不包含上限                           |
| 构建器  | Stream.builder().add(a).add(b).build() | 复杂条件动态构建                               |

```java
// 从集合创建
List<String> list = List.of("a", "b", "c");
Stream<String> s1 = list.stream();

// 从数组创建
String[] arr = {"a", "b", "c"};
Stream<String> s2 = Arrays.stream(arr);

// 从值创建
Stream<String> s3 = Stream.of("a", "b", "c");

// 按行读文件（文件流记得用 try-with-resources 关闭）
try (Stream<String> lines = Files.lines(Paths.get("data.txt"))) {
    lines.forEach(System.out::println);
}

// 整数范围
IntStream.range(0, 5)    // 0，1，2，3，4
IntStream.rangeClosed(0, 5)  // 0，1，2，3，4，5 闭区间
```

---

## 11.2 中间操作

中间操作是连接在管道中间的环节，它们的共同特点是：返回的还是 Stream，所以可以一直链式调下去，但也正因为返回的是 Stream，如果不接终端操作，中间操作什么都不会执行。

### 11.2.1 filter 筛选

接收一个 Predicate（断言），保留满足条件的元素。

```java
// 例：从用户列表中筛选出活跃用户
// 为什么这样写？filter 的参数是 "true 就保留，false 就丢弃"
// 这里 user.isActive() 返回 true/false，正好就是 Predicate 需要的
List<User> activeUsers = users.stream()
    .filter(user -> user.isActive())
    .collect(Collectors.toList());

// 例：筛选同时满足两个条件
// 为什么用 &&？filter 只接受一个 Predicate，多个条件用逻辑运算符组合
List<Order> bigOrders = orders.stream()
    .filter(o -> o.getAmount() > 1000 && o.getStatus() == OrderStatus.PAID)
    .collect(Collectors.toList());

// 例：筛选非空字符串
// 为什么这样写？Objects::nonNull 是一个方法引用，等价于 s -> s != null
List<String> nonEmpty = list.stream()
    .filter(Objects::nonNull)
    .filter(s -> !s.isEmpty())
    .collect(Collectors.toList());

// 例：筛选出指定类型的对象（用 instanceof 模式匹配）
// 为什么用 instanceOf？因为 List<Object> 里混入了不同类型
List<Object> mixed = List.of("hello", 42, "world", 99);
List<String> strings = mixed.stream()
    .filter(x -> x instanceof String)   // 只保留 String 类型
    .map(x -> (String) x)               // 安全地强转
    .collect(Collectors.toList());
// 也可以用 JDK 16+ 的模式匹配：.filter(x -> x instanceof String s).map(x -> s)
```

### 11.2.2 map 转换

把元素从一种类型变成另一种类型，一一映射。

```java
// 例：从用户列表中提取所有用户名
// 为什么这样写？map 的参数是一个 Function<T, R>，T 是输入类型（User），R 是输出类型（String）
// 把 User 变成 String，一一对应，数量不变
List<String> names = users.stream()
    .map(user -> user.getName())
    .collect(Collectors.toList());

// 例：将金额从分转成元（格式化）
// 为什么这样写？map 除了提取字段，也适合做"单位转换"
// 100 分 -> "1.00元"，每个元素独立转换
List<String> formatted = orders.stream()
    .map(o -> String.format("%.2f元", o.getAmountInCents() / 100.0))
    .collect(Collectors.toList());

// 例：多个 map 串联，像流水线一样
// 为什么串起来？每个 map 干一件事，职责单一，好理解好调试
List<String> result = list.stream()
    .map(String::trim)           // 第1站：去前后空格
    .map(String::toLowerCase)    // 第2站：转小写
    .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1)) // 第3站：首字母大写
    .collect(Collectors.toList());

// 例：mapToInt 避免装箱开销
// 为什么用 mapToInt 而不用 map？因为 map 返回 Stream<Integer>，每个数字都要装箱成 Integer 对象
// mapToInt 返回 IntStream，直接操作基本类型 int，省内存
int totalAge = users.stream()
    .mapToInt(User::getAge)
    .sum();  // IntStream 直接有 sum() 方法
```

map 家族还有几个变种：

| 方法                            | 输入          | 输出           | 用途                |
| ----------------------------- | ----------- | ------------ | ----------------- |
| map(Function)                 | Stream\<T\> | Stream\<R\>  | 最通用的类型转换          |
| mapToInt(ToIntFunction)       | Stream\<T\> | IntStream    | 避免装箱，提高数值计算性能     |
| mapToLong(ToLongFunction)     | Stream\<T\> | LongStream   | 同上，输出为 long 原始流   |
| mapToDouble(ToDoubleFunction) | Stream\<T\> | DoubleStream | 同上，输出为 double 原始流 |
| mapToObj(IntFunction)         | IntStream   | Stream\<R\>  | 原始流转回对象流          |

map 和 mapToObj 在功能上没有区别，区别在于调用 mapToObj 的 Stream 本身是原始流（IntStream），所以才需要 toObj。

### 11.2.3 flatMap 扁平化

当每个元素需要展开成多个元素时，用 flatMap。

```java
// 例：每个句子拆成单词
// 为什么用 flatMap 而不是 map？
// map 会得到 Stream<String[]>（每个句子对应一个单词数组），不是我们想要的
// flatMap 先把每个句子变成 Stream<String>（单词流），再把多个单词流合并成一个流
List<String> sentences = List.of("Hello world", "Java stream", "flat map");
List<String> words = sentences.stream()
    .flatMap(s -> Arrays.stream(s.split(" ")))
    .collect(Collectors.toList());
// [Hello, world, Java, stream, flat, map]

// 例：一对多关系——查询所有订单的所有商品
// 为什么用 flatMap？一个订单有多个商品（List<Item>）
// 我们的目标是把所有的商品放到一起，而不是保留"订单->商品"的嵌套关系
List<Item> allItems = orders.stream()
    .flatMap(order -> order.getItems().stream())
    .collect(Collectors.toList());

// 例：flatMap 结合 distinct，查所有订单中涉及的商品分类
// 为什么 flatMap 完还能继续链式调用？因为 flatMap 返回的还是 Stream
List<String> categories = orders.stream()
    .flatMap(order -> order.getItems().stream())
    .map(Item::getCategory)
    .distinct()
    .collect(Collectors.toList());

// 例：过滤空值（JDK 9+ 写法）
// 为什么用 flatMap + Optional::stream？比 filter(Objects::nonNull) 更通用
// 尤其适合链式调用中某个中间步骤可能返回 null 的场景
List<String> nonNull = list.stream()
    .flatMap(item -> Optional.ofNullable(item.getName()).stream())
    .collect(Collectors.toList());
```

### 11.2.4 distinct 去重

按元素的 equals() 去重。

```java
// 例：去除重复数字
// 为什么这样写？distinct 基于 equals() 判断重复
// 对于 Integer，equals 比较的是数值本身
List<Integer> nums = List.of(1, 2, 2, 3, 3, 3);
nums.stream().distinct().collect(Collectors.toList()); // [1, 2, 3]

// 例：按对象某个字段去重（利用 map + distinct + collect 技巧）
// 为什么不用 distinct 直接去重？因为默认是按对象引用去重
// 想要按名字去重，需要先 map 到名字字段，distinct 完再恢复到原对象
// 不过更常见的做法是使用 groupingBy + reducing
List<Person> uniqueByName = persons.stream()
    .filter(distinctByKey(Person::getName))  // 见下方辅助方法
    .collect(Collectors.toList());

// 辅助方法：按提取的 key 去重
public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
    Set<Object> seen = ConcurrentHashMap.newKeySet();
    return t -> seen.add(keyExtractor.apply(t));
}
```

### 11.2.5 sorted 排序

不带参数按自然顺序排，带 Comparator 自定义排序。

```java
// 例：默认自然排序（字符串按字母顺序）
// 为什么这样写？String 实现了 Comparable 接口，sorted() 就用它的 compareTo
list.stream()
    .sorted()
    .collect(Collectors.toList());

// 例：按长度排序（从小到大）
// 为什么用 Comparator.comparingInt？提供了类型安全的比较器构造方式
// 比起手写 (a, b) -> a.length() - b.length()，更不容易溢出
list.stream()
    .sorted(Comparator.comparingInt(String::length))
    .collect(Collectors.toList());

// 例：按年龄降序排列（从大到小）
// 为什么用 reversed()？comparingInt 默认升序，reversed() 反转
// 比单独写一个 Comparator 更清晰
users.stream()
    .sorted(Comparator.comparingInt(User::getAge).reversed())
    .collect(Collectors.toList());

// 例：多条件排序（先按年龄，年龄相同按名字）
// 为什么用 thenComparing？连缀多个比较条件，比写嵌套 if-else 清爽太多
users.stream()
    .sorted(Comparator.comparingInt(User::getAge)
        .thenComparing(User::getName))
    .collect(Collectors.toList());

// 例：自然排序的 null 安全处理
// 为什么用 nullsLast？如果列表里有 null，直接 sorted() 会 NPE
list.stream()
    .sorted(Comparator.nullsLast(Comparator.naturalOrder()))
    .collect(Collectors.toList());
```

### 11.2.6 peek 偷看

peek 接收一个 Consumer，对流中的每个元素执行副作用（通常用来调试），不改变元素本身。

```java
// 例：调试——观察 filter 前后的数据变化
// 为什么用 peek 而不是 forEach？peek 是中间操作，可以插在管道中间观察数据
// forEach 是终端操作，放进去就终结管道了
List<String> result = list.stream()
    .peek(s -> System.out.println("原始: " + s))
    .filter(s -> s.length() > 3)
    .peek(s -> System.out.println("过滤后: " + s))
    .map(String::toUpperCase)
    .peek(s -> System.out.println("大写后: " + s))
    .collect(Collectors.toList());

// 例：日志记录（生产慎用，peek 主要用于调试）
list.stream()
    .peek(item -> log.debug("Processing: {}", item))
    .filter(...)
    .collect(...);
```

要注意的是，peek 是中间操作，没有终端操作时它不会执行，和 forEach 不一样。另外，peek 不应该用来修改元素状态，那是 map 的活。

### 11.2.7 limit 和 skip

limit 截取前 N 个，skip 跳过前 N 个。

```java
// 例：取前 10 条（最常见的分页）
// 为什么这样写？limit(10) 拿到前10个，和数据库的 LIMIT 语义一致
list.stream()
    .limit(10)
    .collect(Collectors.toList());

// 例：分页（第 2 页，每页 10 条）
// 为什么用 skip + limit？skip 跳过前一页的数据，limit 限制本页大小
// 等价于 SQL 的 LIMIT 10 OFFSET 10
list.stream()
    .skip(10)
    .limit(10)
    .collect(Collectors.toList());

// 例：取评分最高的 3 个
// 为什么先排序再 limit？先排好序，再从顶部取3个
// 但如果数据量非常大，排序的开销很大，要考虑性能
products.stream()
    .sorted(Comparator.comparingInt(Product::getScore).reversed())
    .limit(3)
    .collect(Collectors.toList());

// 例：skip + limit 在空列表上安全吗？安全
// skip(10).limit(10) 对一个只有3个元素的列表执行，结果是一个空列表，不会抛异常
```

### 11.2.8 takeWhile 和 dropWhile（JDK 9+）

takeWhile 从头开始取元素，直到第一个不满足条件的元素为止，然后直接截断。dropWhile 正好相反，从头开始丢弃元素，直到第一个不满足条件的元素，然后保留剩下的所有。

```java
// 例：从有序列表里取所有小于 100 的商品（价格已经按从小到大排好序）
// 为什么用 takeWhile 而不是 filter？filter 会遍历全部元素
// takeWhile 在遇到第一个 >= 100 的商品时就停止，后面的不再检查
// 如果数据是"前面符合、后面不符合"的有序结构，takeWhile 效率更好
List<Item> cheapItems = items.stream()
    .takeWhile(item -> item.getPrice() < 100)
    .collect(Collectors.toList());

// 例：从日志中找到从 "ERROR" 开始的所有行
// 为什么用 dropWhile？日志文件是按时间顺序排列的
// 从第一条 ERROR 开始，后面的日志都有分析价值
List<String> errorLogs = logs.stream()
    .dropWhile(line -> !line.contains("ERROR"))
    .collect(Collectors.toList());
// 如果日志中根本没有 ERROR，返回空列表而非 null
```

takeWhile 和 filter 的区别：filter 会遍历所有元素，takeWhile 在遇到第一个不满足条件的元素后就停止处理。对于有序的"前面符合、后面不符合"的数据，takeWhile 效率更高且语义更清楚。

---

## 11.3 终端操作

终端操作是管道终点，它触发整个流式计算的执行。一个 Stream 只能执行一次终端操作，执行完之后 Stream 就关闭了，不能再用。

### 11.3.1 forEach / forEachOrdered

forEach 对流中的每个元素执行操作。在并行流中，forEach 不保证处理顺序，forEachOrdered 保证顺序。

```java
// 例：打印每个元素
// 为什么用 forEach 而不是 for-each 循环？这只是语法糖，没有性能差异
// 在写 Stream 管道时末尾用 forEach 比切回 for 循环更一致性
list.stream().forEach(System.out::println);

// 例：批量更新数据库
// 为什么在 forEach 里调 service？forEach 适合执行"有副作用"的操作
// 但注意：如果数据量大，建议分批处理，不要一次性 forEach 成千上万条更新
users.stream()
    .filter(u -> u.getStatus() == UserStatus.INACTIVE)
    .forEach(u -> userService.deactivate(u.getId()));

// 例：并行流中保持输出顺序
// 为什么用 forEachOrdered？并行流中 forEach 不保证处理顺序
// 如果输出顺序必须和源数据一致，用 forEachOrdered
list.parallelStream()
    .forEachOrdered(System.out::println);
```

### 11.3.2 collect 收集

把 Stream 中的元素收进一个容器（通常就是集合）。它是 Stream 里用得最多的终端操作。

```java
// 例：收进 List
// 为什么用 toList() 不用 new ArrayList<>() 逐个 add？
// Stream 帮你处理了添加逻辑，你不用关心容器的扩容、添加顺序
List<String> list = stream.collect(Collectors.toList());

// 例：收进 Set（自动去重）
// 为什么用 toSet()？如果去重是你"顺便"需要的功能
// 但 toSet() 的去重依赖于元素的 hashCode/equals，自定义对象要小心
Set<String> set = stream.collect(Collectors.toSet());

// 例：收进 Map（必须指定 key 和 value 的映射函数）
// 为什么 key 用 Function.identity()？identity() 返回"元素自身"，等价于 t -> t
// value 用 String::length，提取名字的长度
Map<String, Integer> map = stream.collect(Collectors.toMap(
    Function.identity(),    // key 就是元素本身
    String::length          // value 是长度
));

// 例：toMap 处理重复 key（合并冲突）
// 为什么需要第三个参数？因为 toMap 不允许重复 key
// (existing, incoming) -> existing 表示遇到重复时保留旧的，丢弃新的
// 也可以写成 (a, b) -> a + b 来累加值
Map<String, Double> categoryTotal = items.stream()
    .collect(Collectors.toMap(
        Item::getCategory,
        Item::getPrice,
        Double::sum              // 同品类累加金额
    ));

// 例：控制 Map 的具体实现类型
// 为什么传第四个参数？默认返回 HashMap，你想要 TreeMap（按 key 排序）就传
Map<String, Double> sortedMap = items.stream()
    .collect(Collectors.toMap(
        Item::getCategory,
        Item::getPrice,
        Double::sum,
        TreeMap::new             // 指定返回 TreeMap
    ));
```

### 11.3.3 toList（JDK 16+）

JDK 16 在 Stream 上直接加了 toList() 方法，不需要再调用 .collect(Collectors.toList())。

```java
// JDK 16+
// 为什么有 toList() 还要保留 collect(toList())？
// toList() 更短，但它返回的 List 是不可变的
// 如果你确定后续不会增删元素，用 toList()；如果需要可变 List，用 collect(toList())
List<String> list = stream.toList();            // 不可变
List<String> list2 = stream.collect(Collectors.toList());  // 可变

list.add("x");  // 前者抛 UnsupportedOperationException
                // 后者可以正常添加
```

### 11.3.4 reduce 归约

reduce 把 Stream 中的元素反复组合，最终得到单一结果。

```java
// 例：求和——计算所有订单的金额总和
// 为什么 reduce(0, (a, b) -> a + b) 这样写？
// 0 是初始值（identity），空流时返回 0
// (a, b) -> a + b 是累加器——a 是到目前为止的累加结果，b 是当前元素
// 第一次：a=0（初始值），b=第一个元素的值，相加得新 a
// 第二次：a=上一次结果，b=第二个元素，相加得新 a
// 以此类推，直到所有元素处理完
BigDecimal total = amounts.stream()
    .reduce(BigDecimal.ZERO, BigDecimal::add);

// 例：找出最大值
// 为什么这样写？reduce 的累加器用 Integer::max
// 每次比较当前最大值和下一个元素，较大的成为新最大值
// 返回 Optional 是因为流可能为空，没有初始值的情况下要处理"没有结果"的情况
Optional<Integer> max = list.stream()
    .reduce(Integer::max);

// 例：拼接字符串（用 reduce 手动实现）
// 为什么不用 Collectors.joining？这里只是演示 reduce 的能力
// 实际拼接字符串应该用 joining，因为 reduce 做字符串拼接每次都会创建新 String，效率低
Optional<String> concat = list.stream()
    .reduce((a, b) -> a + "," + b);

// 例：求乘积
// 为什么初始值是 1？数学上，乘法单位元是 1，0 乘任何数都得 0
int product = IntStream.range(1, 6)  // 1,2,3,4,5
    .reduce(1, (a, b) -> a * b);     // 1*1*2*3*4*5 = 120
```

### 11.3.5 findFirst / findAny

```java
// 例：获取符合条件的第一个元素
// 为什么用 findFirst 而不是 filter + limit(1) + collect？
// 因为 findFirst 语义更直接，而且"找到第一个就停止"（短路操作）
// 返回 Optional 是因为可能没有符合条件的元素
Optional<User> firstActive = users.stream()
    .filter(User::isActive)
    .findFirst();

// 例：并行流中用 findAny 替代 findFirst
// 为什么用 findAny？并行流中 findFirst 需要保持顺序，限制了并行效率
// findAny 返回最快找到的那个，不关心顺序
// 在不需要顺序的场景下，findAny 性能更好
Optional<User> anyActive = users.parallelStream()
    .filter(User::isActive)
    .findAny();
```

### 11.3.6 匹配操作

anyMatch、allMatch、noneMatch 接收 Predicate，返回 boolean。

| 方法 | 何时返回 true | 何时短路 |
|------|---------------|----------|
| anyMatch | 只要有一个元素满足条件 | 找到一个满足的就立即返回 |
| allMatch | 所有元素都满足条件 | 找到一个不满足的就立即返回 |
| noneMatch | 所有元素都不满足条件 | 找到一个满足的就立即返回 |

```java
// 例：是否有金额超过 1000 的订单？
// 为什么用 anyMatch？这是"是否存在"型的判断
// 一旦找到一个满足条件的，立即返回 true，后面的元素不再处理（短路）
boolean hasExpensive = orders.stream()
    .anyMatch(o -> o.getAmount() > 1000);

// 例：是不是所有商品都在库存中？
// 为什么用 allMatch？这是"全部满足"型的判断
// 一旦发现任意一个库存不足的，立即返回 false
boolean allInStock = items.stream()
    .allMatch(Item::isInStock);

// 例：是不是没有任何用户被禁用？
// 为什么用 noneMatch？这是"完全不满足"型的判断
// 等价于 !users.stream().anyMatch(User::isDisabled)
boolean noneDisabled = users.stream()
    .noneMatch(User::isDisabled);
```

### 11.3.7 count / min / max

```java
// 例：计算符合条件的元素个数
// 为什么用 count 而不是 collect(toList()).size()？
// count 不需要真的创建集合，更省内存
long adultCount = users.stream()
    .filter(u -> u.getAge() >= 18)
    .count();

// 例：找出最贵的商品
// 为什么返回 Optional？价格列表可能是空的
Optional<Product> mostExpensive = products.stream()
    .max(Comparator.comparingDouble(Product::getPrice));

// 例：按自定义比较器找出最短的名字
// 为什么 min 和 max 参数是一样的 compare？取决于你想取头还是取尾
Optional<String> shortest = names.stream()
    .min(Comparator.comparingInt(String::length));
```

---

## 11.4 常用 Collectors

Collectors 工具类提供了大量工厂方法，用来指定 collect 的行为。

### 11.4.1 toList / toSet / toMap

最基础的三个，把元素收进 List、Set 或 Map。

```java
// toList：保留顺序，允许重复
.collect(Collectors.toList());

// toSet：去重，无顺序保证
.collect(Collectors.toSet());

// toMap：指定 key 和 value 映射，重复 key 抛异常
.collect(Collectors.toMap(Function.identity(), String::length));
```

toMap 必须指定 key 和 value 的映射函数。如果 key 有重复，会抛出 IllegalStateException，需要传入合并函数来处理冲突：

```java
// 例：按分类累加金额（重复 key 时求和）
// 为什么传第三个参数？商品可能按 category 分类，同分类有多个商品
// 如果 key 重复（同分类），就把前一个 price + 当前 price
.collect(Collectors.toMap(
    Item::getCategory,      // key
    Item::getAmount,        // value
    Integer::sum            // 同品类累加金额
));
```

### 11.4.2 joining

专门用来把字符串元素拼接成一个大字符串。

```java
// 例：将名字列表拼接成 "张三,李四,王五" 格式
// 为什么用 joining 而不是手动 for 循环拼接？
// joining 内部用 StringBuilder，比用 String += 拼接高效得多
String names = users.stream()
    .map(User::getName)
    .collect(Collectors.joining(", "));

// 例：格式化为 [A, B, C] 样式
// 三个参数分别是：分隔符、前缀、后缀
// 等价于 "[" + list.stream().collect(joining(", ")) + "]"
String formatted = list.stream()
    .collect(Collectors.joining(", ", "[", "]"));

// 例：拼成 SQL IN 子句
// 为什么用 joining？简单直接的字符串拼接场景
String ids = users.stream()
    .map(u -> "'" + u.getId() + "'")
    .collect(Collectors.joining(", ", "IN (", ")"));
// 输出：IN ('1', '2', '3')
```

### 11.4.3 groupingBy 分组

按某个属性分组，结果是一个 Map，key 是分组依据，value 是该组元素的 List。

```java
// 例：按用户状态分组
// 为什么用 groupingBy 而不是自己写 Map 循环？
// groupingBy 自动创建 Map，自动归类，不用写 map.computeIfAbsent
// .collect(groupingBy(User::getStatus)) 等价于：
//   Map<Status, List<User>> map = new HashMap<>();
//   for (User u : users) {
//       map.computeIfAbsent(u.getStatus(), k -> new ArrayList<>()).add(u);
//   }
Map<UserStatus, List<User>> byStatus = users.stream()
    .collect(Collectors.groupingBy(User::getStatus));
// 结果：{ACTIVE=[...], INACTIVE=[...], BANNED=[...]}

// 例：分组后统计每组数量（groupingBy + counting）
// 为什么传第二个参数？groupingBy 的第二个参数是"下游收集器"
// 默认下游是 toList()，换成 counting() 就变成计数
Map<String, Long> countByCategory = items.stream()
    .collect(Collectors.groupingBy(
        Item::getCategory,
        Collectors.counting()
    ));

// 例：分组后找出每组的最大值
// 为什么用 maxBy？下游收集器可以替换成任何 Collector
// maxBy 返回 Optional 是因为该组可能为空（虽然这里不会）
Map<String, Optional<Product>> mostExpensive = products.stream()
    .collect(Collectors.groupingBy(
        Product::getCategory,
        Collectors.maxBy(Comparator.comparingDouble(Product::getPrice))
    ));

// 例：分组后把每组的价格累加
// 为什么用 mapping + toList 或 summingDouble？
// mapping 是先把元素转换再收集
Map<String, Double> totalByCategory = items.stream()
    .collect(Collectors.groupingBy(
        Item::getCategory,
        Collectors.summingDouble(Item::getPrice)
    ));

// 例：多级分组（groupingBy 嵌套）
// 为什么嵌套？先按状态分组，再按城市分组
// 注意内层 groupingBy 的第二个参数就是另一个 groupingBy
Map<UserStatus, Map<String, List<User>>> byStatusAndCity = users.stream()
    .collect(Collectors.groupingBy(
        User::getStatus,
        Collectors.groupingBy(User::getCity)
    ));

// 例：控制返回的 Map 类型
// 第三个参数指定 Map 实现：如果想结果按 key 排序，传 TreeMap::new
Map<UserStatus, List<User>> sortedGroup = users.stream()
    .collect(Collectors.groupingBy(
        User::getStatus,
        TreeMap::new,          // 指定 Map 实现
        Collectors.toList()    // 下游收集器
    ));
```

### 11.4.4 partitioningBy 分区

partitioningBy 接收一个 Predicate，把元素分成 true 和 false 两个区。相比 groupingBy，分区键固定只有两个值，生成的 Map 类型是 Map\<Boolean, List\<T\>\>。

```java
// 例：将订单分为"已支付"和"未支付"两组
// 为什么用 partitioningBy 而不是 groupingBy？
// 分区只有两个值（true/false），Map 的 key 类型是 Boolean
// 而 groupingBy 的 key 类型不限，分区数不限
Map<Boolean, List<Order>> paidPartition = orders.stream()
    .collect(Collectors.partitioningBy(o -> o.getStatus() == OrderStatus.PAID));
List<Order> paid = paidPartition.get(true);
List<Order> unpaid = paidPartition.get(false);

// 例：分区后统计各分区数量
Map<Boolean, Long> countPartition = orders.stream()
    .collect(Collectors.partitioningBy(
        o -> o.getAmount() > 1000,
        Collectors.counting()
    ));
System.out.println("大额订单数: " + countPartition.get(true));
System.out.println("小额订单数: " + countPartition.get(false));
```

### 11.4.5 summarizingInt 统计

一次遍历把多个统计指标算出来，包括：count、sum、min、average、max。

```java
// 例：统计商品价格的综合信息
// 为什么用 summarizingInt？如果一个统计还不够，但又不想遍历多次
// 它一次性算出 count/sum/min/average/max，避免多次遍历集合
IntSummaryStatistics stats = items.stream()
    .collect(Collectors.summarizingInt(Item::getPrice));

// 直接调用各个统计值
long count = stats.getCount();     // 商品总数
long sum = stats.getSum();         // 价格总和
int min = stats.getMin();          // 最低价
double avg = stats.getAverage();   // 平均价
int max = stats.getMax();          // 最高价

// 例：打印统计摘要
// toString() 本身就格式化了，适合打日志
System.out.println(stats);
// 输出：IntSummaryStatistics{count=100, sum=50000, min=10, average=500.00, max=5000}
```

---

## 11.5 并行流

并行流是 Stream API 里最容易被滥用、也最容易出问题的特性。

**怎么用**

把 .stream() 换成 .parallelStream()，或者在任意中间操作后面加 .parallel()：

```java
// 两种方式等效
list.parallelStream().filter(...).collect(...);
list.stream().parallel().filter(...).collect(...);
```

**工作原理**

并行流底层使用 ForkJoinPool 公共线程池。

```java
// ForkJoinPool 的公共池
ForkJoinPool.commonPool();
```

这个池默认的并行度等于 CPU 核心数减 1。可以通过 JVM 参数调整：

```java
-Djava.util.concurrent.ForkJoinPool.common.parallelism=8
```

大任务被拆分成小任务分到各个线程执行，结果再合并。拆分的策略是 Spliterator（可拆分迭代器），不同集合的拆分效率不同：ArrayList 拆分代价很低，LinkedList 和基于树的集合拆分代价较高。

*什么时候该用并行流*

有一个简单的判断方法：数据量大且操作 CPU 密集，或者数据量大且每个元素的操作独立不需要共享数据，用并行流才有收益。

例子：大量数值计算、独立批次处理、图像处理、日志解析。

```java
// 例：并行生成密钥对（每个批次独立，无共享状态）
// 为什么这里适合并行？每个 batchIndex 之间完全独立
// 没有任何两个线程操作同一个对象
IntStream.range(0, cores)
    .parallel()
    .mapToObj(batchIndex -> keyGeneratorService.getKeyPairList(batchIndex, batchSize))
    .toList();
```

*什么时候不要用*

先看几个反例。

反模式一：数据量太小。几百个元素的集合用并行流，线程创建和任务拆分的开销比计算本身还大，反而比串行慢。

```java
// 反例——数据量小得不值得并行
List<String> list = List.of("a", "b", "c");
list.parallelStream().map(String::toUpperCase).toList(); // 3个元素，并行毫无意义
```

反模式二：操作里有 IO 等待（数据库查询、HTTP 调用）。并行流用的 ForkJoinPool 是固定的，如果所有线程都在等 IO，整个应用的其他并行流也会被阻塞。IO 密集型场景用 CompletableFuture + 自定义线程池更合适。

反模式三：操作之间有共享可变状态。

```java
// 反面案例 - 共享问题
List<Integer> list = new ArrayList<>();
IntStream.range(0, 10000)
    .parallel()
    .forEach(i -> list.add(i));  // ArrayList 不是线程安全的，会丢数据或抛异常
```

改成线程安全的容器也只是掩盖问题，更好的做法是用 collect（无共享状态）：

```java
// 正确做法——用 collect，每个线程收集到自己的容器再合并
List<Integer> list = IntStream.range(0, 10000)
    .parallel()
    .boxed()
    .collect(Collectors.toList());
```

反模式四：forEach 中用并行流操作同一个数据库连接或文件句柄，这些资源本身不是线程安全的。

**并行流的线程安全问题**

并行流本身不提供任何线程安全保证。所有线程安全的责任在操作函数上。有一个简单的原则：你在 Lambda 里引用了外部变量，就要问自己"多个线程同时操作这个变量安全吗"。

具体来说：

| 引用方式 | 是否安全 | 说明 |
|----------|----------|------|
| Lambda 参数（如 filter 中的 item） | 安全 | 每个线程拿到的 item 是独立的 |
| 外部集合只读访问 | 安全 | 只看不改，没有竞争 |
| 外部集合在 forEach 中修改 | 不安全 | 多线程同时 add，轻则丢数据重则抛异常 |
| 外部原子类或锁保护的变量 | 安全 | 但要考虑性能，锁会抵消并行收益 |
| collect 收集到新集合 | 安全 | Collector 设计上保证了线程安全，每个线程收集到自己的容器再合并 |

---

## 11.6 常见坑和反模式

### 11.6.1 坑一：Stream 是一次性的

Stream 用过一次就关闭了，不能重复使用。

```java
Stream<String> stream = list.stream();
stream.forEach(System.out::println);
long count = stream.count();  // IllegalStateException: stream has already been operated upon or closed
```

需要重复处理同一个数据源的话，每次重新调用 .stream() 创建新 Stream。

### 11.6.2 坑二：没有终端操作，中间操作不执行

Stream 是惰性求值的。没有终端操作，整条管道什么都不会做。

```java
list.stream()
    .filter(s -> {
        System.out.println("不会打印");
        return s.length() > 2;
    });
// 没有 forEach、collect 等终端操作，filter 永远不会执行
```

### 11.6.3 坑三：无限流没加 limit

```java
// 会无限生成下去，直到内存溢出
Stream.generate(Math::random)
    .forEach(System.out::println);
```

用 limit 截断：

```java
Stream.generate(Math::random)
    .limit(10)
    .forEach(System.out::println);
```

### 11.6.4 坑四：在 forEach 中修改外层集合

```java
// 反模式
List<String> result = new ArrayList<>();
list.stream()
    .filter(s -> s.length() > 2)
    .forEach(s -> result.add(s));  // 串行可能好，并行一定出问题
```

正确做法是用 collect：

```java
List<String> result = list.stream()
    .filter(s -> s.length() > 2)
    .collect(Collectors.toList());
```

### 11.6.5 坑五：toList() 返回的是不可变 List（JDK 16+）

```java
List<String> list = stream.toList();  // 不可变 List
list.add("x");  // UnsupportedOperationException
```

如果需要可变 List，用 .collect(Collectors.toList())。

### 11.6.6 坑六：并行流 + 顺序操作互相抵消

并行流中显式要求顺序（findFirst、limit、sorted）会降低并行效率，因为这些操作需要在结果上恢复顺序。

```java
// 并行但需要保持顺序，排序操作抵消了并行优势
list.parallelStream()
    .sorted()
    .findFirst();
```

如果不需要顺序，用 findAny 代替 findFirst。

---

## 11.7 原始流 vs 对象流

Stream\<Integer\> 每个元素都是一个 Integer 对象，涉及装箱拆箱。IntStream、LongStream、DoubleStream 直接操作基本类型，避免内存开销。

| 对比维度 | 对象流 Stream\<Integer\> | 原始流 IntStream |
|----------|------------------------|------------------|
| 元素存储 | Integer 对象（装箱） | int（基本类型） |
| 数值操作 | 需要 .mapToInt() 转原始流才有 sum() | 直接有 sum()、average()、summaryStatistics() |
| 内存占用 | 每个元素多 16-24 字节对象头 | 连续内存，无额外开销 |
| 适用场景 | 少量数据或非数值操作 | 大量数值计算 |

```java
// 例：对象流（Integer 对象）
// 为什么这样写会有性能开销？每个 int 先装箱成 Integer
// mapToInt 再拆箱，多余的操作
int sum1 = list.stream().mapToInt(Integer::intValue).sum();

// 例：原始流（直接操作 int）
// 为什么更好？从一开始就是 IntStream，没有装箱拆箱
// 而且直接有 sum()、average()、min()、max() 方法
int sum2 = IntStream.range(0, 1000).sum();
double avg = IntStream.range(0, 1000).average().orElse(0);

// 例：原始流一次性统计
IntSummaryStatistics stats = IntStream.of(1, 2, 3, 4, 5).summaryStatistics();
// stats.getCount() = 5, stats.getSum() = 15
// stats.getMin() = 1, stats.getMax() = 5, stats.getAverage() = 3.0

// 例：什么时候该用原始流，什么时候用对象流？
// 大量数值计算（如处理 100 万个数字）-> 用 IntStream
// 少量数据或非数值操作（如找名字列表最长的）-> Stream<String> 就够
```

---

## 11.8 方法引用

方法引用是 Lambda 的一种简写形式，当你 Lambda 体只是调用一个现有方法时，可以用双冒号替代。

| 类型 | Lambda 写法 | 方法引用写法 |
|------|-------------|--------------|
| 静态方法 | s -> Integer.parseInt(s) | Integer::parseInt |
| 实例方法（某类） | s -> s.length() | String::length |
| 实例方法（外部对象） | x -> list.add(x) | list::add |
| 构造方法 | name -> new Person(name) | Person::new |

```java
// 例：静态方法引用
// 为什么能简写？Lambda 体只有调用一个静态方法，方法签名完全匹配
// s -> Integer.parseInt(s) 等价于 Integer::parseInt
list.stream()
    .map(Integer::parseInt)
    .collect(Collectors.toList());

// 例：实例方法引用
// 为什么 String::length 可以？因为输入是 String，调的是 String 的实例方法
// s -> s.length() 等价于 String::length
list.stream()
    .sorted(Comparator.comparing(String::length))
    .collect(Collectors.toList());

// 例：构造方法引用
// 为什么 Person::new 可以？因为 Function<String, Person> 的输入是 String
// 调用 Person(String name) 构造器正好匹配
list.stream()
    .map(Person::new)
    .collect(Collectors.toList());

// 例：方法引用不适合的场景
// 当参数需要做额外处理时，Lambda 更清楚
// 下面这个用方法引用反而难懂：
list.stream()
    .map(String::toLowerCase)  // OK，一眼就懂
    .collect(Collectors.toList());

// 而这样写就过头了（更推荐 Lambda）：
// list.stream().map(this::complexMethodWithMultipleArgs)  // 不如写 Lambda
```

---

## 11.9 Stream 与 Optional

Stream 的终端操作很多返回 Optional：

```java
Optional<String> first = list.stream().findFirst();
Optional<String> max = list.stream().max(String::compareTo);
```

Optional 的常见处理方式：

```java
// 有值就打印
opt.ifPresent(System.out::println);

// 有值就用，没值用默认值
String value = opt.orElse("default");

// 有值就用，没值通过方法计算（延迟求值，性能更好）
String value = opt.orElseGet(() -> computeDefault());

// 没值抛异常
String value = opt.orElseThrow(() -> new RuntimeException("值不存在"));

// 有值才执行后续操作（链式处理）
opt.map(String::toUpperCase)
   .filter(s -> s.length() > 3)
   .ifPresent(System.out::println);
```

Optional 也可以和 Stream 联动。JDK 9 给 Optional 加了 stream() 方法，把有值的 Optional 转成长度为 1 的 Stream，空的转成长度为 0 的 Stream，结合 flatMap 可以优雅地过滤空值：

```java
// 例：从 List<User> 中提取所有非空的用户名
// 为什么用 flatMap + Optional::stream？
// user.getName() 可能返回 null，我们不能直接 map 然后 collect
// 传统做法：先 .map(User::getName).filter(Objects::nonNull)
// Optional::stream 更优雅，语义也更清晰
List<String> nonNullNames = users.stream()
    .flatMap(user -> Optional.ofNullable(user.getName()).stream())
    .collect(Collectors.toList());
```

---

## 11.10 性能考量

Stream 不是银弹，选择时可以参考以下几点。

小数据集（几十到几百个元素）用 for 循环或 Collection 的 forEach 就够，Stream 的抽象开销在这个量级没有收益。

Stream 的惰性求值机制在数据量大时反而能减少不必要的计算：filter、map 等中间操作是融合在一起的，不会产生中间数组。这比 for 循环里先生成一个新 List 再遍历下一个操作要省内存。

频繁装箱的场景（Stream\<Integer\> 大量数值计算）一定要用 IntStream / LongStream / DoubleStream，装箱带来的 GC 压力在数据量大时很明显。

并行流不是用来"加速"单次毫秒级任务的。它适合的是百毫秒级以上的 CPU 密集任务。数据量小的时候并行拆分和合并的开销大于多核带来的收益。

```java
// 一个实用的选择策略：
// 数据量 < 1000  -> 用 for 循环或 stream()
// 数据量 1000-10000 -> 用 stream()，已经够用
// 数据量 > 10000 且操作 CPU 密集 -> 考虑 parallelStream()
// 数据量 > 10000 且操作有 IO 等待 -> 用 CompletableFuture，不要用并行流
```

---

## 11.11 完整代码示例：综合运用

下面这个类用一个贯穿场景把前面学到的操作串起来跑一遍。

```java
package com.example;

import java.util.*;
import java.util.stream.*;

/**
 * Stream 综合应用——模拟餐厅订单分析
 * 把 filter / map / flatMap / collect 组合起来解决实际问题
 */
public class StreamDemo {

    // 菜品
    static class Dish {
        String name;
        boolean vegetarian;
        int calories;
        String type;  // "MEAT", "FISH", "OTHER"

        Dish(String name, boolean vegetarian, int calories, String type) {
            this.name = name; this.vegetarian = vegetarian;
            this.calories = calories; this.type = type;
        }
    }

    // 订单
    static class Order {
        String customer;
        List<Dish> dishes;
        Order(String customer, List<Dish> dishes) {
            this.customer = customer;
            this.dishes = dishes;
        }
    }

    public static void main(String[] args) {
        // 准备数据
        List<Dish> menu = Arrays.asList(
            new Dish("红烧肉", false, 800, "MEAT"),
            new Dish("清蒸鲈鱼", false, 400, "FISH"),
            new Dish("麻婆豆腐", true, 300, "OTHER"),
            new Dish("宫保鸡丁", false, 550, "MEAT"),
            new Dish("蒜蓉西兰花", true, 150, "OTHER"),
            new Dish("水果沙拉", true, 120, "OTHER")
        );

        List<Order> orders = Arrays.asList(
            new Order("张三", Arrays.asList(menu.get(0), menu.get(3))),
            new Order("李四", Arrays.asList(menu.get(1), menu.get(4))),
            new Order("王五", Arrays.asList(menu.get(2), menu.get(4), menu.get(5)))
        );

        // --- 需求1：素菜有哪些？---
        // filter 筛选素菜 -> map 只要名字 -> collect 收成 List
        System.out.println("素菜: " +
            menu.stream()
                .filter(d -> d.vegetarian)
                .map(d -> d.name)
                .collect(Collectors.toList())
        );

        // --- 需求2：所有低卡菜（<400卡）按卡路里升序 ---
        // filter 筛选 -> sorted 排序 -> map 取名字 -> collect
        System.out.println("低卡菜(升序): " +
            menu.stream()
                .filter(d -> d.calories < 400)
                .sorted(Comparator.comparingInt(d -> d.calories))
                .map(d -> d.name + "(" + d.calories + "cal)")
                .collect(Collectors.toList())
        );

        // --- 需求3：所有订单共点了哪些菜（去重） ---
        // flatMap 摊平所有订单的菜品 -> map 取名字 -> distinct 去重 -> collect
        System.out.println("所有订单的菜品: " +
            orders.stream()
                .flatMap(order -> order.dishes.stream())
                .map(d -> d.name)
                .distinct()
                .collect(Collectors.toList())
        );

        // --- 需求4：分组——按菜品类型分组 ---
        System.out.println("按类型分组: " +
            menu.stream()
                .collect(Collectors.groupingBy(d -> d.type))
        );

        // --- 需求5：每个类型有几道菜？ ---
        System.out.println("按类型计数: " +
            menu.stream()
                .collect(Collectors.groupingBy(d -> d.type, Collectors.counting()))
        );

        // --- 需求6：菜单里有没有超过 900 卡的菜？---
        System.out.println("有超过900卡的菜吗？ " +
            menu.stream().anyMatch(d -> d.calories > 900)
        );

        // --- 需求7：总卡路里 ---
        System.out.println("总卡路里: " +
            menu.stream().mapToInt(d -> d.calories).sum()
        );
    }
}
```

运行这个类，输出如下：

```
素菜: [麻婆豆腐, 蒜蓉西兰花, 水果沙拉]
低卡菜(升序): [水果沙拉(120cal), 蒜蓉西兰花(150cal), 麻婆豆腐(300cal)]
所有订单的菜品: [红烧肉, 宫保鸡丁, 清蒸鲈鱼, 蒜蓉西兰花, 麻婆豆腐, 水果沙拉]
按类型分组: {MEAT=[红烧肉(800cal), 宫保鸡丁(550cal)], FISH=[清蒸鲈鱼(400cal)], OTHER=[麻婆豆腐(300cal), 蒜蓉西兰花(150cal), 水果沙拉(120cal)]}
按类型计数: {MEAT=2, FISH=1, OTHER=3}
有超过900卡的菜吗？ false
总卡路里: 2320
```

对照这个输出去理解前面的知识点：每个需求从"想要什么"出发，反向推导需要哪些操作，先把操作链写出来再补细节，是学习 Stream 最快的方法。
