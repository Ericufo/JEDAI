package com.github.ericufo.jedai.rag

/**
 * 索引统计信息
 */
data class IndexStats(
    val totalDocuments: Int,
    val totalChunks: Int,
    val indexingTimeMs: Long
)

