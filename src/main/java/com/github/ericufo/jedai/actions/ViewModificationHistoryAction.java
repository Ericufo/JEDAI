package com.github.ericufo.jedai.actions;

import com.github.ericufo.jedai.mod.CodeChangeProposal;
import com.github.ericufo.jedai.mod.DiffEntry;
import com.github.ericufo.jedai.mod.DiffViewerHelper;
import com.github.ericufo.jedai.mod.ModificationCommand;
import com.github.ericufo.jedai.mod.ModificationHistoryEntry;
import com.github.ericufo.jedai.mod.ModificationHistoryManager;
import com.github.ericufo.jedai.mod.impl.FileModificationCommand;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
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
    // 使用命令模式重构：历史重放同样复用 FileModificationCommand，避免重复的文件操作代码
    private void applyHistoryModification(Project project, ModificationHistoryEntry entry) {
        LOG.info("使用命令模式重放历史修改：" + entry.getFilePath());
        ModificationCommand command = new FileModificationCommand(
                entry.getFilePath(),
                entry.getOriginalCode(),
                entry.getModifiedCode(),
                entry.getInstruction(),
                entry.getLanguage());
        command.execute(project);
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

