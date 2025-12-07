package com.github.ericufo.jedai.actions;

import com.github.ericufo.jedai.JedaiComponentFactory;
import com.github.ericufo.jedai.chat.IdeContext;
import com.github.ericufo.jedai.mod.CodeChangeProposal;
import com.github.ericufo.jedai.mod.CodeModificationService;
import com.github.ericufo.jedai.mod.DiffViewerHelper;
import com.github.ericufo.jedai.mod.InstructionTemplateManager;
import com.github.ericufo.jedai.impl.DefaultJedaiComponentFactory;
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
    
    // 使用工厂模式重构：通过组件工厂获取具体的代码修改服务，避免直接依赖实现类
    private final CodeModificationService codeModificationService;
    private InstructionTemplateManager templateManager;
    
    public ModifyCodeAction() {
        super("Modify Code with JEDAI");
        JedaiComponentFactory factory = DefaultJedaiComponentFactory.getInstance();
        this.codeModificationService = factory.createCodeModificationService();
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
        
        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setPreferredSize(new Dimension(550, 280));
        
        // 顶部说明区域
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        JLabel titleLabel = new JLabel("Describe the code modification you want to perform:");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
        topPanel.add(titleLabel, BorderLayout.NORTH);
        
        int lineCount = selectedCode.split("\n").length;
        JLabel infoLabel = new JLabel(
            String.format("<html><font color='gray'>Selected code: %d line(s) | You can use a template or enter custom instructions</font></html>", 
            lineCount)
        );
        topPanel.add(infoLabel, BorderLayout.SOUTH);
        mainPanel.add(topPanel, BorderLayout.NORTH);
        
        // 中间输入区域
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        
        // 模板选择标签
        JLabel templateLabel = new JLabel("Select a template or type your own instruction:");
        centerPanel.add(templateLabel, BorderLayout.NORTH);
        
        // 使用所有模板（默认 + 自定义）
        List<String> allTemplates = templateManager.getAllTemplates();
        String[] templateArray = new String[allTemplates.size() + 1];
        templateArray[0] = "-- Select a template or type below --";
        for (int i = 0; i < allTemplates.size(); i++) {
            templateArray[i + 1] = allTemplates.get(i);
        }
        
        JComboBox<String> comboBox = new JComboBox<>(templateArray);
        comboBox.setEditable(true);
        comboBox.setSelectedIndex(0);
        comboBox.setFont(comboBox.getFont().deriveFont(12f));
        
        // 当用户选择模板时，清除提示文本
        comboBox.addActionListener(e -> {
            if (comboBox.getSelectedIndex() > 0) {
                comboBox.getEditor().setItem(comboBox.getSelectedItem());
            }
        });
        
        centerPanel.add(comboBox, BorderLayout.CENTER);
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        // 底部按钮区域
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        
        JButton saveTemplateBtn = new JButton("Save as Template");
        saveTemplateBtn.setToolTipText("Save the current instruction as a reusable template");
        saveTemplateBtn.addActionListener(e -> {
            Object currentText = comboBox.getEditor().getItem();
            if (currentText != null && !currentText.toString().trim().isEmpty()) {
                String template = currentText.toString().trim();
                if (template.equals("-- Select a template or type below --")) {
                    Messages.showWarningDialog(
                        "Please enter an instruction before saving as template.",
                        "JEDAI - Invalid Input"
                    );
                    return;
                }
                templateManager.addCustomTemplate(template);
                Messages.showInfoMessage(
                    "Template saved successfully!\nYou can now select it from the dropdown list.",
                    "JEDAI - Template Saved"
                );
                // 刷新下拉列表
                comboBox.removeAllItems();
                comboBox.addItem("-- Select a template or type below --");
                for (String t : templateManager.getAllTemplates()) {
                    comboBox.addItem(t);
                }
                comboBox.setSelectedItem(template);
            }
        });
        buttonPanel.add(saveTemplateBtn);
        
        JButton manageTemplatesBtn = new JButton("Manage Templates");
        manageTemplatesBtn.setToolTipText("View and manage your custom templates");
        manageTemplatesBtn.addActionListener(e -> {
            showManageTemplatesDialog(project);
            // 刷新下拉列表
            String currentSelection = (String) comboBox.getSelectedItem();
            comboBox.removeAllItems();
            comboBox.addItem("-- Select a template or type below --");
            for (String t : templateManager.getAllTemplates()) {
                comboBox.addItem(t);
            }
            if (currentSelection != null && !currentSelection.equals("-- Select a template or type below --")) {
                comboBox.setSelectedItem(currentSelection);
            }
        });
        buttonPanel.add(manageTemplatesBtn);
        
        bottomPanel.add(buttonPanel, BorderLayout.WEST);
        
        // 添加帮助提示
        JLabel helpLabel = new JLabel(
            "<html><font color='gray' size='2'>Tip: Default templates are always available, custom templates are saved persistently</font></html>"
        );
        bottomPanel.add(helpLabel, BorderLayout.SOUTH);
        
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        int result = JOptionPane.showConfirmDialog(
            null,
            mainPanel,
            "JEDAI Code Modification - Enter Instruction",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        
        if (result == JOptionPane.OK_OPTION) {
            Object selected = comboBox.getEditor().getItem();
            if (selected != null) {
                String instruction = selected.toString().trim();
                if (!instruction.isEmpty() && !instruction.equals("-- Select a template or type below --")) {
                    return instruction;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 显示模板管理对话框
     */
    private void showManageTemplatesDialog(Project project) {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setPreferredSize(new Dimension(600, 400));
        
        // 顶部标题和说明
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        JLabel titleLabel = new JLabel("Manage Custom Templates");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        topPanel.add(titleLabel, BorderLayout.NORTH);
        
        JLabel descLabel = new JLabel(
            "<html><font color='gray'>Your custom templates are listed below. Default templates cannot be modified.</font></html>"
        );
        topPanel.add(descLabel, BorderLayout.SOUTH);
        mainPanel.add(topPanel, BorderLayout.NORTH);
        
        // 中间列表区域
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        
        // 模板列表
        DefaultListModel<String> listModel = new DefaultListModel<>();
        List<String> customTemplates = templateManager.getCustomTemplates();
        
        if (customTemplates.isEmpty()) {
            listModel.addElement("(No custom templates saved yet)");
        } else {
            for (String template : customTemplates) {
                listModel.addElement(template);
            }
        }
        
        JList<String> templateList = new JList<>(listModel);
        templateList.setFont(templateList.getFont().deriveFont(12f));
        templateList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        JScrollPane scrollPane = new JScrollPane(templateList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Custom Templates (" + customTemplates.size() + ")"));
        centerPanel.add(scrollPane, BorderLayout.CENTER);
        
        // 统计信息
        JLabel statsLabel = new JLabel(
            String.format(
                "<html><font color='gray'>Total templates: %d default + %d custom = %d total</font></html>",
                templateManager.getAllTemplates().size() - customTemplates.size(),
                customTemplates.size(),
                templateManager.getAllTemplates().size()
            )
        );
        centerPanel.add(statsLabel, BorderLayout.SOUTH);
        
        mainPanel.add(centerPanel, BorderLayout.CENTER);
        
        // 底部按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        
        JButton deleteBtn = new JButton("Delete Selected");
        deleteBtn.setToolTipText("Delete the selected custom template");
        deleteBtn.addActionListener(e -> {
            int selectedIndex = templateList.getSelectedIndex();
            if (selectedIndex != -1) {
                String template = listModel.getElementAt(selectedIndex);
                if (template.equals("(No custom templates saved yet)")) {
                    return;
                }
                
                int confirm = Messages.showYesNoDialog(
                    project,
                    "Are you sure you want to delete this template?\n\n\"" + template + "\"",
                    "JEDAI - Confirm Delete",
                    Messages.getQuestionIcon()
                );
                
                if (confirm == Messages.YES) {
                    templateManager.removeCustomTemplate(template);
                    listModel.remove(selectedIndex);
                    
                    if (listModel.isEmpty()) {
                        listModel.addElement("(No custom templates saved yet)");
                    }
                    
                    // 更新统计信息
                    List<String> updated = templateManager.getCustomTemplates();
                    statsLabel.setText(
                        String.format(
                            "<html><font color='gray'>Total templates: %d default + %d custom = %d total</font></html>",
                            templateManager.getAllTemplates().size() - updated.size(),
                            updated.size(),
                            templateManager.getAllTemplates().size()
                        )
                    );
                    scrollPane.setBorder(BorderFactory.createTitledBorder("Custom Templates (" + updated.size() + ")"));
                }
            } else {
                Messages.showWarningDialog(
                    project,
                    "Please select a template to delete.",
                    "JEDAI - No Selection"
                );
            }
        });
        buttonPanel.add(deleteBtn);
        
        JButton clearAllBtn = new JButton("Clear All");
        clearAllBtn.setToolTipText("Delete all custom templates");
        clearAllBtn.addActionListener(e -> {
            if (customTemplates.isEmpty()) {
                Messages.showInfoMessage(
                    project,
                    "There are no custom templates to clear.",
                    "JEDAI - No Templates"
                );
                return;
            }
            
            int confirm = Messages.showYesNoDialog(
                project,
                "Are you sure you want to delete ALL custom templates?\n\n" +
                "This will remove " + customTemplates.size() + " template(s).\n" +
                "This action cannot be undone.",
                "JEDAI - Confirm Clear All",
                Messages.getWarningIcon()
            );
            
            if (confirm == Messages.YES) {
                templateManager.clearCustomTemplates();
                listModel.clear();
                listModel.addElement("(No custom templates saved yet)");
                
                // 更新统计信息
                List<String> updated = templateManager.getCustomTemplates();
                statsLabel.setText(
                    String.format(
                        "<html><font color='gray'>Total templates: %d default + %d custom = %d total</font></html>",
                        templateManager.getAllTemplates().size() - updated.size(),
                        updated.size(),
                        templateManager.getAllTemplates().size()
                    )
                );
                scrollPane.setBorder(BorderFactory.createTitledBorder("Custom Templates (" + updated.size() + ")"));
                
                Messages.showInfoMessage(
                    project,
                    "All custom templates have been deleted.",
                    "JEDAI - Cleared"
                );
            }
        });
        buttonPanel.add(clearAllBtn);
        
        JButton closeBtn = new JButton("Close");
        closeBtn.setToolTipText("Close this dialog");
        closeBtn.addActionListener(e -> {
            Window window = SwingUtilities.getWindowAncestor(mainPanel);
            if (window != null) {
                window.dispose();
            }
        });
        buttonPanel.add(closeBtn);
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        JOptionPane.showMessageDialog(
            null,
            mainPanel,
            "JEDAI Code Modification - Template Manager",
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

