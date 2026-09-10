
本指南围绕你项目的操作日志归档模块（LogArchiveService）展开，把 Java I/O 的每一个核心概念都落到真实的业务代码上。你看到的不是玩具 Demo，而是每天在生产环境运行的归档流程。


Java 的 I/O 体系可以按代际划分：

**Java 1.0~1.1 的流式 I/O（java.io）**
以 InputStream/OutputStream 和 Reader/Writer 为中心，通过装饰器模式组合功能（如 BufferedInputStream + FileInputStream）。这是经典 I/O，至今仍在使用，但存在性能瓶颈和 API 设计上的不足。

**Java 1.4 引入的 NIO（java.nio）**
通道（Channel）、缓冲区（Buffer）、选择器（Selector）三大组件。Selector 让一个线程管理多个连接，是高并发网络编程的基础。但日常文件读写中，直接操作 ByteBuffer 的 API 偏底层，代码不够直观。

**Java 1.7 引入的 NIO.2（java.nio.file）**
Files 工具类、Path 接口、Files.walk 等 API 让文件操作变得简洁。Files.readAllBytes、Files.write 等一行代码完成读写，Files.walkFileTree 让目录递归遍历不再需要手写递归。

**Java 8 引入的 Files.lines 和 Files.list**
结合 Stream API，实现了声明式的文件处理。读取大文件时可以写成 .lines().filter(...).collect(...) 的链式调用，无需手动管理循环和资源释放。

**现代 Java 的最佳实践**
NIO.2 的 Files/Paths 作为首选 API，java.io 只在需要兼容旧库或特定场景（如 MultipartFile.transferTo）时使用，包装流简化资源管理，Charset 显式指定避免乱码。



# 1 File

## 1.1 File 类：文件系统的入口

**核心作用**

java.io.File 是文件和目录路径名的抽象表示。它不读文件内容，只操作路径、属性、目录结构。Java 7 之后推荐优先用 java.nio.file.Path，但 File 在以下场景仍然常见：
- 调用 mkdirs() 创建目录（LogArchiveService 第 159 行）
- 与 MultipartFile.transferTo(File) 配合处理文件上传
- 调用 exists() / delete() / length() 等简单属性查询


**File 的主要方法归类**

| 类别   | 方法                                                 | 说明                                |
| ---- | -------------------------------------------------- | --------------------------------- |
| 路径操作 | getPath() / getAbsolutePath() / getCanonicalPath() | 后两者差异：getCanonicalPath 会解析 . 和 .. |
| 判断   | exists() / isFile() / isDirectory() / isHidden()   |                                   |
| 属性   | length() / lastModified() / canRead() / canWrite() |                                   |
| 创建删除 | createNewFile() / delete() / deleteOnExit()        | deleteOnExit 在 JVM 正常关闭时删除        |
| 目录   | mkdir() / mkdirs() / list() / listFiles()          | listFiles 可以传 FileFilter          |

注意事项
- File 的 length() 返回 long 类型，不要直接转型为 int。文件大小的场景要用 long 承载
- delete() 方法不抛异常，只返回 boolean。需要确认删除结果时要检查返回值。Files.delete() 会抛 NoSuchFileException 或 DirectoryNotEmptyException。
- File 的 listFiles() 在空目录返回长度 0 的数组，在不存在或 IO 错误时返回 null，调用前要做好 null 判断。


**项目中的实战**

创建归档目录：
```java
// LogArchiveService.java 第 159 行
new File(ARCHIVE_DIR).mkdirs();
```
这里 ARCHIVE_DIR 是 /main.java.com.example.tmp/archive，mkdirs() 会递归创建所有不存在的父目录。如果 /main.java.com.example.tmp 存在但 archive 不存在，它只创建 archive。

保存上传文件：
```java
// FileUtils.java 第 17~38 行
public static String saveFile(MultipartFile file, String targetPath, String finalFileName) {
    File temp = new File(targetPath);
    if (!temp.exists()) {
        boolean mkdir = temp.mkdirs();
        if (!mkdir) {
            throw new UcspException("保存文件失败！无法创建文件夹！");
        }
    }
    File localFile = new File(targetPath + File.separator + finalFileName);
    if (localFile.exists()) {
        localFile.delete();
    }
    file.transferTo(localFile);
    return targetPath;
}
```

**要点：**
- mkdirs() 与 mkdir() 的区别：mkdirs() 会递归创建不存在的父目录，mkdir() 只创建最后一级，如果父目录不存在会失败。务必用 mkdirs()。
- File.separator 是跨平台的路径分隔符，Windows 是 \，Linux 是 /。写成 "a/b/c" 在 Linux 上没问题，但在 Windows 上会出问题。拼接路径时优先用 File.separator 或 Paths.get()。
- transferTo 是 Spring 的方法，内部本质上是把 MultipartFile 的 InputStream 拷贝到目标 File 对应的 OutputStream 上。
- 常见的坑：上传文件不校验后缀导致安全问题；同一个文件名被覆盖不提示；目录不存在不先创建。




## 1.2 Path 和 Paths

为什么需要 Path？File 的 API 设计有缺陷：很多方法返回 boolean 而非抛异常，length() 在文件不存在时返回 0 而非抛异常，导致难以区分「文件真的长度为 0」和「文件不存在」。Path 接口配合 Files 工具类解决了这些问题。

**项目中的实战**

LogArchiveService 中大量使用 Path + Files：
```java
// 创建 Path 对象
Path csvPath = Paths.get(csvFilePath);     // 字符串转 Path
Path encPath = Paths.get(ARCHIVE_DIR, "oplog_" + timeStamp + ".enc");  // 变参拼接

// 检查文件是否存在
Files.exists(path)    // 返回 boolean

// 读取全部字节
byte[] csvBytes = Files.readAllBytes(Paths.get(csvFilePath));

// 写入全部字节
Files.write(Paths.get(encFilePath), encBytes);

// 删除文件
Files.deleteIfExists(Paths.get(csvFilePath));

// 创建目录
Files.createDirectories(path.getParent());   // 等同于 mkdirs()
```

**要点：**
- Paths.get(String first, String... more) 是推荐的路径拼接方式，内部自动处理分隔符
- Path 是接口，Paths.get() 返回的是实现类（Windows 上返回 WindowsPath）
- Path 提供了 resolve() / relativize() / normalize() 等路径运算方法
- Paths.get(csvFilePath).getParent() 获取父目录路径，然后用 Files.createDirectories 创建

```java title:Path与File的互转
// File -> Path
File file = new File("/tmp/archive/test.csv");
Path path = file.toPath();

// Path -> File
Path path = Paths.get("/tmp/archive/test.csv");
File file = path.toFile();
```

