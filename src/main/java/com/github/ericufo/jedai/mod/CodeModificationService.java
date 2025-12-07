package com.github.ericufo.jedai.mod;

import com.github.ericufo.jedai.chat.IdeContext;

/**
 * 代码修改服务接口
 * 负责根据用户指令生成代码修改提案
 */
public interface CodeModificationService {
    /**
     * 生成代码修改提案
     * @param instruction 用户指令（如"重构这个方法，应用单例模式"）
     * @param ideContext IDE上下文（包含当前文件、选中代码等）
     * @return 代码修改提案，包含差异信息和应用逻辑
     */
    CodeChangeProposal proposeChanges(String instruction, IdeContext ideContext);
}

