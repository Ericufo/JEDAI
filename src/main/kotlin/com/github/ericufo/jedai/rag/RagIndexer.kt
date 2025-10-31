package com.github.ericufo.jedai.rag

/**
 * RAG 索引器接口
 * 负责将课程材料预处理、切分并建立索引
 */
interface RagIndexer {
    /**
     * 索引课程材料
     * @param materials 课程材料列表
     * @return 索引统计信息
     */
    fun index(materials: List<CourseMaterial>): IndexStats
    
    /**
     * 检查索引是否存在
     */
    fun isIndexed(): Boolean
    
    /**
     * 清除索引
     */
    fun clearIndex()
}

