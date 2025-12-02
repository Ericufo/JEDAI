package com.github.ericufo.jedai.impl;

import com.github.ericufo.jedai.JedaiComponentFactory;
import com.github.ericufo.jedai.chat.AnswerOrchestrator;
import com.github.ericufo.jedai.chat.impl.LoggingAnswerOrchestrator;
import com.github.ericufo.jedai.chat.impl.SimpleAnswerOrchestrator;
import com.github.ericufo.jedai.mod.CodeModificationService;
import com.github.ericufo.jedai.mod.impl.SimpleCodeModificationService;
import com.github.ericufo.jedai.rag.RagRetriever;
import com.github.ericufo.jedai.rag.impl.SimpleRagRetriever;

/**
 * Default factory implementation used by the plugin.
 *
 * <p>
 * 中文说明：
 * 这是 JEDAI 的默认组件工厂实现，集中负责创建各个子系统的默认实现：
 * - RAG 检索：SimpleRagRetriever
 * - 问答编排器：SimpleAnswerOrchestrator，并通过 LoggingAnswerOrchestrator 做装饰（增加日志）
 * - 代码修改服务：SimpleCodeModificationService
 *
 * 这样所有“具体类选择”都收口在工厂里，UI / Action 只依赖接口，不再依赖具体实现。
 * </p>
 */
public class DefaultJedaiComponentFactory implements JedaiComponentFactory {

    private static final DefaultJedaiComponentFactory INSTANCE = new DefaultJedaiComponentFactory();

    public static DefaultJedaiComponentFactory getInstance() {
        return INSTANCE;
    }

    private DefaultJedaiComponentFactory() {
    }

    @Override
    public RagRetriever createRagRetriever() {
        return new SimpleRagRetriever();
    }

    @Override
    public AnswerOrchestrator createAnswerOrchestrator() {
        // Core implementation
        AnswerOrchestrator core = new SimpleAnswerOrchestrator();
        // Wrap with logging decorator (Decorator pattern)
        return new LoggingAnswerOrchestrator(core);
    }

    @Override
    public CodeModificationService createCodeModificationService() {
        return new SimpleCodeModificationService();
    }
}