在需要调用只接受 File 参数的遗留方法时（如 new FileInputStream(file)），用 path.toFile() 桥接。


注意事项

- Paths.get() 不会验证路径对应的文件或目录是否存在，它只是创建了一个路径字符串的容器。真正涉及磁盘的操作在 Files 工具类的方法中。
- Windows 上的路径需要处理盘符（C:）和反斜杠。Paths.get 能够正确处理 Windows 路径，但在拼接时用"a/b/c"这种 Unix 风格写法也能被 Windows 版 Java 正确解析。
- Paths.get("") 返回空路径，resolve("child") 会得到 "child"。这种行为的实际意义有限，但你要知道不要传空路径。


## 1.3 Files 工具类：现代文件操作的核心

 设计思想

Files 是一个工具类，所有方法都是静态的。它把文件操作抽象成原子操作，你不再需要手动创建 FileInputStream 再包一层 BufferedReader。每做一件事就调用一个方法。


实战：LogArchiveService 是 Files 类的绝佳教材，几乎覆盖了所有高频方法：
（ucspjavaserver/4.0_thrift_sqlite_diankeyuan）

**读取文件全部内容：**
```java
// 第 198 行
byte[] csvBytes = Files.readAllBytes(Paths.get(csvFilePath));
```
这个方法适合文件大小在合理范围内的场景（比如归档文件几 MB 到几十 MB）。内部一次性分配 byte[] 数组，如果文件太大（几百 MB 以上）会 OOM。大文件要用 Files.newBufferedReader 逐行读或 Files.newInputStream 分块读。

**写入文件：**
```java
// 第 204 行
Files.write(Paths.get(encFilePath), encBytes);
```
Files.write 默认行为是覆盖已存在的文件。如果不想覆盖，可以用 StandardOpenOption.APPEND 或 StandardOpenOption.CREATE_NEW。
CREATE_NEW 在文件已存在时会抛 FileAlreadyExistsException，这是比 File.exists() 检查更原子化的方式，避免了检查-创建之间的竞态条件。

**带 BOM 的 CSV 写入：**
```java
// 第 167 行
try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(csvFilePath), StandardCharsets.UTF_8)) {
    writer.write('﻿');  // UTF-8 BOM，确保 Excel 打开不乱码：ZWNBSP
    CSVPrinter csvPrinter = new CSVPrinter(writer, ...);
    // ...
}
```
Files.newBufferedWriter 返回 BufferedWriter，内部使用了缓冲区，不需要再额外包装。BOM（Byte Order Mark）在 UTF-8 中不是必须的，但 Windows 版 Excel 依赖 BOM 才能正确识别 UTF-8 编码。这是典型的「标准合规 vs 现实兼容」问题，这里选择兼容现实。

**删除文件：**
```java
// 第 208 行
Files.deleteIfExists(Paths.get(csvFilePath));
```
deleteIfExists 在文件不存在时返回 false 而非抛异常，适合清理临时文件的场景。如果确定文件必须存在，用 Files.delete(path)，它会在文件不存在时抛 NoSuchFileException。

**检查文件是否存在：**
```java
// 第 297 行
if (!Files.exists(path)) {
    log.warn("归档文件不存在: {}", archive.getArchivePath());
    return null;
}
```
注意不要在检查存在性和后续操作之间有太长时间间隔，否则可能产生 TOCTOU（Time-of-Check Time-of-Use）竞态问题。也就是说，你检查完 exists 为 true，下一秒别人把文件删了，你再读就出错了。高并发场景下应该直接用 try-catch 捕捉后续操作的异常，而不是先检查再操作。

**创建目录结构：**
```java
// ReportTest.java 第 301~303 行
Path path = Paths.get(savePath);
if (path.getParent() != null) {
    Files.createDirectories(path.getParent());
}
```
Files.createDirectories 对应 File.mkdirs()，createDirectory 对应 File.mkdir()。差别在于 createDirectory 要求所有父目录已存在。

**流式 API 的运用**
```java
Files.lines(Paths.get("/main.java.com.example.tmp/archive/data.csv"), StandardCharsets.UTF_8)
    .skip(1)                    // 跳过 CSV 表头
    .filter(line -> line.contains("ERROR"))
    .limit(100)
    .forEach(System.out::println);
```
Files.lines 返回` Stream<String>`，内部按需读取不一次性加载全文件。配合 try-with-resources 确保文件句柄在流关闭时释放。


**注意事项**

- Files.readAllBytes 和 Files.readAllLines 都是"全量读取"，适合小文件。项目中的归档日志文件通常几百 KB 到几 MB，readAllBytes 完全 OK。但如果要处理百 MB 级别的 CSV，必须用Files.newBufferedReader 逐行处理。

- Files.write 默认使用 UTF-8 编码（Charset.defaultCharset()），但显式指定编码永远是好习惯，避免部署环境差异导致的问题。

- Files.copy 支持从 InputStream 拷贝到 Path（从网络下载保存到本地），也支持 Path to Path 拷贝。ReportTest 中的 FileOutputStream 写文件可以改写为：
```java
Files.write(Paths.get(savePath), data);
```
更简洁，不需要手动 flush 和 close。

Files.list 和 Files.walk 的区别：list 只遍历当前目录一层，walk 递归遍历所有子目录，walk 返回的 Stream 必须 close（或包装在 try-with-resources 中），因为它持有目录句柄。


## 1.4 字节流与字符流：InputStream/OutputStream vs Reader/Writer

选型原则：字节流处理二进制数据（图片、压缩文件、加密数据、PDF），字符流处理文本数据（CSV、JSON、XML、TXT）。选错会导致乱码或数据损坏。

字节流场景（你项目中的二进制处理）

**加密/解密场景：**
```java
// LogArchiveService 第 198~204 行
byte[] csvBytes = Files.readAllBytes(Paths.get(csvFilePath));    // 读字节
String encBase64 = KeyProtectionManager.encrypt(csvBytes);        // SM4 加密
byte[] encBytes = Base64.getDecoder().decode(encBase64);         // Base64 转字节
Files.write(Paths.get(encFilePath), encBytes);                    // 写字节
```
这里全程以 byte[] 为操作单位，因为加密算法处理的是字节而非字符。

**文件下载保存场景：**
```java
// ReportTest.java 第 305~308 行
try (FileOutputStream fos = new FileOutputStream(savePath)) {
    fos.write(data);
    fos.flush();
}
```
二进制响应体直接写入磁盘，同样用字节流。


字符流场景

**CSV 文件写入：**
```java
// LogArchiveService 第 167 行
try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(csvFilePath), StandardCharsets.UTF_8)) {
    // writer 写入字符
}
```
CSV 文件是文本文件，字符流加上 UTF-8 编码，确保中文正确。

