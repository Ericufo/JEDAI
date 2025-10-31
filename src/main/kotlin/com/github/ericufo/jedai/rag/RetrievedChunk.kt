package com.github.ericufo.jedai.rag

/**
 * 检索到的知识块，包含内容和来源信息
 */
data class RetrievedChunk(
    val content: String,
    val sourceDoc: String,        // 文件名或相对路径
    val page: Int?,               // 页码（如果支持）
    val pageRange: IntRange? = null,  // 页码范围（可选）
    val score: Double             // 相关性分数
)

