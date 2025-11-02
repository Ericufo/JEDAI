package com.github.ericufo.jedai.rag.impl;

import com.github.ericufo.jedai.rag.CourseMaterial;
import com.github.ericufo.jedai.rag.IndexStats;
import com.github.ericufo.jedai.rag.RagIndexer;
import com.intellij.openapi.diagnostic.Logger;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

/**
 * RAG 索引器的简单实现（骨架）
 * TODO: 成员A需要实现具体的索引逻辑
 */
public class SimpleRagIndexer implements RagIndexer {
    private static final Logger LOG = Logger.getInstance(SimpleRagIndexer.class);

    private static final InMemoryEmbeddingStore<TextSegment> EMBEDDING_STORE = new InMemoryEmbeddingStore<>();
    private static boolean indexed = false;

    @Override
    public IndexStats index(List<CourseMaterial> materials) {
        LOG.info("索引课程材料：" + materials.size() + " 个文件");
        // TODO: 实现文本抽取、分块、索引逻辑
        // 1. 提取文本（PDFBox/Tika）
        // 2. 分块（保留页码信息）
        // 3. 建立索引（Lucene或向量索引）

        long startTime = System.currentTimeMillis();

        List<Document> documents = new ArrayList<>();
        for (CourseMaterial material : materials) {
            Path path = material.getFile().toPath();
            DocumentParser parser = getParser(material.getType());
            Document document = loadDocument(path, parser);
            documents.add(document);
        }

        DocumentSplitter splitter = DocumentSplitters.recursive(300, 0);
        List<TextSegment> segments = splitter.splitAll(documents);

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        EMBEDDING_STORE.removeAll();
        EMBEDDING_STORE.addAll(embeddings, segments);
        indexed = true;

        long indexingTime = System.currentTimeMillis() - startTime;

        return new IndexStats(materials.size(), segments.size(), indexingTime);
    }

    private DocumentParser getParser(CourseMaterial.MaterialType type) {
        switch (type) {
            case PDF:
                //return new ApachePdfBoxDocumentParser();
            case MARKDOWN:
                //return new MarkdownDocumentParser();
            case TEXT:
                return new TextDocumentParser();
            default:
                throw new IllegalArgumentException("Unsupported material type: " + type);
        }
    }

    public static InMemoryEmbeddingStore<TextSegment> getEmbeddingStore() {
        return EMBEDDING_STORE;
    }
    
    @Override
    public boolean isIndexed() {
        // TODO: 检查索引是否存在
        return indexed;
    }
    
    @Override
    public void clearIndex() {
        // TODO: 清除索引
        EMBEDDING_STORE.removeAll();
        indexed = false;
        LOG.info("清除索引");
    }
}

