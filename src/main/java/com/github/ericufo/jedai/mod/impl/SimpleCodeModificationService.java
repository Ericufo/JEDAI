package com.github.ericufo.jedai.mod.impl;

import com.github.ericufo.jedai.chat.IdeContext;
import com.github.ericufo.jedai.mod.*;
import com.intellij.openapi.diagnostic.Logger;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 代码修改服务的完整实现
 * 成员C负责：使用LLM生成代码修改建议并安全应用到文件
 *
 * <p>
 * 重构说明（中文）：
 * 使用命令模式重构：本类在原有基础上引入 Command，将“生成改动”与“应用改动”解耦。
 * - 仍然负责与 LLM 交互、生成修改后的代码文本；
 * - 不再直接修改文件，而是为每次修改创建 {@link ModificationCommand}
 * （当前实现为 {@link FileModificationCommand}），交由命令对象执行；
 * - 批量修改（proposeBatchChanges）通过批量创建并执行命令来完成。
 * 这样可以把“生成改动”和“应用改动”的职责解耦，便于历史记录、批量操作和将来扩展撤销功能。
 * </p>
 */
public class SimpleCodeModificationService implements CodeModificationService {
    private static final Logger LOG = Logger.getInstance(SimpleCodeModificationService.class);

    // DeepSeek API配置
    private static final String DEEPSEEK_BASE_URL = "https://cloud.infini-ai.com/maas/deepseek-v3.2-exp/nvidia";
    private static final String DEEPSEEK_MODEL = "deepseek-v3.2-exp";

    // 默认API Key（建议修改为你自己的API Key）
    // 优先级：环境变量 > 系统属性 > 此处的默认值
    private static final String DEFAULT_API_KEY = "sk-4hrklq5w3w4x7bcz";

    // 缓存LLM模型实例（性能优化）
    private static ChatLanguageModel cachedModel = null;

    /**
     * 批量生成多个文件的修改提案
     * 
     * @param instruction 修改指令
     * @param ideContexts 多个IDE上下文（每个代表一个文件/代码片段）
     * @return 包含多个文件修改的提案
     */
    public CodeChangeProposal proposeBatchChanges(String instruction, List<IdeContext> ideContexts) {
        LOG.info("生成批量代码修改提案：指令='" + instruction + "'，文件数=" + ideContexts.size());

        if (ideContexts == null || ideContexts.isEmpty()) {
            LOG.warn("IDE上下文列表为空");
            return createErrorProposal("No files selected for batch modification");
        }

        try {
            List<DiffEntry> allDiffEntries = new ArrayList<>();
            List<ModificationCommand> commands = new ArrayList<>();

            // 为每个文件生成修改
            for (IdeContext ideContext : ideContexts) {
                if (ideContext.getSelectedCode() == null || ideContext.getSelectedCode().trim().isEmpty()) {
                    LOG.warn("跳过空代码片段：" + ideContext.getFilePath());
                    continue;
                }

                String originalCode = ideContext.getSelectedCode();
                String filePath = ideContext.getFilePath();

                if (filePath == null) {
                    LOG.warn("文件路径为空，跳过");
                    continue;
                }

                // 调用LLM生成修改后的代码
                String modifiedCode = generateModifiedCode(instruction, originalCode, ideContext);

                // 创建DiffEntry
                DiffEntry diffEntry = new DiffEntry(filePath, originalCode, modifiedCode);
                allDiffEntries.add(diffEntry);

                // 使用命令模式重构：每个文件的修改封装为命令对象，后续统一执行
                final String finalFilePath = filePath;
                final String finalModifiedCode = modifiedCode;
                final String finalOriginalCode = originalCode;
                final String finalLanguage = ideContext.getLanguage();

                commands.add(new FileModificationCommand(
                        finalFilePath,
                        finalOriginalCode,
                        finalModifiedCode,
                        instruction,
                        finalLanguage));
            }

            if (allDiffEntries.isEmpty()) {
                return createErrorProposal("No valid files to modify");
            }

            // 创建批量摘要
            String summary = String.format(
                    "Batch AI Code Modification: %s\n%d file(s) will be modified",
                    instruction,
                    allDiffEntries.size());

            // 创建批量应用函数
            return new CodeChangeProposal(
                    summary,
                    allDiffEntries,
                    project -> {
                        LOG.info("开始应用批量修改");
                        for (ModificationCommand command : commands) {
                            command.execute(project);
                        }
                        LOG.info("批量修改应用完成");
                    });

        } catch (Exception e) {
            LOG.error("生成批量代码修改提案时出错", e);
            return createErrorProposal("Error: " + e.getMessage());
        }
    }

