package com.github.ericufo.jedai.actions;

import com.github.ericufo.jedai.chat.IdeContext;
import com.github.ericufo.jedai.mod.CodeChangeProposal;
import com.github.ericufo.jedai.mod.DiffEntry;
import com.github.ericufo.jedai.mod.DiffViewerHelper;
import com.github.ericufo.jedai.mod.ModificationHistoryEntry;
import com.github.ericufo.jedai.mod.ModificationHistoryManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * 查看修改历史Action（成员C负责）
 */
public class ViewModificationHistoryAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ViewModificationHistoryAction.class);
    
    public ViewModificationHistoryAction() {
        super("View Modification History");
    }
    
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        
        ModificationHistoryManager historyManager = ModificationHistoryManager.getInstance();
        List<ModificationHistoryEntry> history = historyManager.getHistory();
        
        if (history.isEmpty()) {
            Messages.showInfoMessage(
                project,
                "No modification history available yet.\n\nModifications will be recorded automatically when you use 'Modify Code with JEDAI'.",
                "JEDAI - Modification History"
            );
            return;
        }
        
        showHistoryDialog(project, history);
    }
    
    /**
     * 显示历史记录对话框
     */
    private void showHistoryDialog(Project project, List<ModificationHistoryEntry> history) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setPreferredSize(new Dimension(700, 400));
        
        JLabel titleLabel = new JLabel("Modification History (" + history.size() + " entries):");
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // 历史记录列表
        DefaultListModel<ModificationHistoryEntry> listModel = new DefaultListModel<>();
        for (ModificationHistoryEntry entry : history) {
            listModel.addElement(entry);
        }
        
        JList<ModificationHistoryEntry> historyList = new JList<>(listModel);
        historyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        historyList.setCellRenderer(new HistoryListCellRenderer());
        
        JScrollPane scrollPane = new JScrollPane(historyList);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        
        JButton viewDiffBtn = new JButton("View Diff");
        viewDiffBtn.addActionListener(e -> {
            ModificationHistoryEntry selected = historyList.getSelectedValue();
            if (selected != null) {
                showHistoryDiff(project, selected);
            } else {
                Messages.showInfoMessage("Please select a history entry first.", "JEDAI");
            }
        });
        buttonPanel.add(viewDiffBtn);
        
        JButton replayBtn = new JButton("Replay Modification");
        replayBtn.addActionListener(e -> {
            ModificationHistoryEntry selected = historyList.getSelectedValue();
            if (selected != null) {
                replayModification(project, selected);
            } else {
                Messages.showInfoMessage("Please select a history entry first.", "JEDAI");
            }
        });
        buttonPanel.add(replayBtn);
        
        JButton clearHistoryBtn = new JButton("Clear All History");
        clearHistoryBtn.addActionListener(e -> {
            int confirm = Messages.showYesNoDialog(
                project,
                "Are you sure you want to clear all modification history?",
                "Confirm Clear",
                Messages.getQuestionIcon()
            );
            if (confirm == Messages.YES) {
                ModificationHistoryManager.getInstance().clearHistory();
                listModel.clear();
                Messages.showInfoMessage("History cleared successfully.", "JEDAI");
            }
        });
        buttonPanel.add(clearHistoryBtn);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        int result = JOptionPane.showConfirmDialog(
            null,
            panel,
            "JEDAI - Modification History",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
    }
    
    /**
     * 显示历史记录的Diff
     */
    private void showHistoryDiff(Project project, ModificationHistoryEntry entry) {
        try {
            DiffEntry diffEntry = new DiffEntry(
                entry.getFilePath(),
                entry.getOriginalCode(),
                entry.getModifiedCode()
            );
            
            CodeChangeProposal proposal = new CodeChangeProposal(
                "History: " + entry.getInstruction(),
                Collections.singletonList(diffEntry),
                p -> LOG.info("历史记录仅供查看")
            );
            
            // 使用只读模式显示Diff
            DiffViewerHelper.showDiffReadOnly(project, proposal);
            
        } catch (Exception e) {
            LOG.error("显示历史Diff时出错", e);
            Messages.showErrorDialog(
                project,
                "Failed to display diff: " + e.getMessage(),
                "JEDAI Error"
            );
        }
    }
    
    /**
     * 重放修改（将历史记录中的修改再次应用）
     */
    private void replayModification(Project project, ModificationHistoryEntry entry) {
        int confirm = Messages.showYesNoDialog(
            project,
            "Do you want to replay this modification?\n\n" +
            "Instruction: " + entry.getInstruction() + "\n" +
            "File: " + entry.getFileName() + "\n\n" +
            "This will apply the same changes to the current code.",
            "Replay Modification",
            Messages.getQuestionIcon()
        );
        
        if (confirm == Messages.YES) {
            try {
                DiffEntry diffEntry = new DiffEntry(
                    entry.getFilePath(),
                    entry.getOriginalCode(),
                    entry.getModifiedCode()
                );
                
                CodeChangeProposal proposal = new CodeChangeProposal(
                    "Replay: " + entry.getInstruction(),
                    Collections.singletonList(diffEntry),
                    p -> applyHistoryModification(p, entry)
                );
                
                DiffViewerHelper.showDiff(project, proposal);
                
            } catch (Exception e) {
                LOG.error("重放修改时出错", e);
                Messages.showErrorDialog(
                    project,
                    "Failed to replay modification: " + e.getMessage(),
                    "JEDAI Error"
                );
            }
        }
    }
    
    /**
     * 应用历史修改到文件
     */
    private void applyHistoryModification(Project project, ModificationHistoryEntry entry) {
        LOG.info("开始应用历史修改到文件：" + entry.getFilePath());
        
        try {
            // 查找VirtualFile
            VirtualFile virtualFile = findVirtualFile(project, entry.getFilePath());
            if (virtualFile == null) {
                LOG.error("找不到文件：" + entry.getFilePath());
                Messages.showErrorDialog(
                    project,
                    "File not found: " + entry.getFilePath(),
                    "JEDAI Error"
                );
                return;
            }
            
            // 获取Document
            Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
            if (document == null) {
                LOG.error("无法获取文档：" + entry.getFilePath());
                Messages.showErrorDialog(
                    project,
                    "Cannot get document for: " + entry.getFilePath(),
                    "JEDAI Error"
                );
                return;
            }
            
            // 使用WriteCommandAction安全地修改文档
            WriteCommandAction.runWriteCommandAction(project, "JEDAI Replay Modification", null, () -> {
                // 获取当前文档内容
                String currentText = document.getText();
                
                // 尝试查找原始代码
                int startIndex = currentText.indexOf(entry.getOriginalCode());
                
                if (startIndex != -1) {
                    // 找到原始代码，进行替换
                    int endIndex = startIndex + entry.getOriginalCode().length();
                    document.replaceString(startIndex, endIndex, entry.getModifiedCode());
                    LOG.info("成功应用历史修改（精确匹配）");
                } else {
                    // 如果找不到精确匹配，询问用户是否替换整个文档
                    LOG.warn("未找到原始代码的精确匹配");
                    
                    // 在EDT线程上显示确认对话框
                    ApplicationManager.getApplication().invokeLater(() -> {
                        int result = Messages.showYesNoDialog(
                            project,
                            "Cannot find the exact original code in the file.\n" +
                            "The file may have been modified since this history entry was created.\n\n" +
                            "Do you want to replace the entire file content with the modified code?",
                            "JEDAI - Code Mismatch",
                            Messages.getQuestionIcon()
                        );
                        
                        if (result == Messages.YES) {
                            WriteCommandAction.runWriteCommandAction(project, "JEDAI Replace File", null, () -> {
                                document.setText(entry.getModifiedCode());
                                FileDocumentManager.getInstance().saveDocument(document);
                                LOG.info("已替换整个文件内容");
                            });
                        }
                    });
                    return;
                }
                
                // 保存文档
                FileDocumentManager.getInstance().saveDocument(document);
                
                // 自动格式化代码
                try {
                    PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                    if (psiFile != null) {
                        int formatStart = startIndex;
                        int formatEnd = startIndex + entry.getModifiedCode().length();
                        if (formatEnd > document.getTextLength()) {
                            formatEnd = document.getTextLength();
                        }
                        CodeStyleManager.getInstance(project)
                            .reformatText(psiFile, formatStart, formatEnd);
                        LOG.info("代码格式化完成");
                    }
                } catch (Exception formatException) {
                    LOG.warn("代码格式化失败（不影响修改应用）", formatException);
                }
            });
            
            LOG.info("历史修改成功应用到：" + entry.getFilePath());
            
        } catch (Exception e) {
            LOG.error("应用历史修改时出错", e);
            Messages.showErrorDialog(
                project,
                "Failed to apply modification: " + e.getMessage(),
                "JEDAI Error"
            );
        }
    }
    
    /**
     * 查找VirtualFile
     */
    private VirtualFile findVirtualFile(Project project, String filePath) {
        // 先尝试相对路径
        if (project.getBaseDir() != null) {
            VirtualFile file = project.getBaseDir().findFileByRelativePath(filePath);
            if (file != null) {
                return file;
            }
        }
        
        // 再尝试绝对路径
        java.io.File file = new java.io.File(filePath);
        if (file.exists()) {
            return VfsUtil.findFileByIoFile(file, true);
        }
        
        return null;
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabled(e.getProject() != null);
    }
    
    /**
     * 自定义列表渲染器
     */
    private static class HistoryListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                    boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof ModificationHistoryEntry) {
                ModificationHistoryEntry entry = (ModificationHistoryEntry) value;
                setText(String.format(
                    "<html><b>%s</b><br/><small>%s - %s</small></html>",
                    entry.getInstruction(),
                    entry.getTimestamp().toString(),
                    entry.getFileName()
                ));
            }
            
            return c;
        }
    }
}

