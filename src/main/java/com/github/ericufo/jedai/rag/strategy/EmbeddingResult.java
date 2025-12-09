package com.github.ericufo.jedai.rag.strategy;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import java.util.List;

/**
 * 嵌入结果类
 * 封装嵌入向量和对应的文本片段
 */
public class EmbeddingResult {
    private final Embedding embedding;
    private final TextSegment segment;
    
    public EmbeddingResult(Embedding embedding, TextSegment segment) {
        this.embedding = embedding;
        this.segment = segment;
    }
    
    /**
     * 获取嵌入向量
     * @return 嵌入向量
     */
    public Embedding getEmbedding() {
        return embedding;
    }
    
    /**
     * 获取文本片段
     * @return 文本片段
     */
    public TextSegment getSegment() {
        return segment;
    }
}