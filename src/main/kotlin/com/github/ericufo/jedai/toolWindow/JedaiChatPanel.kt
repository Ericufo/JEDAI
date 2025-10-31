package com.github.ericufo.jedai.toolWindow

import com.github.ericufo.jedai.chat.Answer
import com.github.ericufo.jedai.chat.AnswerOrchestrator
import com.github.ericufo.jedai.chat.IdeContext
import com.github.ericufo.jedai.chat.impl.SimpleAnswerOrchestrator
import com.github.ericufo.jedai.rag.RagRetriever
import com.github.ericufo.jedai.rag.impl.SimpleRagRetriever
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import java.awt.Font
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*

/**
 * JEDAI ToolWindow Factory
 * 创建聊天界面用于与TA交互
 */
class MyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val chatPanel = JedaiChatPanel(project)
        val content = ContentFactory.getInstance().createContent(chatPanel, null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true
}

/**
 * JEDAI 聊天面板
 * TODO: 成员B需要完善UI，包括：
 * - 消息历史显示（用户消息和AI回答）
 * - 引用卡片的展示（显示来源文档和页码）
 * - 加载状态指示
 * - 错误处理
 */
class JedaiChatPanel(private val project: Project) : SimpleToolWindowPanel(false, true) {
    
    // TODO: 成员B需要将这些改为通过Service获取
    private val ragRetriever: RagRetriever = SimpleRagRetriever()
    private val answerOrchestrator: AnswerOrchestrator = SimpleAnswerOrchestrator()
    
    private val messageArea = JBTextArea().apply {
        isEditable = false
        lineWrap = true
        wrapStyleWord = true
        font = font.deriveFont(14f)
    }
    
    private val inputField = JBTextField().apply {
        toolTipText = "输入问题并按Enter发送"
        addActionListener { sendMessage() }
        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER && !e.isShiftDown) {
                    sendMessage()
                }
            }
        })
    }
    
    init {
        layout = BorderLayout()
        
        // 顶部：标题和说明
        val headerPanel = JPanel(BorderLayout()).apply {
            add(JLabel("JEDAI Teaching Assistant").apply {
                font = font.deriveFont(16f).deriveFont(font.style or Font.BOLD)
            }, BorderLayout.WEST)
        }
        
        // 中间：消息显示区域
        val scrollPane = JBScrollPane(messageArea).apply {
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        }
        
        // 底部：输入区域
        val inputPanel = JPanel(BorderLayout()).apply {
            add(JLabel("问题:"), BorderLayout.WEST)
            add(inputField, BorderLayout.CENTER)
            add(JButton("发送").apply {
                addActionListener { sendMessage() }
            }, BorderLayout.EAST)
        }
        
        add(headerPanel, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)
        add(inputPanel, BorderLayout.SOUTH)
        
        // 初始提示
        appendMessage("系统", "欢迎使用 JEDAI Teaching Assistant！\n请在下方输入问题，或右键选中代码后选择 'Ask JEDAI about Selection'。")
    }
    
    private fun sendMessage() {
        val question = inputField.text.trim()
        if (question.isEmpty()) return
        
        // 显示用户消息
        appendMessage("你", question)
        inputField.text = ""
        
        // 构建IDE上下文（可以从当前编辑器获取）
        val ideContext = IdeContext(
            projectName = project.name,
            filePath = null,
            selectedCode = null,
            language = null
        )
        
        // TODO: 成员B需要在这里实现：
        // 1. 显示加载状态
        // 2. 异步调用RAG检索和答案生成
        // 3. 显示答案和引用
        // 4. 错误处理
        
        try {
            val retrievedChunks = ragRetriever.search(question, k = 5)
            val answer = answerOrchestrator.generateAnswer(question, ideContext, retrievedChunks)
            
            // 显示答案
            appendMessage("JEDAI", answer.content)
            
            // 显示引用
            if (answer.citations.isNotEmpty()) {
                val citationsText = answer.citations.joinToString("\n") { chunk ->
                    "  📄 ${chunk.sourceDoc}" + (chunk.page?.let { " (Page $it)" } ?: "")
                }
                appendMessage("引用", citationsText)
            } else if (answer.isGeneralKnowledge) {
                appendMessage("系统", "⚠️ 此回答基于通用知识，未找到相关课程材料。")
            }
        } catch (e: Exception) {
            appendMessage("错误", "生成答案时出错：${e.message}")
        }
    }
    
    private fun appendMessage(sender: String, content: String) {
        val message = "[$sender] $content\n\n"
        messageArea.append(message)
        messageArea.caretPosition = messageArea.document.length
    }
}

