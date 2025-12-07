package com.github.ericufo.jedai;

import com.github.ericufo.jedai.chat.AnswerOrchestrator;
import com.github.ericufo.jedai.mod.CodeModificationService;
import com.github.ericufo.jedai.rag.RagRetriever;

/**
 * Factory for creating core JEDAI components.
 *
 * <p>
 * 中文说明：
 * 使用工厂模式重构：这是一个“组件工厂”接口，用于集中创建插件中的核心服务对象（RAG、聊天编排器、代码修改服务）。
 * 通过引入工厂模式，UI / Action 不再直接 new 具体实现类，从而降低耦合，方便后续替换不同实现
 * （例如切换到不同的 LLM、不同的 RAG 检索实现）。
 * </p>
 */
public interface JedaiComponentFactory {

    RagRetriever createRagRetriever();

    AnswerOrchestrator createAnswerOrchestrator();

    CodeModificationService createCodeModificationService();
}
