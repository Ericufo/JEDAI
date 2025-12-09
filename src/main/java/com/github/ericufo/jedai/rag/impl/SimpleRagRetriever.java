package com.github.ericufo.jedai.rag.impl;

import com.github.ericufo.jedai.rag.RagRetriever;
import com.github.ericufo.jedai.rag.RetrievedChunk;
import com.github.ericufo.jedai.rag.strategy.EmbeddingResult;
import com.github.ericufo.jedai.rag.strategy.EmbeddingStrategy;
import com.github.ericufo.jedai.rag.strategy.StorageStrategy;
import com.github.ericufo.jedai.rag.strategy.selector.StrategySelector;
import com.intellij.openapi.diagnostic.Logger;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 重构后的SimpleRagRetriever实现
 * 使用策略模式，将嵌入和存储逻辑分离
 */
public class SimpleRagRetriever implements RagRetriever {
    private static final Logger LOG = Logger.getInstance(SimpleRagRetriever.class);
    
    private final StrategySelector strategySelector;
    private final EmbeddingStrategy embeddingStrategy;
    private final StorageStrategy storageStrategy;
    
    /**
     * 创建SimpleRagRetriever实例
     */
    public SimpleRagRetriever() {
        this.strategySelector = new StrategySelector();
        this.embeddingStrategy = strategySelector.getDefaultEmbedding();
        this.storageStrategy = strategySelector.getDefaultStorage();
    }
    
    /**
     * 使用自定义策略创建SimpleRagRetriever实例
     * @param embeddingStrategy 嵌入策略
     * @param storageStrategy 存储策略
     */
    public SimpleRagRetriever(EmbeddingStrategy embeddingStrategy, StorageStrategy storageStrategy) {
        this.embeddingStrategy = embeddingStrategy;
        this.storageStrategy = storageStrategy;
        this.strategySelector = null;
    }
    
    /**
     * 使用自定义策略选择器创建SimpleRagRetriever实例
     * @param strategySelector 策略选择器
     */
    public SimpleRagRetriever(StrategySelector strategySelector) {
        this.strategySelector = strategySelector;
        this.embeddingStrategy = strategySelector.getDefaultEmbedding();
        this.storageStrategy = strategySelector.getDefaultStorage();
    }

    @Override
    public List<RetrievedChunk> search(String query, int maxResults) {
        LOG.info("检索查询: " + query + ", 最大结果数: " + maxResults);
        
        try {
            // 将查询字符串转换为TextSegment
            TextSegment querySegment = TextSegment.from(query);
            
            // 生成查询的嵌入向量
            List<EmbeddingResult> embeddingResults = embeddingStrategy.embed(List.of(querySegment));
            if (embeddingResults.isEmpty()) {
                LOG.warn("无法生成查询的嵌入向量");
                return List.of();
            }
            Embedding queryEmbedding = embeddingResults.get(0).getEmbedding();
            LOG.info("成功生成查询嵌入向量，维度: " + queryEmbedding.dimension());
            
            // 检查存储中是否有数据
            int storageSize = storageStrategy.getStorageSize();
            LOG.info("存储中的数据量: " + storageSize);
            if (storageSize == 0) {
                LOG.warn("存储中没有数据，请确保已先索引文档");
                return List.of();
            }
            
            // 从存储中检索相关文本片段
            List<TextSegment> relevantSegments = storageStrategy.retrieve(queryEmbedding, maxResults);
            
            // 转换为RetrievedChunk列表
            List<RetrievedChunk> results = relevantSegments.stream()
                    .map(segment -> new RetrievedChunk(segment.text(), "unknown", null, 1.0)) // 使用默认相关性分数
                    .collect(Collectors.toList());
            
            LOG.info("检索到 " + results.size() + " 个相关文本片段");
            return results;
        } catch (Exception e) {
            LOG.error("检索时出错: " + e.getMessage(), e);
            return List.of();
        }
    }

    public boolean isReady() {
        try {
            // 检查存储中是否有数据
            return storageStrategy.getStorageSize() > 0;
        } catch (Exception e) {
            LOG.error("检查准备状态时出错: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取存储策略
     * @return 存储策略实例
     */
    public StorageStrategy getStorageStrategy() {
        return storageStrategy;
    }
    
    /**
     * 获取嵌入策略
     * @return 嵌入策略实例
     */
    public EmbeddingStrategy getEmbeddingStrategy() {
        return embeddingStrategy;
    }
}