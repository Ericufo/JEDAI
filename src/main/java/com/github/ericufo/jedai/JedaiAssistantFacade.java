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
 * 这是“外观（Facade）”类，对外暴露一个统一的助教接口。
 * UI / Action 代码只需要和本类交互，而不必直接了解或依赖
 * RagRetriever、AnswerOrchestrator、CodeModificationService 等多个子系统。
 * 通过它可以：
 * - 统一发起问答（一次性 / 流式）
 * - 发起 AI 代码修改请求
 * 从而简化上层调用逻辑，降低耦合度。
 * </p>
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
