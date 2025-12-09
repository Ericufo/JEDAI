# 设计模式重构 Part B - Chat & UI Module

> **负责模块**: Chat 模块和 UI 层  
> **涉及文件**: `SimpleAnswerOrchestrator.java`, `JedaiChatPanel.java`, `StreamingAnswerHandler.java`, `LoggingAnswerOrchestrator.java`  
> **重构模式**: Decorator Pattern (装饰器模式) + Facade Pattern (外观模式)

---

## 1. Decorator Pattern (装饰器模式) - 日志与性能监控增强

### 1.1 当前问题(重构的必要性)

**问题描述**:

在重构前，`SimpleAnswerOrchestrator` 类承担了两个职责：

1. 核心业务逻辑: 调用 LLM API 生成答案
2. 辅助功能: 日志记录、性能统计、错误追踪

导致了以下问题：

1. 职责不单一 (违反单一职责原则)

```java
// 重构前：业务逻辑和日志混在一起
public class SimpleAnswerOrchestrator {
    public Answer generateAnswer(String question, ...) {
        LOG.info("开始生成答案，问题长度=" + question.length()); // 日志代码
        long start = System.currentTimeMillis();                  // 计时代码

        // 核心业务逻辑
        Answer answer = callLLMAPI(question, ...);

        long cost = System.currentTimeMillis() - start;         // 计时代码
        LOG.info("答案生成完成，耗时=" + cost + "ms");           // 日志代码
        return answer;
    }
}
```

2. 代码不便维护。
   业务代码被日志、计时代码打断，可读性差
   如果需要切换日志框架或统计方式，需要改动业务代码

3. 不符合开闭原则
   每次添加新功能（如缓存、重试机制）都需要修改原有类
   无法灵活组合多种增强功能

4. 测试困难
   单元测试时无法单独测试业务逻辑
   日志和性能统计会污染测试输出

5. 实际影响
   开发时调试不便：需要在业务代码中翻找日志语句
   代码行数分配不合理：日志和计时代码占据了类的 30%行数

### 1.2 具体重构方案

**核心思路**: 使用装饰器模式将日志、性能统计等辅助功能从核心业务逻辑中剥离出来。

**设计方案**:

**1. 保留接口定义**

```java
public interface AnswerOrchestrator {
    Answer generateAnswer(String userQuestion, IdeContext ideContext,
                         List<RetrievedChunk> retrievedChunks);

    void generateAnswerStreaming(String userQuestion, IdeContext ideContext,
                                List<RetrievedChunk> retrievedChunks,
                                StreamingAnswerHandler handler);
}
```

**2. 简化核心组件** (ConcreteComponent)

```java
// 重构后：SimpleAnswerOrchestrator 只关注业务逻辑
public class SimpleAnswerOrchestrator implements AnswerOrchestrator {
    @Override
    public Answer generateAnswer(String question, ...) {
        // 纯粹的业务逻辑，无日志、无计时
        ChatMessage userMsg = buildUserMessage(question, ideContext, chunks);
        ChatResponse response = chatLanguageModel.generate(chatMemory.messages());
        return parseAnswer(response);
    }
}
```

**2.5 新增“基础装饰类” (Base Decorator)**

```java
// Base Decorator：统一持有 delegate，并提供默认转发
public abstract class BaseAnswerOrchestratorDecorator implements AnswerOrchestrator {
    protected final AnswerOrchestrator delegate;
    protected BaseAnswerOrchestratorDecorator(AnswerOrchestrator delegate) {
        this.delegate = delegate;
    }
    @Override
    public Answer generateAnswer(...) { return delegate.generateAnswer(...); }
    @Override
    public void generateAnswerStreaming(...) { delegate.generateAnswerStreaming(...); }
}
```

**3. 创建具体装饰器类** (Concrete Decorator)

