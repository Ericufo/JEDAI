package com.github.ericufo.jedai.rag.strategy;

import com.github.ericufo.jedai.rag.CourseMaterial;
import dev.langchain4j.data.document.Document;
import java.io.IOException;

/**
 * 解析策略接口
 * 负责将课程材料解析为文档
 */
public interface ParserStrategy {
    /**
     * 解析课程材料
     * @param material 课程材料
     * @return 解析后的文档
     * @throws ParsingException 解析异常
     * @throws IOException IO异常
     */
    Document parse(CourseMaterial material) throws ParsingException, IOException;
    
    /**
     * 检查是否可以解析指定类型的材料
     * @param material 课程材料
     * @return 是否可以解析
     */
    boolean canParse(CourseMaterial material);
    
    /**
     * 获取策略名称
     * @return 策略名称
     */
    String getStrategyName();
}