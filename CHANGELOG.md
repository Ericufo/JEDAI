# JEDAI Changelog

## [Unreleased]

## [2025-11-10]
### Added
- UI & RAG modification

## [2025-11-9]
### Added
- 实现流式答案显示（类似 GPT 逐字输出）
- 新增主题切换功能
- 新增用户字体大小调整功能（5 档可选）
- use general knowledge while insufficient context

## [2025-11-3]
### Added
- in memory rag implemented

## [2025-11-2]
### Added
- Rag indexer implemented


## [2025-11-1] Part B：聊天界面与对话管理
### Added
- 聊天界面完善：富文本显示、消息时间戳、加载动画
- LLM 对话集成：完整调用逻辑、对话上下文记忆（5轮对话）
- 右键询问集成："Ask JEDAI about Selection" 功能整合到主聊天窗口
- Markdown 处理策略优化

## [2025-11-1] Part A：LLM 集成与核心功能
### Added
- LLM 集成：成功集成 DeepSeek V3.2-exp API，使用 LangChain4j 框架
- 核心功能：AI 驱动的代码修改、自动代码格式化、自定义指令模板系统
- 批量修改支持、完整的修改历史功能
- 修复工作：去除无用系统模版和注册项，去除test

## [2025-10-23]
### Added
- 搭建项目基本架构
- 完善插件 ToolWindow 界面及消息收发逻辑
- 环境配置说明：使用本地 IDEA 安装路径加速启动

## [Initial]
### Added
- Initial scaffold created from [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)