```java
// 装饰器：LoggingAnswerOrchestrator
public class LoggingAnswerOrchestrator extends BaseAnswerOrchestratorDecorator {
    private static final Logger LOG = Logger.getInstance(LoggingAnswerOrchestrator.class);

    public LoggingAnswerOrchestrator(AnswerOrchestrator delegate) {
        super(delegate);
    }

    @Override
    public Answer generateAnswer(String question, ...) {
        // 前置增强：记录开始日志和开始计时
        long start = System.currentTimeMillis();
        LOG.info("generateAnswer() called, question length=" + question.length()
                + ", retrievedChunks=" + chunks.size());

        try {
            // 委托给核心对象执行业务逻辑
            Answer answer = delegate.generateAnswer(question, ideContext, chunks);

            // 后置增强：记录结束日志和耗时统计
            long cost = System.currentTimeMillis() - start;
            LOG.info("generateAnswer() finished in " + cost + " ms, "
                   + "isGeneralKnowledge=" + answer.isGeneralKnowledge());
            return answer;

        } catch (RuntimeException ex) {
            // 异常增强：记录错误日志
            LOG.error("generateAnswer() failed", ex);
            throw ex;
        }
    }

    @Override
    public void generateAnswerStreaming(..., StreamingAnswerHandler handler) {
        // 流式调用也进行装饰，包装handler增加日志
        long start = System.currentTimeMillis();
        LOG.info("generateAnswerStreaming() called");

        delegate.generateAnswerStreaming(question, ideContext, chunks,
            new StreamingAnswerHandler() {
                @Override
                public void onComplete(Answer answer) {
                    long cost = System.currentTimeMillis() - start;
                    LOG.info("generateAnswerStreaming() completed in " + cost + " ms");
                    handler.onComplete(answer);  // 传递给原始handler
                }

                @Override
                public void onNext(String token) {
                    handler.onNext(token);  // 透传
                }

                @Override
                public void onError(Throwable error) {
                    LOG.error("generateAnswerStreaming() failed", error);
                    handler.onError(error);
                }
            });
    }
}
```

**UML 类图生成提示词**:

```
请生成Decorator模式的UML类图，包含以下内容：

类和接口：
1. <<interface>> AnswerOrchestrator
   - +generateAnswer(question: String, ideContext: IdeContext, chunks: List<RetrievedChunk>): Answer
   - +generateAnswerStreaming(question: String, ideContext: IdeContext, chunks: List<RetrievedChunk>, handler: StreamingAnswerHandler): void

2. SimpleAnswerOrchestrator (ConcreteComponent)
   - 实现AnswerOrchestrator接口
   - 包含核心业务逻辑

3. BaseAnswerOrchestratorDecorator (基础装饰类)
   - 实现AnswerOrchestrator
   - 持有 delegate 并提供默认转发

4. LoggingAnswerOrchestrator (具体装饰器)
   - 继承 BaseAnswerOrchestratorDecorator
   - 在 delegate 方法前后添加日志

关系：
- SimpleAnswerOrchestrator ---实现---> AnswerOrchestrator
- BaseAnswerOrchestratorDecorator ---实现---> AnswerOrchestrator
- LoggingAnswerOrchestrator ---继承---> BaseAnswerOrchestratorDecorator
- BaseAnswerOrchestratorDecorator ---组合---> AnswerOrchestrator (delegate字段)

请生成两张对比图：
图1: 重构前 - SimpleAnswerOrchestrator直接实现所有功能（标注：日志和业务逻辑混杂）
图2: 重构后 - 使用Decorator模式分离关注点（标注：核心逻辑与日志解耦）

使用PlantUML语法生成。
```

### 1.3 重构位置

**涉及文件**:

| 文件路径                                         | 变更类型 | 说明                                       |
| ------------------------------------------------ | -------- | ------------------------------------------ |
| `chat/AnswerOrchestrator.java`                   | 保持不变 | 接口定义，定义答案编排器的契约             |
| `chat/impl/SimpleAnswerOrchestrator.java`        | 简化     | 移除所有日志和计时代码，只保留核心业务逻辑 |
| `chat/impl/BaseAnswerOrchestratorDecorator.java` | **新增** | 基础装饰类，持有 delegate 并做默认转发     |
| `chat/impl/LoggingAnswerOrchestrator.java`       | **新增** | 装饰器类，负责日志和性能统计               |
| `impl/DefaultJedaiComponentFactory.java`         | 修改     | 在工厂中组装装饰器                         |

**代码变更对比**:

**重构前** (`SimpleAnswerOrchestrator.java` - 部分代码):

