package com.github.ericufo.jedai.rag.pipeline.stages;

import com.github.ericufo.jedai.rag.pipeline.PipelineException;
import com.github.ericufo.jedai.rag.pipeline.PipelineStage;
import com.github.ericufo.jedai.rag.strategy.EmbeddingStrategy;
import com.github.ericufo.jedai.rag.strategy.EmbeddingResult;
import dev.langchain4j.data.segment.TextSegment;

import java.util.List;

/**
 * 嵌入阶段
 * 使用嵌入策略将文本片段转换为向量嵌入
 */
public class EmbeddingStage implements PipelineStage<List<TextSegment>, List<EmbeddingResult>> {
    private final EmbeddingStrategy embedding;
    
    /**
     * 创建嵌入阶段
     * @param embedding 嵌入策略
     */
    public EmbeddingStage(EmbeddingStrategy embedding) {
        this.embedding = embedding;
    }
    
    @Override
    public List<EmbeddingResult> execute(List<TextSegment> segments) throws PipelineException {
        try {
            return embedding.embed(segments);
        } catch (Exception e) {
            throw new PipelineException("Failed to embed text segments", e);
        }
    }
    
    @Override
    public String getStageName() {
        return "EmbeddingStage(" + embedding.getStrategyName() + ")";
    }
}