package com.github.ericufo.jedai.actions;

import com.github.ericufo.jedai.chat.IdeContext;
import com.github.ericufo.jedai.mod.CodeChangeProposal;
import com.github.ericufo.jedai.mod.CodeModificationService;
import com.github.ericufo.jedai.mod.DiffViewerHelper;
import com.github.ericufo.jedai.mod.impl.SimpleCodeModificationService;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;

/**
 * 右键菜单Action：请求AI修改代码（F2功能）
 * 例如：重构方法、应用设计模式、修复bug等
 */
public class ModifyCodeAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(ModifyCodeAction.class);
    
    // TODO: 成员C需要将这个改为通过Service获取
    private final CodeModificationService codeModificationService = new SimpleCodeModificationService();
    
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
        
        if (selectedText == null || selectedText.trim().isEmpty()) {
            Messages.showInfoMessage(
                project,
                "Please select some code first.",
                "JEDAI"
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
        
        // 获取用户指令
        String instruction = Messages.showInputDialog(
            project,
            "Enter your instruction for code modification:\n(e.g., 'Refactor this method using Singleton pattern', 'Fix the bug in this code')",
            "JEDAI - Modify Code",
            Messages.getQuestionIcon(),
            "Refactor this code",
            null
        );
        
        if (instruction == null) return;
        
        LOG.info("代码修改指令：" + instruction + "，选中代码：" + selectedText.substring(0, Math.min(50, selectedText.length())) + "...");
        
        try {
            // 生成代码修改提案
            CodeChangeProposal proposal = codeModificationService.proposeChanges(instruction, ideContext);
            
            // 显示Diff预览
            DiffViewerHelper.showDiff(project, proposal);
        } catch (Exception ex) {
            LOG.error("生成代码修改提案时出错", ex);
            Messages.showErrorDialog(
                project,
                "生成代码修改提案时出错：" + ex.getMessage(),
                "JEDAI Error"
            );
        }
    }
}

