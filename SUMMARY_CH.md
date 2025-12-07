# JEDAI 设计模式重构总结（中文版）

> 基于 `requirrement.md`（软件设计模式课程期末项目要求）

## 1. 项目概述

- **项目名称**：JEDAI – IntelliJ IDEA 教学助教插件
- **主要功能**：
  - 基于课程讲义（slides）的 RAG + LLM 课程问答
  - 编辑器右键、自动携带代码上下文的智能问答
  - AI 驱动的代码修改（Diff 预览 + 历史记录）

本次重构的目标是在“原有功能基本可用”的前提下，引入并落实多种设计模式，对结构进行优化，而不改变对用户暴露的主要行为。

---

## 2. 已应用的设计模式

### 2.1 创建型模式 – Factory / Abstract Factory

- **模式**：工厂 / 抽象工厂
- **涉及文件**：
  - `JedaiComponentFactory.java`
  - `impl/DefaultJedaiComponentFactory.java`
- **动机**：
  - 重构前：`JedaiChatPanel` 等 UI 类直接 `new SimpleRagRetriever`、`new SimpleAnswerOrchestrator`、`new SimpleCodeModificationService`，UI 与具体实现高度耦合。
- **重构做法**：
  - 定义 `JedaiComponentFactory` 接口，统一负责创建核心组件。
  - 实现 `DefaultJedaiComponentFactory`，集中封装“选用哪个具体类”的决策。
- **收益**：
  - UI / Action 只依赖工厂 & 接口，不依赖具体实现。
  - 方便后续切换不同 LLM / 不同 RAG 实现，或引入测试用 mock 实现。

---

### 2.2 结构型模式 – Facade（外观）

- **模式**：Facade
- **涉及文件**：
  - `JedaiAssistantFacade.java`
  - `toolWindow/JedaiChatPanel.java`
- **动机**：
  - 重构前：`JedaiChatPanel` 要自己做很多事：RAG 检索、调用 LLM、处理代码修改，逻辑杂糅在 UI 类中。
- **重构做法**：
  - 新增 `JedaiAssistantFacade`：
    - `askQuestion(...)`
    - `askQuestionStreaming(...)`
    - `modifyCode(...)`
  - 面板和 Action 只调用 Facade，而不再直接操作 `RagRetriever` / `AnswerOrchestrator` / `CodeModificationService`。
- **收益**：
  - 上层 UI 代码极大简化，只关注“我要问问题 / 我要改代码”。
  - 内部实现变化（比如 RAG 策略、LLM 提供商变化）不会影响 UI。

---

### 2.3 结构型模式 – Decorator（装饰器）

- **模式**：Decorator
- **涉及文件**：
  - `chat/impl/LoggingAnswerOrchestrator.java`
  - `impl/DefaultJedaiComponentFactory.java`
- **动机**：
  - 希望为 `AnswerOrchestrator` 增加日志与耗时统计，但又不想在 `SimpleAnswerOrchestrator` 里堆日志代码。
- **重构做法**：
  - 新增 `LoggingAnswerOrchestrator`，实现 `AnswerOrchestrator` 接口，内部持有一个 `delegate`。
  - 工厂中创建顺序：`new SimpleAnswerOrchestrator()` → 外面再包一层 `new LoggingAnswerOrchestrator(core)`。
- **收益**：
  - 业务逻辑与日志逻辑解耦。
  - 若后续需要再增加监控、埋点，只需再包一层 Decorator 即可。

---

### 2.4 行为型模式 – Strategy（策略）

- **模式**：Strategy
- **涉及文件**：
  - `rag/SearchStrategy.java`
  - `rag/impl/EmbeddingSearchStrategy.java`
  - `rag/impl/SimpleRagRetriever.java`
- **动机**：
  - 原 `SimpleRagRetriever` 把“嵌入检索算法”写死在类里面，想切换其他算法就必须改类本身。
- **重构做法**：
  - 提取 `SearchStrategy` 接口，表示“如何根据 query 检索 Top-k 结果”的策略。
  - 实现 `EmbeddingSearchStrategy`，复用原 Embedding 逻辑。
  - `SimpleRagRetriever` 中组合一个 `SearchStrategy` 实例，`search()` 只负责调用策略。