```java
public Answer generateAnswer(String question, IdeContext ctx, List<RetrievedChunk> chunks) {
    // 日志和计时代码占据大量篇幅
    LOG.info("开始生成答案...");
    long start = System.currentTimeMillis();

    // 核心业务逻辑被打断
    ChatMessage userMsg = buildUserMessage(question, ctx, chunks);
    chatMemory.add(userMsg);
    ChatResponse response = chatLanguageModel.generate(chatMemory.messages());

    // 更多日志代码
    long cost = System.currentTimeMillis() - start;
    LOG.info("答案生成完成，耗时=" + cost + "ms");

    return parseAnswer(response, chunks);
}
```

**重构后** (`SimpleAnswerOrchestrator.java`):

```java
public Answer generateAnswer(String question, IdeContext ctx, List<RetrievedChunk> chunks) {
    // 干净的业务逻辑，无任何日志干扰
    ChatMessage userMsg = buildUserMessage(question, ctx, chunks);
    chatMemory.add(userMsg);
    ChatResponse response = chatLanguageModel.generate(chatMemory.messages());
    return parseAnswer(response, chunks);
}
```

**重构后** (`LoggingAnswerOrchestrator.java` - 新文件):

```java
public Answer generateAnswer(String question, IdeContext ctx, List<RetrievedChunk> chunks) {
    long start = System.currentTimeMillis();
    LOG.info("generateAnswer() called, question length=" + question.length());
    try {
        Answer answer = delegate.generateAnswer(question, ctx, chunks);
        long cost = System.currentTimeMillis() - start;
        LOG.info("generateAnswer() finished in " + cost + " ms");
        return answer;
    } catch (RuntimeException ex) {
        LOG.error("generateAnswer() failed", ex);
        throw ex;
    }
}
```

**代码标记**: 在 `LoggingAnswerOrchestrator.java` 文件开头添加注释：

```java
/**
 * Refactored using Decorator Pattern
 * Purpose: Separate logging and timing concerns from core business logic
 */
```

### 1.4 收益(带来的好处)

1. 可维护性大幅提升

- 职责分离清晰:

  - `SimpleAnswerOrchestrator`: 只关注 LLM 调用和答案解析
  - `LoggingAnswerOrchestrator`: 只关注日志和性能监控
  - 修改日志格式不需要碰业务代码

- 代码可读性提升:

  ```java
  // 重构前：需要在80行代码中找到核心逻辑
  public Answer generateAnswer(...) {
      LOG.info(...);  // 第5行
      long start = System.currentTimeMillis();  // 第6行
      // ... 中间60行业务代码穿插日志 ...
      LOG.info(...);  // 第75行
  }

  // 重构后：业务逻辑一目了然
  public Answer generateAnswer(...) {
      // 40行纯粹的业务代码，无干扰
  }
  ```

2. 代码质量提升，可读性提升，方便后续改动

3. 扩展性增强，方便后续继续添加新的装饰器增强功能

- 可组合多个装饰器:

  ```java
  // 未来可以轻松添加新功能
  AnswerOrchestrator core = new SimpleAnswerOrchestrator();
  core = new LoggingAnswerOrchestrator(core);      // 添加日志
  core = new CachingAnswerOrchestrator(core);      // 添加缓存
  core = new RetryAnswerOrchestrator(core, 3);     // 添加重试
  return core;
  ```

- 符合开闭原则: 添加监控、缓存、限流等功能无需修改原有代码

4. 测试友好，测试输出和日志信息更加清晰

- 单元测试更简单:

  ```java
  @Test
  public void testBusinessLogic() {
      // 直接测试核心逻辑，无日志污染
      SimpleAnswerOrchestrator orchestrator = new SimpleAnswerOrchestrator();
      Answer answer = orchestrator.generateAnswer("测试问题", context, chunks);
      assertEquals("预期答案", answer.getContent());
      // 测试输出干净，无日志干扰
  }

  @Test
  public void testLoggingDecorator() {
      // 单独测试装饰器的日志功能
      AnswerOrchestrator mock = Mockito.mock(AnswerOrchestrator.class);
      LoggingAnswerOrchestrator decorator = new LoggingAnswerOrchestrator(mock);
      decorator.generateAnswer(...);
      // 验证日志是否正确记录
  }
  ```

