package com.github.ericufo.jedai.rag.strategy.impl;

import com.github.ericufo.jedai.rag.strategy.EmbeddingStrategy;
import com.github.ericufo.jedai.rag.strategy.EmbeddingResult;
import com.github.ericufo.jedai.rag.strategy.EmbeddingException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 模拟嵌入策略实现
 * 用于测试和演示，生成随机向量
 */
public class MockEmbeddingStrategy implements EmbeddingStrategy {
    private static final int DIMENSION = 384;
    private final Random random = new Random(42); // 固定种子，保证结果可重现
    
    @Override
    public List<EmbeddingResult> embed(List<TextSegment> segments) throws EmbeddingException {
        try {
            List<EmbeddingResult> results = new ArrayList<>();
            
            for (TextSegment segment : segments) {
                // 生成随机向量
                float[] vector = new float[DIMENSION];
                for (int i = 0; i < DIMENSION; i++) {
                    vector[i] = random.nextFloat() * 2 - 1; // -1到1之间的随机数
                }
                
                Embedding embedding = Embedding.from(vector);
                results.add(new EmbeddingResult(embedding, segment));
            }
            
            return results;
        } catch (Exception e) {
            throw new EmbeddingException("Failed to embed text segments", e);
        }
    }
    
    @Override
    public String getStrategyName() {
        return "MockEmbedding";
    }
    
    @Override
    public int getEmbeddingDimension() {
        return DIMENSION;
    }
}