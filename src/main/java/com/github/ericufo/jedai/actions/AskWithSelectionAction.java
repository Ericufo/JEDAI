package com.github.ericufo.jedai.actions;

import com.github.ericufo.jedai.chat.IdeContext;
import com.github.ericufo.jedai.toolWindow.JedaiChatPanel;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiFile;
import com.intellij.ui.content.Content;

/**
 * 右键菜单Action：询问选中代码相关的问题
 * 现在答案会显示在JEDAI ToolWindow的聊天界面中（类似Cursor的体验）
 */
public class AskWithSelectionAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(AskWithSelectionAction.class);

    public AskWithSelectionAction() {
        super("Ask JEDAI about Selection");
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null)
            return;

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null)
            return;

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
                    "JEDAI");
            return;
        }

        // 构建IDE上下文
        Integer lineNumber = selectionModel.getSelectionStartPosition() != null
                ? selectionModel.getSelectionStartPosition().line
                : null;
        IdeContext ideContext = new IdeContext(
                project.getName(),
                filePath,
                selectedText,
                language,
                lineNumber);

        // 生成问题提示
        String userQuestion = Messages.showInputDialog(
                project,
                "Ask a question about the selected code:",
                "JEDAI - Ask Question",
                Messages.getQuestionIcon(),
                "What does this code do?（解释一下这段代码）",
                null);

        if (userQuestion == null)
            return;

        LOG.info("用户问题：" + userQuestion + "，选中代码：" + selectedText.substring(0, Math.min(50, selectedText.length()))
                + "...");

        // 获取JEDAI ToolWindow
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("JEDAI");
        if (toolWindow == null) {
            Messages.showErrorDialog(project, "JEDAI ToolWindow not found.", "Error");
            return;
        }

        // 激活ToolWindow（如果未显示）
        toolWindow.activate(null);

        // 获取JedaiChatPanel实例
        Content content = toolWindow.getContentManager().getContent(0);
        if (content != null && content.getComponent() instanceof JedaiChatPanel) {
            JedaiChatPanel chatPanel = (JedaiChatPanel) content.getComponent();

            // 在聊天界面中显示问题和答案（带代码上下文）
            chatPanel.askQuestionWithContext(userQuestion, ideContext);
        } else {
            Messages.showErrorDialog(project, "Failed to get chat panel.", "Error");
        }
    }
}
