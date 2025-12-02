package com.github.ericufo.jedai.rag.impl;

import com.github.ericufo.jedai.rag.RagRetriever;
import com.github.ericufo.jedai.rag.RetrievedChunk;
import com.github.ericufo.jedai.rag.SearchStrategy;
import com.intellij.openapi.diagnostic.Logger;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.List;

/**
 * Simple RAG retriever implementation for searching and retrieving relevant
 * text chunks
 */
public class SimpleRagRetriever implements RagRetriever {
    private static final Logger LOG = Logger.getInstance(SimpleRagRetriever.class);

    private static final EmbeddingStore<TextSegment> EMBEDDING_STORE = SimpleRagIndexer.getEmbeddingStore();
    private static final EmbeddingModel EMBEDDING_MODEL = createEmbeddingModel();

    // Strategy (Strategy pattern) for the actual search algorithm
    // 中文说明：通过组合一个 SearchStrategy 实例，将具体“怎么检索”的算法
    // 从检索器中抽离出来，SimpleRagRetriever 只负责“调用策略”，不关心实现细节。
    private final SearchStrategy searchStrategy;

    /**
     * Creates an instance of AllMiniLmL6V2QuantizedEmbeddingModel
     * 
     * @return the embedding model instance
     */
    private static EmbeddingModel createEmbeddingModel() {
        ClassLoader cl = SimpleRagIndexer.class.getClassLoader();
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(cl);
            return new AllMiniLmL6V2QuantizedEmbeddingModel();
        } finally {
            Thread.currentThread().setContextClassLoader(original);
        }
    }

    public SimpleRagRetriever() {
        // default embedding-based strategy with a reasonable score threshold
        this.searchStrategy = new EmbeddingSearchStrategy(EMBEDDING_STORE, EMBEDDING_MODEL, 0.80);
    }

    /**
     * Searches for relevant text chunks based on the query using configured
     * strategy.
     *
     * @param query the search query string
     * @param k     the number of top results to return
     * @return list of retrieved chunks with relevance scores
     */
    @Override
    public List<RetrievedChunk> search(String query, int k) {
        LOG.info("index query：" + query + "，return top-" + k);
        return searchStrategy.search(query, k);
    }
}