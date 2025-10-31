package com.github.ericufo.jedai.rag.impl;

import com.github.ericufo.jedai.rag.CourseMaterial;
import com.github.ericufo.jedai.rag.IndexStats;
import com.github.ericufo.jedai.rag.RagIndexer;
import com.intellij.openapi.diagnostic.Logger;

import java.util.List;

/**
 * RAG 索引器的简单实现（骨架）
 * TODO: 成员A需要实现具体的索引逻辑
 */
public class SimpleRagIndexer implements RagIndexer {
    private static final Logger LOG = Logger.getInstance(SimpleRagIndexer.class);
    
    @Override
    public IndexStats index(List<CourseMaterial> materials) {
        LOG.info("索引课程材料：" + materials.size() + " 个文件");
        // TODO: 实现文本抽取、分块、索引逻辑
        // 1. 提取文本（PDFBox/Tika）
        // 2. 分块（保留页码信息）
        // 3. 建立索引（Lucene或向量索引）
        return new IndexStats(materials.size(), 0, 0);
    }
    
    @Override
    public boolean isIndexed() {
        // TODO: 检查索引是否存在
        return false;
    }
    
    @Override
    public void clearIndex() {
        // TODO: 清除索引
        LOG.info("清除索引");
    }
}

