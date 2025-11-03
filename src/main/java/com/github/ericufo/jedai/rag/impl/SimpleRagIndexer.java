package com.github.ericufo.jedai.rag.impl;

import com.github.ericufo.jedai.rag.CourseMaterial;
import com.github.ericufo.jedai.rag.IndexStats;
import com.github.ericufo.jedai.rag.RagIndexer;
import com.intellij.openapi.diagnostic.Logger;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;

public class SimpleRagIndexer implements RagIndexer {
    private static final Logger LOG = Logger.getInstance(SimpleRagIndexer.class);

    private static final InMemoryEmbeddingStore<TextSegment> EMBEDDING_STORE = new InMemoryEmbeddingStore<>();
    //private static boolean indexed = false;
    private static final Path INDEX_FILE_PATH = Paths.get("rag_materials_cache.json");

    static {
        // Load from persistent file if exists
        if (Files.exists(INDEX_FILE_PATH)) {
            try {
                String json = Files.readString(INDEX_FILE_PATH);
                if (!json.isEmpty()) {
                    EMBEDDING_STORE.fromJson(json);
                }
            } catch (IOException e) {
                LOG.error("Failed to load index from file", e);
            }
        }
    }

    /**
     *  create AllMiniLmL6V2QuantizedEmbeddingModel
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

    public static EmbeddingStore<TextSegment> getEmbeddingStore() {
        return EMBEDDING_STORE;
    }

    @Override
    public IndexStats index(List<CourseMaterial> materials) {
        LOG.info("索引课程材料：" + materials.size() + " 个文件");

        long startTime = System.currentTimeMillis();

        List<TextSegment> allSegments = new ArrayList<>();
        for (CourseMaterial material : materials) {
            Path path = material.getFile().toPath();
            String fileName = material.getFile().getName();
            List<TextSegment> segments = parseAndSplit(material, path, fileName);
            allSegments.addAll(segments);
        }

        EmbeddingModel embeddingModel = createEmbeddingModel();
        List<Embedding> embeddings = embeddingModel.embedAll(allSegments).content();

        EMBEDDING_STORE.removeAll();
        EMBEDDING_STORE.addAll(embeddings, allSegments);

        // Persist to file
        try {
            Files.writeString(INDEX_FILE_PATH, EMBEDDING_STORE.serializeToJson());
        } catch (IOException e) {
            LOG.error("Failed to save index to file", e);
        }

        long indexingTime = System.currentTimeMillis() - startTime;

        return new IndexStats(materials.size(), allSegments.size(), indexingTime);
    }

    private List<TextSegment> parseAndSplit(CourseMaterial material, Path path, String fileName) {
        List<TextSegment> segments = new ArrayList<>();
        CourseMaterial.MaterialType type = material.getType();
        DocumentSplitter splitter = DocumentSplitters.recursive(500, 100); // 统一切割器：最大500字符，重叠100

        try {
            switch (type) {
                case PDF:
                    try (PDDocument pdfDocument = PDDocument.load(material.getFile())) {
                        PDFTextStripper stripper = new PDFTextStripper();
                        for (int page = 1; page <= pdfDocument.getNumberOfPages(); page++) {
                            stripper.setStartPage(page);
                            stripper.setEndPage(page);
                            String pageText = stripper.getText(pdfDocument);
                            if (!pageText.trim().isEmpty()) {
                                Document pageDoc = new Document(pageText);
                                List<TextSegment> pageSegments = splitter.split(pageDoc);
                                for (TextSegment seg : pageSegments) {
                                    Metadata metadata = seg.metadata()
                                            .add("file_name", fileName)
                                            .add("page_number", page)
                                            .add("page_range", page + "-" + page);
                                    segments.add(TextSegment.from(seg.text(), metadata));
                                }
                            }
                        }
                    }
                    break;
                case TEXT:
                    Document textDoc = loadDocument(path, new TextDocumentParser());
                    List<TextSegment> textSegments = splitter.split(textDoc);
                    for (TextSegment seg : textSegments) {
                        Metadata metadata = seg.metadata()
                                .add("file_name", fileName)
                                .add("page_number", 1)
                                .add("page_range", "1-1");
                        segments.add(TextSegment.from(seg.text(), metadata));
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported material type: " + type);
            }
        } catch (IOException e) {
            LOG.error("Failed to parse document: " + fileName, e);
        }

        return segments;
    }

    
    @Override
    public boolean isIndexed() {
        try {
            return Files.exists(INDEX_FILE_PATH) && !Files.readString(INDEX_FILE_PATH).isEmpty();
        } catch (IOException e) {
            LOG.error("Failed to check index file", e);
            return false;
        }
    }
    
    @Override
    public void clearIndex() {
        EMBEDDING_STORE.removeAll();
        try {
            Files.deleteIfExists(INDEX_FILE_PATH);
        } catch (IOException e) {
            LOG.error("Failed to delete index file", e);
        }
        LOG.info("清除索引");
    }
}

