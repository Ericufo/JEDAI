package com.github.ericufo.jedai.rag.pipeline;

import com.github.ericufo.jedai.rag.CourseMaterial;

import java.util.List;

/**
 * RAG处理管道
 * 负责协调各个阶段的执行
 */
public class RagProcessingPipeline {
    private final List<PipelineStage<?, ?>> stages;
    private PipelineListener listener;
    
    /**
     * 创建RAG处理管道
     * @param stages 管道阶段列表
     */
    public RagProcessingPipeline(List<PipelineStage<?, ?>> stages) {
        this.stages = stages;
    }
    
    /**
     * 设置管道监听器
     * @param listener 管道监听器
     */
    public void setListener(PipelineListener listener) {
        this.listener = listener;
    }
    
    /**
     * 执行管道处理
     * @param material 课程材料
     * @throws PipelineException 管道异常
     */
    public void execute(CourseMaterial material) throws PipelineException {
        if (listener != null) {
            listener.onPipelineStart();
        }
        
        Object current = material;
        
        try {
            for (PipelineStage<?, ?> stage : stages) {
                String stageName = stage.getStageName();
                
                if (listener != null) {
                    listener.onStageStart(stageName, current);
                }
                
                try {
                    // 执行阶段
                    current = executeStage(stage, current);
                    
                    if (listener != null) {
                        listener.onStageComplete(stageName, current);
                    }
                } catch (Exception e) {
                    if (listener != null) {
                        listener.onStageError(stageName, e instanceof PipelineException ? e : new PipelineException(e));
                    }
                    throw new PipelineException("Error in stage: " + stageName, e);
                }
            }
            
            if (listener != null) {
                listener.onPipelineComplete();
            }
        } catch (PipelineException e) {
            if (listener != null) {
                listener.onPipelineError(e);
            }
            throw e;
        }
    }
    
    /**
     * 执行单个阶段
     * @param stage 管道阶段
     * @param input 输入数据
     * @return 输出数据
     * @throws PipelineException 管道异常
     */
    @SuppressWarnings("unchecked")
    private Object executeStage(PipelineStage<?, ?> stage, Object input) throws PipelineException {
        try {
            return ((PipelineStage<Object, Object>) stage).execute(input);
        } catch (Exception e) {
            throw new PipelineException("Failed to execute stage: " + stage.getStageName(), e);
        }
    }
}