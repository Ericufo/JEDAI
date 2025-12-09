package com.github.ericufo.jedai.rag.strategy;

/**
 * 解析异常
 */
public class ParsingException extends Exception {
    public ParsingException(String message) {
        super(message);
    }
    
    public ParsingException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ParsingException(Throwable cause) {
        super(cause);
    }
}