package com.github.ericufo.jedai.rag.pipeline;

/**
 * 管道阶段接口
 * 定义管道中每个阶段的处理逻辑
 * @param <I> 输入类型
 * @param <O> 输出类型
 */
public interface PipelineStage<I, O> {
    /**
     * 执行阶段处理
     * @param input 输入数据
     * @return 输出数据
     * @throws PipelineException 管道异常
     */
    O execute(I input) throws PipelineException;
    
    /**
     * 获取阶段名称
     * @return 阶段名称
     */
    String getStageName();
}