**归档内容读取并转为文本：**
```java
// LogArchiveService 第 306 行
return new String(csvBytes, StandardCharsets.UTF_8);
```
解密得到的是字节数组，但最终返回给用户的应该是可读文本，所以用 new String(bytes, charset) 做一次解码。

字节流与字符流之间的桥梁：InputStreamReader 和 OutputStreamWriter 负责在两者之间转换：
```java
// 从字节流读取字符（指定编码）
try (BufferedReader reader = new BufferedReader(
    new InputStreamReader(new FileInputStream("data.csv"), StandardCharsets.UTF_8))) {
    String line;
    while ((line = reader.readLine()) != null) {
        // 处理每一行
    }
}
```

**注意事项**

字符流不指定编码会导致跨平台行为不一致。Windows 默认 GBK，Linux 默认 UTF-8。同一个程序在两套环境上的表现会不同。指定编码可以消除这种不确定性。
使用 Files.newBufferedWriter(Path, Charset) 时已经内置了缓冲，不需要再外面包一层 BufferedWriter。Reader 的 read() 方法返回 int，范围 0~65535（char 的标准范围），不是 byte。读取到末尾返回 -1。



## 1.5 缓冲流：为什么需要缓冲区

**问题场景**

每次调用 read() 或 write() 都对应一次系统调用（操作系统级的 IO 操作）。磁盘 IO 的延迟通常在毫秒级，一次读 1 字节和读 8KB 的时间开销差不多。如果逐字节读写，性能会差几个数量级。

缓冲的原理：缓冲流内部维护一个字节数组（默认 8192 字节，即 8KB）。写入时数据先积累到缓冲区，缓冲区满了或手动 flush() 才真正写入磁盘。读取时一次从磁盘读入 8KB 到缓冲区，后续读取直接从缓冲区返回，不再触发磁盘 IO。


**项目中的实战**

```java
// LogArchiveService 第 167 行
try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(csvFilePath), StandardCharsets.UTF_8)) {
```

Files.newBufferedWriter 返回的是 BufferedWriter。为什么需要缓冲？第 177~193 行的 for 循环中，每一条日志记录调用一次 csvPrinter.printRecord()，这个操作最终会调用 writer.write()。没有缓冲的话，每写一条记录就写一次磁盘，归档 10000 条记录就是 10000 次系统调用。

设置了缓冲后，八次 write() 的数据在缓冲区积攒，满了才一次性真正写入。总的系统调用次数从 10000 次降为 10000 / 8192 * 平均记录长度 ≈ 几十次。

csvPrinter.flush() 在第 194 行手动调用，确保最后一批数据从缓冲区写入磁盘。如果忘记 flush，可能会丢最后一块数据。但在 try-with-resources 块结束时 writer.close() 会自动 flush，所以这里的 flush 实际上是双重保障。

**缓冲区大小的选择**

Java 默认缓冲区大小 8192 字节（8KB），这个值经过大量实测，适合绝大多数场景。通常不需要修改。如果流式传输大文件，可以适当增大缓冲区减少系统调用次数，但收益递减：8KB 到 64KB 有明显的提升，64KB 到 1MB 几乎没有提升。

BufferedOutputStream 和 BufferedWriter 的缓冲区在写入大量小数据块时效果明显。如果一次写入的数据本身就是大块（比如调用一次 write(`byte[]`) 写入几 MB），那缓冲区的作用不大，因为系统调用次数本来就少。

**注意事项**

不 close 流会导致资源泄漏。操作系统对一个进程能打开的文件句柄数有限制（Linux 通常 1024），泄漏多了会报 Too many open files。用 try-with-resources 自动关闭是彻底解决这个问题的方式。

flush 和 close 的区别：flush 把缓冲区数据写入磁盘，但不释放资源。close 先 flush 再释放资源。close 之后就不能再写了。

BufferedReader 的 readLine() 返回 null 表示读取结束。这个 API 比逐字节判断更友好。但要注意 readLine() 返回的字符串不含换行符，需要手动追加。



## 1.6 try-with-resources 与资源管理

**问题场景**

Java 7 之前，释放资源的典型代码长这样：
```java
BufferedReader reader = null;
try {
    reader = new BufferedReader(new FileReader("test.txt"));
    // 读文件
} finally {
    if (reader != null) {
        try { reader.close(); } catch (IOException e) { /* 忽略 */ }
    }
}
```

这种写法的问题：finally 块中又嵌套了一个 try-catch，代码冗长，容易遗漏。

在你项目中的实战

```java
// LogArchiveService 第 167 行
try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(csvFilePath), StandardCharsets.UTF_8)) {
    // 使用 writer
    csvPrinter.flush();
}  // 自动调用 writer.close()
```

try-with-resources 的写法：在 try 后面的括号里声明资源变量，资源类必须实现 AutoCloseable 接口。try 块结束时，编译器自动插入 close 调用（按声明顺序的逆序）。

对比清理代码中不规范的写法：
```java
// LogArchiveService 第 378 行（没有使用 try-with-resources）
private boolean deleteFileQuietly(String filePath) {
    if (filePath == null) return false;
    try {
        return Files.deleteIfExists(Paths.get(filePath));
    } catch (IOException e) {
        log.warn("删除文件失败: {}", filePath, e);
        return false;
    }
}
```
这里 Files.deleteIfExists 不涉及需要 close 的资源（Path 不是 Closeable），所以不需要 try-with-resources。如果打开了 InputStream 或 Reader，必须用。

**多个资源的管理**

```java
try (BufferedWriter writer = Files.newBufferedWriter(path, charset);
     CSVPrinter csvPrinter = new CSVPrinter(writer, format)) {
    csvPrinter.printRecord(...);
}
// 先关闭 CSVPrinter，再关闭 BufferedWriter
```

多个资源用分号分隔，关闭顺序与声明顺序相反。CSVPrinter 的 close 方法会先 flush 它内部的数据，然后关闭它包装的 writer。

**注意事项**

try-with-resources 也可以有 catch 和 finally 块，它们在资源关闭之后执行。catch 可以捕获资源初始化或使用过程中的异常，finally 用于其他清理工作。

使用 try-with-resources 时，如果 try 块和 close 都抛异常，close 的异常会被抑制（suppressed），你只能看到 try 块的异常。通过 Throwable.getSuppressed() 可以获取被抑制的异常。这在调试资源关闭失败时有用。

如果资源类没有实现 AutoCloseable（比如早期的某些 API），就不能用在 try-with-resources 中。



## 1.7 八、序列化与反序列化：把对象变成字节

