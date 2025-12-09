package com.github.ericufo.jedai.rag.strategy;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import java.util.List;

/**
 * 存储策略接口
 * 负责存储嵌入向量和文本片段
 */
public interface StorageStrategy {
    /**
     * 存储嵌入结果
     * @param results 嵌入结果列表
     * @throws StorageException 存储异常
     */
    void store(List<EmbeddingResult> results) throws StorageException;
    
    /**
     * 清除所有存储的数据
     * @throws StorageException 存储异常
     */
    void clear() throws StorageException;
    
    /**
     * 获取存储的数据大小
     * @return 存储的数据大小
     * @throws StorageException 存储异常
     */
    int getStorageSize() throws StorageException;
    
    /**
     * 获取策略名称
     * @return 策略名称
     */
    String getStrategyName();
    
    /**
     * 根据查询嵌入向量检索相关的文本片段
     * @param queryEmbedding 查询嵌入向量
     * @param maxResults 最大结果数
     * @return 相关的文本片段列表
     * @throws StorageException 存储异常
     */
    List<TextSegment> retrieve(Embedding queryEmbedding, int maxResults) throws StorageException;
}