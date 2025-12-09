package com.github.ericufo.jedai.rag.pipeline;

/**
 * 管道异常
 */
public class PipelineException extends Exception {
    public PipelineException(String message) {
        super(message);
    }
    
    public PipelineException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public PipelineException(Throwable cause) {
        super(cause);
    }
}