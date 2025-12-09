package com.github.ericufo.jedai.rag.strategy.selector;

import com.github.ericufo.jedai.rag.CourseMaterial;
import com.github.ericufo.jedai.rag.strategy.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 策略选择器
 * 负责根据材料类型或名称选择合适的策略
 */
public class StrategySelector {
    private final Map<String, ParserStrategy> parserStrategies = new HashMap<>();
    private final Map<String, SplitterStrategy> splitterStrategies = new HashMap<>();
    private final Map<String, EmbeddingStrategy> embeddingStrategies = new HashMap<>();
    private final Map<String, StorageStrategy> storageStrategies = new HashMap<>();
    
    /**
     * 创建策略选择器并注册默认策略
     */
    public StrategySelector() {
        registerDefaultStrategies();
    }
    
    /**
     * 注册默认策略
     */
    private void registerDefaultStrategies() {
        // 注册解析策略
        registerParserStrategy(new com.github.ericufo.jedai.rag.strategy.impl.PdfParserStrategy());
        registerParserStrategy(new com.github.ericufo.jedai.rag.strategy.impl.TextParserStrategy());
        
        // 注册切分策略
        registerSplitterStrategy(new com.github.ericufo.jedai.rag.strategy.impl.RecursiveSplitterStrategy());
        registerSplitterStrategy(new com.github.ericufo.jedai.rag.strategy.impl.SlidingWindowSplitterStrategy());
        
        // 注册嵌入策略
        registerEmbeddingStrategy(new com.github.ericufo.jedai.rag.strategy.impl.AllMiniLmEmbeddingStrategy());
        registerEmbeddingStrategy(new com.github.ericufo.jedai.rag.strategy.impl.MockEmbeddingStrategy());
        
        // 注册存储策略
        registerStorageStrategy(new com.github.ericufo.jedai.rag.strategy.impl.InMemoryStorageStrategy());
    }
    
    /**
     * 注册解析策略
     * @param strategy 解析策略
     */
    public void registerParserStrategy(ParserStrategy strategy) {
        parserStrategies.put(strategy.getStrategyName(), strategy);
    }
    
    /**
     * 注册切分策略
     * @param strategy 切分策略
     */
    public void registerSplitterStrategy(SplitterStrategy strategy) {
        splitterStrategies.put(strategy.getStrategyName(), strategy);
    }
    
    /**
     * 注册嵌入策略
     * @param strategy 嵌入策略
     */
    public void registerEmbeddingStrategy(EmbeddingStrategy strategy) {
        embeddingStrategies.put(strategy.getStrategyName(), strategy);
    }
    
    /**
     * 注册存储策略
     * @param strategy 存储策略
     */
    public void registerStorageStrategy(StorageStrategy strategy) {
        storageStrategies.put(strategy.getStrategyName(), strategy);
    }
    
    /**
     * 根据材料类型选择解析策略
     * @param material 课程材料
     * @return 解析策略
     */
    public ParserStrategy selectParser(CourseMaterial material) {
        for (ParserStrategy strategy : parserStrategies.values()) {
            if (strategy.canParse(material)) {
                return strategy;
            }
        }
        throw new IllegalArgumentException("No parser strategy found for material type: " + material.getType());
    }
    
    /**
     * 根据名称选择切分策略
     * @param strategyName 策略名称
     * @return 切分策略
     */
    public SplitterStrategy selectSplitter(String strategyName) {
        SplitterStrategy strategy = splitterStrategies.get(strategyName);
        if (strategy == null) {
            throw new IllegalArgumentException("No splitter strategy found with name: " + strategyName);
        }
        return strategy;
    }
    
    /**
     * 根据名称选择嵌入策略
     * @param strategyName 策略名称
     * @return 嵌入策略
     */
    public EmbeddingStrategy selectEmbedding(String strategyName) {
        EmbeddingStrategy strategy = embeddingStrategies.get(strategyName);
        if (strategy == null) {
            throw new IllegalArgumentException("No embedding strategy found with name: " + strategyName);
        }
        return strategy;
    }
    
    /**
     * 根据名称选择存储策略
     * @param strategyName 策略名称
     * @return 存储策略
     */
    public StorageStrategy selectStorage(String strategyName) {
        StorageStrategy strategy = storageStrategies.get(strategyName);
        if (strategy == null) {
            throw new IllegalArgumentException("No storage strategy found with name: " + strategyName);
        }
        return strategy;
    }
    
    /**
     * 获取默认解析策略
     * @return 默认解析策略
     */
    public ParserStrategy getDefaultParser() {
        return parserStrategies.get("PdfParser");
    }
    
    /**
     * 获取默认切分策略
     * @return 默认切分策略
     */
    public SplitterStrategy getDefaultSplitter() {
        return splitterStrategies.get("RecursiveSplitter(500,100)");
    }
    
    /**
     * 获取默认嵌入策略
     * @return 默认嵌入策略
     */
    public EmbeddingStrategy getDefaultEmbedding() {
        return embeddingStrategies.get("AllMiniLm");
    }
    
    /**
     * 获取默认存储策略
     * @return 默认存储策略
     */
    public StorageStrategy getDefaultStorage() {
        return storageStrategies.get("InMemoryStorage");
    }
}