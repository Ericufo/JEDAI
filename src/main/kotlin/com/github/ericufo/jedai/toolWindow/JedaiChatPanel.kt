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
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import javax.swing.*
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants

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
 * JEDAI 聊天面板 - 完善版
 * 
 * - 使用 JTextPane 替代 JBTextArea，以支持文本样式
 * - 添加时间戳显示
 * - RagRetriever 和 AnswerOrchestrator 架构
 * - 保留异步处理逻辑
 * - 保留引用显示功能
 */
class JedaiChatPanel(private val project: Project) : SimpleToolWindowPanel(false, true) {
    
    // 使用同事的 RAG 和 Chat 模块
    private val ragRetriever: RagRetriever = SimpleRagRetriever()
    private val answerOrchestrator: AnswerOrchestrator = SimpleAnswerOrchestrator()
    
    // 使用 JTextPane 支持富文本样式
    private val chatDisplayArea: JTextPane
    private val inputField: JTextField
    private val sendButton: JButton

    init {
        layout = BorderLayout()
        
        // 创建聊天显示区域（使用JTextPane替代JBTextArea）
        chatDisplayArea = JTextPane().apply {
            isEditable = false
            background = Color.WHITE
            font = Font("Microsoft YaHei", Font.PLAIN, 14)
        }
        
        // 显示欢迎消息
        showWelcomeMessage()
        
        val scrollPane = JBScrollPane(chatDisplayArea).apply {
            preferredSize = Dimension(400, 300)
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        }

        // 创建输入面板
        val inputPanel = JPanel(BorderLayout()).apply {
            border = JBUI.Borders.empty(5)
            
            inputField = JTextField().apply {
                preferredSize = Dimension(0, 30)
                font = Font("Microsoft YaHei", Font.PLAIN, 14)
                toolTipText = "输入问题并按Enter发送"
            }
            
            sendButton = JButton("发送").apply {
                preferredSize = Dimension(80, 30)
                addActionListener { sendMessage() }
            }
            
            add(inputField, BorderLayout.CENTER)
            add(sendButton, BorderLayout.EAST)
        }
        
        // 添加回车键发送
        inputField.addActionListener { sendMessage() }

        // 组装界面
        add(scrollPane, BorderLayout.CENTER)
        add(inputPanel, BorderLayout.SOUTH)
        
        border = JBUI.Borders.empty(10)
    }

