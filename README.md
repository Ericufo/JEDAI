# 本分支用于完成软件设计模式项目重构

项目要求见 design_pattern_requirrement.md

# JEDAI - Java Enterprise Development AI Assistant

[![Build Status](https://github.com/Ericufo/JEDAI/workflows/Build/badge.svg)](https://github.com/Ericufo/JEDAI/actions)

<!-- Plugin description -->

JEDAI is an intelligent IntelliJ IDEA plugin designed as a teaching assistant for Java Enterprise Application Development courses. It combines RAG (Retrieval-Augmented Generation) technology with AI-powered code modification capabilities to provide contextual assistance based on course materials.

**Key Features:**

- RAG-powered course Q&A with intelligent question answering
- AI-driven code modification with visual diff preview
- Interactive chat interface with streaming responses
- Context-aware responses based on course materials
- Seamless integration with IntelliJ IDEA's native interface
<!-- Plugin description end -->

JEDAI is an intelligent IntelliJ IDEA plugin designed as a teaching assistant for Java Enterprise Application Development courses. It combines RAG (Retrieval-Augmented Generation) technology with AI-powered code modification capabilities to provide contextual assistance based on course materials.

## Features

### RAG-Powered Course Q&A

- **Intelligent Question Answering**: Ask questions about Java programming concepts and get answers based on course lecture materials
- **Context-Aware Responses**: Answers are generated using relevant course content as context
- **Source Attribution**: Clear indication of whether responses are based on course materials or general knowledge

### AI-Powered Code Modification

- **Smart Code Refactoring**: Right-click on code selections to access AI-driven modification suggestions
- **Visual Diff Preview**: Review AI-generated changes using IntelliJ's built-in diff viewer before applying
- **Batch Modification Support**: Handle complex code transformation tasks efficiently

### Interactive Chat Interface

- **Rich Text Display**: Enhanced chat interface with formatted text, timestamps, and loading animations
- **Streaming Responses**: Real-time streaming answers similar to modern AI chat interfaces
- **Context Memory**: Maintains conversation context with sliding window memory (5 recent exchanges)

### User Experience

- **Theme Customization**: Multiple theme options for personalized interface
- **Font Size Adjustment**: 5-level font size control for optimal readability
- **Seamless Integration**: Fully integrated with IntelliJ IDEA's native interface

## Supported Course Materials

JEDAI can process and index various course material formats:

- **PDF Lecture Slides**: Automatic parsing and indexing of PDF content with page-level granularity
- **Text Documents**: Support for text-based course materials
- **Smart Chunking**: Intelligent text segmentation with 500-character chunks and 100-character overlap

## 🛠️ Quick Start

### Prerequisites

- IntelliJ IDEA 2025.2.3 or later
- Java 17 or later

### Configuration

Update `build.gradle.kts` for faster development startup:

```kotlin
intellijPlatform {
    // Use local IDEA installation for faster startup
    local("D:\\Program Files\\IDEA\\IntelliJ IDEA 2025.2.3")

    // Alternative: Use remote version (comment above line, uncomment below)
    // create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))
}
```

## 💡 Usage

### Using the Chat Interface

1. Open the JEDAI tool window (`View` > `Tool Windows` > `JEDAI`)
2. Type your question about Java programming or course concepts
3. Receive AI-generated answers with course material context

### Code Modification

1. Select code in the editor
2. Right-click and choose `Modify Code with JEDAI`
3. Enter your modification instructions or select from templates
4. Review the proposed changes in the diff viewer
5. Apply the modifications if satisfied

### Contextual Questions

1. Select code in the editor
2. Right-click and choose `Ask JEDAI about Selection`
3. Your question will automatically include the selected code context
4. Receive contextual answers in the chat interface

## 🔧 Technical Architecture

JEDAI is built on modern AI and software engineering principles:

### Core Technologies

- **LangChain4j**: AI orchestration framework for LLM integration
- **DeepSeek V3.2-exp**: Advanced language model for code understanding and generation
- **In-Memory Vector Store**: Efficient semantic search and retrieval
- **IntelliJ Platform API**: Native integration with IDE features

### RAG Implementation

- **Document Processing**: Automatic parsing of PDF and text materials
- **Vector Indexing**: Semantic embedding generation using AllMiniLmL6V2QuantizedEmbeddingModel
- **Similarity Search**: Efficient retrieval of relevant course content
- **Context Augmentation**: Dynamic context injection for LLM prompts

## Acknowledgments

- Built using the [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
- Powered by [LangChain4j](https://github.com/langchain4j/langchain4j) for AI orchestration
- Course materials provided by Java Enterprise Application Development curriculum

---

**JEDAI** - Your intelligent Java development companion within IntelliJ IDEA.