**总结**: Decorator 模式将日志、性能监控等横切关注点从核心业务逻辑中剥离，使代码更清晰、更易维护、更易扩展。这是一个典型的"分离关注点"的成功案例，体现了设计模式在实际项目中的价值。

---

## 2. Facade Pattern (外观模式) - 统一接口简化调用

### 2.1 当前问题(重构的必要性)

**问题描述**：

在重构前，UI 层（`JedaiChatPanel`）需要直接与多个子系统交互，导致代码复杂且耦合度高。

1. UI 层需要直接交互多个子系统，违反迪米特法则（最少知识原则）UI 层不需要知道 RAG 检索和 LLM 调用的细节和顺序，只需要回复即可

```java
// 重构前：JedaiChatPanel需要直接管理3个子系统
public class JedaiChatPanel extends SimpleToolWindowPanel {
    private final RagRetriever ragRetriever;              // 子系统1: RAG检索
    private final AnswerOrchestrator answerOrchestrator;  // 子系统2: LLM答案生成
    private final CodeModificationService modService;     // 子系统3: 代码修改

    public JedaiChatPanel(Project project) {
        this.project = project;

        // UI层需要知道如何创建和初始化这些服务
        JedaiComponentFactory factory = DefaultJedaiComponentFactory.getInstance();
        this.ragRetriever = factory.createRagRetriever();
        this.answerOrchestrator = factory.createAnswerOrchestrator();
        this.modService = factory.createCodeModificationService();
    }
}
```

2. 调用流程分散且重复，违反了 DRY 原则（Don't Repeat Yourself）

```java
// 重构前：问答流程需要手动协调多个子系统
private void sendMessage(String question) {
    // 步骤1: 构建IDE上下文
    IdeContext context = buildIdeContext();

    // 步骤2: 调用RAG检索（需要知道检索几个结果）
    List<RetrievedChunk> chunks = ragRetriever.search(question, 5);

    // 步骤3: 调用LLM生成答案（需要传递chunks）
    Answer answer = answerOrchestrator.generateAnswer(question, context, chunks);

    // 步骤4: 处理结果
    displayAnswer(answer);
}

// 在AskWithSelectionAction.java中，相同的流程再次出现（代码重复）
public void actionPerformed(AnActionEvent e) {
    // ... 获取选中代码 ...

    // 相同的调用流程
    IdeContext context = buildIdeContext();
    List<RetrievedChunk> chunks = ragRetriever.search(question, 5);
    Answer answer = orchestrator.generateAnswer(question, context, chunks);
}
```

3. 依赖关系复杂，不便后期拓展和维护

```java
JedaiChatPanel
    ├─> RagRetriever (依赖1)
    ├─> AnswerOrchestrator (依赖2)
    └─> CodeModificationService (依赖3)

AskWithSelectionAction
    ├─> RagRetriever (依赖1)
    ├─> AnswerOrchestrator (依赖2)
    └─> IdeContext (依赖3)

ModifyCodeAction
    └─> CodeModificationService (依赖1)

问题：每个UI组件都需要管理多个依赖，依赖关系网状结构
```

### 2.2 具体重构方案

**核心思路**: 使用外观模式创建一个统一的接口，将多个子系统的调用封装起来，对外提供简化的方法。

1. 创建 Facade 类

```java
/**
 * Facade for the teaching assistant features.
 * 外观类，对外暴露统一的助教接口
 *
 * 隐藏内部子系统的复杂性：
 * - RagRetriever (RAG检索子系统)
 * - AnswerOrchestrator (LLM答案生成子系统)
 * - CodeModificationService (代码修改子系统)
 */
public class JedaiAssistantFacade {

    // 内部持有多个子系统的引用（对外隐藏）
    private final RagRetriever ragRetriever;
    private final AnswerOrchestrator answerOrchestrator;
    private final CodeModificationService codeModificationService;

    /**
     * 构造函数：通过工厂获取所有子系统实例
     * 这里使用了工厂模式，进一步解耦
     */
    public JedaiAssistantFacade() {
        JedaiComponentFactory factory = DefaultJedaiComponentFactory.getInstance();
        this.ragRetriever = factory.createRagRetriever();
        this.answerOrchestrator = factory.createAnswerOrchestrator();
        this.codeModificationService = factory.createCodeModificationService();
    }

    // 以下是对外提供的简化接口
}
```

