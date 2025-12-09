package com.github.ericufo.jedai.chat.impl;

import com.github.ericufo.jedai.chat.Answer;
import com.github.ericufo.jedai.chat.AnswerOrchestrator;
import com.github.ericufo.jedai.chat.IdeContext;
import com.github.ericufo.jedai.chat.StreamingAnswerHandler;
import com.github.ericufo.jedai.rag.RetrievedChunk;

import java.util.List;

/**
 * Base Decorator for {@link AnswerOrchestrator}.
 *
 * <p>中文说明：
 * 这是“基础装饰类（Base Decorator）”，持有被装饰对象的引用，并提供默认转发实现。
 * 所有具体装饰器（Concrete Decorator）继承自本类，避免重复存放 delegate 字段，
 * 同时符合教材上的装饰器模式结构：Interface -> Base Decorator -> Concrete Decorator -> Concrete Component。
 * </p>
 */
public abstract class BaseAnswerOrchestratorDecorator implements AnswerOrchestrator {

    protected final AnswerOrchestrator delegate;

    protected BaseAnswerOrchestratorDecorator(AnswerOrchestrator delegate) {
        this.delegate = delegate;
    }

    @Override
    public Answer generateAnswer(String userQuestion, IdeContext ideContext,
            List<RetrievedChunk> retrievedChunks) {
        return delegate.generateAnswer(userQuestion, ideContext, retrievedChunks);
    }

    @Override
    public void generateAnswerStreaming(String userQuestion, IdeContext ideContext,
            List<RetrievedChunk> retrievedChunks, StreamingAnswerHandler handler) {
        delegate.generateAnswerStreaming(userQuestion, ideContext, retrievedChunks, handler);
    }
}

