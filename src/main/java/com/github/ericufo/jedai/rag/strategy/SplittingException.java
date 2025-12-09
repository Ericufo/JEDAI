package com.github.ericufo.jedai.rag.strategy;

/**
 * 切分异常
 */
public class SplittingException extends Exception {
    public SplittingException(String message) {
        super(message);
    }
    
    public SplittingException(String message, Throwable cause) {
        super(message, cause);
    }
}