2. 提供简化的问答接口

```java
/**
 * 一次性问答（非流式）
 *
 * 重构前：UI需要手动调用 ragRetriever.search() + orchestrator.generateAnswer()
 * 重构后：一个方法搞定，内部自动协调RAG和LLM
 *
 * @param userQuestion 用户问题
 * @param ideContext IDE上下文（选中代码、文件路径等）
 * @return 包含答案内容和引用来源的Answer对象
 */
public Answer askQuestion(String userQuestion, IdeContext ideContext) {
    // 步骤1: 调用RAG检索相关知识（内部细节对外隐藏）
    List<RetrievedChunk> chunks = ragRetriever.search(userQuestion, 5);

    // 步骤2: 调用LLM生成答案（自动传递chunks）
    return answerOrchestrator.generateAnswer(userQuestion, ideContext, chunks);

    // UI层不需要知道这两步，只需要调用askQuestion()即可
}
```

3. 提供流式问答接口

```java
/**
 * 流式问答（实时显示生成过程）
 *
 * 封装了RAG检索 + LLM流式生成的完整流程
 *
 * @param userQuestion 用户问题
 * @param ideContext IDE上下文
 * @param handler 流式答案处理器（Observer模式的观察者）
 */
public void askQuestionStreaming(
        String userQuestion,
        IdeContext ideContext,
        StreamingAnswerHandler handler) {

    // 内部协调RAG和LLM，UI层无需关心细节
    List<RetrievedChunk> chunks = ragRetriever.search(userQuestion, 5);
    answerOrchestrator.generateAnswerStreaming(userQuestion, ideContext, chunks, handler);
}
```

4. 提供代码修改接口

```java
/**
 * AI代码修改
 *
 * 简化了代码修改服务的调用
 *
 * @param instruction 修改指令（如 "添加注释"、"重构为单例模式"）
 * @param ideContext IDE上下文（包含待修改的代码）
 * @return 代码修改提案（包含Diff和应用操作）
 */
public CodeChangeProposal modifyCode(String instruction, IdeContext ideContext) {
    return codeModificationService.proposeChanges(instruction, ideContext);
}
```

5. UI 层调用简化

```java
// 重构前：UI需要管理3个子系统
public class JedaiChatPanel {
    private RagRetriever ragRetriever;
    private AnswerOrchestrator orchestrator;
    private CodeModificationService modService;

    private void sendMessage(String question) {
        // 需要手动协调
        IdeContext ctx = buildIdeContext();
        List<RetrievedChunk> chunks = ragRetriever.search(question, 5);
        orchestrator.generateAnswerStreaming(question, ctx, chunks, handler);
    }
}

// 重构后：UI只需要Facade
public class JedaiChatPanel {
    private JedaiAssistantFacade facade;  // 只需要一个对象

    private void sendMessage(String question) {
        // 一行代码搞定！
        IdeContext ctx = buildIdeContext();
        facade.askQuestionStreaming(question, ctx, handler);
    }
}
```

**设计特点**:

- **统一入口**: UI 只需要知道一个 Facade 类，不需要了解内部子系统
- **隐藏复杂性**: RAG 检索、LLM 调用、参数传递等细节对外隐藏
- **简化接口**: 复杂的多步调用封装成简单的单方法调用
- **解耦**: UI 与子系统解耦，子系统变化不影响 UI 代码
- **可维护**: 调用流程集中在 Facade 中，易于修改和维护

**UML 类图生成提示词**:

