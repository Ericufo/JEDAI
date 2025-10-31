package com.github.ericufo.jedai.chat;

import com.github.ericufo.jedai.rag.RetrievedChunk;

import java.util.List;

/**
 * 答案编排器接口
 * 负责将用户问题、IDE上下文和检索到的知识块组合，调用LLM生成答案
 */
public interface AnswerOrchestrator {
    /**
     * 生成答案
     * @param userQuestion 用户问题
     * @param ideContext IDE上下文（可选）
     * @param retrievedChunks 检索到的知识块列表
     * @return 生成的答案，包含内容、引用和知识来源标记
     */
    Answer generateAnswer(String userQuestion, IdeContext ideContext, List<RetrievedChunk> retrievedChunks);
}

