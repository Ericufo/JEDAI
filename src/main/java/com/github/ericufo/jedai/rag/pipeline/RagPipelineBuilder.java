package com.github.ericufo.jedai.rag.pipeline;

import com.github.ericufo.jedai.rag.strategy.*;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG管道构建器
 * 使用建造者模式构建RAG处理管道
 */
public class RagPipelineBuilder {
    private ParserStrategy parserStrategy;
    private SplitterStrategy splitterStrategy;
    private EmbeddingStrategy embeddingStrategy;
    private StorageStrategy storageStrategy;
    private PipelineListener listener;
    
    /**
     * 设置解析策略
     * @param parserStrategy 解析策略
     * @return 当前构建器实例
     */
    public RagPipelineBuilder withParserStrategy(ParserStrategy parserStrategy) {
        this.parserStrategy = parserStrategy;
        return this;
    }
    
    /**
     * 设置切分策略
     * @param splitterStrategy 切分策略
     * @return 当前构建器实例
     */
    public RagPipelineBuilder withSplitterStrategy(SplitterStrategy splitterStrategy) {
        this.splitterStrategy = splitterStrategy;
        return this;
    }
    
    /**
     * 设置嵌入策略
     * @param embeddingStrategy 嵌入策略
     * @return 当前构建器实例
     */
    public RagPipelineBuilder withEmbeddingStrategy(EmbeddingStrategy embeddingStrategy) {
        this.embeddingStrategy = embeddingStrategy;
        return this;
    }
    
    /**
     * 设置存储策略
     * @param storageStrategy 存储策略
     * @return 当前构建器实例
     */
    public RagPipelineBuilder withStorageStrategy(StorageStrategy storageStrategy) {
        this.storageStrategy = storageStrategy;
        return this;
    }
    
    /**
     * 设置管道监听器
     * @param listener 管道监听器
     * @return 当前构建器实例
     */
    public RagPipelineBuilder withListener(PipelineListener listener) {
        this.listener = listener;
        return this;
    }
    
    /**
     * 构建RAG处理管道
     * @return RAG处理管道
     */
    public RagProcessingPipeline build() {
        // 验证必要组件
        if (parserStrategy == null) {
            throw new IllegalStateException("Parser strategy is required");
        }
        if (splitterStrategy == null) {
            throw new IllegalStateException("Splitter strategy is required");
        }
        if (embeddingStrategy == null) {
            throw new IllegalStateException("Embedding strategy is required");
        }
        if (storageStrategy == null) {
            throw new IllegalStateException("Storage strategy is required");
        }
        
        // 创建管道阶段
        List<PipelineStage<?, ?>> stages = new ArrayList<>();
        stages.add(new com.github.ericufo.jedai.rag.pipeline.stages.ParsingStage(parserStrategy));
        stages.add(new com.github.ericufo.jedai.rag.pipeline.stages.SplittingStage(splitterStrategy));
        stages.add(new com.github.ericufo.jedai.rag.pipeline.stages.EmbeddingStage(embeddingStrategy));
        stages.add(new com.github.ericufo.jedai.rag.pipeline.stages.StorageStage(storageStrategy));
        
        // 创建管道
        RagProcessingPipeline pipeline = new RagProcessingPipeline(stages);
        if (listener != null) {
            pipeline.setListener(listener);
        }
        
        return pipeline;
    }
}