package com.github.ericufo.jedai.chat;

import com.github.ericufo.jedai.rag.RetrievedChunk;

import java.util.List;

/**
 * 生成的答案
 */
public class Answer {
    private final String content;
    private final List<RetrievedChunk> citations;
    private final boolean isGeneralKnowledge;

    public Answer(String content, List<RetrievedChunk> citations, boolean isGeneralKnowledge) {
        // 验证：如果有引用，则不应标记为通用知识
        if (!citations.isEmpty() && isGeneralKnowledge) {
            throw new IllegalArgumentException("如果有引用，则不应标记为通用知识");
        }
        this.content = content;
        this.citations = citations;
        this.isGeneralKnowledge = isGeneralKnowledge;
    }

    public String getContent() {
        return content;
    }

    public List<RetrievedChunk> getCitations() {
        return citations;
    }

    public boolean isGeneralKnowledge() {
        return isGeneralKnowledge;
    }
}

