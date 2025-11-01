package com.github.ericufo.jedai.chat.impl;

import com.github.ericufo.jedai.chat.Answer;
import com.github.ericufo.jedai.chat.AnswerOrchestrator;
import com.github.ericufo.jedai.chat.IdeContext;
import com.github.ericufo.jedai.rag.RetrievedChunk;
import com.intellij.openapi.diagnostic.Logger;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ChatMessage;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 答案编排器的完整实现
 * 复用DeepSeek LLM配置
 * 支持对话上下文记忆
 */
public class SimpleAnswerOrchestrator implements AnswerOrchestrator {
    private static final Logger LOG = Logger.getInstance(SimpleAnswerOrchestrator.class);

    // DeepSeek API配置
    private static final String DEEPSEEK_BASE_URL = "https://cloud.infini-ai.com/maas/deepseek-v3.2-exp/nvidia";
    private static final String DEEPSEEK_MODEL = "deepseek-v3.2-exp";

    // 默认API Key
    // 优先级：环境变量 > 系统属性 > 此处的默认值
    private static final String DEFAULT_API_KEY = "sk-4hrklq5w3w4x7bcz";

    // 对话历史长度限制（保留最近N轮对话）
    // 每轮对话包含1条用户消息和1条AI消息，所以实际保留的消息数是 MAX_HISTORY_TURNS * 2 + 1(系统消息)
    private static final int MAX_HISTORY_TURNS = 5;

    // 缓存LLM模型实例（性能优化，单例模式）
    private static ChatLanguageModel cachedModel = null;

    // 对话记忆（滑动窗口模式，自动保留最近的消息）
    // 当超过限制时，自动丢弃最旧的消息，不会强制结束对话
    private final ChatMemory chatMemory;

    // 系统提示词（角色定义）
    // 要求使用纯文本格式，尽量少使用Markdown符号，简化UI渲染
    private static final String SYSTEM_PROMPT = "You are a helpful teaching assistant for a Java Enterprise Application Development course. "
            +
            "Answer student questions based on the provided course materials and conversation history. " +
            "Be clear, concise, and educational in your responses. " +
            "\n\nIMPORTANT FORMATTING RULES:\n" +
            "- Use plain text only. Do NOT use Markdown formatting symbols.\n" +
            "- Avoid: ** (bold), ## (headers), ``` (code blocks), - (lists), ` (inline code)\n" +
            "- For code examples: simply write the code with proper indentation\n" +
            "- Use line breaks and indentation to organize content\n" +
            "- Use numbered lists like '1.', '2.' for steps\n" +
            "- Use simple dashes or arrows like '-->' for emphasis";

    /**
     * 构造函数：初始化对话记忆
     */
    public SimpleAnswerOrchestrator() {
        // 初始化滑动窗口记忆：保留最近 MAX_HISTORY_TURNS*2+1 条消息
        // +1 是因为系统消息会一直保留
        this.chatMemory = MessageWindowChatMemory.withMaxMessages(MAX_HISTORY_TURNS * 2 + 1);

        // 添加系统提示词作为第一条消息（会一直保留在记忆中）
        this.chatMemory.add(SystemMessage.from(SYSTEM_PROMPT));

        LOG.info("初始化对话记忆，最大保留 " + MAX_HISTORY_TURNS + " 轮对话");
    }

    @Override
    public Answer generateAnswer(String userQuestion, IdeContext ideContext, List<RetrievedChunk> retrievedChunks) {
        LOG.info("生成答案：问题='" + userQuestion + "'，检索到 " + retrievedChunks.size() + " 个知识块");
        LOG.info("当前对话记忆包含 " + chatMemory.messages().size() + " 条消息");

        try {
            // 构建用户消息内容（包含问题、上下文、检索材料）
            String userMessageContent = buildUserMessage(userQuestion, ideContext, retrievedChunks);

            // 将用户消息添加到对话记忆
            chatMemory.add(UserMessage.from(userMessageContent));
            LOG.info("用户消息已添加到对话记忆");

            // 调用LLM生成答案（自动携带对话历史）
            ChatLanguageModel model = getOrCreateModel();
            LOG.info("开始调用DeepSeek API（携带对话历史）...");

            // generate() 返回 Response<AiMessage>，需要提取内容
            Response<AiMessage> aiResponse = model.generate(chatMemory.messages());
            AiMessage aiMessage = aiResponse.content();
            String responseText = aiMessage.text();

            LOG.info("DeepSeek API调用成功");

            // 将AI回答添加到对话记忆（使用返回的 AiMessage 对象）
            chatMemory.add(aiMessage);
            LOG.info("AI回答已添加到对话记忆");

            // 构建Answer对象
            if (retrievedChunks.isEmpty()) {
                // 无课程材料，标记为通用知识
                return new Answer(
                        responseText.trim(),
                        Collections.emptyList(),
                        true);
            } else {
                // 有课程材料，标记为基于材料的答案
                return new Answer(
                        responseText.trim(),
                        retrievedChunks,
                        false);
            }

        } catch (Exception e) {
            LOG.error("LLM调用失败，降级到示例实现", e);
            // 降级到示例实现
            return createFallbackAnswer(userQuestion, retrievedChunks);
        }
    }

