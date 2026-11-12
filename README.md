# JavaDemos

Java 基础与进阶的学习/实验集合，每个 Demo 独立可运行，方便快速验证一个知识点。

## 项目结构

```
JavaDemos/
├── src/main/java/com/example/
│   ├── Patterns/          # 设计模式（单例、策略、装饰、适配器、建造者）
│   ├── debug/             # IDE 调试技巧演示（断点类型、条件表达式、线程调试等）
│   ├── fileIO/            # 文件操作基础
│   ├── stream/            # Stream API
│   └── thread/            # 线程工具类（BatchTask 并行任务、DebugPractice）
│
├── src/test/java/com/example/io/   # Java IO 实战 Lab（1~16）
│
├── juc/                            # 高并发专题
│   └── src/
│       ├── sync/                  # synchronized 原理与用法
│       ├── lock/                  # Lock 基础
│       ├── reentrantlock/         # ReentrantLock 四大应用场景
│       ├── future/                # Future 模式
│       └── cf/                    # CompletableFuture
│
├── docs/                          # 专题文档
└── Stream_API.md                  # Stream API 笔记
```

## 高并发 (juc) 专题

| 目录 | 文件 | 说明 |
|---|---|---|
| `sync/` | `SynchronizedDemo` | synchronized 基本用法 |
| | `ClassLockDemo` | 类锁 vs 对象锁 |
| | `SyncByteAnalyzeDemo` | synchronized 字节码分析 |
| `lock/` | `NonFairLock` | 非公平锁演示 |
| `reentrantlock/` | `RecursiveReentrantLockDemo` | 场景一：递归方法中的可重入 |
| | `BankSystemReentrantLockDemo` | 场景二：类内多方法互相调用（银行转账） |
| | `InheritanceReentrantLockDemo` | 场景三：继承 + 子类重写父类同步方法 |
| | `AdvancedReentrantLockDemo` | 场景四：tryLock / 超时 / 公平锁 |
| `future/` | `FirstFuture` | Future 入门 |
| | `FutureWithExecutor` | Future 配合线程池 |
| | `FutureLostDemo` | Future 的坑：get() 阻塞问题 |
| `cf/` | `CompletableFutureDemo` | CompletableFuture 组合编排 |

## Java IO 实战 Lab

| Lab | 主题 |
|---|---|
| 01 | File 基础 |
| 02 | Path 与路径操作 |
| 03 | Files 工具类 |
| 04 | Stream vs Reader |
| 05 | 缓冲流 |
| 06 | Try-With-Resources |
| 07 | 字符集编码 |
| 08 | 对象序列化 |
| 09 | DataStream |
| 10 | 装饰器模式实战 |
| 11 | 文件读写模板 |
| 12 | 原子写 |
| 13 | Classpath 资源 |
| 14 | RandomAccessFile |
| 15 | FileChannel |
| 16 | ZIP 归档 |

## 设计模式

单例 / 策略 / 装饰器 / 适配器 / 建造者（Phone、Computer 示例）。

## 调试技巧

`src/main/java/com/example/debug/` 下演示：
- 断点类型（行断点、条件断点、方法断点、异常断点）
- Evaluate Expression 在 Debug 时修改变量
- 单步走入 / 步过 / 步出
- 线程调试
- Watch & Variables 面板使用

## 运行方式

项目用 Maven + IntelliJ IDEA 构建，直接打开根目录即可。每个 Demo 都有独立的 `main` 方法，右键 Run 即可运行。

## 持续更新中

计划补充的方向：
- [ ] JVM（内存结构、GC、类加载）
- [ ] 集合源码（HashMap、ConcurrentHashMap）
- [ ] 网络编程（Socket、NIO）
- [ ] 注解 & 反射