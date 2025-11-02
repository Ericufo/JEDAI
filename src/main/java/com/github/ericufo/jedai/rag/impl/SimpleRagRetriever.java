package com.github.ericufo.jedai.rag.impl;

import com.github.ericufo.jedai.rag.RagRetriever;
import com.github.ericufo.jedai.rag.RetrievedChunk;
import com.intellij.openapi.diagnostic.Logger;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 检索器的简单实现（骨架）
 * TODO: 成员A需要实现具体的检索逻辑
 */
public class SimpleRagRetriever implements RagRetriever {
    private static final Logger LOG = Logger.getInstance(SimpleRagRetriever.class);

    private static final EmbeddingStore<TextSegment> EMBEDDING_STORE = SimpleRagIndexer.getEmbeddingStore();
    @Override
    public List<RetrievedChunk> search(String query, int k) {
        LOG.info("检索查询：" + query + "，返回 top-" + k);
        // TODO: 实现检索逻辑
        // 1. 向量化查询（使用Embedding API）
        // 2. 检索最相关的chunks（BM25或向量相似度）
        // 3. 返回带有页码/范围信息的RetrievedChunk列表
        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        List<EmbeddingMatch<TextSegment>> matches = EMBEDDING_STORE.findRelevant(queryEmbedding, k);

        List<RetrievedChunk> chunks = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : matches) {
            TextSegment segment = match.embedded();
            String content = segment.text();
            String sourceDoc = segment.metadata().getString("file_name");
            Integer page = segment.metadata().getInteger("page_number");
            double score = match.score();

            chunks.add(new RetrievedChunk(content, sourceDoc, page, score));
        }

        return chunks;
    }
}

