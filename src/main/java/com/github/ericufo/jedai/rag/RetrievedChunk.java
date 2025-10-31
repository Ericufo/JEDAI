package com.github.ericufo.jedai.rag;

/**
 * 检索到的知识块，包含内容和来源信息
 */
public class RetrievedChunk {
    private final String content;
    private final String sourceDoc;
    private final Integer page;
    private final Integer[] pageRange;
    private final double score;

    public RetrievedChunk(String content, String sourceDoc, Integer page, Integer[] pageRange, double score) {
        this.content = content;
        this.sourceDoc = sourceDoc;
        this.page = page;
        this.pageRange = pageRange;
        this.score = score;
    }

    public RetrievedChunk(String content, String sourceDoc, Integer page, double score) {
        this(content, sourceDoc, page, null, score);
    }

    public String getContent() {
        return content;
    }

    public String getSourceDoc() {
        return sourceDoc;
    }

    public Integer getPage() {
        return page;
    }

    public Integer[] getPageRange() {
        return pageRange;
    }

    public double getScore() {
        return score;
    }
}

