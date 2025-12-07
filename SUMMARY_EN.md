# JEDAI Design Pattern Refactoring Summary

> Based on `requirrement.md` (Software Design Patterns course project requirements)

## 1. Project Overview

- **Project Name**: JEDAI – IntelliJ IDEA Teaching Assistant Plugin
- **Main Functions**:
  - Course-specific Q&A based on lecture slides (RAG + LLM)
  - Context-aware assistance from editor selection (right-click actions)
  - AI-driven code modification with diff preview and history

This refactoring focuses on introducing and documenting design patterns on top of an already functional plugin, without changing its external behaviour.

## 2. Applied Design Patterns

### 2.1 Creational Pattern – Factory / Abstract Factory

- **Pattern**: Factory / Abstract Factory
- **Files**:
  - `src/main/java/com/github/ericufo/jedai/JedaiComponentFactory.java`
  - `src/main/java/com/github/ericufo/jedai/impl/DefaultJedaiComponentFactory.java`
- **Motivation**:
  - Before refactoring, UI classes (`JedaiChatPanel`) directly instantiated concrete implementations like `SimpleRagRetriever`, `SimpleAnswerOrchestrator`, and `SimpleCodeModificationService`.
  - This made it hard to swap implementations (e.g., mock vs. production, different LLM providers).
- **Refactoring**:
  - Introduced `JedaiComponentFactory` as an abstraction for creating core components.
  - Implemented `DefaultJedaiComponentFactory` which centralizes creation logic and wraps some components with decorators.
- **Benefits**:
  - Decouples UI and actions from concrete implementations.
  - Enables future extension (e.g., different RAG strategies or LLM providers) by adding new factory implementations.

### 2.2 Structural Pattern – Facade

- **Pattern**: Facade
- **Files**:
  - `src/main/java/com/github/ericufo/jedai/JedaiAssistantFacade.java`
  - `src/main/java/com/github/ericufo/jedai/toolWindow/JedaiChatPanel.java`
- **Motivation**:
  - Previously, `JedaiChatPanel` directly coordinated RAG retrieval and LLM orchestration.
  - UI code knew too much about internal subsystems (RAG, Chat, Code Modification).
- **Refactoring**:
  - Introduced `JedaiAssistantFacade` as a single entry point:
    - `askQuestion(...)`
    - `askQuestionStreaming(...)`
    - `modifyCode(...)`
  - `JedaiChatPanel` now calls only the Facade for Q&A, instead of talking to `RagRetriever` and `AnswerOrchestrator` directly.
- **Benefits**:
  - Simplified UI logic and reduced coupling to subsystems.
  - Easier to test and evolve internal implementations without changing UI.

### 2.3 Structural Pattern – Decorator

- **Pattern**: Decorator
- **Files**:
  - `src/main/java/com/github/ericufo/jedai/chat/impl/LoggingAnswerOrchestrator.java`
  - `src/main/java/com/github/ericufo/jedai/impl/DefaultJedaiComponentFactory.java`
- **Motivation**:
  - We needed additional logging and timing for `AnswerOrchestrator` without modifying `SimpleAnswerOrchestrator`.
- **Refactoring**:
  - Implemented `LoggingAnswerOrchestrator` which wraps an `AnswerOrchestrator` and adds logging around method calls.
  - Factory now returns `new LoggingAnswerOrchestrator(new SimpleAnswerOrchestrator())`.
- **Benefits**:
  - Clean separation between core business logic (prompt construction, LLM call) and cross-cutting concerns (logging, timing).
  - Multiple decorators can be added in future without touching the core class.

### 2.4 Behavioral Pattern – Strategy

- **Pattern**: Strategy
- **Files**:
  - `src/main/java/com/github/ericufo/jedai/rag/SearchStrategy.java`
  - `src/main/java/com/github/ericufo/jedai/rag/impl/EmbeddingSearchStrategy.java`
  - `src/main/java/com/github/ericufo/jedai/rag/impl/SimpleRagRetriever.java`
- **Motivation**:
  - The original `SimpleRagRetriever` contained a fixed embedding-based search algorithm.
  - It was difficult to experiment with other retrieval algorithms such as BM25 or hybrid search.
- **Refactoring**:
  - Extracted a `SearchStrategy` interface.
  - Implemented `EmbeddingSearchStrategy` that uses LangChain4j’s embedding store.
  - `SimpleRagRetriever` now delegates to a `SearchStrategy` instance.
- **Benefits**:
  - Makes it trivial to plug in new retrieval strategies by implementing `SearchStrategy`.
  - Keeps `SimpleRagRetriever` focused on orchestration instead of algorithm details.

### 2.5 Behavioral Pattern – Command

