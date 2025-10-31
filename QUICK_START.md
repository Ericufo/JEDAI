# JEDAI 快速开始指南

## ✅ 骨架搭建完成

所有基础架构已搭建完毕，可以开始并行开发！

## 🎯 当前状态

- ✅ 项目编译通过
- ✅ RAG 模块接口和骨架（`rag/`）
- ✅ Chat 模块接口和骨架（`chat/`）
- ✅ 代码修改模块接口和骨架（`mod/`）
- ✅ ToolWindow 聊天界面骨架
- ✅ 右键菜单 Actions（询问和修改代码）
- ✅ plugin.xml 配置完成

## 🚀 运行插件

```bash
./gradlew runIde
```

这会启动一个 IntelliJ IDEA 沙箱，你可以看到：
- 右侧的 "JEDAI" ToolWindow（聊天界面）
- 编辑器右键菜单中的两个选项：
  - "Ask JEDAI about Selection"
  - "Modify Code with JEDAI"

## 👥 并行开发任务

### 成员 A：RAG 模块
**文件位置**：`src/main/kotlin/com/github/ericufo/jedai/rag/impl/`

**需要实现**：
1. `SimpleRagIndexer.kt`
   - `index()` - 处理 PDF/文档，提取文本和页码，建立索引
   - 推荐工具：Apache PDFBox, Tika, Lucene
   
2. `SimpleRagRetriever.kt`
   - `search()` - 实现检索逻辑（BM25 或向量相似度）
   - 返回带页码的 `RetrievedChunk` 列表

**测试方式**：
```kotlin
val indexer = SimpleRagIndexer()
val materials = listOf(CourseMaterial(File("goal.pdf"), MaterialType.PDF))
indexer.index(materials)

val retriever = SimpleRagRetriever()
val chunks = retriever.search("什么是单例模式？", k = 5)
```

---

### 成员 B：Chat 模块和 UI
**文件位置**：
- `src/main/kotlin/com/github/ericufo/jedai/chat/impl/SimpleAnswerOrchestrator.kt`
- `src/main/kotlin/com/github/ericufo/jedai/toolWindow/JedaiChatPanel.kt`
- `src/main/kotlin/com/github/ericufo/jedai/actions/AskWithSelectionAction.kt`

**需要实现**：
1. `SimpleAnswerOrchestrator.kt`
   - `generateAnswer()` - 调用 LLM API（已配置 LangChain4j）
   - 构建 Prompt：问题 + IDE上下文 + RAG检索结果
   - 解析响应，返回带引用的 `Answer`

2. `JedaiChatPanel.kt`
   - 改进 UI（消息历史、引用卡片、加载状态）
   - 异步调用（避免阻塞 UI）
   
3. `AskWithSelectionAction.kt`
   - 完善：在 ToolWindow 中显示答案（而非弹窗）

**已配置依赖**：
```kotlin
// build.gradle.kts 已包含
implementation("dev.langchain4j:langchain4j:0.35.0")
implementation("dev.langchain4j:langchain4j-open-ai:0.35.0")
```

**测试方式**：
- 运行 `./gradlew runIde`
- 在 JEDAI ToolWindow 输入问题
- 或右键选中代码 → "Ask JEDAI about Selection"

---

### 成员 C：代码修改模块（F2）
**文件位置**：
- `src/main/kotlin/com/github/ericufo/jedai/mod/impl/SimpleCodeModificationService.kt`
- `src/main/kotlin/com/github/ericufo/jedai/mod/DiffViewerHelper.kt`

**需要实现**：
1. `SimpleCodeModificationService.kt`
   - `proposeChanges()` - 调用 LLM 生成修改后的代码
   - 对比原代码生成 `DiffEntry`
   - 实现 `apply()` 函数（使用 `WriteCommandAction`）

2. `DiffViewerHelper.kt` **（已实现基础功能）**
   - 完善：添加 "Apply" 按钮
   - 支持多文件修改

**关键 API**：
```kotlin
// 安全地修改文件
WriteCommandAction.runWriteCommandAction(project) {
    val document = FileDocumentManager.getInstance().getDocument(virtualFile)
    document?.setText(newText)
}
```

**测试方式**：
- 运行 `./gradlew runIde`
- 选中代码 → 右键 → "Modify Code with JEDAI"
- 输入指令（如 "Refactor this using Singleton pattern"）
- 查看 Diff 预览

---

## 📚 关键接口

### RAG 模块
```kotlin
interface RagRetriever {
    fun search(query: String, k: Int = 5): List<RetrievedChunk>
}
```

### Chat 模块
```kotlin
interface AnswerOrchestrator {
    fun generateAnswer(
        userQuestion: String,
        ideContext: IdeContext?,
        retrievedChunks: List<RetrievedChunk>
    ): Answer
}
```

### 代码修改模块
```kotlin
interface CodeModificationService {
    fun proposeChanges(
        instruction: String,
        ideContext: IdeContext?
    ): CodeChangeProposal
}
```

---

## 🔧 常见问题

**Q: 如何调试插件？**
A: 运行 `./gradlew runIde`，在沙箱 IDE 中操作，在原始 IDE 中设置断点调试。

**Q: 如何添加新的依赖？**
A: 在 `build.gradle.kts` 的 `intellijPlatform` 块中添加：
```kotlin
dependencies {
    intellijPlatform {
        implementation("your-dependency:version")
    }
}
```

**Q: 日志在哪里？**
A: 沙箱 IDE 中：Help → Show Log in Explorer

**Q: 如何打包插件？**
A: `./gradlew buildPlugin`，生成的 zip 在 `build/distributions/`

---

## 📖 详细文档

- 完整开发指南：`DEVELOPMENT.md`
- 项目目标：`Goal.md`
- 变更日志：`CHANGELOG.md`

---

## ⚠️ 注意事项

1. **线程安全**：UI 操作在 EDT 线程，后台任务用 `executeOnPooledThread()`
2. **写操作**：使用 `WriteCommandAction` 包裹所有文件修改
3. **错误处理**：外部 API 调用要有 try-catch 和超时
4. **日志记录**：使用 `thisLogger()` 记录关键操作

---

祝开发顺利！🎉

