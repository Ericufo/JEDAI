package com.github.ericufo.jedai.chat

import com.github.ericufo.jedai.rag.RetrievedChunk

/**
 * IDE上下文信息
 */
data class IdeContext(
    val projectName: String,
    val filePath: String? = null,
    val selectedCode: String? = null,
    val language: String? = null,
    val lineNumber: Int? = null
)

