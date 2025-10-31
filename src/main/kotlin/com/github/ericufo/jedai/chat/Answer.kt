package com.github.ericufo.jedai.chat

import com.github.ericufo.jedai.rag.RetrievedChunk

/**
 * 生成的答案
 */
data class Answer(
    val content: String,
    val citations: List<RetrievedChunk>,
    val isGeneralKnowledge: Boolean  // true表示基于通用知识，false表示基于课程材料
) {
    init {
        // 验证：如果有引用，则不应标记为通用知识
        require(!(citations.isNotEmpty() && isGeneralKnowledge)) {
            "如果有引用，则不应标记为通用知识"
        }
    }
}

