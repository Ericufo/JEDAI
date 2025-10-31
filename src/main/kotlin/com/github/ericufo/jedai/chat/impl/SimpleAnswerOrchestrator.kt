package com.github.ericufo.jedai.chat.impl

import com.github.ericufo.jedai.chat.Answer
import com.github.ericufo.jedai.chat.AnswerOrchestrator
import com.github.ericufo.jedai.chat.IdeContext
import com.github.ericufo.jedai.rag.RetrievedChunk
import com.intellij.openapi.diagnostic.thisLogger

/**
 * 答案编排器的简单实现（骨架）
 * TODO: 成员B需要实现具体的LLM调用逻辑
 */
class SimpleAnswerOrchestrator : AnswerOrchestrator {
    private val logger = thisLogger()
    
    override fun generateAnswer(
        userQuestion: String,
        ideContext: IdeContext?,
        retrievedChunks: List<RetrievedChunk>
    ): Answer {
        logger.info("生成答案：问题='$userQuestion'，检索到 ${retrievedChunks.size} 个知识块")
        
        // TODO: 实现LLM调用逻辑
        // 1. 构建Prompt：包含用户问题、IDE上下文、检索到的知识块
        // 2. 调用LLM API（OpenAI/DeepSeek等）
        // 3. 解析响应，提取答案和引用
        // 4. 如果没有检索到相关材料，标记为通用知识
        
        return if (retrievedChunks.isEmpty()) {
            Answer(
                content = "Response is based on general knowledge; no specific course material is referenced.",
                citations = emptyList(),
                isGeneralKnowledge = true
            )
        } else {
            Answer(
                content = "TODO: 实现LLM调用，生成基于课程材料的答案",
                citations = retrievedChunks,
                isGeneralKnowledge = false
            )
        }
    }
}

