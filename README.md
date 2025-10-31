# JEDAI

![Build](https://github.com/Ericufo/JEDAI/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

## Template ToDo list

- [x] Create a new [IntelliJ Platform Plugin Template][template] project.
- [x] Get familiar with the [template documentation][template].
- [x] Adjust the [pluginGroup](./gradle.properties) and [pluginName](./gradle.properties), as well as the [id](./src/main/resources/META-INF/plugin.xml) and [sources package](./src/main/kotlin).
- [ ] Adjust the plugin description in `README` (see [Tips][docs:plugin-description])
- [ ] Review the [Legal Agreements](https://plugins.jetbrains.com/docs/marketplace/legal-agreements.html?from=IJPluginTemplate).
- [ ] [Publish a plugin manually](https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html?from=IJPluginTemplate) for the first time.
- [ ] Set the `MARKETPLACE_ID` in the above README badges. You can obtain it once the plugin is published to JetBrains Marketplace.
- [ ] Set the [Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html?from=IJPluginTemplate) related [secrets](https://github.com/JetBrains/intellij-platform-plugin-template#environment-variables).
- [ ] Set the [Deployment Token](https://plugins.jetbrains.com/docs/marketplace/plugin-upload.html?from=IJPluginTemplate).
- [ ] Click the <kbd>Watch</kbd> button on the top of the [IntelliJ Platform Plugin Template][template] to be notified about releases containing new features and fixes.
- [ ] Configure the [CODECOV_TOKEN](https://docs.codecov.com/docs/quick-start) secret for automated test coverage reports on PRs

<!-- Plugin description -->

This Fancy IntelliJ Platform Plugin is going to be your implementation of the brilliant ideas that you have.

This specific section is a source for the [plugin.xml](/src/main/resources/META-INF/plugin.xml) file which will be extracted by the [Gradle](/build.gradle.kts) during the build process.

To keep everything working, do not remove `<!-- ... -->` sections.

<!-- Plugin description end -->

## Development

### 开发日志

**2025-10-23 更新**

1. **已完成工作**
   - 搭建了项目基本架构
   - 完善了插件 ToolWindow 界面及消息收发逻辑
   - 当前界面可用，暂未接入实际的 LLM API

2. **环境配置说明**
   - 更改了一下环境配置，使用本地 IDEA 安装路径来运行插件（加速启动，避免每次下载）
   - 需要将 `build.gradle.kts` 中的路径修改为你本地的 IDEA 安装路径
   - 若不想使用本地路径，可取消注释，保持原来的
   
   配置位置：`build.gradle.kts`  49 行
   
   ```kotlin
   intellijPlatform {
       // 使用本地IDEA安装路径（需修改为自己的路径）
       local("D:\\Program Files\\IDEA\\IntelliJ IDEA 2025.2.3")
       
       // 如果使用远程版本，注释掉上面一行，取消注释下面的行：
       // create(providers.gradleProperty("platformType"), providers.gradleProperty("platformVersion"))
   }
   ```

**2025-11-1 更新**
1. **已完成工作**
   - **LLM集成**: 成功集成DeepSeek V3.2-exp API
     - 使用LangChain4j框架
     - API Key优先级：环境变量 > 系统属性 > 代码默认值
     - 单例模式缓存模型实例（性能优化）
     - 智能降级机制（LLM失败时自动使用示例实现）
   
   - **核心功能**:
     - AI驱动的代码修改（选中代码 → 右键 → "Modify Code with JEDAI"）
     - 自动代码格式化（应用修改后自动格式化）
     - 自定义指令模板系统（持久化存储，支持添加/删除/管理）
     - 批量修改支持（API已实现：`proposeBatchChanges()`）
     - 完整的修改历史功能（Tools → View JEDAI Modification History）
       - 查看历史Diff
       - 重放历史修改（已修复：现可正确回退到历史代码）
       - 搜索和清空历史
   
   - **目前测试方式**:
     ```bash
     # 可选：设置环境变量（代码中已有默认值）
     export DEEPSEEK_API_KEY="sk-4hrklq5w3w4x7bcz"

     # 启动插件
     ./gradlew runIde
     
     # 使用功能
     # 1. 选中代码 → 右键 → "Modify Code with JEDAI"
     # 2. 输入指令或选择模板
     # 3. 查看Diff → 应用修改
     # 4. Tools → View JEDAI Modification History 查看历史
     ```

     需注意的是：
     由于使用的是免费LLM服务，服务速率有所限制，详情如下：

     RPM=12、RPD=300、TPM=12000；

     服务提供商：无问芯穹<https://docs.infini-ai.com/>
     
     LLM模型选择：DeepSeek V3.2-exp

## Installation

- Using the IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "JEDAI"</kbd> >
  <kbd>Install</kbd>

- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/Ericufo/JEDAI/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

---

Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