    /**
     * 清空对话历史
     * 用户可以调用此方法重置对话，开始全新的对话
     */
    public void clearChatHistory() {
        LOG.info("清空对话历史");
        chatMemory.clear();

        // 重新添加系统提示词
        chatMemory.add(SystemMessage.from(SYSTEM_PROMPT));
        LOG.info("对话历史已清空，系统提示词已重新添加");
    }

    /**
     * 获取当前对话历史中的消息数量
     * 用于调试和UI显示
     */
    public int getChatHistorySize() {
        return chatMemory.messages().size();
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
                    .temperature(0.7) // Q&A场景使用稍高的温度，增加答案多样性
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
     * 构建用户消息内容
     * 将用户问题、IDE上下文、检索到的课程材料组合成用户消息
     * 注意：系统角色定义已在构造函数中作为 SystemMessage 添加，此处不再包含
     */
    private String buildUserMessage(String userQuestion, IdeContext ideContext, List<RetrievedChunk> retrievedChunks) {
        StringBuilder message = new StringBuilder();

        // 如果有课程材料，添加到上下文
        if (!retrievedChunks.isEmpty()) {
            message.append("=== Course Materials (Retrieved) ===\n");
            for (int i = 0; i < retrievedChunks.size(); i++) {
                RetrievedChunk chunk = retrievedChunks.get(i);
                message.append(String.format("[Source %d: %s", i + 1, chunk.getSourceDoc()));
                if (chunk.getPage() != null) {
                    message.append(", Page " + chunk.getPage());
                }
                message.append("]\n");
                message.append(chunk.getContent()).append("\n\n");
            }
            message.append("=== End of Course Materials ===\n\n");
        }

        // 如果有IDE上下文（选中的代码），添加到消息
        if (ideContext != null && ideContext.getSelectedCode() != null
                && !ideContext.getSelectedCode().trim().isEmpty()) {
            message.append("=== Student's Code Context ===\n");
            if (ideContext.getFilePath() != null) {
                message.append("File: ").append(ideContext.getFilePath()).append("\n");
            }
            if (ideContext.getLanguage() != null) {
                message.append("Language: ").append(ideContext.getLanguage()).append("\n");
            }
            message.append("Selected Code:\n```\n");
            message.append(ideContext.getSelectedCode());
            message.append("\n```\n\n");
        }

        // 学生问题
        message.append("=== My Question ===\n");
        message.append(userQuestion).append("\n\n");

        // 答案要求
        if (!retrievedChunks.isEmpty()) {
            message.append("Please answer based on the course materials provided above. ");
            message.append("Be specific and reference the concepts from the materials.");
        } else {
            message.append("No specific course materials were found for this question. ");
            message.append("Please provide a helpful answer based on general Java knowledge.");
        }

        return message.toString();
    }

    /**
     * 创建降级答案（当LLM调用失败时）
     */
    private Answer createFallbackAnswer(String userQuestion, List<RetrievedChunk> retrievedChunks) {
        StringBuilder fallbackContent = new StringBuilder();

        if (retrievedChunks.isEmpty()) {
            fallbackContent.append(
                    "Sorry, the LLM service is temporarily unavailable, and no course materials were found for your question.\n\n");
            fallbackContent.append("Your question: ").append(userQuestion).append("\n\n");
            fallbackContent
                    .append("Response is based on general knowledge; no specific course material is referenced.");

            return new Answer(
                    fallbackContent.toString(),
                    Collections.emptyList(),
                    true);
        } else {
            fallbackContent.append("Sorry, the LLM service is temporarily unavailable. ");
            fallbackContent.append("However, I found the following relevant course materials:\n\n");

            for (int i = 0; i < Math.min(3, retrievedChunks.size()); i++) {
                RetrievedChunk chunk = retrievedChunks.get(i);
                fallbackContent.append(String.format("[%d] %s", i + 1, chunk.getSourceDoc()));
                if (chunk.getPage() != null) {
                    fallbackContent.append(" (Page ").append(chunk.getPage()).append(")");
                }
                fallbackContent.append(":\n");
                fallbackContent.append(chunk.getContent().substring(0, Math.min(200, chunk.getContent().length())));
                if (chunk.getContent().length() > 200) {
                    fallbackContent.append("...");
                }
                fallbackContent.append("\n\n");
            }

            return new Answer(
                    fallbackContent.toString(),
                    retrievedChunks,
                    false);
        }
    }
}
