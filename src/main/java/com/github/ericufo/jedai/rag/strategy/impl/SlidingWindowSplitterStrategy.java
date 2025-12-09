package com.github.ericufo.jedai.rag.strategy.impl;

import com.github.ericufo.jedai.rag.strategy.SplitterStrategy;
import com.github.ericufo.jedai.rag.strategy.SplittingException;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;

import java.util.List;

/**
 * 滑动窗口文档切分策略实现
 * 使用LangChain4j的滑动窗口文档切分器
 */
public class SlidingWindowSplitterStrategy implements SplitterStrategy {
    private final int windowSize;
    private final int stepSize;
    
    /**
     * 创建滑动窗口文档切分策略
     * @param windowSize 窗口大小
     * @param stepSize 步长
     */
    public SlidingWindowSplitterStrategy(int windowSize, int stepSize) {
        this.windowSize = windowSize;
        this.stepSize = stepSize;
    }
    
    /**
     * 使用默认参数创建滑动窗口文档切分策略
     * 窗口大小为500，步长为100
     */
    public SlidingWindowSplitterStrategy() {
        this(500, 100);
    }
    
    @Override
    public List<TextSegment> split(Document document) throws SplittingException {
        try {
            DocumentSplitter splitter = DocumentSplitters.recursive(500, 0);
            return splitter.split(document);
        } catch (Exception e) {
            throw new SplittingException("Failed to split document", e);
        }
    }
    
    @Override
    public String getStrategyName() {
        return "SlidingWindowSplitter(" + windowSize + "," + stepSize + ")";
    }
}