    @Override
    public CodeChangeProposal proposeChanges(String instruction, IdeContext ideContext) {
        LOG.info("生成代码修改提案：指令='" + instruction + "'");

        // 验证输入
        if (ideContext == null || ideContext.getSelectedCode() == null
                || ideContext.getSelectedCode().trim().isEmpty()) {
            LOG.warn("IDE上下文或选中代码为空");
            return createErrorProposal("No code selected");
        }

        String originalCode = ideContext.getSelectedCode();
        String filePath = ideContext.getFilePath();

        if (filePath == null) {
            LOG.warn("文件路径为空");
            return createErrorProposal("No file path available");
        }

        try {
            // 调用LLM生成修改后的代码
            String modifiedCode = generateModifiedCode(instruction, originalCode, ideContext);

            // 创建DiffEntry
            DiffEntry diffEntry = new DiffEntry(
                    filePath,
                    originalCode,
                    modifiedCode);

            // 创建摘要
            String summary = buildSummary(instruction, originalCode, modifiedCode);

            // 创建命令对象（Command 模式）
            final String finalFilePath = filePath;
            final String finalModifiedCode = modifiedCode;
            final String finalInstruction = instruction;
            final String finalLanguage = ideContext.getLanguage();

            return new CodeChangeProposal(
                    summary,
                    Collections.singletonList(diffEntry),
                    project -> {
                        ModificationCommand command = new FileModificationCommand(
                                finalFilePath,
                                originalCode,
                                finalModifiedCode,
                                finalInstruction,
                                finalLanguage);
                        command.execute(project);
                    });

        } catch (Exception e) {
            LOG.error("生成代码修改提案时出错", e);
            return createErrorProposal("Error: " + e.getMessage());
        }
    }

    /**
     * 使用LLM生成修改后的代码
     * 已集成DeepSeek V3.2-exp API
     */
    private String generateModifiedCode(String instruction, String originalCode, IdeContext ideContext) {
        LOG.info("调用LLM生成修改后的代码");

        // 构建Prompt
        String prompt = buildPrompt(instruction, originalCode, ideContext);
        LOG.info("LLM Prompt构建完成");

        try {
            // 获取LLM模型实例
            ChatLanguageModel model = getOrCreateModel();

            // 调用LLM生成修改后的代码
            LOG.info("开始调用DeepSeek API...");
            String response = model.generate(prompt);
            LOG.info("DeepSeek API调用成功");

            // 提取代码（去除markdown标记等）
            String extractedCode = extractCodeFromResponse(response);

            return extractedCode;

        } catch (Exception e) {
            LOG.error("LLM调用失败，降级到示例实现", e);
            // 降级到示例实现
            return addCommentsExample(originalCode, instruction);
        }
    }

    /**
     * 获取或创建LLM模型实例（单例模式，性能优化）
     */
    private ChatLanguageModel getOrCreateModel() {
        if (cachedModel == null) {
            String apiKey = getApiKey();

            LOG.info("创建DeepSeek模型实例");
            cachedModel = OpenAiChatModel.builder()
                    .apiKey(apiKey)
                    .baseUrl(DEEPSEEK_BASE_URL)
                    .modelName(DEEPSEEK_MODEL)
                    .temperature(0.3)
                    .timeout(Duration.ofSeconds(60))
                    .build();
        }

        return cachedModel;
    }

