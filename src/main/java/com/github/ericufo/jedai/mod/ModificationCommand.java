package com.github.ericufo.jedai.mod;

import com.intellij.openapi.project.Project;

/**
 * Command abstraction for code modifications.
 *
 * <p>
 * 中文说明：
 * 这是“命令模式”的抽象命令接口，用于表示一次代码修改操作。
 * 每一个具体的代码修改（例如对某个文件应用一次 LLM 生成的补丁）
 * 都会被封装成一个 {@code ModificationCommand} 对象，统一暴露 execute/undo 等方法。
 * 这样 SimpleCodeModificationService 不再直接操作文档，而是负责创建并触发命令。
 * </p>
 */
public interface ModificationCommand {

    void execute(Project project);

    /**
     * Optional undo support (not fully implemented yet).
     * For the current course project we mainly use execute(),
     * but the hook is here to illustrate the Command pattern fully.
     */
    default void undo(Project project) {
        // no-op by default
    }
}