- **收益**：
  - 方便平行增加 BM25、Hybrid 等策略，只需新增策略类，不动现有调用方。
  - 检索器职责更清晰：自己不做算法，只负责“调用谁”。

---

### 2.5 行为型模式 – Command（命令）

- **模式**：Command
- **涉及文件**：
  - `mod/ModificationCommand.java`
  - `mod/impl/FileModificationCommand.java`
  - `mod/impl/SimpleCodeModificationService.java`
- **动机**：
  - 原先 `SimpleCodeModificationService` 里有一个很长的 `applyChanges` 方法：
    - 负责找 VirtualFile、改 Document、保存、格式化、记录历史；职责过多，不利扩展。
- **重构做法**：
  - 定义 `ModificationCommand` 接口，表示“一次代码修改操作”。
  - 实现 `FileModificationCommand`：
    - 封装文件路径、原代码、新代码、语言、指令等信息；
    - `execute()` 内部完成修改 + 保存 + 格式化 + 写历史。
  - `SimpleCodeModificationService`：
    - 在 `proposeChanges()` / `proposeBatchChanges()` 中只负责：
      - 调用 LLM 得到新代码
      - 构建 `DiffEntry`
      - 创建 `FileModificationCommand` 并在 `CodeChangeProposal.apply` 中执行。
- **收益**：
  - “生成改动”与“应用改动”解耦，更容易做批量修改和（未来）撤销/重放。
  - 历史记录管理与 UI 都可以围绕命令对象来设计。

---

### 2.6 额外模式 – Observer（观察者，回调版）

- **模式**：Observer（基于回调的实现）
- **涉及文件**：
  - `chat/StreamingAnswerHandler.java`
  - `chat/impl/SimpleAnswerOrchestrator.java`
  - `toolWindow/JedaiChatPanel.java`
- **说明**：
  - `SimpleAnswerOrchestrator.generateAnswerStreaming(...)` 作为“被观察者”，在 LLM 流式返回时不断触发回调。
  - `StreamingAnswerHandler` 作为“观察者接口”，定义 `onNext` / `onComplete` / `onError`。
  - `JedaiChatPanel` 中的匿名实现负责把 token 刷新到 UI。
- **收益**：
  - 自然实现 ChatGPT 式流式输出，不阻塞 UI。
  - 生产者（LLM Streaming）与消费者（UI 渲染）解耦。

---

## 3. 重构过程中的 AI 工具使用

- 利用 LLM 辅助：
  - 讨论与评估不同模式选择（例如 Facade vs. Mediator）。
  - 生成模式骨架代码，再由人工微调以满足 IntelliJ SDK 约束（EDT、WriteCommandAction 等）。
- 使用体验：
  - 对于“提炼接口、抽取类”这类机械性重构，AI 可以显著加快节奏。
  - 但在涉及线程安全、IDE 专有 API 时，需要人工复查和本地运行验证。

---

## 4. 关键问题与解决方案讨论

1. **UI 与后端服务强耦合**

   - 问题：面板直接依赖多个具体实现类，改一个子模块会牵一大片。
   - 解决：通过 `JedaiComponentFactory` + `JedaiAssistantFacade` 解耦创建与编排逻辑。

2. **日志与业务逻辑混杂**

   - 问题：在 `SimpleAnswerOrchestrator` 中直接写日志，容易让类越来越“胖”。
   - 解决：用 `LoggingAnswerOrchestrator` 装饰器，将日志抽离出去。

3. **RAG 检索算法缺乏扩展性**

   - 问题：只能用 Embedding 检索，难以实验其他算法。
   - 解决：采用 Strategy 模式，将算法抽象为 `SearchStrategy`。

4. **代码修改流程过于复杂**

   - 问题：单个方法同时负责调用 LLM、修改文档、格式化、记录历史。
   - 解决：引入 Command 模式，把“应用修改”封装进 `FileModificationCommand`。

5. **流式调用下的 UI 响应性**
   - 问题：如果直接在 EDT 上跑网络 IO，会导致 IDE 假死。
   - 解决：使用后台线程 + Streaming 回调 + `invokeLater` 回到 EDT，形成一个类似 Observer 的事件流架构。

整体来看，本次重构在不破坏原有功能的前提下，引入了 5 种课堂设计模式 + 1 种额外模式，并结合 AI 工具完成了设计与实现，对课程要求有较完整的覆盖。
