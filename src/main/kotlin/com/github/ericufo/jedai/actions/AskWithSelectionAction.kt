package com.github.ericufo.jedai.actions

import com.github.ericufo.jedai.chat.IdeContext
import com.github.ericufo.jedai.chat.Answer
import com.github.ericufo.jedai.chat.AnswerOrchestrator
import com.github.ericufo.jedai.chat.impl.SimpleAnswerOrchestrator
import com.github.ericufo.jedai.rag.RagRetriever
import com.github.ericufo.jedai.rag.impl.SimpleRagRetriever
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

/**
 * 右键菜单Action：询问选中代码相关的问题
 */
class AskWithSelectionAction : AnAction("Ask JEDAI about Selection") {
    private val logger = thisLogger()
    
    // TODO: 成员B需要将这些改为通过Service获取，而不是直接实例化
    private val ragRetriever: RagRetriever = SimpleRagRetriever()
    private val answerOrchestrator: AnswerOrchestrator = SimpleAnswerOrchestrator()
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val selectionModel = editor.selectionModel
        
        // 获取选中的代码
        val selectedText = selectionModel.selectedText
        val file = e.getData(CommonDataKeys.PSI_FILE)
        val filePath = file?.virtualFile?.path
        val language = file?.language?.displayName
        
        if (selectedText.isNullOrBlank()) {
            Messages.showInfoMessage(
                project,
                "Please select some code first.",
                "JEDAI"
            )
            return
        }
        
        // 构建IDE上下文
        val ideContext = IdeContext(
            projectName = project.name,
            filePath = filePath,
            selectedCode = selectedText,
            language = language,
            lineNumber = selectionModel.selectionStartPosition?.line
        )
        
        // 生成问题提示
        val userQuestion = Messages.showInputDialog(
            project,
            "Ask a question about the selected code:",
            "JEDAI - Ask Question",
            Messages.getQuestionIcon(),
            "What does this code do?",
            null
        ) ?: return
        
        logger.info("用户问题：$userQuestion，选中代码：${selectedText.take(50)}...")
        
        // TODO: 成员B需要在这里实现：
        // 1. 调用RAG检索器获取相关知识块
        // 2. 调用答案编排器生成答案
        // 3. 在ToolWindow中显示答案（或者弹窗显示）
        val retrievedChunks = ragRetriever.search(userQuestion, k = 5)
        val answer = answerOrchestrator.generateAnswer(userQuestion, ideContext, retrievedChunks)
        
        // 临时显示答案（后续应该在ToolWindow中显示）
        showAnswer(project, answer)
    }
    
    private fun showAnswer(project: Project, answer: Answer) {
        val citationsText = if (answer.citations.isNotEmpty()) {
            answer.citations.joinToString("\n") { chunk ->
                "- ${chunk.sourceDoc}" + (chunk.page?.let { " (Page $it)" } ?: "")
            }
        } else {
            "No citations"
        }
        
        val message = """
            ${answer.content}
            
            ${if (answer.isGeneralKnowledge) "⚠️ Based on general knowledge" else "📚 Based on course materials"}
            
            Citations:
            $citationsText
        """.trimIndent()
        
        Messages.showInfoMessage(project, message, "JEDAI Answer")
    }
}

