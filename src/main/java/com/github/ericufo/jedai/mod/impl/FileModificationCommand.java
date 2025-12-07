package com.github.ericufo.jedai.mod.impl;

import com.github.ericufo.jedai.mod.ModificationCommand;
import com.github.ericufo.jedai.mod.ModificationHistoryEntry;
import com.github.ericufo.jedai.mod.ModificationHistoryManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;

import java.io.File;

/**
 * Concrete command representing a single file modification.
 *
 * <p>
 * 使用命令模式重构：这是“命令模式”的具体命令实现类，用来表示“对某个文件应用一次修改”的操作。
 * 之前这些逻辑全部写在 SimpleCodeModificationService.applyChanges() 中，
 * 现在被提取到该命令中，Service 只负责创建命令并调用 execute()，
 * 从而让修改逻辑可复用、可扩展（例如批量执行、未来的撤销/重放等）。
 * </p>
 */
public class FileModificationCommand implements ModificationCommand {

    private static final Logger LOG = Logger.getInstance(FileModificationCommand.class);

    private final String filePath;
    private final String originalCode;
    private final String modifiedCode;
    private final String instruction;
    private final String language;

    public FileModificationCommand(String filePath,
            String originalCode,
            String modifiedCode,
            String instruction,
            String language) {
        this.filePath = filePath;
        this.originalCode = originalCode;
        this.modifiedCode = modifiedCode;
        this.instruction = instruction;
        this.language = language;
    }

    @Override
    public void execute(Project project) {
        LOG.info("开始应用代码修改到文件：" + filePath);

        try {
            VirtualFile virtualFile = findVirtualFile(project, filePath);
            if (virtualFile == null) {
                LOG.error("找不到文件：" + filePath);
                return;
            }

            Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
            if (document == null) {
                LOG.error("无法获取文档：" + filePath);
                return;
            }

            WriteCommandAction.runWriteCommandAction(project, "JEDAI Code Modification", null, () -> {
                String currentText = document.getText();
                int startIndex = currentText.indexOf(originalCode);
                int endIndex;

                if (startIndex != -1) {
                    endIndex = startIndex + originalCode.length();
                    document.replaceString(startIndex, endIndex, modifiedCode);
                    LOG.info("代码修改已应用");
                } else {
                    LOG.warn("未找到原始代码的精确匹配，替换整个文档");
                    startIndex = 0;
                    endIndex = document.getTextLength();
                    document.setText(modifiedCode);
                }

                FileDocumentManager.getInstance().saveDocument(document);

                try {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                    if (psiFile != null) {
                        LOG.info("开始格式化代码...");
                        int formatStartOffset = startIndex;
                        int formatEndOffset = startIndex + modifiedCode.length();

                        if (formatEndOffset > document.getTextLength()) {
                            formatEndOffset = document.getTextLength();
                        }

                        CodeStyleManager.getInstance(project)
                                .reformatText(psiFile, formatStartOffset, formatEndOffset);
                        LOG.info("代码格式化完成");
                    }
                } catch (Exception formatException) {
                    LOG.warn("代码格式化失败（不影响修改应用）", formatException);
                }
            });

            try {
                ModificationHistoryEntry historyEntry = new ModificationHistoryEntry(
                        instruction, filePath, originalCode, modifiedCode, language);
                ModificationHistoryManager.getInstance().addEntry(historyEntry);
                LOG.info("修改已记录到历史");
            } catch (Exception historyException) {
                LOG.warn("记录历史失败（不影响修改应用）", historyException);
            }

            LOG.info("代码修改成功应用到：" + filePath);

        } catch (Exception e) {
            LOG.error("应用代码修改时出错", e);
        }
    }

    private VirtualFile findVirtualFile(Project project, String filePath) {
        if (project != null && project.getBaseDir() != null) {
            VirtualFile file = project.getBaseDir().findFileByRelativePath(filePath);
            if (file != null) {
                return file;
            }
        }

        File file = new File(filePath);
        if (file.exists()) {
            return VfsUtil.findFileByIoFile(file, true);
        }

        return null;
    }
}
