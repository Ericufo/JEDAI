package com.github.ericufo.jedai.rag;

import java.util.List;

/**
 * Strategy interface for different retrieval algorithms.
 *
 * <p>
 * 中文说明：
 * 这是“检索策略”的抽象接口，用于封装不同的 RAG 检索算法（如 BM25、向量检索、混合检索等）。
 * 通过引入策略模式，SimpleRagRetriever 不再写死具体算法，只依赖本接口，
 * 这样要更换或新增检索实现时，只需新增一个 {@code SearchStrategy} 的实现类即可。
 * </p>
 */
public interface SearchStrategy {

    List<RetrievedChunk> search(String query, int k);
}
