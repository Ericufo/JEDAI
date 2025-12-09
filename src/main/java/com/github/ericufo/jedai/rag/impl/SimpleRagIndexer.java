package com.github.ericufo.jedai.rag.impl;

import com.github.ericufo.jedai.rag.CourseMaterial;
import com.github.ericufo.jedai.rag.IndexStats;
import com.github.ericufo.jedai.rag.RagIndexer;
import com.github.ericufo.jedai.rag.pipeline.PipelineException;
import com.github.ericufo.jedai.rag.pipeline.PipelineListener;
import com.github.ericufo.jedai.rag.pipeline.RagPipelineBuilder;
import com.github.ericufo.jedai.rag.pipeline.RagProcessingPipeline;
import com.github.ericufo.jedai.rag.strategy.StorageStrategy;
import com.github.ericufo.jedai.rag.strategy.selector.StrategySelector;
import com.intellij.openapi.diagnostic.Logger;

import java.util.List;

/**
 * 重构后的SimpleRagIndexer实现
 * 使用策略模式和管道模式，将原本的单一类拆分为多个策略和管道阶段
 */
public class SimpleRagIndexer implements RagIndexer, PipelineListener {
    private static final Logger LOG = Logger.getInstance(SimpleRagIndexer.class);
    
    private final StrategySelector strategySelector;
    private int totalChunks = 0;
    
    /**
     * 创建SimpleRagIndexer实例
     */
    public SimpleRagIndexer() {
        this.strategySelector = new StrategySelector();
    }
    
    /**
     * 使用自定义策略选择器创建SimpleRagIndexer实例
     * @param strategySelector 策略选择器
     */
    public SimpleRagIndexer(StrategySelector strategySelector) {
        this.strategySelector = strategySelector;
    }

    @Override
    public IndexStats index(List<CourseMaterial> materials) {
        LOG.info("开始索引 " + materials.size() + " 个课程材料");
        
        long startTime = System.currentTimeMillis();
        totalChunks = 0;
        
        // 清除现有索引
        try {
            clearIndex();
        } catch (Exception e) {
            LOG.warn("清除现有索引时出错: " + e.getMessage());
        }
        
        // 为每个材料创建并执行管道
        for (CourseMaterial material : materials) {
            try {
                // 选择策略
                var parser = strategySelector.selectParser(material);
                var splitter = strategySelector.getDefaultSplitter();
                var embedding = strategySelector.getDefaultEmbedding();
                var storage = strategySelector.getDefaultStorage();
                
                // 构建管道
                RagProcessingPipeline pipeline = new RagPipelineBuilder()
                        .withParserStrategy(parser)
                        .withSplitterStrategy(splitter)
                        .withEmbeddingStrategy(embedding)
                        .withStorageStrategy(storage)
                        .withListener(this)
                        .build();
                
                // 执行管道
                pipeline.execute(material);
                
                // 更新总块数
                try {
                    totalChunks += storage.getStorageSize();
                } catch (Exception e) {
                    LOG.warn("获取存储大小时出错: " + e.getMessage());
                }
            } catch (PipelineException e) {
                LOG.error("处理材料 " + material.getFile().getName() + " 时出错: " + e.getMessage());
            }
        }
        
        long indexingTime = System.currentTimeMillis() - startTime;
        LOG.info("索引完成，共处理 " + materials.size() + " 个材料，生成 " + totalChunks + " 个文本块，耗时 " + indexingTime + "ms");
        
        return new IndexStats(materials.size(), totalChunks, indexingTime);
    }

    @Override
    public boolean isIndexed() {
        try {
            StorageStrategy storage = strategySelector.getDefaultStorage();
            return storage.getStorageSize() > 0;
        } catch (Exception e) {
            LOG.error("检查索引状态时出错: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void clearIndex() {
        try {
            StorageStrategy storage = strategySelector.getDefaultStorage();
            storage.clear();
            LOG.info("索引已清除");
        } catch (Exception e) {
            LOG.error("清除索引时出错: " + e.getMessage());
        }
    }
    
    // PipelineListener实现
    @Override
    public void onStageStart(String stageName, Object input) {
        LOG.debug("开始执行阶段: " + stageName);
    }

    @Override
    public void onStageComplete(String stageName, Object output) {
        LOG.debug("阶段执行完成: " + stageName);
    }

    @Override
    public void onStageError(String stageName, Exception exception) {
        LOG.error("阶段执行出错: " + stageName + ", 错误: " + exception.getMessage());
    }

    @Override
    public void onPipelineStart() {
        LOG.debug("开始执行管道");
    }

    @Override
    public void onPipelineComplete() {
        LOG.debug("管道执行完成");
    }

    @Override
    public void onPipelineError(Exception exception) {
        LOG.error("管道执行出错: " + exception.getMessage());
    }
}