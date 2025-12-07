package com.github.ericufo.jedai.rag;

import java.util.List;

/**
 * RAG 检索器接口
 * 负责从知识库中检索最相关的知识块
 */
public interface RagRetriever {
    /**
     * 检索最相关的知识块
     * @param query 用户查询
     * @param k 返回的top-k结果数量，默认5
     * @return 检索到的知识块列表，按相关性排序
     */
    List<RetrievedChunk> search(String query, int k);
    
    /**
     * 检索最相关的知识块（使用默认k=5）
     */
    default List<RetrievedChunk> search(String query) {
        return search(query, 5);
    }
}

