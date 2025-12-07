package com.github.ericufo.jedai.chat;

import com.github.ericufo.jedai.rag.RetrievedChunk;

import java.util.List;

/**
 * 答案编排器接口
 * 负责将用户问题、IDE上下文和检索到的知识块组合，调用LLM生成答案
 */
public interface AnswerOrchestrator {
    /**
     * 生成答案（一次性返回完整答案）
     * 
     * @param userQuestion    用户问题
     * @param ideContext      IDE上下文（可选）
     * @param retrievedChunks 检索到的知识块列表
     * @return 生成的答案，包含内容、引用和知识来源标记
     */
    Answer generateAnswer(String userQuestion, IdeContext ideContext, List<RetrievedChunk> retrievedChunks);

    /**
     * 流式生成答案（逐步返回答案片段）
     * 
     * @param userQuestion    用户问题
     * @param ideContext      IDE上下文（可选）
     * @param retrievedChunks 检索到的知识块列表
     * @param handler         流式答案处理器，接收逐步生成的token
     */
    void generateAnswerStreaming(String userQuestion, IdeContext ideContext, List<RetrievedChunk> retrievedChunks,
            StreamingAnswerHandler handler);
}
