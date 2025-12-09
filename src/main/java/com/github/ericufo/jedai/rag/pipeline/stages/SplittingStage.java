package com.github.ericufo.jedai.rag.pipeline.stages;

import com.github.ericufo.jedai.rag.pipeline.PipelineException;
import com.github.ericufo.jedai.rag.pipeline.PipelineStage;
import com.github.ericufo.jedai.rag.strategy.SplitterStrategy;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;

import java.util.List;

/**
 * 切分阶段
 * 使用切分策略将文档切分为文本片段
 */
public class SplittingStage implements PipelineStage<Document, List<TextSegment>> {
    private final SplitterStrategy splitter;
    
    /**
     * 创建切分阶段
     * @param splitter 切分策略
     */
    public SplittingStage(SplitterStrategy splitter) {
        this.splitter = splitter;
    }
    
    @Override
    public List<TextSegment> execute(Document document) throws PipelineException {
        try {
            return splitter.split(document);
        } catch (Exception e) {
            throw new PipelineException("Failed to split document", e);
        }
    }
    
    @Override
    public String getStageName() {
        return "SplittingStage(" + splitter.getStrategyName() + ")";
    }
}