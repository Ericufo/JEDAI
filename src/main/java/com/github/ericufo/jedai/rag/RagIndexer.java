package com.github.ericufo.jedai.rag;

import java.util.List;

/**
 * RAG 索引器接口
 * 负责将课程材料预处理、切分并建立索引
 */
public interface RagIndexer {
    /**
     * 索引课程材料
     * @param materials 课程材料列表
     * @return 索引统计信息
     */
    IndexStats index(List<CourseMaterial> materials);
    
    /**
     * 检查索引是否存在
     */
    boolean isIndexed();
    
    /**
     * 清除索引
     */
    void clearIndex();
}

