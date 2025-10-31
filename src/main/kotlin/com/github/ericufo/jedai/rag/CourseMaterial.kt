package com.github.ericufo.jedai.rag

import java.io.File

/**
 * 课程材料表示
 */
data class CourseMaterial(
    val file: File,
    val type: MaterialType
)

enum class MaterialType {
    PDF,
    MARKDOWN,
    SLIDES,
    TEXT
}

