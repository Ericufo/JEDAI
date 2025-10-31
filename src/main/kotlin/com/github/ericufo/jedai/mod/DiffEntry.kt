package com.github.ericufo.jedai.mod

import com.github.ericufo.jedai.chat.IdeContext
import com.intellij.openapi.project.Project

/**
 * 单个文件的差异条目
 */
data class DiffEntry(
    val filePath: String,
    val beforeText: String,
    val afterText: String
)

