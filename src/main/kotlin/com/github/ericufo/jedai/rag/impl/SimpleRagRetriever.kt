package com.github.ericufo.jedai.rag.impl

import com.github.ericufo.jedai.rag.RagRetriever
import com.github.ericufo.jedai.rag.RetrievedChunk
import com.intellij.openapi.diagnostic.thisLogger

/**
 * RAG 检索器的简单实现（骨架）
 * TODO: 成员A需要实现具体的检索逻辑
 */
class SimpleRagRetriever : RagRetriever {
    private val logger = thisLogger()
    
    override fun search(query: String, k: Int): List<RetrievedChunk> {
        logger.info("检索查询：$query，返回 top-$k")
        // TODO: 实现检索逻辑
        // 1. 向量化查询（使用Embedding API）
        // 2. 检索最相关的chunks（BM25或向量相似度）
        // 3. 返回带有页码/范围信息的RetrievedChunk列表
        return emptyList()
    }
}

