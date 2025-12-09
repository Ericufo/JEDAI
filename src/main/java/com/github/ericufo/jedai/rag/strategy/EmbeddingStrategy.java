package com.github.ericufo.jedai.rag.strategy;

import dev.langchain4j.data.segment.TextSegment;
import java.util.List;

/**
 * 嵌入策略接口
 * 负责将文本片段转换为向量嵌入
 */
public interface EmbeddingStrategy {
    /**
     * 将文本片段转换为向量嵌入
     * @param segments 文本片段列表
     * @return 嵌入结果列表
     * @throws EmbeddingException 嵌入异常
     */
    List<EmbeddingResult> embed(List<TextSegment> segments) throws EmbeddingException;
    
    /**
     * 获取策略名称
     * @return 策略名称
     */
    String getStrategyName();
    
    /**
     * 获取嵌入维度
     * @return 嵌入维度
     */
    int getEmbeddingDimension();
}