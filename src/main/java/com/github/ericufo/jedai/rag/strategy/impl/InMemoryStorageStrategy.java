package com.github.ericufo.jedai.rag.strategy.impl;

import com.github.ericufo.jedai.rag.strategy.EmbeddingResult;
import com.github.ericufo.jedai.rag.strategy.StorageException;
import com.github.ericufo.jedai.rag.strategy.StorageStrategy;
import com.intellij.openapi.diagnostic.Logger;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 内存存储策略实现
 * 使用LangChain4j的InMemoryEmbeddingStore存储向量
 */
public class InMemoryStorageStrategy implements StorageStrategy {
    private static final Logger LOG = Logger.getInstance(InMemoryStorageStrategy.class);
    
    private static final Path INDEX_FILE_PATH = Paths.get("rag_materials_meta.json");
    private final InMemoryEmbeddingStore<TextSegment> embeddingStore;
    
    /**
     * 创建内存存储策略实例
     */
    public InMemoryStorageStrategy() {
        this.embeddingStore = new InMemoryEmbeddingStore<>();
        loadFromPersistentFile();
    }
    
    /**
     * 从持久化文件加载索引
     */
    private void loadFromPersistentFile() {
        if (Files.exists(INDEX_FILE_PATH)) {
            try {
                String json = Files.readString(INDEX_FILE_PATH);
                if (!json.isEmpty()) {
                    ObjectMapper objectMapper = new ObjectMapper();
                    
                    // 定义要反序列化的类型
                    TypeFactory typeFactory = TypeFactory.defaultInstance();
                    CollectionType listType = typeFactory.constructCollectionType(
                            ArrayList.class, 
                            typeFactory.constructMapType(
                                    HashMap.class, 
                                    typeFactory.constructType(String.class),
                                    typeFactory.constructType(Object.class)
                            )
                    );
                    
                    // 从JSON反序列化数据
                    List<Map<String, Object>> dataList = objectMapper.readValue(json, listType);
                    
                    // 转换并添加到嵌入存储
                    List<Embedding> embeddings = new ArrayList<>();
                    List<TextSegment> segments = new ArrayList<>();
                    
                    for (Map<String, Object> data : dataList) {
                        // 反序列化嵌入向量
                        List<Double> vectorList = (List<Double>) data.get("embedding");
                        float[] vector = new float[vectorList.size()];
                        for (int i = 0; i < vectorList.size(); i++) {
                            vector[i] = vectorList.get(i).floatValue();
                        }
                        embeddings.add(Embedding.from(vector));
                        
                        // 反序列化文本段
                        String text = (String) data.get("text");
                        // 在LangChain4j 0.35.0中，TextSegment的metadata处理方式可能已改变
                        // 这里直接创建TextSegment，metadata暂时不处理
                        TextSegment segment = TextSegment.from(text);
                        segments.add(segment);
                    }
                    
                    // 添加到嵌入存储
                    embeddingStore.addAll(embeddings, segments);
                    
                    LOG.info("成功从持久化文件加载了 " + dataList.size() + " 条记录");
                }
            } catch (IOException e) {
                LOG.error("从文件加载索引失败", e);
            } catch (Exception e) {
                LOG.error("反序列化索引数据失败", e);
            }
        }
    }
    