**背后的原理**

序列化将内存中的 Java 对象转换成字节序列——可以存入文件、通过网络传输或在进程间共享。反序列化是将字节序列恢复为 Java 对象。

Java 原生序列化通过 ObjectOutputStream/ObjectInputStream 实现，类需要实现 Serializable 接口。序列化时，对象的类名、字段名、字段值、对象间的引用关系都被编码。如果一个字段是 transient 的，它会被跳过。

**项目中的情况**

你的项目并没有使用 Java 原生序列化（ObjectOutputStream），而是采用了 JSON 序列化。LogArchiveEntity 在保存到数据库时，MyBatis-Plus 的 BaseMapper 自动将 Java 对象与数据库记录映射。在 Thrift RPC 调用时，CommonRequest/CommonResponse 使用 Thrift 的序列化协议（TBinaryProtocol）。

为什么要用 JSON/Thrift 而非 Java 原生序列化？Java 原生序列化有严重的安全问题：反序列化时自动执行被序列化对象的构造函数和 readObject 方法，攻击者可以构造恶意字节流进行反序列化攻击（如 fastjson 反序列化 RCE）。JSON 和 Thrift 序列化只处理纯数据，不执行任意代码。



```java title:使用JSON序列化保存到文件（最佳实践）
import com.alibaba.fastjson2.JSON;

// 序列化对象列表为 JSON 字符串
List<OperationLogEntity> logs = ...;
String jsonStr = JSON.toJSONString(logs);
// 写入文件
Files.write(Paths.get("/main.java.com.example.tmp/archive/logs.json"), jsonStr.getBytes(StandardCharsets.UTF_8));

// 从文件读取并反序列化
byte[] bytes = Files.readAllBytes(Paths.get("/tmp/archive/logs.json"));
List<OperationLogEntity> logs = JSON.parseArray(new String(bytes, StandardCharsets.UTF_8), OperationLogEntity.class);
```

**注意事项**

Serializable 接口不要求实现任何方法，它只是一个标记接口。没有实现 Serializable 的类被序列化时会抛 NotSerializableException。

serialVersionUID 的作用：反序列化时 JVM 用这个 ID 校验类的版本一致性。不显式声明的话，JVM 根据类结构自动生成一个。如果类新增字段后再反序列化旧数据，自动生成的 UID 会不同，导致 InvalidClassException。所以实现了 Serializable 的类应该显式声明 serialVersionUID：
```java
private static final long serialVersionUID = 1L;
```

transient 关键字标记的字段不会被序列化。密码、密钥等敏感信息应该用 transient 标记，或者在序列化前置为 null。



## 1.8 九、装饰器模式在 Java I/O 中的体现

### 1.8.1 为什么 I/O 要用装饰器

假设你需要读文件、用缓冲、自动处理压缩。如果为每种组合都写一个类，类的数量会呈爆炸式增长。装饰器模式的解决思路：有一个核心组件（FileInputStream），然后让增强组件（BufferedInputStream）包装它，再让另一个增强（如 GzipInputStream）包装前者。

```java
InputStream in = new GzipInputStream(new BufferedInputStream(new FileInputStream("data.gz")));
```

### 1.8.2 你项目中的间接体现

直接使用 Files 工具类时装饰器模式被隐藏了，但底层仍然是这个组合：
```java
// Files.newBufferedWriter 内部等价于：
new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(path), charset))
```

你项目中的 CSVPrinter 也是装饰器模式的一种变体：
```java
BufferedWriter writer = Files.newBufferedWriter(path, charset);
CSVPrinter printer = new CSVPrinter(writer, format);
```
CSVPrinter 装饰了 BufferedWriter，增加了 CSV 格式化的能力。

### 1.8.3 注意事项

装饰链的关闭顺序与构造顺序相反。最外层的装饰器 close 时，会调用内层的 close，逐层传递。不按这个顺序关闭可能导致数据丢失。用 try-with-resources 声明所有装饰器时，编译器会自动处理关顺序。

不要在装饰链中间层持有引用并继续使用。一旦最外层关闭，内层流也被关闭了。

---

## 1.9 十、编码与乱码：Charset 的全局理解

### 1.9.1 核心概念

字符编码的流程可以用一句话概括：字符 -> 编码（字符集规则） -> 字节序列 -> 解码（相同字符集规则） -> 字符。

乱码的原因永远是：编码时用的字符集和解码时用的字符集不一致。

几个关键字符集：
- ASCII：7 位表示英文字母、数字、符号，1 字节
- ISO-8859-1（Latin-1）：兼容 ASCII，1 字节表示西欧字符
- GBK：中文编码，1~2 字节变长
- UTF-8：Unicode 的一种变长编码，英文 1 字节，中文 3 字节
- UTF-16：大部分字符 2 字节，中文也是 2 字节

UTF-8 是目前的事实标准。Linux/Unix 系统的默认编码通常是 UTF-8，Windows 中文版默认是 GBK。

### 1.9.2 你项目中涉及的编码问题

**BOM 的处理：**
```java
// LogArchiveService 第 169 行
writer.write('﻿');  // UTF-8 BOM
```
BOM（Byte Order Mark）是放在文件最开头的特殊字节序列，用于标记编码方式。在 UTF-16 中 BOM 表示字节序是大端还是小端。在 UTF-8 中 BOM 不是必须的，但 Windows 的 Excel 通过 BOM 判断 CSV 文件是否是 UTF-8 编码。没有 BOM，Excel 默认用系统编码（中文 Windows 是 GBK）打开，中文会乱码。

**Base64 编码的需求：**
```java
// LogArchiveService 第 202~203 行
String encBase64 = KeyProtectionManager.encrypt(csvBytes);
byte[] encBytes = Base64.getDecoder().decode(encBase64);
```
这里的 Base64 不是字符编码，而是二进制数据到文本的编码方案。KeyProtectionManager 的 encrypt 方法返回 Base64 字符串，所以需要先解码回字节再写入文件。设计上可以把直接返回字节数组的方法作为底层方法，Base64 编码放在业务层处理，职责更清晰。

### 1.9.3 排查乱码的思路

如果你的程序出现乱码，按以下顺序排查：
1. 确认读文件时指定的 Charset 与文件实际编码一致。
2. 确认数据在传输过程中的每个环节都没有被重新编码。
3. 确认浏览器或终端显示的编码。

Tomcat 的 URIEncoding 默认是 ISO-8859-1，如果你通过 URL 传中文参数，需要改配置为 UTF-8 或在代码中手动转码。

### 1.9.4 注意事项

