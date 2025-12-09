package com.github.ericufo.jedai.rag.pipeline.stages;

import com.github.ericufo.jedai.rag.CourseMaterial;
import com.github.ericufo.jedai.rag.pipeline.PipelineException;
import com.github.ericufo.jedai.rag.pipeline.PipelineStage;
import com.github.ericufo.jedai.rag.strategy.ParserStrategy;
import dev.langchain4j.data.document.Document;

/**
 * 解析阶段
 * 使用解析策略将课程材料解析为文档
 */
public class ParsingStage implements PipelineStage<CourseMaterial, Document> {
    private final ParserStrategy parser;
    
    /**
     * 创建解析阶段
     * @param parser 解析策略
     */
    public ParsingStage(ParserStrategy parser) {
        this.parser = parser;
    }
    
    @Override
    public Document execute(CourseMaterial material) throws PipelineException {
        try {
            return parser.parse(material);
        } catch (Exception e) {
            throw new PipelineException("Failed to parse material: " + material.getFile().getName(), e);
        }
    }
    
    @Override
    public String getStageName() {
        return "ParsingStage(" + parser.getStrategyName() + ")";
    }
}