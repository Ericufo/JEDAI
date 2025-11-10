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
        
        // 构建清晰的标题
        String fileName = new File(entry.getFilePath()).getName();
        String title = "JEDAI Code Modification - " + fileName;
        
        SimpleDiffRequest request = new SimpleDiffRequest(
            title,
            beforeContent,
            afterContent,
            "Current Code",
            "Proposed Changes"
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
        
        int fileIndex = 1;
        int totalFiles = proposal.getDiffEntries().size();
        
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
            
            // 构建清晰的标题，显示当前文件编号
            String fileName = new File(entry.getFilePath()).getName();
            String title = String.format("JEDAI Code Modification [%d/%d] - %s", 
                fileIndex, totalFiles, fileName);
            
            SimpleDiffRequest request = new SimpleDiffRequest(
                title,
                beforeContent,
                afterContent,
                "Current Code",
                "Proposed Changes"
            );
            
            requests.add(request);
            fileIndex++;
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
            // 创建自定义确认对话框面板
            javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout(10, 10));
            panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
            panel.setPreferredSize(new java.awt.Dimension(500, 200));
            
            // 顶部说明
            javax.swing.JPanel topPanel = new javax.swing.JPanel(new java.awt.BorderLayout(5, 5));
            javax.swing.JLabel titleLabel = new javax.swing.JLabel("Apply Code Modifications?");
            titleLabel.setFont(titleLabel.getFont().deriveFont(java.awt.Font.BOLD, 14f));
            topPanel.add(titleLabel, java.awt.BorderLayout.NORTH);
            
            javax.swing.JLabel descLabel = new javax.swing.JLabel(
                "<html><font color='gray'>You have reviewed the diff. Do you want to apply these changes?</font></html>"
            );
            topPanel.add(descLabel, java.awt.BorderLayout.SOUTH);
            panel.add(topPanel, java.awt.BorderLayout.NORTH);
            
            // 中间信息区域
            javax.swing.JPanel centerPanel = new javax.swing.JPanel();
            centerPanel.setLayout(new javax.swing.BoxLayout(centerPanel, javax.swing.BoxLayout.Y_AXIS));
            centerPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Modification Summary"));
            
            // 摘要信息
            javax.swing.JLabel summaryLabel = new javax.swing.JLabel(
                "<html><b>Description:</b> " + proposal.getSummary() + "</html>"
            );
            summaryLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            centerPanel.add(summaryLabel);
            centerPanel.add(javax.swing.Box.createVerticalStrut(10));
            
            // 文件数量
            int fileCount = proposal.getDiffEntries().size();
            javax.swing.JLabel fileLabel = new javax.swing.JLabel(
                "<html><b>Files to be modified:</b> " + fileCount + " file(s)</html>"
            );
            fileLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
            centerPanel.add(fileLabel);
            centerPanel.add(javax.swing.Box.createVerticalStrut(5));
            
            // 文件列表（如果文件不多，列出来）
            if (fileCount <= 5) {
                for (DiffEntry entry : proposal.getDiffEntries()) {
                    String fileName = new java.io.File(entry.getFilePath()).getName();
                    javax.swing.JLabel fileItemLabel = new javax.swing.JLabel(
                        "<html>&nbsp;&nbsp;• " + fileName + "</html>"
                    );
                    fileItemLabel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
                    centerPanel.add(fileItemLabel);
                }
            }
            
            panel.add(centerPanel, java.awt.BorderLayout.CENTER);
            
            // 底部警告提示
            javax.swing.JLabel warningLabel = new javax.swing.JLabel(
                "<html><font color='#CC7832'>Note: This action will modify your source files. Make sure you have reviewed the changes.</font></html>"
            );
            panel.add(warningLabel, java.awt.BorderLayout.SOUTH);
            
            // 显示对话框
            int result = javax.swing.JOptionPane.showConfirmDialog(
                null,
                panel,
                "JEDAI Code Modification - Confirm Changes",
                javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.QUESTION_MESSAGE
            );
            
            if (result == javax.swing.JOptionPane.YES_OPTION) {
                try {
                    LOG.info("用户确认应用代码修改");
                    proposal.apply(project);
                    
                    // 成功提示
                    javax.swing.JPanel successPanel = new javax.swing.JPanel(new java.awt.BorderLayout(10, 10));
                    successPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
                    
                    javax.swing.JLabel successIcon = new javax.swing.JLabel("✓", javax.swing.SwingConstants.CENTER);
                    successIcon.setFont(successIcon.getFont().deriveFont(java.awt.Font.BOLD, 32f));
                    successIcon.setForeground(new java.awt.Color(98, 150, 85));
                    successPanel.add(successIcon, java.awt.BorderLayout.WEST);
                    
                    javax.swing.JLabel successMsg = new javax.swing.JLabel(
                        "<html><b>Success!</b><br/>" +
                        "Code modifications have been applied to " + fileCount + " file(s).<br/>" +
                        "You can now review the changes in your editor.</html>"
                    );
                    successPanel.add(successMsg, java.awt.BorderLayout.CENTER);
                    
                    Messages.showInfoMessage(
                        project,
                        "Code modifications have been applied successfully!\n\n" +
                        "Modified " + fileCount + " file(s).",
                        "JEDAI - Success"
                    );
                } catch (Exception e) {
                    LOG.error("应用代码修改时出错", e);
                    Messages.showErrorDialog(
                        project,
                        "Failed to apply changes:\n\n" + e.getMessage() + "\n\n" +
                        "Please check the logs for more details.",
                        "JEDAI - Error"
                    );
                }
            } else {
                LOG.info("用户取消应用代码修改");
                Messages.showInfoMessage(
                    project,
                    "Code modifications were not applied.\n\n" +
                    "Your files remain unchanged.",
                    "JEDAI - Cancelled"
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
        
        // 构建只读预览的标题
        String fileName = new File(entry.getFilePath()).getName();
        String title = "JEDAI Code Modification (Preview Only) - " + fileName;
        
        SimpleDiffRequest request = new SimpleDiffRequest(
            title,
            beforeContent,
            afterContent,
            "Current Code",
            "Preview Changes"
        );
        
        DiffManager.getInstance().showDiff(project, request);
    }
}