Charset.defaultCharset() 取决于 JVM 启动时的系统编码，可以配置 -Dfile.encoding=UTF-8 强制指定。为了防止环境差异，不要依赖默认编码，每次读写都显式指定。

StandardCharsets.UTF_8 是 Java 7 引入的常量，比 Charset.forName("UTF-8") 更高效，因为它避免了字符串查找的过程。StandardCharsets 还提供了 ISO_8859_1、US_ASCII、UTF_16 等常量。

new String(bytes, "utf-8") 拼写错误（写成 utf8、utf_8、utf-8 其实都可以，但 Java 的 Charset 名字是标准化的）不会编译报错，运行时会抛 UnsupportedCharsetException。用 StandardCharsets.UTF_8 或 Charset.forName 可以避免这个问题。



## 1.10 项目完整 I/O 流程串讲

### 1.10.1 归档流程的全链路 I/O 分析

以 LogArchiveService.doArchive 方法为例，一次完整的归档涉及以下步骤：

1. 用 File.mkdirs() 创建 /main.java.com.example.tmp/archive 目录（如果不存）
2. 用 Files.newBufferedWriter + UTF-8 创建 CSV 文件，先写 BOM，再逐行写入日志记录
3. 每 2000 条记录从数据库读取一批，写入 CSV，循环直到全部写完
4. 关闭写入器（自动 flush）
5. 用 Files.readAllBytes 读取 CSV 文件的全部字节
6. 用 Sm3Util.hash 计算 SM3 摘要（对字节数组计算，确保完整性）
7. 用 KeyProtectionManager.encrypt 对 CSV 字节进行 SM4 加密
8. 对加密后的 Base64 字符串用 Base64.getDecoder().decode 解码
9. 用 Files.write 写入 .enc 文件
10. 用 Files.deleteIfExists 删除明文 CSV
11. 归档完成，如果过程中抛异常，清理所有已创建的临时文件

**设计思路：**
- 先写明文再加密，是为了在加密前计算 SM3 摘要。如果直接加密后再算摘要，验证时需要先解密才能算摘要，逻辑上更复杂。
- 分页读取而不是一次性查全量，是因为日志表可能有几十万条记录，全部加载到内存会导致 OOM。
- 明文 CSV 在归档完成后立即删除，生产环境只保留密文文件。

### 1.10.2 文件清理流程

cleanupOldArchives 的逻辑：
1. 查数据库，获取 archiveTime 早于指定时间的所有归档记录
2. 遍历每条记录，先调用 Files.deleteIfExists 删除物理文件
3. 再调用 logArchiveDAO.removeById 删除数据库记录

这里遵循了"先删文件再删库记录"的顺序。如果先删记录再删文件，文件删除失败会导致已删除的记录无法恢复，成为僵尸文件。反过来，文件删了但库记录删除失败，只是多一条无用的记录，影响较小。

### 1.10.3 文件读取与完整性校验

readArchiveContent 的流程：
1. 根据 archiveId 查数据库，获取 archivePath
2. 用 Files.exists 判断文件是否存在
3. 用 Files.readAllBytes 读取加密文件内容
4. 用 Base64.getEncoder().encodeToString 编码为 Base64 字符串
5. 调用 KeyProtectionManager.decrypt 解密
6. 用 new String(bytes, UTF-8) 把字节转为可读文本

verifyArchiveIntegrity 的流程与读取类似，但多了一步 SM3 摘要对比：
1. 读取并解密文件内容
2. 用 Sm3Util.hash 对解密后的字节重新计算 SM3 摘要
3. 将计算结果与数据库中存储的 digest 字段对比
4. 一致则文件完整，不一致说明文件被篡改或损坏

### 1.10.4 关键权衡

为什么用 SM3 + SM4 而不是直接对文件加密？SM3 摘要用于完整性校验——你拿到加密文件后可以先解密、算摘要、对比数据库中的摘要值，确认文件未被篡改。纯加密只能保证机密性（别人看不懂），不能保证完整性（别人有没有改过内容）。

为什么用 Base64 中转？KeyProtectionManager 的接口设计为输入输出 Base64 字符串，这层转换在 Service 层完成。如果将来替换加密组件，业务代码可以不改，只改这一层转换。



# 2 十二 、常用 I/O 场景的代码模板

### 2.1.1 逐行读取文本文件

```java
Path path = Paths.get("/tmp/archive/data.csv");
try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
    String line;
    while ((line = reader.readLine()) != null) {
        // 处理每一行
    }
}
```
适合大文件。逐行读取不会把整个文件加载到内存。

### 2.1.2 用 Stream API 读取文本文件

```java
try (Stream<String> lines = Files.lines(Paths.get("/tmp/archive/data.csv"), StandardCharsets.UTF_8)) {
    lines.skip(1)                          // 跳过表头
         .filter(l -> !l.trim().isEmpty()) // 过滤空行
         .forEach(line -> {
             // 处理每一行
         });
}
```
必须把 Stream 放在 try-with-resources 中，Files.lines 返回的 Stream 持有文件句柄，不关闭会泄漏。

### 2.1.3 写入多行到文本文件

```java
List<String> lines = Arrays.asList("id,name,age", "1,张三,30", "2,李四,25");
Files.write(Paths.get("/main.java.com.example.tmp/output.csv"), lines, StandardCharsets.UTF_8);
```
Files.write 的 Collection 重载会自动用行分隔符连接元素，适合小数据量的一次性写入。

### 2.1.4 复制文件

```java
Files.copy(Paths.get("/main.java.com.example.tmp/source.txt"), Paths.get("/main.java.com.example.tmp/target.txt"), StandardCopyOption.REPLACE_EXISTING);
```
REPLACE_EXISTING 表示目标存在时覆盖。可以用 COPY_ATTRIBUTES 同时复制文件属性。

### 2.1.5 文件下载到本地

```java
byte[] data = httpClient.download(url);    // 从远程获取数据
Path target = Paths.get("D:/download/report.csv");
Files.createDirectories(target.getParent());
Files.write(target, data);
```
ReportTest 中的实现基本如此。

### 2.1.6 需要分块处理的大文件

```java
// 用法：每页 2000 条，分页处理
int pageSize = 2000;
int totalPages = (int) Math.ceil((double) total / pageSize);
for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
    List<Record> pageData = queryPage(pageNum, pageSize);
    for (Record r : pageData) {
        // 逐条处理
    }
}
```
这正是你项目中的 STREAM_PAGE_SIZE 策略，避免了内存溢出。

### 2.1.7 压缩与解压（ZipUtils）

```java
// 压缩目录
ZipUtils.zipDirectory("/data/logs", "/main.java.com.example.tmp/logs_backup.zip");

// 解压到指定目录
ZipUtils.unzip("/main.java.com.example.tmp/logs_backup.zip", "/data/restored_logs");
```

