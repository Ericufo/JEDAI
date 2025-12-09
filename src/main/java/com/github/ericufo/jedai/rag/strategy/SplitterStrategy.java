package com.github.ericufo.jedai.rag.strategy;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import java.util.List;

/**
 * 文档切分策略接口
 * 负责将文档切分为文本片段
 */
public interface SplitterStrategy {
    /**
     * 切分文档
     * @param document 文档
     * @return 切分后的文本片段列表
     * @throws SplittingException 切分异常
     */
    List<TextSegment> split(Document document) throws SplittingException;
    
    /**
     * 获取策略名称
     * @return 策略名称
     */
    String getStrategyName();
}