    /**
     * 保存索引到持久化文件
     */
    private void saveToPersistentFile() {
        try {
            // 获取所有存储的嵌入向量
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(createDummyEmbedding())
                    .maxResults(Integer.MAX_VALUE)
                    .build();
            
            EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
            List<EmbeddingMatch<TextSegment>> matches = result.matches();
            
            // 准备要序列化的数据
            List<Map<String, Object>> dataList = new ArrayList<>();
            
            for (EmbeddingMatch<TextSegment> match : matches) {
                Map<String, Object> data = new HashMap<>();
                
                // 序列化嵌入向量
                float[] vector = match.embedding().vector();
                List<Double> vectorList = new ArrayList<>();
                for (float v : vector) {
                    vectorList.add((double) v);
                }
                data.put("embedding", vectorList);
                
                // 序列化文本段
                TextSegment segment = match.embedded();
                data.put("text", segment.text());
                // 在LangChain4j 0.35.0中，metadata可能以不同的方式处理
                // 这里暂时存储一个空的metadata映射，后续可以根据需要实现
                data.put("metadata", new HashMap<String, String>());
                
                dataList.add(data);
            }
            
            // 使用Jackson序列化为JSON
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            String json = objectMapper.writeValueAsString(dataList);
            
            // 写入文件
            Files.writeString(INDEX_FILE_PATH, json);
            
            LOG.info("成功保存 " + dataList.size() + " 条记录到持久化文件");
        } catch (IOException e) {
            LOG.error("保存索引到文件失败", e);
        } catch (Exception e) {
            LOG.error("序列化索引数据失败", e);
        }
    }
    
    @Override
    public void store(List<EmbeddingResult> results) throws StorageException {
        try {
            List<Embedding> embeddings = results.stream()
                    .map(EmbeddingResult::getEmbedding)
                    .collect(Collectors.toList());
            
            List<TextSegment> segments = results.stream()
                    .map(EmbeddingResult::getSegment)
                    .collect(Collectors.toList());
            
            embeddingStore.addAll(embeddings, segments);
            saveToPersistentFile();
            
            LOG.info("存储了 " + results.size() + " 个嵌入向量，当前总存储量: " + getStorageSize());
        } catch (Exception e) {
            throw new StorageException("存储嵌入向量失败", e);
        }
    }
    
    @Override
    public void clear() throws StorageException {
        try {
            embeddingStore.removeAll();
            Files.deleteIfExists(INDEX_FILE_PATH);
            LOG.info("已清除所有存储数据");
        } catch (IOException e) {
            throw new StorageException("清除存储数据失败", e);
        }
    }
    
    @Override
    public int getStorageSize() throws StorageException {
        try {
            // 使用搜索所有结果来获取存储大小
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(createDummyEmbedding())
                    .maxResults(Integer.MAX_VALUE)
                    .build();
            
            EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
            return result.matches().size();
        } catch (Exception e) {
            throw new StorageException("获取存储大小失败", e);
        }
    }
    
    /**
     * 创建一个虚拟的嵌入向量用于获取存储大小
     * @return 虚拟嵌入向量
     */
    private Embedding createDummyEmbedding() {
        // 创建一个全零的嵌入向量，维度与AllMiniLm相同(384)
        float[] vector = new float[384];
        return Embedding.from(vector);
    }
    
    @Override
    public String getStrategyName() {
        return "InMemoryStorage";
    }
    
    @Override
    public List<TextSegment> retrieve(Embedding queryEmbedding, int maxResults) throws StorageException {
        try {
            LOG.info("开始检索，最大结果数: " + maxResults);
            
            EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                    .queryEmbedding(queryEmbedding)
                    .maxResults(maxResults)
                    .build();
            
            EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
            List<EmbeddingMatch<TextSegment>> matches = result.matches();
            
            LOG.info("检索到 " + matches.size() + " 个匹配结果");
            
            // 打印所有匹配的分数用于调试
            for (EmbeddingMatch<TextSegment> match : matches) {
                LOG.info("匹配分数: " + match.score());
            }
            
            // 降低相似度阈值到0.3，以便获取更多结果
            List<TextSegment> segments = matches.stream()
                    .filter(match -> match.score() > 0.3)
                    .map(EmbeddingMatch::embedded)
                    .collect(Collectors.toList());
            
            LOG.info("过滤后保留 " + segments.size() + " 个相关文本片段");
            return segments;
        } catch (Exception e) {
            throw new StorageException("检索相关文本片段失败", e);
        }
    }
    
    /**
     * 获取嵌入存储实例
     * @return 嵌入存储实例
     */
    public InMemoryEmbeddingStore<TextSegment> getEmbeddingStore() {
        return embeddingStore;
    }
}