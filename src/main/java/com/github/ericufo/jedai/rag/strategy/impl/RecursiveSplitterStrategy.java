package com.github.ericufo.jedai.rag.strategy.impl;

import com.github.ericufo.jedai.rag.strategy.SplitterStrategy;
import com.github.ericufo.jedai.rag.strategy.SplittingException;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;

import java.util.List;

/**
 * 递归文档切分策略实现
 * 使用LangChain4j的递归文档切分器
 */
public class RecursiveSplitterStrategy implements SplitterStrategy {
    private final int maxSegmentSize;
    private final int maxOverlapSize;
    
    /**
     * 创建递归文档切分策略
     * @param maxSegmentSize 最大片段大小
     * @param maxOverlapSize 最大重叠大小
     */
    public RecursiveSplitterStrategy(int maxSegmentSize, int maxOverlapSize) {
        this.maxSegmentSize = maxSegmentSize;
        this.maxOverlapSize = maxOverlapSize;
    }
    
    /**
     * 使用默认参数创建递归文档切分策略
     * 最大片段大小为500，最大重叠大小为100
     */
    public RecursiveSplitterStrategy() {
        this(500, 100);
    }
    
    @Override
    public List<TextSegment> split(Document document) throws SplittingException {
        try {
            DocumentSplitter splitter = DocumentSplitters.recursive(maxSegmentSize, maxOverlapSize);
            return splitter.split(document);
        } catch (Exception e) {
            throw new SplittingException("Failed to split document", e);
        }
    }
    
    @Override
    public String getStrategyName() {
        return "RecursiveSplitter(" + maxSegmentSize + "," + maxOverlapSize + ")";
    }
}