    /**
     * 显示欢迎消息
     */
    private fun showWelcomeMessage() {
        val doc = chatDisplayArea.styledDocument
        
        // 标题样式
        val titleStyle = SimpleAttributeSet().apply {
            StyleConstants.setFontSize(this, 18)
            StyleConstants.setBold(this, true)
            StyleConstants.setForeground(this, Color(25, 118, 210))
        }
        
        // 普通文本样式
        val normalStyle = SimpleAttributeSet().apply {
            StyleConstants.setFontSize(this, 14)
            StyleConstants.setForeground(this, Color.BLACK)
        }
        
        // 灰色文本样式
        val grayStyle = SimpleAttributeSet().apply {
            StyleConstants.setFontSize(this, 12)
            StyleConstants.setForeground(this, Color.GRAY)
        }
        
        // 功能项样式
        val featureStyle = SimpleAttributeSet().apply {
            StyleConstants.setFontSize(this, 13)
            StyleConstants.setForeground(this, Color.DARK_GRAY)
        }
        
        try {
            doc.insertString(doc.length, "JEDAI Teaching Assistant\n\n", titleStyle)
            doc.insertString(doc.length, "欢迎使用 Java Enterprise Application Development 课程助教插件！\n\n", grayStyle)
            doc.insertString(doc.length, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n", grayStyle)
            doc.insertString(doc.length, "功能介绍\n\n", normalStyle)
            doc.insertString(doc.length, "- 课程知识问答：基于课程slides的智能问答系统\n", featureStyle)
            doc.insertString(doc.length, "- 上下文感知：选中代码右键即可快速提问\n", featureStyle)
            doc.insertString(doc.length, "- AI代码助手：代码重构、Bug修复等智能建议\n", featureStyle)
            doc.insertString(doc.length, "- 来源追溯：回答会标注具体的课程资料来源\n\n", featureStyle)
            doc.insertString(doc.length, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n", grayStyle)
            doc.insertString(doc.length, "提示：您可以直接在下方输入框中提问，或右键选中代码后选择 'Ask JEDAI about Selection'。\n\n", grayStyle)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 发送消息
     */
    private fun sendMessage() {
        val question = inputField.text.trim()
        if (question.isEmpty()) {
            return
        }
        
        // 清空输入框
        inputField.text = ""
        
        // 显示用户消息
        appendMessage(question, isUser = true)
        
        // 禁用输入控件
        setInputEnabled(false)
        
        // 构建IDE上下文（可以从当前编辑器获取）
        val ideContext = IdeContext(
            projectName = project.name,
            filePath = null,
            selectedCode = null,
            language = null
        )
        
        // 异步处理消息（避免阻塞UI线程）
        SwingUtilities.invokeLater {
            try {
                // 调用同事实现的 RAG 检索
                val retrievedChunks = ragRetriever.search(question, k = 5)
                
                // 调用同事实现的答案编排器
                val answer = answerOrchestrator.generateAnswer(question, ideContext, retrievedChunks)
                
                // 显示答案
                appendMessage(answer.content, isUser = false)
                
                // 显示引用信息
                if (answer.citations.isNotEmpty()) {
                    val citationsText = buildString {
                        append("\n📚 参考来源：\n")
                        answer.citations.forEach { chunk ->
                            append("  • ${chunk.sourceDoc}")
                            chunk.page?.let { append(" (第 $it 页)") }
                            append("\n")
                        }
                    }
                    appendCitation(citationsText)
                } else if (answer.isGeneralKnowledge) {
                    appendSystemMessage("⚠️ 此回答基于通用知识，未找到相关课程材料。")
                }
            } catch (e: Exception) {
                appendSystemMessage("❌ 生成答案时出错：${e.message}")
                e.printStackTrace()
            } finally {
                setInputEnabled(true)
                inputField.requestFocus()
            }
        }
    }

    /**
     * 添加消息到聊天显示区
     */
    private fun appendMessage(message: String, isUser: Boolean) {
        val doc = chatDisplayArea.styledDocument
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())
        
        // 消息发送者样式
        val senderStyle = SimpleAttributeSet().apply {
            StyleConstants.setBold(this, true)
            StyleConstants.setForeground(this, if (isUser) Color(13, 71, 161) else Color(56, 142, 60))
        }
        
        // 消息内容样式
        val messageStyle = SimpleAttributeSet().apply {
            StyleConstants.setForeground(this, Color.BLACK)
        }
        
        // 时间戳样式
        val timestampStyle = SimpleAttributeSet().apply {
            StyleConstants.setFontSize(this, 10)
            StyleConstants.setForeground(this, Color.GRAY)
        }
        
        try {
            // 添加分隔线
            doc.insertString(doc.length, "\n", null)
            
            // 添加发送者和时间戳
            val sender = if (isUser) "User" else "Assistant"
            doc.insertString(doc.length, "$sender ", senderStyle)
            doc.insertString(doc.length, "$timestamp\n", timestampStyle)
            
            // 添加消息内容
            doc.insertString(doc.length, "$message\n", messageStyle)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // 滚动到底部
        SwingUtilities.invokeLater {
            chatDisplayArea.caretPosition = doc.length
        }
    }

    /**
     * 添加引用信息
     */
    private fun appendCitation(citationText: String) {
        val doc = chatDisplayArea.styledDocument
        
        val citationStyle = SimpleAttributeSet().apply {
            StyleConstants.setFontSize(this, 12)
            StyleConstants.setForeground(this, Color(100, 100, 100))
            StyleConstants.setItalic(this, true)
        }
        
        try {
            doc.insertString(doc.length, citationText, citationStyle)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 添加系统消息
     */
    private fun appendSystemMessage(message: String) {
        val doc = chatDisplayArea.styledDocument
        
        val systemStyle = SimpleAttributeSet().apply {
            StyleConstants.setFontSize(this, 12)
            StyleConstants.setForeground(this, Color(255, 152, 0))
        }
        
        try {
            doc.insertString(doc.length, "\n$message\n", systemStyle)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 设置输入控件启用状态
     */
    private fun setInputEnabled(enabled: Boolean) {
        inputField.isEnabled = enabled
        sendButton.isEnabled = enabled
    }
    
    /**
     * 公开方法：从Action调用，发送带有代码上下文的问题（未完成）
     */
    fun askQuestion(question: String, codeContext: String?) {
        inputField.text = question
        // TODO: 可以将 codeContext 传递给 IDE上下文
        sendMessage()
    }
}

