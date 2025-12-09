package com.github.ericufo.jedai.rag.strategy;

/**
 * 存储异常
 */
public class StorageException extends Exception {
    public StorageException(String message) {
        super(message);
    }
    
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public StorageException(Throwable cause) {
        super(cause);
    }
}