ZipUtils 中的实现要点：
- 压缩时用 ZipOutputStream 包装 FileOutputStream，推入 ZipEntry，按 1024 字节缓冲区读写文件内容
- 解压时用 ZipInputStream 包装 FileInputStream，遍历 ZipEntry，对非目录项写入目标文件
- 压缩和解压都涉及 1024 字节的缓冲区 byte[]，这是常见的缓冲区大小，平衡了内存占用和 IO 效率

### 2.1.8 原子写入（先写临时文件再 rename）

```java
// DnsConfig.java 第 47~70 行
File tmpFile = new File("/etc/resolv.conf.main.java.com.example.tmp");
try (DataOutputStream dout = new DataOutputStream(Files.newOutputStream(tmpFile.toPath()))) {
    // 写入新配置到临时文件
    dout.write(line.getBytes());
    dout.flush();
}
// 原子替换
File dst = new File("/etc/resolv.conf");
dst.delete();
tmpFile.renameTo(dst);
```

这个模式解决了"写入过程中程序崩溃导致配置文件不完整"的问题。先写入临时文件，确认写入成功后再 rename 覆盖目标文件。renameTo 在操作系统层面是原子操作（在同一个文件系统内），所以要么目标文件保持旧内容，要么被新内容完整替换，不会出现半写状态。

---

## 2.2 十三、资源加载：从 classpath 读取文件

### 2.2.1 问题场景

你的应用在启动时需要加载证书（JKS 文件）、配置（yml）、本地库（dll/so）等资源文件。这些文件不在磁盘上的任意路径，而是打包在 JAR 包内部的资源，或者放在 classpath 指定的目录中。

### 2.2.2 你项目中的实战

**SSL keystore 加载（SslThriftServer）：**
```java
// SslThriftServer.java 第 39 行
try (InputStream keyStore = serverConfig.getKeyStore()) {
    TSSLTransportFactory.TSSLTransportParameters params = new TSSLTransportFactory.TSSLTransportParameters();
    params.setKeyStore(keyStore, serverConfig.getKeyStorePassword());
    // ...
}
```
keyStore 的 InputStream 来自 ThriftSslConfig，在配置类中通过 ResourceLoader 加载。

**Thrift 客户端 truststore 加载：**
```java
// SslThriftClient.java 相关代码
FileInputStream fis = new FileInputStream(trustStorePath);
KeyStore keyStore = KeyStore.getInstance("JKS");
keyStore.load(fis, trustStorePassword.toCharArray());
```
这里用 FileInputStream 读取路径指定的 JKS 文件。trustStorePath 可以是 classpath: 前缀路径，也可以是指对文件路径。

**TLCP 证书文件验证（ThriftServerComponent）：**
```java
// ThriftServerComponent.java 相关代码
Files.exists(Paths.get(caCertPath));
Files.exists(Paths.get(signCertPath));
Files.exists(Paths.get(signKeyPath));
```
启动时检查 TLCP 的 5 个证书/密钥文件是否存在，如果缺失则启动失败，明确提示管理员配置。

**加载本地库（LibraryLoader）：**
```java
// 从 JAR 包提取 native 库到临时目录
InputStream in = getClass().getResourceAsStream("/lib/" + libName);
File tmpFile = File.createTempFile("native-", ".lib");
tmpFile.deleteOnExit();
try (FileOutputStream out = new FileOutputStream(tmpFile)) {
    byte[] buffer = new byte[4096];
    int len;
    while ((len = in.read(buffer)) != -1) {
        out.write(buffer, 0, len);
    }
}
System.load(tmpFile.getAbsolutePath());
```
这个模式用于从 JAR 包中提取 .dll/.so 文件到临时目录，然后调用 System.load 加载。File.createTempFile 在 /main.java.com.example.tmp 创建临时文件，deleteOnExit 在 JVM 正常关闭时删除。

### 2.2.3 classpath 资源的读取方式

```java
// 方式一：从 classpath 根目录读取（推荐）
InputStream in = getClass().getClassLoader().getResourceAsStream("keys/fullchain.jks");

// 方式二：相对当前类所在包
InputStream in = getClass().getResourceAsStream("/keys/fullchain.jks");  // 前导 / 表示 classpath 根

// 方式三：Spring 的 ResourceLoader
Resource resource = resourceLoader.getResource("classpath:keys/fullchain.jks");
InputStream in = resource.getInputStream();
```

### 2.2.4 classpath 与文件系统的区别

classpath 资源不一定是磁盘文件。在 JAR 包中，资源是 ZIP 包里的条目，不能直接用 File 访问，必须通过 InputStream。
- 错误写法：new File("classpath:keys/fullchain.jks") 会报错，"classpath:" 不是合法文件路径
- 正确写法：getClass().getClassLoader().getResourceAsStream("keys/fullchain.jks") 返回 InputStream

Spring 的 Resource 接口和 ResourceLoader 统一了这两种场景：无论资源在 classpath 中、文件系统中还是远程 URL 中，都可以用 Resource 抽象访问。

### 2.2.5 注意事项

getResourceAsStream 返回的 InputStream 同样需要关闭。虽然有些资源不关闭也不至于泄漏文件句柄，但统一用 try-with-resources 关闭是好习惯。

classpath 资源路径不要以 / 开头，如果调的是类加载器的 getResourceAsStream。如果调的是 Class 的 getResourceAsStream，则 / 开头表示从 classpath 根开始。这个细节容易混淆，建议统一用不带 / 的相对路径加类加载器方式。

开发环境（IDE 运行）和部署环境（JAR 运行）中，classpath 资源的读取方式完全一致，不需要为环境切换修改代码。

---

## 2.3 十四、RandomAccessFile：随机读写大文件

### 2.3.1 与传统流式读写的区别

FileInputStream 和 FileOutputStream 只能顺序读写。ReadAccessFile 可以：
- 跳转到文件的任意位置进行读写
- 同时读写（一个对象包含读和写两个方法）
- 获取文件指针当前位置

### 2.3.2 在你项目中的实战

你的 FileProtector（ucsp-sdk 中的 HSM 文件加密器）使用 RandomAccessFile 实现并行加密：

```java
// FileProtector.java 核心逻辑
long blockSize = 1024 * 1024 * blockSize;       // 每块大小（MB）
long blockNum = originFileSize / blockSize + ...; // 总块数

// 逐块分配加密任务，每块由独立线程处理
for (long i = 1; i <= blockNum; i++) {
    long endIdx = blockSize * i;
    if (i == blockNum) {
        endIdx = originFileSize;  // 最后一块取到文件末尾
    }
    EncryptRunnable task = new EncryptRunnable(originFile, start, endIdx);
    executorService.execute(task);
    start += blockSize;
}

// EncryptRunnable 内部
RandomAccessFile raf = new RandomAccessFile(file, "r");
raf.seek(startPos);        // 跳转到块的起始位置
raf.read(blockBytes);      // 读取本块数据
```

