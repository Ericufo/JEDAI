package com.github.ericufo.jedai.rag.pipeline.stages;

import com.github.ericufo.jedai.rag.pipeline.PipelineException;
import com.github.ericufo.jedai.rag.pipeline.PipelineStage;
import com.github.ericufo.jedai.rag.strategy.EmbeddingResult;
import com.github.ericufo.jedai.rag.strategy.StorageStrategy;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;

import java.util.ArrayList;
import java.util.List;

/**
 * 存储阶段
 * 使用存储策略将嵌入向量和文本片段存储起来
 */
public class StorageStage implements PipelineStage<List<EmbeddingResult>, Void> {
    private final StorageStrategy storage;
    
    /**
     * 创建存储阶段
     * @param storage 存储策略
     */
    public StorageStage(StorageStrategy storage) {
        this.storage = storage;
    }
    
    @Override
    public Void execute(List<EmbeddingResult> results) throws PipelineException {
        try {
            storage.store(results);
            return null;
        } catch (Exception e) {
            throw new PipelineException("Failed to store embeddings", e);
        }
    }
    
    @Override
    public String getStageName() {
        return "StorageStage(" + storage.getStrategyName() + ")";
    }
}