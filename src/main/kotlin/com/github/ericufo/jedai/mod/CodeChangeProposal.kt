package com.github.ericufo.jedai.mod

import com.intellij.openapi.project.Project

/**
 * 代码修改提案
 * 包含差异信息和应用逻辑
 */
data class CodeChangeProposal(
    val summary: String,                    // 变更摘要
    val diffEntries: List<DiffEntry>,       // 差异条目列表
    val apply: (Project) -> Unit             // 应用变更的函数
)