**要点：**
- RandomAccessFile 的模式参数："r" 只读，"rw" 读写，"rws" 读写+同步元数据
- seek(long pos) 跳转到指定位置，单位为字节
- 读取时可以精确控制从文件哪个位置开始读、读多少字节
- 并行加密利用了多核 CPU：100MB 文件分成 10 块，每块由一个线程加密，理论上耗时可以降到单线程的 1/10（受限于 HSM 设备本身并发能力）

### 2.3.3 与 FileChannel 的关系

RandomAccessFile 底层使用 FileChannel 实现。通过 raf.getChannel() 可以获得底层 FileChannel，用于更高级的操作（如 MappedByteBuffer、文件锁）。

### 2.3.4 使用场景

- 大文件的分段处理（并行加密/解密、并行校验和）
- 需要跳转到文件特定位置读取/写入（如修改某条记录）
- 断点续传（记录已传输的位置，从中断处继续）
- 文件锁（通过 FileLock 实现跨进程同步）

### 2.3.5 注意事项

RandomAccessFile 不是 InputStream 或 OutputStream 的子类，不能用于需要流对象的方法参数。它也不支持装饰器模式，不能包装 BufferedInputStream 等。对大文件的单线程读取，FileInputStream 的 read(byte[]) 顺序读性能优于 RandomAccessFile，因为流式读取有更好的预读优化。

RandomAccessFile 构造函数路径不存在时会抛 FileNotFoundException。"r" 模式下文件不存在也抛异常。"rw" 模式下文件不存在会自动创建。

多线程共享一个 RandomAccessFile 是不安全的。每一线程应该有自己的 RandomAccessFile 实例，或在外部同步访问。

---

## 2.4 十五、文件上传与下载的模式分析

### 2.4.1 你项目中的四种上传模式

**模式一：MultipartFile.transferTo（Spring 标准表单上传）**
```java
// FileUtils.java
public static String saveFile(MultipartFile file, String targetPath, String finalFileName) {
    File temp = new File(targetPath);
    temp.mkdirs();
    File localFile = new File(targetPath + File.separator + finalFileName);
    file.transferTo(localFile);   // Spring 内部把 multipart 内容写磁盘
    return targetPath;
}
```
适用场景：HTTP 表单上传。transferTo 内部把 multipart 数据直接写入目标文件，避免了先读到内存再写磁盘的中间步骤。

**模式二：byte[] 直接写入（Thrift RPC 上传）**
```java
// FileController.java 第 88~116 行
public String handleFileUpload(byte[] bytes) {
    Path destinationPath = Paths.get(uploadBasePath, finalFileName);
    Files.createDirectories(destinationPath.getParent());
    Files.write(destinationPath, bytes);  // 字节数组直接写入文件
    return finalFileName;
}
```
适用场景：Thrift RPC 的二进传输。Thrift 协议支持 byte[] 类型，文件以字节数组形式从客户端传输到服务端。

**模式三：InputStream 分块写入（避免 OOM）**
```java
// PhysicalDeviceController（设备日志上传）
try (InputStream in = file.getInputStream(); 
     OutputStream out = Files.newOutputStream(targetPath)) {
    byte[] buffer = new byte[8192];
    int len;
    while ((len = in.read(buffer)) != -1) {
        out.write(buffer, 0, len);
    }
}
```
适用场景：上传的文件可能很大（设备上报的日志可能几百 MB）。分块写入不会把整个文件加载到内存。

### 2.4.2 你项目中的三种下载模式

**模式一：FileSystemResource（Spring 资源抽象）**
```java
// FileController.java 第 52~62 行
Resource fileResource = new FileSystemResource(absoluteFilePath);
return ResponseEntity.ok()
        .headers(headers)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(fileResource);
```
FileSystemResource 是 Spring 对文件系统资源的封装。`ResponseEntity<Resource> `返回时，Spring MVC 自动处理分块读取输出流，不会把整个文件加载到内存。

**模式二：byte[] 全量返回（Thrift RPC 下载）**
```java
// 数据库备份下载
byte[] data = Files.readAllBytes(backupFilePath);
return data;  // 通过 Thrift RPC 返回 byte[]
```
适用场景：文件较小（几 MB 以内）。对几十 MB 以上的文件，readAllBytes 会消耗大量内存，需要考虑分块传输。

**模式三：InputStream 流式输出（Servlet 响应）**
```java
// PhysicalDeviceController
try (InputStream in = Files.newInputStream(filePath);
     OutputStream out = response.getOutputStream()) {
    response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
    byte[] buffer = new byte[8192];
    int len;
    while ((len = in.read(buffer)) != -1) {
        out.write(buffer, 0, len);
    }
    out.flush();
}
```
适用场景：直接处理 HTTP 响应的下载。手动控制缓冲区大小，适合大文件。

### 2.4.3 路径穿越的安全防护

```java
// FileController.java 第 47 行
if (filePath == null || filePath.isEmpty() || filePath.contains("..")) {
    UcspException.cast("文件路径不合法！");
}
```
".." 在文件路径中表示父目录。不加这个检查，攻击者可以传入 "../../etc/passwd" 读取系统文件。但简单的 contains("..") 检查可以被绕过（如 "...." 在某些系统上等价于 ".."）。生产环境建议使用 Path.normalize() 后检查结果是否以预期目录开头：
```java
Path basePath = Paths.get(uploadBaseDir).normalize();
Path resolvedPath = basePath.resolve(filePath).normalize();
if (!resolvedPath.startsWith(basePath)) {
    throw new SecurityException("路径越权");
}
```

---

## 2.5 十六、DataOutputStream：二进制数据写入

### 2.5.1 作用

DataOutputStream 提供了 writeInt、writeLong、writeUTF 等方法，可以直接将 Java 基本类型写入输出流，省去手动将 int 转成字节数组的步骤。它对应 DataInputStream 做读取。

### 2.5.2 你项目中的实战

```java
// DnsConfig.java 第 56~62 行
DataOutputStream dout = new DataOutputStream(Files.newOutputStream(tmpFile.toPath()));
String line = "nameserver " + dns + "\n";
dout.write(line.getBytes());   // 写入的是字节，不是 DataOutputStream 的特性
dout.flush();
```

