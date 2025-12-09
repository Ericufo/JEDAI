package com.github.ericufo.jedai.rag;

/**
 * 索引统计信息
 */
public class IndexStats {
    private final int totalDocuments;
    private final int totalChunks;
    private final long indexingTimeMs;

    public IndexStats(int totalDocuments, int totalChunks, long indexingTimeMs) {
        this.totalDocuments = totalDocuments;
        this.totalChunks = totalChunks;
        this.indexingTimeMs = indexingTimeMs;
    }

    public int getMaterialCount() {
        return totalDocuments;
    }

    public int getChunkCount() {
        return totalChunks;
    }

    public long getIndexingTime() {
        return indexingTimeMs;
    }

    // 保留原有方法名以确保兼容性
    public int getTotalDocuments() {
        return totalDocuments;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public long getIndexingTimeMs() {
        return indexingTimeMs;
    }
}