package com.github.ericufo.jedai.actions;

import com.github.ericufo.jedai.chat.Answer;
import com.github.ericufo.jedai.chat.AnswerOrchestrator;
import com.github.ericufo.jedai.chat.IdeContext;
import com.github.ericufo.jedai.chat.impl.SimpleAnswerOrchestrator;
import com.github.ericufo.jedai.rag.RagRetriever;
import com.github.ericufo.jedai.rag.RetrievedChunk;
import com.github.ericufo.jedai.rag.impl.SimpleRagRetriever;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiFile;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 右键菜单Action：询问选中代码相关的问题
 */
public class AskWithSelectionAction extends AnAction {
    private static final Logger LOG = Logger.getInstance(AskWithSelectionAction.class);
    
    // TODO: 成员B需要将这些改为通过Service获取，而不是直接实例化
    private final RagRetriever ragRetriever = new SimpleRagRetriever();
    private final AnswerOrchestrator answerOrchestrator = new SimpleAnswerOrchestrator();
    
    public AskWithSelectionAction() {
        super("Ask JEDAI about Selection");
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
        
        // 生成问题提示
        String userQuestion = Messages.showInputDialog(
            project,
            "Ask a question about the selected code:",
            "JEDAI - Ask Question",
            Messages.getQuestionIcon(),
            "What does this code do?",
            null
        );
        
        if (userQuestion == null) return;
        
        LOG.info("用户问题：" + userQuestion + "，选中代码：" + selectedText.substring(0, Math.min(50, selectedText.length())) + "...");
        
        // TODO: 成员B需要在这里实现：
        // 1. 调用RAG检索器获取相关知识块
        // 2. 调用答案编排器生成答案
        // 3. 在ToolWindow中显示答案（或者弹窗显示）
        List<RetrievedChunk> retrievedChunks = ragRetriever.search(userQuestion, 5);
        Answer answer = answerOrchestrator.generateAnswer(userQuestion, ideContext, retrievedChunks);
        
        // 临时显示答案（后续应该在ToolWindow中显示）
        showAnswer(project, answer);
    }
    
    private void showAnswer(Project project, Answer answer) {
        String citationsText;
        if (!answer.getCitations().isEmpty()) {
            citationsText = answer.getCitations().stream()
                .map(chunk -> "- " + chunk.getSourceDoc() + 
                    (chunk.getPage() != null ? " (Page " + chunk.getPage() + ")" : ""))
                .collect(Collectors.joining("\n"));
        } else {
            citationsText = "No citations";
        }
        
        String message = answer.getContent() + "\n\n" +
            (answer.isGeneralKnowledge() ? "⚠️ Based on general knowledge" : "📚 Based on course materials") + "\n\n" +
            "Citations:\n" + citationsText;
        
        Messages.showInfoMessage(project, message, "JEDAI Answer");
    }
}