这里的 DataOutputStream 实际上没有用到它的类型化写入特性，只是单纯地当作 OutputStream 用。这是因为写入的是文本内容而非二进制结构数据。直接使用 Files.newOutputStream 或 Files.write 更简洁。

### 2.5.3 DataOutputStream 的真正用途

```java
// 二进制数据的类型化写入
ByteArrayOutputStream baos = new ByteArrayOutputStream();
DataOutputStream dos = new DataOutputStream(baos);
dos.writeInt(42);            // 4 字节
dos.writeLong(System.currentTimeMillis());  // 8 字节
dos.writeUTF("操作日志");    // UTF-8 编码的字符串（先写 2 字节长度，再写内容）
dos.close();
byte[] binaryData = baos.toByteArray();
DataInputStream dis = new DataInputStream(new ByteArrayInputStream(binaryData));
int i = dis.readInt();
long l = dis.readLong();
String s = dis.readUTF();
```

这个模式在你项目中没有直接使用（因为目前的二进制数据主要是 SM4 加密后的字节数组，不需要类型化读写），但如果你将来需要设计紧凑的二进制文件格式（比如记录头+多字段的结构），DataOutputStream/DataInputStream 是最直接的选择。

---

## 2.6 十七、FileChannel 的潜在应用场景

### 2.6.1 你项目中何时应该考虑 FileChannel

目前你的项目没有使用 FileChannel，但在以下场景中，它的特性恰恰对应你项目的需求：

**场景一：大文件数据库备份的下载**
```java
// 目前代码
byte[] data = Files.readAllBytes(backupPath);  // 潜在 OOM

// 用 FileChannel 优化
try (FileChannel channel = FileChannel.open(backupPath, StandardOpenOption.READ)) {
    MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
    // 分块返回，不占用堆内存
}
```
如果数据库备份文件很大（几百 MB），FileChannel.map 使用操作系统的虚拟内存映射，不占用 JVM 堆内存。

**场景二：直接传输到网络通道（零拷贝）**
```java
// 如果将来要从 FileChannel 直接传输到 SocketChannel
try (FileChannel fileChannel = FileChannel.open(Paths.get("/tmp/archive/backup.enc"), StandardOpenOption.READ);
     SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(host, port))) {
    fileChannel.transferTo(0, fileChannel.size(), socketChannel);  // 零拷贝
}
```
transferTo 方法利用操作系统的 DMA 机制，直接从磁盘到网卡，跳过 JVM 内存。适合大文件传输场景。

**场景三：文件锁防止并发归档**
```java
try (FileChannel channel = FileChannel.open(Paths.get(lockFile), 
        StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
    FileLock lock = channel.tryLock();  // 非阻塞尝试获取锁
    if (lock == null) {
        log.warn("其他进程正在归档，跳过本次执行");
        return;
    }
    // 执行归档...
    lock.release();
}
```
如果你的项目部署了多个实例，或者防止定时任务重叠执行，FileLock 提供跨进程的同步机制。

### 2.6.2 FileChannel 的局限

FileChannel 是阻塞的，不支持异步 IO。如果需要异步文件操作，需要使用 AsynchronousFileChannel（Java 7 引入）。AsynchronousFileChannel 的 read/write 方法返回 Future，或接受 CompletionHandler 回调。

FileChannel 的 lock() 方法在锁被其他进程持有时会阻塞，建议用 tryLock() 做非阻塞尝试。

---

## 2.7 十八、项目中的数据持久化与文件的关系

### 2.7.1 数据流向

在你的系统中，操作日志经历以下旅程：

```
日志写入 -> ucsp_operation_log (PostgreSQL) 
              |
        定时任务触发归档
              |
        分页读取 + 写入 CSV (临时明文)
              |
        SM3 摘要 + SM4 加密
              |
        .enc 文件 (永久保存) + ucsp_log_archive (归档元数据)
              |
        从 ucsp_operation_log 物理删除
```

### 2.7.2 数据库不可用时文件系统的角色

数据库存储结构化数据（日志记录），文件系统存储大块数据（加密的归档文件）。两者通过 archivePath 字段关联。这种设计的好处：
- 数据库不需要存储大字段（如长文本、二进制数据），查询性能更好
- 文件系统天然支持大文件，不受数据库单行大小的限制
- 归档文件可以独立于数据库进行备份、迁移、校验

### 2.7.3 SQLite 文件型数据库说明

你的项目中使用了 SQLite（通过 sqlite-jdbc 驱动和 sqlite-dialect）。SQLite 是一个文件型数据库，它的数据就是一个 .db 文件。操作 SQLite 本质上也是在操作文件系统。SQLite 不适合高并发写入场景，但适合单机应用、测试环境或数据量不大的离线应用。

---

## 2.8 十九、总结：你的项目告诉你的 I/O 真理

**选择 NIO.2 作为默认 API。** 你的归档模块中，Files.readAllBytes、Files.write、Files.deleteIfExists、Files.newBufferedWriter、Files.exists 几乎覆盖了日常文件操作的全部需求。不要为了"炫技"去用 FileChannel 或 ByteBuffer。

**显式编码是必要的。** 你的模块中每一处文件读写都显式指定了 StandardCharsets.UTF_8。这不是多余，是为了消除环境差异。

**资源管理用 try-with-resources。** 你的 BufferedWriter 放在 try-with-resources 中，文件句柄不会泄漏。一旦忘了 close，症状就是运行一段时间后报 Too many open files。

**缓冲对你的场景至关重要。** 没有 BufferedWriter，你的 CSV 写入性能会差两个数量级。

**异常处理必须包含资源清理。** 你的 catch 块中调用 deleteFileQuietly 清理临时文件，避免残存文件堆积磁盘。

**路径操作用 Paths.get。** 不需要手工拼接带分隔符的字符串。路径穿越检查不能只靠 contains("..")，要用 Path.normalize + startsWith 做真正的校验。

**加密操作处理的是字节。** 你的 SM3 和 SM4 全部以` byte[]` 为操作单元，加解密完成后才用 new String(bytes, charset) 转为文本。

**大文件涉及分块或流式处理。** 几 MB 的文件用 readAllBytes 没问题，几百 MB 的文件必须逐块读取或用 FileChannel 零拷贝。

文件 I/O 本质上只有三件事：把数据从磁盘读到内存（输入）、把数据从内存写回磁盘（输出）、用完关闭资源。剩下的各种类和模式都是在这三件事上做优化和便捷性封装。File 是入口和路径描述，Files 提供便捷的读写操作，Buffered 提高性能，Reader/Writer 处理文本，InputStream/OutputStream 处理二进制，Charset 确保编码正确，try-with-resources 保证资源释放。你的归档模块已经把这些都用上了。
