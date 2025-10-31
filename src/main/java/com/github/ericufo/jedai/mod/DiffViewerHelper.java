package com.github.ericufo.jedai.mod;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffDialogHints;
import com.intellij.diff.DiffManager;
import com.intellij.diff.chains.DiffRequestChain;
import com.intellij.diff.chains.SimpleDiffRequestChain;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.DiffRequest;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Diff查看器辅助类（完整实现）
 * 用于在IntelliJ IDEA中显示代码修改的Diff预览
 * 成员C负责：提供用户友好的Diff预览和应用流程
 */
public class DiffViewerHelper {
    private static final Logger LOG = Logger.getInstance(DiffViewerHelper.class);
    
    /**
     * 显示代码修改提案的Diff预览，并提供Apply选项
     * @param project 项目
     * @param proposal 代码修改提案
     */
    public static void showDiff(Project project, CodeChangeProposal proposal) {
        if (proposal.getDiffEntries().isEmpty()) {
            LOG.warn("代码修改提案为空");
            Messages.showWarningDialog(
                project,
                "No changes to display",
                "JEDAI Code Modification"
            );
            return;
        }
        
        try {
            // 如果只有一个文件，使用简单的Diff请求
            if (proposal.getDiffEntries().size() == 1) {
                showSingleFileDiff(project, proposal);
            } else {
                // 支持多文件Diff
                showMultipleFilesDiff(project, proposal);
            }
        } catch (Exception e) {
            LOG.error("显示Diff时出错", e);
            Messages.showErrorDialog(
                project,
                "Error displaying diff: " + e.getMessage(),
                "JEDAI Error"
            );
        }
    }
    
    /**
     * 显示单个文件的Diff
     */
    private static void showSingleFileDiff(Project project, CodeChangeProposal proposal) {
        DiffEntry entry = proposal.getDiffEntries().get(0);
        
        // 创建Diff内容
        DiffContentFactory contentFactory = DiffContentFactory.getInstance();
        VirtualFile virtualFile = findVirtualFile(project, entry.getFilePath());
        
        DiffContent beforeContent;
        DiffContent afterContent;
        
        if (virtualFile != null) {
            beforeContent = contentFactory.create(project, entry.getBeforeText(), virtualFile.getFileType());
            afterContent = contentFactory.create(project, entry.getAfterText(), virtualFile.getFileType());
        } else {
            beforeContent = contentFactory.create(project, entry.getBeforeText());
            afterContent = contentFactory.create(project, entry.getAfterText());
        }
        
        SimpleDiffRequest request = new SimpleDiffRequest(
            proposal.getSummary(),
            beforeContent,
            afterContent,
            "Original",
            "Modified by JEDAI"
        );
        
        // 显示Diff，并在关闭后询问是否应用
        DiffManager.getInstance().showDiff(project, request);
        
        // 延迟询问用户是否应用修改（在Diff窗口关闭后）
        promptToApplyChanges(project, proposal);
    }
    
    /**
     * 显示多个文件的Diff
     */
    private static void showMultipleFilesDiff(Project project, CodeChangeProposal proposal) {
        List<DiffRequest> requests = new ArrayList<>();
        DiffContentFactory contentFactory = DiffContentFactory.getInstance();
        
        for (DiffEntry entry : proposal.getDiffEntries()) {
            VirtualFile virtualFile = findVirtualFile(project, entry.getFilePath());
            
            DiffContent beforeContent;
            DiffContent afterContent;
            
            if (virtualFile != null) {
                beforeContent = contentFactory.create(project, entry.getBeforeText(), virtualFile.getFileType());
                afterContent = contentFactory.create(project, entry.getAfterText(), virtualFile.getFileType());
            } else {
                beforeContent = contentFactory.create(project, entry.getBeforeText());
                afterContent = contentFactory.create(project, entry.getAfterText());
            }
            
            SimpleDiffRequest request = new SimpleDiffRequest(
                "File: " + new File(entry.getFilePath()).getName(),
                beforeContent,
                afterContent,
                "Original",
                "Modified"
            );
            
            requests.add(request);
        }
        
        // 创建Diff链（支持在多个文件间切换）
        DiffRequestChain chain = new SimpleDiffRequestChain(requests);
        DiffManager.getInstance().showDiff(project, chain, DiffDialogHints.DEFAULT);
        
        // 询问用户是否应用修改
        promptToApplyChanges(project, proposal);
    }
    
    /**
     * 询问用户是否应用修改
     */
    private static void promptToApplyChanges(Project project, CodeChangeProposal proposal) {
        // 在EDT线程上显示确认对话框
        javax.swing.SwingUtilities.invokeLater(() -> {
            int result = Messages.showYesNoDialog(
                project,
                "Do you want to apply these changes to your code?\n\n" + 
                proposal.getSummary() + "\n\n" +
                "This will modify " + proposal.getDiffEntries().size() + " file(s).",
                "Apply JEDAI Code Modification",
                "Apply Changes",
                "Cancel",
                Messages.getQuestionIcon()
            );
            
            if (result == Messages.YES) {
                try {
                    LOG.info("用户确认应用代码修改");
                    proposal.apply(project);
                    
                    Messages.showInfoMessage(
                        project,
                        "Code modifications have been applied successfully!",
                        "JEDAI Success"
                    );
                } catch (Exception e) {
                    LOG.error("应用代码修改时出错", e);
                    Messages.showErrorDialog(
                        project,
                        "Failed to apply changes: " + e.getMessage(),
                        "JEDAI Error"
                    );
                }
            } else {
                LOG.info("用户取消应用代码修改");
                Messages.showInfoMessage(
                    project,
                    "Code modifications were not applied.",
                    "JEDAI Cancelled"
                );
            }
        });
    }
    
    /**
     * 查找VirtualFile（支持相对路径和绝对路径）
     */
    private static VirtualFile findVirtualFile(Project project, String filePath) {
        // 先尝试相对路径
        if (project.getBaseDir() != null) {
            VirtualFile file = project.getBaseDir().findFileByRelativePath(filePath);
            if (file != null) {
                return file;
            }
        }
        
        // 再尝试绝对路径
        File file = new File(filePath);
        if (file.exists()) {
            return VfsUtil.findFileByIoFile(file, true);
        }
        
        LOG.warn("找不到文件：" + filePath);
        return null;
    }
    
    /**
     * 显示仅预览的Diff（不提供Apply选项）
     * 用于只读场景
     */
    public static void showDiffReadOnly(Project project, CodeChangeProposal proposal) {
        if (proposal.getDiffEntries().isEmpty()) {
            return;
        }
        
        DiffEntry entry = proposal.getDiffEntries().get(0);
        DiffContentFactory contentFactory = DiffContentFactory.getInstance();
        VirtualFile virtualFile = findVirtualFile(project, entry.getFilePath());
        
        DiffContent beforeContent;
        DiffContent afterContent;
        
        if (virtualFile != null) {
            beforeContent = contentFactory.create(project, entry.getBeforeText(), virtualFile.getFileType());
            afterContent = contentFactory.create(project, entry.getAfterText(), virtualFile.getFileType());
        } else {
            beforeContent = contentFactory.create(project, entry.getBeforeText());
            afterContent = contentFactory.create(project, entry.getAfterText());
        }
        
        SimpleDiffRequest request = new SimpleDiffRequest(
            proposal.getSummary() + " (Preview Only)",
            beforeContent,
            afterContent,
            "Original",
            "Modified"
        );
        
        DiffManager.getInstance().showDiff(project, request);
    }
}

