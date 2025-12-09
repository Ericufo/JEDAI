package com.github.ericufo.jedai.rag.strategy.impl;

import com.github.ericufo.jedai.rag.strategy.EmbeddingStrategy;
import com.github.ericufo.jedai.rag.strategy.EmbeddingResult;
import com.github.ericufo.jedai.rag.strategy.EmbeddingException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;

import java.util.ArrayList;
import java.util.List;

/**
 * AllMiniLm嵌入策略实现
 * 使用LangChain4j的AllMiniLmL6V2QuantizedEmbeddingModel
 */
public class AllMiniLmEmbeddingStrategy implements EmbeddingStrategy {
    private final EmbeddingModel model;
    
    /**
     * 创建AllMiniLm嵌入策略
     */
    public AllMiniLmEmbeddingStrategy() {
        this.model = createEmbeddingModel();
    }
    
    /**
     * 创建嵌入模型实例
     * @return 嵌入模型实例
     */
    private static EmbeddingModel createEmbeddingModel() {
        ClassLoader cl = AllMiniLmEmbeddingStrategy.class.getClassLoader();
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cl);
            return new AllMiniLmL6V2QuantizedEmbeddingModel();
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }
    
    @Override
    public List<EmbeddingResult> embed(List<TextSegment> segments) throws EmbeddingException {
        try {
            List<Embedding> embeddings = model.embedAll(segments).content();
            List<EmbeddingResult> results = new ArrayList<>();
            
            for (int i = 0; i < embeddings.size(); i++) {
                results.add(new EmbeddingResult(embeddings.get(i), segments.get(i)));
            }
            
            return results;
        } catch (Exception e) {
            throw new EmbeddingException("Failed to embed text segments", e);
        }
    }
    
    @Override
    public String getStrategyName() {
        return "AllMiniLm";
    }
    
    @Override
    public int getEmbeddingDimension() {
        return 384; // AllMiniLmL6V2的嵌入维度
    }
}