    /**
     * 获取API Key（优先级：环境变量 > 系统属性 > 默认值）
     */
    private String getApiKey() {
        // 优先从环境变量读取
        String key = System.getenv("DEEPSEEK_API_KEY");
        if (key != null && !key.isEmpty()) {
            LOG.info("从环境变量DEEPSEEK_API_KEY读取API密钥");
            return key;
        }

        // 从系统属性读取
        key = System.getProperty("deepseek.api.key");
        if (key != null && !key.isEmpty()) {
            LOG.info("从系统属性deepseek.api.key读取API密钥");
            return key;
        }

        // 使用默认的API Key
        if (DEFAULT_API_KEY != null && !DEFAULT_API_KEY.isEmpty()) {
            LOG.info("使用代码中配置的默认API密钥");
            return DEFAULT_API_KEY;
        }

        // 如果都没有，返回空字符串（会导致API调用失败，但不会崩溃）
        LOG.warn("未找到DeepSeek API密钥！请在代码中设置DEFAULT_API_KEY或设置环境变量DEEPSEEK_API_KEY");
        return "";
    }

    /**
     * 从LLM响应中提取代码
     */
    private String extractCodeFromResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            LOG.warn("LLM响应为空");
            return response;
        }

        String code = response.trim();

        // 移除markdown代码块标记
        if (code.startsWith("```")) {
            // 找到第一个换行符（跳过```java或```等标记）
            int firstNewline = code.indexOf('\n');
            // 找到最后一个```
            int lastBackticks = code.lastIndexOf("```");

            if (firstNewline != -1 && lastBackticks > firstNewline) {
                code = code.substring(firstNewline + 1, lastBackticks).trim();
            }
        }

        LOG.info("代码提取完成");
        return code;
    }

    /**
     * 构建LLM Prompt
     */
    private String buildPrompt(String instruction, String originalCode, IdeContext ideContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert Java developer. ");
        prompt.append("Please modify the following code according to the instruction.\n\n");
        prompt.append("Instruction: ").append(instruction).append("\n\n");

        if (ideContext.getLanguage() != null) {
            prompt.append("Programming Language: ").append(ideContext.getLanguage()).append("\n");
        }

        if (ideContext.getFilePath() != null) {
            prompt.append("File: ").append(ideContext.getFilePath()).append("\n");
        }

        prompt.append("\nOriginal Code:\n");
        prompt.append("```\n");
        prompt.append(originalCode);
        prompt.append("\n```\n\n");
        prompt.append("Please provide ONLY the modified code without explanations.\n");
        prompt.append("Modified Code:\n");

        return prompt.toString();
    }


    private String addCommentsExample(String originalCode, String instruction) {
        // 简单示例：如果指令包含"comment"，则添加注释
        if (instruction.toLowerCase().contains("comment")) {
            return "// Modified by JEDAI: " + instruction + "\n" + originalCode;
        }

        // 如果指令包含"refactor"，模拟重构
        if (instruction.toLowerCase().contains("refactor")) {
            return "// Refactored code (TODO: implement actual refactoring)\n" + originalCode;
        }

        // 默认：返回带有TODO注释的代码
        return "// TODO: Implement modification for: " + instruction + "\n" + originalCode;
    }

    /**
     * 构建修改摘要
     */
    private String buildSummary(String instruction, String originalCode, String modifiedCode) {
        int originalLines = originalCode.split("\n").length;
        int modifiedLines = modifiedCode.split("\n").length;
        int linesChanged = Math.abs(modifiedLines - originalLines);

        return String.format("AI Code Modification: %s\nLines changed: %d → %d (%+d)",
                instruction,
                originalLines,
                modifiedLines,
                modifiedLines - originalLines);
    }

    // 原来的 applyChanges 逻辑已经被提取到 FileModificationCommand 中

    /**
     * 创建错误提案
     */
    private CodeChangeProposal createErrorProposal(String errorMessage) {
        return new CodeChangeProposal(
                "Error: " + errorMessage,
                Collections.emptyList(),
                project -> LOG.error("尝试应用错误提案"));
    }
}
