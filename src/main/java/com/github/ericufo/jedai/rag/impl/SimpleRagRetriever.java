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

public class SimpleRagRetriever implements RagRetriever {
    private static final Logger LOG = Logger.getInstance(SimpleRagRetriever.class);

    private static final EmbeddingStore<TextSegment> EMBEDDING_STORE = SimpleRagIndexer.getEmbeddingStore();

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

    @Override
    public List<RetrievedChunk> search(String query, int k) {
        LOG.info("检索查询：" + query + "，返回 top-" + k);
        EmbeddingModel embeddingModel = createEmbeddingModel();
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

