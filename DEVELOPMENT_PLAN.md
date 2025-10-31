# JEDAI 开发指南

## 项目概述

JEDAI 是一个 IntelliJ IDEA 插件，作为 Java 企业应用开发课程的 Teaching Assistant（TA），提供基于 RAG 的问答和代码修改功能。

## 项目结构

```
src/main/kotlin/com/github/ericufo/jedai/
├── rag/                    # RAG模块（成员A负责）
│   ├── RetrievedChunk.kt      # 检索到的知识块数据类
│   ├── CourseMaterial.kt      # 课程材料数据类
│   ├── IndexStats.kt          # 索引统计信息
│   ├── RagIndexer.kt          # 索引器接口
│   ├── RagRetriever.kt        # 检索器接口
│   └── impl/                  # 实现类
│       ├── SimpleRagIndexer.kt
│       └── SimpleRagRetriever.kt
│
├── chat/                   # 聊天编排模块（成员B负责）
│   ├── IdeContext.kt          # IDE上下文数据类
│   ├── Answer.kt              # 答案数据类
│   ├── AnswerOrchestrator.kt  # 答案编排器接口
│   └── impl/
│       └── SimpleAnswerOrchestrator.kt
│
├── mod/                    # 代码修改模块（成员C负责）
│   ├── DiffEntry.kt           # Diff条目数据类
│   ├── CodeChangeProposal.kt  # 代码修改提案
│   ├── CodeModificationService.kt  # 代码修改服务接口
│   ├── DiffViewerHelper.kt    # Diff查看器辅助类
│   └── impl/
│       └── SimpleCodeModificationService.kt
│
├── toolWindow/            # UI模块（成员B负责）
│   ├── MyToolWindowFactory.kt  # ToolWindow工厂
│   └── JedaiChatPanel.kt      # 聊天面板UI
│
├── actions/               # Action模块（成员B和C负责）
│   ├── AskWithSelectionAction.kt   # 右键询问Action
│   └── ModifyCodeAction.kt         # 右键修改代码Action
│
└── services/              # 服务模块
    └── MyProjectService.kt
```

## 并行开发任务分配

### 成员A：RAG模块（知识库与检索）

**职责**：
- 实现课程材料的文本抽取（PDF、Markdown等）
- 实现文本分块，保留页码信息
- 实现向量化和索引（Lucene或向量数据库）
- 实现检索逻辑（BM25或向量相似度）

**需要实现的接口**：
- `RagIndexer.index()` - 索引课程材料
- `RagRetriever.search()` - 检索相关知识块

**开发建议**：
1. 使用 Apache PDFBox 提取PDF文本和页码
2. 使用 Apache Tika 处理多种文档格式
3. 使用 Apache Lucene 建立全文索引（BM25）
4. 或使用向量数据库（如本地SQLite存储向量）
5. 集成 Embedding API（OpenAI/DeepSeek）进行向量化

**关键文件**：
- `rag/impl/SimpleRagIndexer.kt`
- `rag/impl/SimpleRagRetriever.kt`

---

### 成员B：UI与聊天编排模块

**职责**：
- 完善 ToolWindow 聊天UI（消息历史、引用展示）
- 实现答案编排器（调用LLM生成答案）
- 集成 RAG 检索结果
- 实现右键菜单Action的完整功能

**需要实现的接口**：
- `AnswerOrchestrator.generateAnswer()` - 生成答案
- `JedaiChatPanel` - 完善UI展示

**开发建议**：
1. 使用 LangChain4j（已在build.gradle.kts中配置）调用LLM
2. 构建Prompt：包含问题、IDE上下文、检索到的知识块
3. 解析LLM响应，提取答案和引用
4. 在UI中展示答案和引用卡片
5. 实现异步调用，避免阻塞UI线程

**关键文件**：
- `chat/impl/SimpleAnswerOrchestrator.kt`
- `toolWindow/JedaiChatPanel.kt`
- `actions/AskWithSelectionAction.kt`

---

### 成员C：代码修改模块（F2功能）

