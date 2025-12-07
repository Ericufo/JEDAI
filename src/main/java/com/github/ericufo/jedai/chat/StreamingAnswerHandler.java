package com.github.ericufo.jedai.chat;

/**
 * 流式答案处理器接口
 * 用于接收LLM逐步生成的答案token
 */
public interface StreamingAnswerHandler {

    /**
     * 接收到新的token（可能是单个字或多个字）
     * 
     * @param token 新生成的文本片段
     */
    void onNext(String token);

    /**
     * 答案生成完成
     * 
     * @param answer 完整的答案对象（包含引用信息）
     */
    void onComplete(Answer answer);

    /**
     * 生成过程中发生错误
     * 
     * @param error 错误信息
     */
    void onError(Throwable error);
}
