# AY202526S1-50002870006-Assignment-A（C 组任务文档）

## 一、任务基础信息

- **课程名称**：Java Enterprise Application Development（课程编号：50002870006）
- **作业类型**：Assignment A
- **组别要求**：C 组需完成 F0（基础功能）+ F2（扩展功能 2）

## 二、核心功能实现要求

### （一）F0：基础功能

1. **插件定位与开发**
   - 设计并开发一款作为 Java Enterprise Application Development 课程助教（TA）的 IntelliJ IDEA 插件，需适配 IntelliJ IDEA 环境，确保功能稳定运行。
2. **课程专属 Q&A 聊天机器人（基于 RAG 技术）**
   - 预处理课程资料（如 lecture slides），将资料拆分为可搜索的知识 chunks 并建立索引，形成专属知识库。
   - 当用户提出编程相关问题时，插件需从知识库中检索最相关的 chunks，将这些 chunks 作为上下文信息与原始问题一同发送至 LLM 服务，生成结合通用 Java 编程知识与课程特定可验证信息的回复。
   - 回复需明确标注来源：基于课程资料生成的回复，需注明文档名称及页码/范围；无相关课程资料、依赖 LLM 通用知识生成的回复，需明确说明（如“Response is based on general knowledge; no specific course material is referenced”）。
3. **用户界面设计**
   - 创建嵌入 IntelliJ IDEA 的用户友好界面（如 sidebar 或 tool window），支持用户与 TA 聊天机器人的便捷交互。
4. **上下文感知功能**
   - 插件需感知用户编程上下文，至少在编辑器中实现右键上下文菜单功能：用户选中代码段后，通过右键菜单可快速发起与该代码段相关的问题咨询。

### （二）F2：扩展功能 2（仅 C 组适用）

1. **AI 驱动的源代码修改功能**
   - 支持根据用户指令执行重大代码修改任务，例如基于设计模式重构方法、修复源代码中的 bug 等。
   - 代码修改前需展示建议：通过 IntelliJ IDEA 内置的 diff viewer 以并列对比形式呈现 AI 生成的修改建议，供用户审核；用户确认后，方可应用修改。

## 三、参考资料

1. IntelliJ IDEA 插件开发：https://plugins.jetbrains.com/docs/intellij/developingplugins.html
2. IntelliJ IDEA API 及插件开发咨询：https://intellij-support.jetbrains.com/hc/en-us/community/topics/200366979（有志愿者提供解答）
3. LLM API 推荐（鼓励使用免费服务）：
   - 阿里云百炼：https://bailian.aliyun.com
   - DeepSeek：https://www.deepseek.com
4. LangChain4j：https://github.com/langchain4j/langchain4j
5. Apache Tika：https://tika.apache.org
6. RAG 概念指南：https://aws.amazon.com/what-is/retrieval-augmented-generation
