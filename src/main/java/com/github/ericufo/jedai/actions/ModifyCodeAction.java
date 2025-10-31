package com.github.ericufo.jedai.actions;

import com.github.ericufo.jedai.chat.IdeContext;
import com.github.ericufo.jedai.mod.CodeChangeProposal;
import com.github.ericufo.jedai.mod.CodeModificationService;
import com.github.ericufo.jedai.mod.DiffViewerHelper;
import com.github.ericufo.jedai.mod.InstructionTemplateManager;
import com.github.ericufo.jedai.mod.impl.SimpleCodeModificationService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * 右键菜单Action：请求AI修改代码（F2功能）
 * 完整实现 - 成员C负责
 * 
 * 功能：
 * - 获取用户指令
 * - 异步调用LLM生成修改建议
 * - 显示Diff预览
 * - 提供用户确认和应用流程
 */
public class ModifyCodeAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ModifyCodeAction.class);
    
    private final CodeModificationService codeModificationService = new SimpleCodeModificationService();
    private InstructionTemplateManager templateManager;
    
    public ModifyCodeAction() {
        super("Modify Code with JEDAI");
    }
    
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) return;
        
        SelectionModel selectionModel = editor.getSelectionModel();
        
        // 获取选中的代码
        String selectedText = selectionModel.getSelectedText();
        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        String filePath = file != null && file.getVirtualFile() != null ? file.getVirtualFile().getPath() : null;
        String language = file != null && file.getLanguage() != null ? file.getLanguage().getDisplayName() : null;
        
        // 验证选中的代码
        if (selectedText == null || selectedText.trim().isEmpty()) {
            Messages.showWarningDialog(
                project,
                "Please select some code to modify.\n\n" +
                "Tip: Select the code block you want to modify, then right-click and choose 'Modify Code with JEDAI'.",
                "JEDAI - No Code Selected"
            );
            return;
        }
        
        // 构建IDE上下文
        Integer lineNumber = selectionModel.getSelectionStartPosition() != null 
            ? selectionModel.getSelectionStartPosition().line : null;
        IdeContext ideContext = new IdeContext(
            project.getName(),
            filePath,
            selectedText,
            language,
            lineNumber
        );
        
        // 获取用户指令（带建议列表）
        String instruction = showInstructionDialog(project, selectedText);
        
        if (instruction == null || instruction.trim().isEmpty()) {
            LOG.info("用户取消代码修改操作");
            return;
        }
        
        LOG.info("代码修改指令：" + instruction);
        LOG.info("选中代码片段：" + selectedText.substring(0, Math.min(100, selectedText.length())) + "...");
        
        // 异步生成代码修改提案（使用进度条）
        generateProposalAsync(project, instruction, ideContext);
    }
    
    /**
     * 显示指令输入对话框（带常用指令建议 + 自定义模板管理）
     */
    private String showInstructionDialog(Project project, String selectedCode) {
        // 获取模板管理器
        if (templateManager == null) {
            templateManager = ServiceManager.getService(InstructionTemplateManager.class);
        }
        
        // 创建带下拉建议的输入对话框
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        JLabel label = new JLabel("Enter your instruction for code modification:");
        panel.add(label);
        panel.add(Box.createVerticalStrut(10));
        
        // 使用所有模板（默认 + 自定义）
        List<String> allTemplates = templateManager.getAllTemplates();
        JComboBox<String> comboBox = new JComboBox<>(allTemplates.toArray(new String[0]));
        comboBox.setEditable(true);
        comboBox.setSelectedIndex(-1);
        panel.add(comboBox);
        
        // 按钮面板
        panel.add(Box.createVerticalStrut(5));
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        
        JButton saveTemplateBtn = new JButton("💾 Save as Template");
        saveTemplateBtn.addActionListener(e -> {
            Object currentText = comboBox.getEditor().getItem();
            if (currentText != null && !currentText.toString().trim().isEmpty()) {
                String template = currentText.toString().trim();
                templateManager.addCustomTemplate(template);
                // 刷新下拉列表
                comboBox.removeAllItems();
                for (String t : templateManager.getAllTemplates()) {
                    comboBox.addItem(t);
                }
                comboBox.setSelectedItem(template);
                Messages.showInfoMessage("Template saved successfully!", "JEDAI");
            }
        });
        buttonPanel.add(saveTemplateBtn);
        
        JButton manageTemplatesBtn = new JButton("⚙️ Manage Templates");
        manageTemplatesBtn.addActionListener(e -> {
            showManageTemplatesDialog(project);
            // 刷新下拉列表
            comboBox.removeAllItems();
            for (String t : templateManager.getAllTemplates()) {
                comboBox.addItem(t);
            }
        });
        buttonPanel.add(manageTemplatesBtn);
        
        panel.add(buttonPanel);
        
        panel.add(Box.createVerticalStrut(10));
        JLabel infoLabel = new JLabel("<html><i>Selected " + selectedCode.split("\n").length + " line(s) of code</i></html>");
        panel.add(infoLabel);
        
        int result = JOptionPane.showConfirmDialog(
            null,
            panel,
            "JEDAI - Modify Code",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (result == JOptionPane.OK_OPTION) {
            Object selected = comboBox.getEditor().getItem();
            return selected != null ? selected.toString().trim() : null;
        }
        
        return null;
    }
    
    /**
     * 显示模板管理对话框
     */
    private void showManageTemplatesDialog(Project project) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setPreferredSize(new Dimension(500, 300));
        
        JLabel titleLabel = new JLabel("Manage Custom Templates:");
        panel.add(titleLabel, BorderLayout.NORTH);
        
        // 模板列表
        DefaultListModel<String> listModel = new DefaultListModel<>();
        for (String template : templateManager.getCustomTemplates()) {
            listModel.addElement(template);
        }
        
        JList<String> templateList = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(templateList);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // 按钮面板
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
        
        JButton deleteBtn = new JButton("Delete Selected");
        deleteBtn.addActionListener(e -> {
            int selectedIndex = templateList.getSelectedIndex();
            if (selectedIndex != -1) {
                String template = listModel.getElementAt(selectedIndex);
                templateManager.removeCustomTemplate(template);
                listModel.remove(selectedIndex);
            }
        });
        buttonPanel.add(deleteBtn);
        
        JButton clearAllBtn = new JButton("Clear All");
        clearAllBtn.addActionListener(e -> {
            int confirm = Messages.showYesNoDialog(
                project,
                "Are you sure you want to delete all custom templates?",
                "Confirm Delete",
                Messages.getQuestionIcon()
            );
            if (confirm == Messages.YES) {
                templateManager.clearCustomTemplates();
                listModel.clear();
            }
        });
        buttonPanel.add(clearAllBtn);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        JOptionPane.showMessageDialog(
            null,
            panel,
            "JEDAI - Manage Templates",
            JOptionPane.PLAIN_MESSAGE
        );
    }
    
    /**
     * 异步生成代码修改提案
     */
    private void generateProposalAsync(Project project, String instruction, IdeContext ideContext) {
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "JEDAI: Generating Code Modifications...", true) {
            private CodeChangeProposal proposal;
            
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setText("Analyzing code...");
                indicator.setIndeterminate(false);
                indicator.setFraction(0.2);
                
                try {
                    // 模拟进度（实际应根据LLM调用进度更新）
                    Thread.sleep(500);
                    indicator.setText("Generating modifications with AI...");
                    indicator.setFraction(0.5);
                    
                    // 生成代码修改提案
                    proposal = codeModificationService.proposeChanges(instruction, ideContext);
                    
                    indicator.setText("Preparing diff view...");
                    indicator.setFraction(0.9);
                    Thread.sleep(300);
                    
                    indicator.setFraction(1.0);
                    
                } catch (InterruptedException e) {
                    LOG.warn("代码修改生成被中断");
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    LOG.error("生成代码修改提案时出错", e);
                    ApplicationManager.getApplication().invokeLater(() -> 
                        Messages.showErrorDialog(
                            project,
                            "Failed to generate code modifications:\n" + e.getMessage(),
                            "JEDAI Error"
                        )
                    );
                }
            }
            
            @Override
            public void onSuccess() {
                if (proposal != null) {
                    // 在EDT线程显示Diff
                    ApplicationManager.getApplication().invokeLater(() -> {
                        try {
                            DiffViewerHelper.showDiff(project, proposal);
                        } catch (Exception e) {
                            LOG.error("显示Diff时出错", e);
                            Messages.showErrorDialog(
                                project,
                                "Failed to display diff: " + e.getMessage(),
                                "JEDAI Error"
                            );
                        }
                    });
                }
            }
            
            @Override
            public void onThrowable(@NotNull Throwable error) {
                LOG.error("后台任务出错", error);
                ApplicationManager.getApplication().invokeLater(() ->
                    Messages.showErrorDialog(
                        project,
                        "An unexpected error occurred:\n" + error.getMessage(),
                        "JEDAI Error"
                    )
                );
            }
        });
    }
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // 动态更新Action的可用性
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        Project project = e.getProject();
        
        // 只有在有编辑器和项目时才启用
        boolean enabled = editor != null && project != null;
        
        // 如果有编辑器，检查是否有选中的文本
        if (enabled) {
            SelectionModel selectionModel = editor.getSelectionModel();
            String selectedText = selectionModel.getSelectedText();
            enabled = selectedText != null && !selectedText.trim().isEmpty();
        }
        
        e.getPresentation().setEnabled(enabled);
        e.getPresentation().setVisible(true);
    }
}

