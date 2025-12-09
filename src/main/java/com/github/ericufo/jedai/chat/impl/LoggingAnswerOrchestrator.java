package com.github.ericufo.jedai.chat.impl;

import com.github.ericufo.jedai.chat.Answer;
import com.github.ericufo.jedai.chat.AnswerOrchestrator;
import com.github.ericufo.jedai.chat.IdeContext;
import com.github.ericufo.jedai.chat.StreamingAnswerHandler;
import com.github.ericufo.jedai.rag.RetrievedChunk;
import com.intellij.openapi.diagnostic.Logger;

import java.util.List;

/**
 * Decorator for AnswerOrchestrator that adds logging and timing.
 *
 * <p>
 * 说明：
 * “装饰器（Decorator）”实现，用来在不修改原有
 * SimpleAnswerOrchestrator 代码的前提下，为答案编排器增加日志和耗时统计。
 * 该类实现与 {@link AnswerOrchestrator} 相同的接口，并持有一个内部 delegate，
 * 所有真实业务逻辑都委托给内部对象，只在前后插入日志逻辑。
 * </p>
 */
public class LoggingAnswerOrchestrator extends BaseAnswerOrchestratorDecorator {

    private static final Logger LOG = Logger.getInstance(LoggingAnswerOrchestrator.class);

    public LoggingAnswerOrchestrator(AnswerOrchestrator delegate) {
        super(delegate);
        // ✅ 证明 Decorator Pattern 已投入使用
        LOG.info("✅ [Decorator Pattern] LoggingAnswerOrchestrator initialized - wrapping "
                + delegate.getClass().getSimpleName());
    }

    @Override
    public Answer generateAnswer(String userQuestion, IdeContext ideContext, List<RetrievedChunk> retrievedChunks) {
        long start = System.currentTimeMillis();
        LOG.info("generateAnswer() called, question length=" + userQuestion.length()
                + ", retrievedChunks=" + retrievedChunks.size());
        try {
            Answer answer = delegate.generateAnswer(userQuestion, ideContext, retrievedChunks);
            long cost = System.currentTimeMillis() - start;
            LOG.info("generateAnswer() finished in " + cost + " ms, isGeneralKnowledge="
                    + answer.isGeneralKnowledge());
            return answer;
        } catch (RuntimeException ex) {
            LOG.error("generateAnswer() failed", ex);
            throw ex;
        }
    }

    @Override
    public void generateAnswerStreaming(String userQuestion, IdeContext ideContext,
            List<RetrievedChunk> retrievedChunks, StreamingAnswerHandler handler) {
        long start = System.currentTimeMillis();
        LOG.info("generateAnswerStreaming() called, question length=" + userQuestion.length()
                + ", retrievedChunks=" + retrievedChunks.size());
        delegate.generateAnswerStreaming(userQuestion, ideContext, retrievedChunks, new StreamingAnswerHandler() {
            @Override
            public void onNext(String token) {
                handler.onNext(token);
            }

            @Override
            public void onComplete(Answer answer) {
                long cost = System.currentTimeMillis() - start;
                LOG.info("generateAnswerStreaming() completed in " + cost + " ms, totalChars="
                        + answer.getContent().length());
                handler.onComplete(answer);
            }

            @Override
            public void onError(Throwable error) {
                LOG.error("generateAnswerStreaming() failed", error);
                handler.onError(error);
            }
        });
    }
}