- **Pattern**: Command
- **Files**:
  - `src/main/java/com/github/ericufo/jedai/mod/ModificationCommand.java`
  - `src/main/java/com/github/ericufo/jedai/mod/impl/FileModificationCommand.java`
  - `src/main/java/com/github/ericufo/jedai/mod/impl/SimpleCodeModificationService.java`
- **Motivation**:
  - Originally, `SimpleCodeModificationService` directly modified files and recorded history in one long method.
  - This violated single-responsibility and made it hard to extend to batch modifications or undo operations.
- **Refactoring**:
  - Introduced `ModificationCommand` as an abstraction for a code modification action.
  - Implemented `FileModificationCommand`:
    - Encapsulates file path, original code, modified code, and metadata.
    - Executes the change using `WriteCommandAction` and records history.
  - `SimpleCodeModificationService` now creates command objects and executes them instead of applying changes directly.
- **Benefits**:
  - Encapsulates modification logic in reusable command objects.
  - Aligns naturally with history replay and potential undo/redo features.

### 2.6 Extra Pattern – Observer (via Streaming Callbacks)

- **Pattern**: Observer (callback-based variant)
- **Files**:
  - `src/main/java/com/github/ericufo/jedai/chat/StreamingAnswerHandler.java`
  - `src/main/java/com/github/ericufo/jedai/chat/impl/SimpleAnswerOrchestrator.java`
  - `src/main/java/com/github/ericufo/jedai/toolWindow/JedaiChatPanel.java`
- **Motivation**:
  - The plugin already supports streaming token-by-token responses, which is naturally event-driven.
- **Explanation**:
  - `SimpleAnswerOrchestrator` acts as the observable source of streaming events.
  - `StreamingAnswerHandler` represents observers that react to `onNext`, `onComplete`, and `onError` events.
  - `JedaiChatPanel` provides a concrete handler which updates the UI in real time.
- **Benefits**:
  - Achieves a ChatGPT-like streaming experience without blocking the UI.
  - Clean separation between the producer (LLM streaming) and consumer (UI rendering).

## 3. AI Tool Usage During Refactoring

- **LLM as Design Assistant**:
  - Used to explore suitable patterns for each module (e.g., Facade vs. Mediator for coordinating RAG and Chat).
  - Generated initial skeletons for classes like `JedaiAssistantFacade`, `LoggingAnswerOrchestrator`, and `FileModificationCommand`.
- **LLM for Code Refactoring**:
  - Assisted in safely extracting long methods (e.g., original `applyChanges` logic) into command classes.
  - Helped reason about edge cases when moving logic (e.g., document ranges, history recording).
- **Limitations & Lessons Learned**:
  - LLM sometimes suggested overly generic patterns; we had to adapt them to IntelliJ Platform constraints (EDT vs. background threads, `WriteCommandAction`, etc.).
  - Generated code still required careful manual review, especially around thread safety and IntelliJ API usage.

## 4. Discussion of Important Issues and Solutions

1. **Coupling Between UI and Backend Services**

   - _Issue_: UI code was tightly coupled to concrete implementations for RAG, Chat, and Code Modification.
   - _Solution_: Introduced `JedaiComponentFactory` and `JedaiAssistantFacade` to decouple creation and orchestration from presentation.

2. **Cross-Cutting Concerns (Logging & Timing)**

   - _Issue_: Mixing logging with business logic in `SimpleAnswerOrchestrator`.
   - _Solution_: Introduced `LoggingAnswerOrchestrator` using the Decorator pattern.

3. **Algorithm Flexibility in RAG Retrieval**

   - _Issue_: Only a single hard-coded embedding search algorithm was available.
   - _Solution_: Applied the Strategy pattern to separate the retrieval algorithm (`SearchStrategy`) from the retriever orchestration.

4. **Complex Code Modification Flow**

   - _Issue_: Single methods handled everything from LLM calls to document updates and history logging.
   - _Solution_: Command pattern (`ModificationCommand`, `FileModificationCommand`) encapsulates modification logic, making it easier to extend with batch operations and potential undo.

5. **Asynchronous Streaming and UI Responsiveness**
   - _Issue_: Long-running LLM calls could block the UI if not handled carefully.
   - _Solution_: Combined background thread execution (`executeOnPooledThread`), streaming callbacks (`StreamingAnswerHandler`), and EDT updates (`SwingUtilities.invokeLater`), naturally following an Observer-like pattern.

Overall, the refactoring aligns well with the course requirements by applying multiple design patterns on a real-world IntelliJ plugin, while also demonstrating practical use of AI tools in identifying refactoring opportunities, generating code skeletons, and validating design decisions.

