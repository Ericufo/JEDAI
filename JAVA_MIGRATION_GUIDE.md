# Java 迁移完成指南

## ✅ 迁移完成

所有 Kotlin 代码已成功转换为 Java！

## 📁 新的文件结构

```
src/main/java/com/github/ericufo/jedai/
├── rag/                    # RAG 模块（成员A）
│   ├── RetrievedChunk.java
│   ├── CourseMaterial.java
│   ├── IndexStats.java
│   ├── RagIndexer.java
│   ├── RagRetriever.java
│   └── impl/
│       ├── SimpleRagIndexer.java
│       └── SimpleRagRetriever.java
│
├── chat/                   # Chat 模块（成员B）
│   ├── IdeContext.java
│   ├── Answer.java
│   ├── AnswerOrchestrator.java
│   └── impl/
│       └── SimpleAnswerOrchestrator.java
│
├── mod/                    # 代码修改模块（成员C）
│   ├── DiffEntry.java
│   ├── CodeChangeProposal.java
│   ├── CodeModificationService.java
│   ├── DiffViewerHelper.java
│   └── impl/
│       └── SimpleCodeModificationService.java
│
├── toolWindow/            # UI 模块（成员B）
│   ├── JedaiToolWindowFactory.java
│   └── JedaiChatPanel.java
│
└── actions/               # Action 模块（成员B 和 C）
    ├── AskWithSelectionAction.java
    └── ModifyCodeAction.java
```

## 🔄 主要改动

### 1. 数据类转换
Kotlin 的 `data class` 已转换为标准 Java 类，包含：
- 私有 final 字段
- 构造函数
- Getter 方法

**Kotlin 示例：**
```kotlin
data class RetrievedChunk(
    val content: String,
    val sourceDoc: String,
    val page: Int?
)
```

**Java 转换：**
```java
public class RetrievedChunk {
    private final String content;
    private final String sourceDoc;
    private final Integer page;
    
    public RetrievedChunk(String content, String sourceDoc, Integer page) {
        this.content = content;
        this.sourceDoc = sourceDoc;
        this.page = page;
    }
    
    public String getContent() { return content; }
    public String getSourceDoc() { return sourceDoc; }
    public Integer getPage() { return page; }
}
```

### 2. 接口默认方法
Kotlin 的默认参数值已转换为 Java 的 `default` 方法：

**Kotlin：**
```kotlin
fun search(query: String, k: Int = 5): List<RetrievedChunk>
```

**Java：**
```java
List<RetrievedChunk> search(String query, int k);

default List<RetrievedChunk> search(String query) {
    return search(query, 5);
}
```

### 3. Lambda 表达式
Kotlin 的 lambda 已转换为 Java 的函数式接口：

**Kotlin：**
```kotlin
data class CodeChangeProposal(
    val apply: (Project) -> Unit
)
```

**Java：**
```java
public class CodeChangeProposal {
    private final Consumer<Project> applyFunction;
    
    public void apply(Project project) {
        applyFunction.accept(project);
    }
}
```

### 4. 日志记录
**Kotlin：**
```kotlin
private val logger = thisLogger()
```

**Java：**
```java
private static final Logger LOG = Logger.getInstance(ClassName.class);
```

### 5. 空安全
Kotlin 的 `?` 已转换为 Java 的显式 null 检查：

**Kotlin：**
```kotlin
val text = value?.toString() ?: "default"
```

**Java：**
```java
String text = value != null ? value.toString() : "default";
```

## 🚀 运行测试

```bash
./gradlew runIde
```

应该能正常启动，功能与之前相同。

## 📝 开发注意事项

### 1. Getter/Setter 使用
在 Java 中访问对象属性时，必须使用 getter 方法：

```java
// ❌ 错误（Kotlin 风格）
String doc = chunk.sourceDoc;

// ✅ 正确（Java 风格）
String doc = chunk.getSourceDoc();
```

### 2. 集合操作
Java 的集合 API 与 Kotlin 不同：

```java
// Kotlin: list.isEmpty()
// Java: list.isEmpty()  (相同)

// Kotlin: list.first()
// Java: list.get(0)

// Kotlin: list.map { it.toString() }
// Java: list.stream().map(Object::toString).collect(Collectors.toList())
```

### 3. 字符串模板
```java
// Kotlin: "值是 $value"
// Java: "值是 " + value

// Kotlin: "结果是 ${result.value}"
// Java: "结果是 " + result.getValue()
```

### 4. 异常处理
Java 要求显式处理检查型异常：

```java
try {
    // 可能抛出异常的代码
} catch (IOException e) {
    LOG.error("错误", e);
}
```

## 🔧 IDE 配置

如果你的团队使用 IntelliJ IDEA：

1. **格式化设置**：File → Settings → Editor → Code Style → Java
2. **导入优化**：启用 "Optimize imports on the fly"
3. **代码模板**：Settings → Editor → File and Code Templates

## 📚 Java 开发资源

### 推荐的 Java 编码规范
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [Oracle Java Code Conventions](https://www.oracle.com/java/technologies/javase/codeconventions-contents.html)

### IntelliJ Platform 开发
- [IntelliJ Platform SDK - Java](https://plugins.jetbrains.com/docs/intellij/developing-plugins.html)
- 所有 API 示例在文档中都有 Java 版本

### Java 8+ 特性
项目使用 Java 21，可以使用：
- Lambda 表达式
- Stream API
- Optional 类
- Records（Java 14+，可选）
- Switch 表达式（Java 14+）

## ⚠️ 常见陷阱

### 1. Null 处理
Java 没有 Kotlin 的空安全机制，需要手动检查：

```java
if (value != null) {
    // 使用 value
}
```

或使用 `Optional`：
```java
Optional.ofNullable(value).ifPresent(v -> {
    // 使用 v
});
```

### 2. 不变性
Java 的 `final` 只能防止重新赋值，不能防止对象内部状态改变：

```java
final List<String> list = new ArrayList<>();
list.add("item");  // ✅ 可以修改内容
// list = new ArrayList<>();  // ❌ 不能重新赋值
```

### 3. 默认值
Java 没有默认参数，需要方法重载：

```java
public void method(String param) {
    method(param, "default");
}

public void method(String param, String defaultValue) {
    // 实现
}
```

## 📦 依赖管理

`build.gradle.kts` 中的依赖对 Java 和 Kotlin 都有效：

```kotlin
dependencies {
    intellijPlatform {
        implementation("dev.langchain4j:langchain4j:0.35.0")
        implementation("dev.langchain4j:langchain4j-open-ai:0.35.0")
        implementation("org.apache.tika:tika-core:2.9.2")
    }
}
```

这些库在 Java 代码中可以直接使用。

## 🎯 下一步

1. **编译测试**：`./gradlew build`
2. **运行测试**：`./gradlew test`
3. **运行插件**：`./gradlew runIde`
4. **开始实现**：按照 `QUICK_START.md` 中的任务分配开始开发

---

迁移完成！祝开发顺利！🎉

