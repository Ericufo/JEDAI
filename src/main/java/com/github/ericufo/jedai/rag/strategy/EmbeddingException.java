package com.github.ericufo.jedai.rag.strategy;

/**
 * 嵌入异常
 */
public class EmbeddingException extends Exception {
    public EmbeddingException(String message) {
        super(message);
    }
    
    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }
}