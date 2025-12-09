package com.github.ericufo.jedai.rag.pipeline;

/**
 * 管道监听器接口
 * 用于监听管道执行过程中的事件
 */
public interface PipelineListener {
    /**
     * 阶段开始时调用
     * @param stageName 阶段名称
     * @param input 输入数据
     */
    void onStageStart(String stageName, Object input);
    
    /**
     * 阶段成功完成时调用
     * @param stageName 阶段名称
     * @param output 输出数据
     */
    void onStageComplete(String stageName, Object output);
    
    /**
     * 阶段失败时调用
     * @param stageName 阶段名称
     * @param exception 异常信息
     */
    void onStageError(String stageName, Exception exception);
    
    /**
     * 管道开始时调用
     */
    void onPipelineStart();
    
    /**
     * 管道成功完成时调用
     */
    void onPipelineComplete();
    
    /**
     * 管道失败时调用
     * @param exception 异常信息
     */
    void onPipelineError(Exception exception);
}