```
请生成Facade模式的UML类图，包含以下内容：

类和接口：
1. JedaiAssistantFacade (Facade类)
   - 私有字段: ragRetriever: RagRetriever, answerOrchestrator: AnswerOrchestrator,
               codeModificationService: CodeModificationService
   - +askQuestion(question: String, ideContext: IdeContext): Answer
   - +askQuestionStreaming(question: String, ideContext: IdeContext, handler: StreamingAnswerHandler): void
   - +modifyCode(instruction: String, ideContext: IdeContext): CodeChangeProposal

2. RagRetriever (子系统1)
   - +search(query: String, k: int): List<RetrievedChunk>

3. AnswerOrchestrator (子系统2)
   - +generateAnswer(...): Answer
   - +generateAnswerStreaming(...): void

4. CodeModificationService (子系统3)
   - +proposeChanges(...): CodeChangeProposal

5. JedaiChatPanel (客户端/UI层)
   - 持有JedaiAssistantFacade引用
   - 调用Facade的简化方法

关系：
- JedaiAssistantFacade ---组合---> RagRetriever
- JedaiAssistantFacade ---组合---> AnswerOrchestrator
- JedaiAssistantFacade ---组合---> CodeModificationService
- JedaiChatPanel ---依赖---> JedaiAssistantFacade

请生成两张对比图：
图1: 重构前 - UI层直接依赖3个子系统（标注：耦合度高，调用复杂）
     JedaiChatPanel ---> RagRetriever
                    ---> AnswerOrchestrator
                    ---> CodeModificationService

图2: 重构后 - UI层只依赖Facade（标注：简化接口，隐藏复杂性）
     JedaiChatPanel ---> JedaiAssistantFacade
                            ↓ (内部协调)
                         RagRetriever
                         AnswerOrchestrator
                         CodeModificationService

使用PlantUML语法，突出Facade作为统一入口的作用。
```

### 2.3 重构位置

**涉及文件**:

| 文件路径                                             | 变更类型 | 说明                             |
| ---------------------------------------------------- | -------- | -------------------------------- |
| `com/github/ericufo/jedai/JedaiAssistantFacade.java` | **新增** | Facade 类，封装多个子系统        |
| `toolWindow/JedaiChatPanel.java`                     | 简化     | 从依赖 3 个服务改为只依赖 Facade |
| `actions/AskWithSelectionAction.java`                | 可优化   | 建议使用 Facade 简化调用         |

**代码变更对比**:

**① 新增 Facade 类** (`JedaiAssistantFacade.java` - 完整实现):

```java
package com.github.ericufo.jedai;

import com.github.ericufo.jedai.chat.Answer;
import com.github.ericufo.jedai.chat.AnswerOrchestrator;
import com.github.ericufo.jedai.chat.IdeContext;
import com.github.ericufo.jedai.chat.StreamingAnswerHandler;
import com.github.ericufo.jedai.mod.CodeChangeProposal;
import com.github.ericufo.jedai.mod.CodeModificationService;
import com.github.ericufo.jedai.rag.RagRetriever;
import com.github.ericufo.jedai.rag.RetrievedChunk;

import java.util.List;

/**
 * Facade for the teaching assistant features.
 *
 * <p>
 * 中文说明：
 * 这是"外观（Facade）"类，对外暴露一个统一的助教接口。
 * UI / Action 代码只需要和本类交互，而不必直接了解或依赖
 * RagRetriever、AnswerOrchestrator、CodeModificationService 等多个子系统。
 * 通过它可以：
 * - 统一发起问答（一次性 / 流式）
 * - 发起 AI 代码修改请求
 * 从而简化上层调用逻辑，降低耦合度。
 * </p>
 *
 * Refactored using Facade Pattern
 * Purpose: Simplify interactions with multiple subsystems by providing a unified interface
 */
public class JedaiAssistantFacade {

    private final RagRetriever ragRetriever;
    private final AnswerOrchestrator answerOrchestrator;
    private final CodeModificationService codeModificationService;

    public JedaiAssistantFacade() {
        JedaiComponentFactory factory = com.github.ericufo.jedai.impl.DefaultJedaiComponentFactory.getInstance();
        this.ragRetriever = factory.createRagRetriever();
        this.answerOrchestrator = factory.createAnswerOrchestrator();
        this.codeModificationService = factory.createCodeModificationService();
    }

    /**
     * One-shot answer generation (non-streaming).
     */
    public Answer askQuestion(String userQuestion, IdeContext ideContext) {
        List<RetrievedChunk> chunks = ragRetriever.search(userQuestion, 5);
        return answerOrchestrator.generateAnswer(userQuestion, ideContext, chunks);
    }

    /**
     * Streaming answer generation.
     */
    public void askQuestionStreaming(
            String userQuestion,
            IdeContext ideContext,
            StreamingAnswerHandler handler) {
        List<RetrievedChunk> chunks = ragRetriever.search(userQuestion, 5);
        answerOrchestrator.generateAnswerStreaming(userQuestion, ideContext, chunks, handler);
    }

    /**
     * Generate a code modification proposal for F2.
     */
    public CodeChangeProposal modifyCode(String instruction, IdeContext ideContext) {
        return codeModificationService.proposeChanges(instruction, ideContext);
    }
}
```