**职责**：
- 实现代码修改提案生成（调用LLM生成修改后的代码）
- 实现Diff查看器集成
- 实现代码应用的逻辑（使用WriteCommandAction）
- 完善右键菜单Action

**需要实现的接口**：
- `CodeModificationService.proposeChanges()` - 生成修改提案
- `DiffViewerHelper.showDiff()` - 显示Diff预览
- `CodeChangeProposal.apply` - 应用代码修改

**开发建议**：
1. 使用LLM生成修改后的代码（可以是完整文件或补丁）
2. 对比原代码生成Diff
3. 使用 `DiffManager.getInstance().showDiff()` 显示Diff预览
4. 使用 `WriteCommandAction.runWriteCommandAction()` 安全地应用修改
5. 支持多文件修改

**关键文件**：
- `mod/impl/SimpleCodeModificationService.kt`
- `mod/DiffViewerHelper.kt`
- `actions/ModifyCodeAction.kt`

---

## 如何运行和测试

### 1. 运行插件（沙箱IDE）

```bash
./gradlew runIde
```

这会启动一个带插件的IntelliJ IDEA沙箱实例。

### 2. 在沙箱中测试

1. 打开右侧的 "JEDAI" ToolWindow
2. 输入问题测试聊天功能
3. 在编辑器中选中代码，右键选择 "Ask JEDAI about Selection"
4. 右键选择 "Modify Code with JEDAI" 测试代码修改功能

### 3. 构建插件

```bash
./gradlew buildPlugin
```

生成的插件zip文件在 `build/distributions/` 目录。

---

## 接口约定

### RAG模块接口

```kotlin
// 检索知识块
val chunks: List<RetrievedChunk> = ragRetriever.search(query, k = 5)

// 每个chunk包含：
// - content: String（知识块内容）
// - sourceDoc: String（来源文档名）
// - page: Int?（页码）
// - score: Double（相关性分数）
```

### Chat模块接口

```kotlin
// 生成答案
val answer: Answer = answerOrchestrator.generateAnswer(
    userQuestion = "问题",
    ideContext = IdeContext(...),
    retrievedChunks = chunks
)

// Answer包含：
// - content: String（答案内容）
// - citations: List<RetrievedChunk>（引用列表）
// - isGeneralKnowledge: Boolean（是否基于通用知识）
```

### 代码修改模块接口

```kotlin
// 生成修改提案
val proposal: CodeChangeProposal = codeModificationService.proposeChanges(
    instruction = "重构这个方法，应用单例模式",
    ideContext = IdeContext(...)
)

// 显示Diff预览
DiffViewerHelper.showDiff(project, proposal)

// 应用修改（用户确认后）
proposal.apply(project)
```

---

## 开发注意事项

1. **线程安全**：所有UI操作必须在EDT线程，后台任务使用`ApplicationManager.getApplication().executeOnPooledThread()`
2. **代码修改安全**：使用`WriteCommandAction`包裹所有文件写入操作
3. **错误处理**：所有外部API调用都要有try-catch和超时处理
4. **资源管理**：及时关闭文件流、数据库连接等资源
5. **日志记录**：使用`thisLogger()`记录关键操作和错误

---

## 下一步开发建议

### 里程碑1（基础功能）
- [ ] 成员A：完成PDF文本抽取和基本索引
- [ ] 成员B：完成ToolWindow UI骨架和LLM调用
- [ ] 成员C：完成单文件Diff显示

### 里程碑2（功能完善）
- [ ] 成员A：完成向量检索和rerank
- [ ] 成员B：完成答案生成和引用展示
- [ ] 成员C：完成代码应用逻辑

### 里程碑3（优化）
- [ ] 所有成员：错误处理、性能优化、用户体验改进

---

## 常见问题

**Q: 如何添加新的依赖？**
A: 在 `build.gradle.kts` 的 `dependencies` 块中添加，然后在 `gradle/libs.versions.toml` 中管理版本。

**Q: 如何调试插件？**
A: 运行 `./gradlew runIde`，在沙箱IDE中设置断点，在原始IDE中调试。

**Q: 如何查看日志？**
A: 在沙箱IDE中，Help -> Show Log in Explorer，查看日志文件。

---

祝开发顺利！

