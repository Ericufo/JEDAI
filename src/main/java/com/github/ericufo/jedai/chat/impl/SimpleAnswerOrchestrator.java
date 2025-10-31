package com.github.ericufo.jedai.chat.impl;

import com.github.ericufo.jedai.chat.Answer;
import com.github.ericufo.jedai.chat.AnswerOrchestrator;
import com.github.ericufo.jedai.chat.IdeContext;
import com.github.ericufo.jedai.rag.RetrievedChunk;
import com.intellij.openapi.diagnostic.Logger;

import java.util.Collections;
import java.util.List;

/**
 * 答案编排器的简单实现（骨架）
 * TODO: 成员B需要实现具体的LLM调用逻辑
 */
public class SimpleAnswerOrchestrator implements AnswerOrchestrator {
    private static final Logger LOG = Logger.getInstance(SimpleAnswerOrchestrator.class);
    
    @Override
    public Answer generateAnswer(String userQuestion, IdeContext ideContext, List<RetrievedChunk> retrievedChunks) {
        LOG.info("生成答案：问题='" + userQuestion + "'，检索到 " + retrievedChunks.size() + " 个知识块");
        
        // TODO: 实现LLM调用逻辑
        // 1. 构建Prompt：包含用户问题、IDE上下文、检索到的知识块
        // 2. 调用LLM API（OpenAI/DeepSeek等）
        // 3. 解析响应，提取答案和引用
        // 4. 如果没有检索到相关材料，标记为通用知识
        
        if (retrievedChunks.isEmpty()) {
            return new Answer(
                "Response is based on general knowledge; no specific course material is referenced.",
                Collections.emptyList(),
                true
            );
        } else {
            return new Answer(
                "TODO: 实现LLM调用，生成基于课程材料的答案",
                retrievedChunks,
                false
            );
        }
    }
}