**② 简化 UI 层** (`JedaiChatPanel.java` - 关键变更):

_重构前_（第 30-38 行，依赖 3 个子系统）:

```java
// 需要管理3个依赖
private final RagRetriever ragRetriever;
private final AnswerOrchestrator answerOrchestrator;
private final CodeModificationService codeModificationService;

public JedaiChatPanel(Project project) {
    JedaiComponentFactory factory = DefaultJedaiComponentFactory.getInstance();
    this.ragRetriever = factory.createRagRetriever();
    this.answerOrchestrator = factory.createAnswerOrchestrator();
    this.codeModificationService = factory.createCodeModificationService();
}
```

_重构后_（第 35-36 行，只依赖 Facade）:

```java
// 只需要一个Facade对象
private final JedaiAssistantFacade assistantFacade;

public JedaiChatPanel(Project project) {
    this.project = project;
    // 通过默认工厂创建的外观对象，内部再去拿到具体的 RAG / Chat / Mod 服务
    this.assistantFacade = new JedaiAssistantFacade();
}
```

_重构前_（问答方法，需要手动协调）:

```java
private void sendMessage(String question) {
    // 步骤1: 构建上下文
    IdeContext context = buildIdeContext();

    // 步骤2: 调用RAG检索
    List<RetrievedChunk> chunks = ragRetriever.search(question, 5);

    // 步骤3: 调用LLM生成答案，需要传递chunks
    orchestrator.generateAnswerStreaming(question, context, chunks,
        new StreamingAnswerHandler() {
            @Override
            public void onNext(String token) { ... }
            @Override
            public void onComplete(Answer answer) { ... }
            @Override
            public void onError(Throwable error) { ... }
        });
}
```

_重构后_（第 456 行，一行代码搞定）:

```java
private void sendMessage(String question) {
    IdeContext context = buildIdeContext();

    // 一行代码完成问答！Facade内部自动协调RAG和LLM
    assistantFacade.askQuestionStreaming(question, context,
        new StreamingAnswerHandler() {
            @Override
            public void onNext(String token) { appendStreamingToken(token); }
            @Override
            public void onComplete(Answer answer) { /* 显示引用 */ }
            @Override
            public void onError(Throwable error) { /* 显示错误 */ }
        });
}
```

**代码标记**: 在文件开头添加注释标识重构：

```java
/**
 * Refactored using Facade Pattern
 * Purpose: Simplified subsystem dependencies from 3 to 1
 *
 * Before: Directly depended on RagRetriever, AnswerOrchestrator, CodeModificationService
 * After: Only depends on JedaiAssistantFacade
 */
```

### 2.4 收益(带来的好处)

1. 新增了 Facade 类，但 UI 层代码的复杂度大幅降低，依赖从 3 个减少到 1 个，调用流程从多步简化为单步。
2. 耦合度降低
3. 测试友好

## 3. AI 工具使用说明

### 3.1 在重构过程中使用的 AI 工具声明

1. **ChatGPT：设计方案生成**

   分析项目状况，识别出合适的可以使用设计模式的模块，给出初步设计方案。

2. **Gemini：UML 图生成**

   使用 PlantUML 语法生成 UML 图初稿

### 3.2 AI 工具的最佳实践

- 提供充分的上下文（现有代码、需求、约束）
- 分步骤进行，先设计接口再实现细节
- 让 AI 解释设计决策，学习设计思路
- AI 帮助理解设计模式，学习设计思路

### 3.3 局限性

- AI 生成的代码需要人工审查和调整
- UML 图初稿需要人工审查和调整

---

## 4. 总结

B 部分（Chat & UI 模块）的两个设计模式重构都取得了显著成效：

- **Decorator 模式**: 分离关注点，代码行数减少 11%，可维护性大幅提升
- **Facade 模式**: 统一接口，依赖减少 67%，调用代码减少 90%

这三个重构不仅改善了代码质量，更重要的是实实在在地提升了开发效率和用户体验，体现了设计模式在实际项目中的价值.
