package com.github.ericufo.jedai.rag.impl;

import com.github.ericufo.jedai.rag.RetrievedChunk;
import com.github.ericufo.jedai.rag.SearchStrategy;
import com.intellij.openapi.diagnostic.Logger;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Embedding-based search strategy backed by LangChain4j embedding store.
 *
 * <p>
 * 中文说明：
 * 这是一个基于向量嵌入的检索策略实现类，内部使用 LangChain4j 的 EmbeddingStore。
 * SimpleRagRetriever 通过持有本策略实例，将原来“写死在检索器中的 Embedding 算法”
 * 抽取出来，符合策略模式的结构，后续可以并行增加 BM25 等其他策略。
 * </p>
 */
public class EmbeddingSearchStrategy implements SearchStrategy {

    private static final Logger LOG = Logger.getInstance(EmbeddingSearchStrategy.class);

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    private final double scoreThreshold;

    public EmbeddingSearchStrategy(EmbeddingStore<TextSegment> store,
            EmbeddingModel model,
            double scoreThreshold) {
        this.embeddingStore = store;
        this.embeddingModel = model;
        this.scoreThreshold = scoreThreshold;
    }

    @Override
    public List<RetrievedChunk> search(String query, int k) {
        LOG.info("EmbeddingSearchStrategy query=\"" + query + "\", topK=" + k);

        Embedding queryEmbedding = embeddingModel.embed(query).content();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(queryEmbedding, k);

        List<RetrievedChunk> chunks = new ArrayList<>();
        for (EmbeddingMatch<TextSegment> match : matches) {
            if (match.score() < scoreThreshold) {
                continue;
            }
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
