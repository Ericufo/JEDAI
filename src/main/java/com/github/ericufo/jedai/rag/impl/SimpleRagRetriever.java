package com.github.ericufo.jedai.rag.impl;

import com.github.ericufo.jedai.rag.RagRetriever;
import com.github.ericufo.jedai.rag.RetrievedChunk;
import com.intellij.openapi.diagnostic.Logger;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple RAG retriever implementation for searching and retrieving relevant text chunks
 */
public class SimpleRagRetriever implements RagRetriever {
    private static final Logger LOG = Logger.getInstance(SimpleRagRetriever.class);

    private static final EmbeddingStore<TextSegment> EMBEDDING_STORE = SimpleRagIndexer.getEmbeddingStore();

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

    /**
     * Searches for relevant text chunks based on the query
     * 
     * @param query the search query string
     * @param k the number of top results to return
     * @return list of retrieved chunks with relevance scores
     */
    @Override
    public List<RetrievedChunk> search(String query, int k) {
        LOG.info("index query：" + query + "，return top-" + k);
        EmbeddingModel embeddingModel = createEmbeddingModel();
        Embedding queryEmbedding = embeddingModel.embed(query).content();

        List<EmbeddingMatch<TextSegment>> matches = EMBEDDING_STORE.findRelevant(queryEmbedding, k);

        List<RetrievedChunk> chunks = new ArrayList<>();
        double threshold = 0.80;
        for (EmbeddingMatch<TextSegment> match : matches) {
            if (match.score() < threshold) continue;
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