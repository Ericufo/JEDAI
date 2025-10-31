package com.github.ericufo.jedai.toolWindow;

import com.github.ericufo.jedai.chat.Answer;
import com.github.ericufo.jedai.chat.AnswerOrchestrator;
import com.github.ericufo.jedai.chat.IdeContext;
import com.github.ericufo.jedai.chat.impl.SimpleAnswerOrchestrator;
import com.github.ericufo.jedai.rag.RagRetriever;
import com.github.ericufo.jedai.rag.RetrievedChunk;
import com.github.ericufo.jedai.rag.impl.SimpleRagRetriever;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * JEDAI 聊天面板
 * TODO: 成员B需要完善UI，包括：
 * - 消息历史显示（用户消息和AI回答）
 * - 引用卡片的展示（显示来源文档和页码）
 * - 加载状态指示
 * - 错误处理
 */
public class JedaiChatPanel extends SimpleToolWindowPanel {
    
    private final Project project;
    
    // TODO: 成员B需要将这些改为通过Service获取
    private final RagRetriever ragRetriever = new SimpleRagRetriever();
    private final AnswerOrchestrator answerOrchestrator = new SimpleAnswerOrchestrator();
    
    private final JBTextArea messageArea;
    private final JBTextField inputField;
    
    public JedaiChatPanel(Project project) {
        super(false, true);
        this.project = project;
        
        // 消息显示区域
        messageArea = new JBTextArea();
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(messageArea.getFont().deriveFont(14f));
        
        // 输入框
        inputField = new JBTextField();
        inputField.setToolTipText("输入问题并按Enter发送");
        inputField.addActionListener(e -> sendMessage());
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    sendMessage();
                }
            }
        });
        
        initUI();
        
        // 初始提示
        appendMessage("系统", "欢迎使用 JEDAI Teaching Assistant！\n请在下方输入问题，或右键选中代码后选择 'Ask JEDAI about Selection'。");
    }
    
    private void initUI() {
        setLayout(new BorderLayout());
        
        // 顶部：标题和说明
        JPanel headerPanel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("JEDAI Teaching Assistant");
        titleLabel.setFont(titleLabel.getFont().deriveFont(16f).deriveFont(Font.BOLD));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        // 中间：消息显示区域
        JBScrollPane scrollPane = new JBScrollPane(messageArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        // 底部：输入区域
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(new JLabel("问题:"), BorderLayout.WEST);
        inputPanel.add(inputField, BorderLayout.CENTER);
        JButton sendButton = new JButton("发送");
        sendButton.addActionListener(e -> sendMessage());
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
    }
    
    private void sendMessage() {
        String question = inputField.getText().trim();
        if (question.isEmpty()) {
            return;
        }
        
        // 显示用户消息
        appendMessage("你", question);
        inputField.setText("");
        
        // 构建IDE上下文（可以从当前编辑器获取）
        IdeContext ideContext = new IdeContext(project.getName());
        
        // TODO: 成员B需要在这里实现：
        // 1. 显示加载状态
        // 2. 异步调用RAG检索和答案生成
        // 3. 显示答案和引用
        // 4. 错误处理
        
        try {
            List<RetrievedChunk> retrievedChunks = ragRetriever.search(question, 5);
            Answer answer = answerOrchestrator.generateAnswer(question, ideContext, retrievedChunks);
            
            // 显示答案
            appendMessage("JEDAI", answer.getContent());
            
            // 显示引用
            if (!answer.getCitations().isEmpty()) {
                StringBuilder citationsText = new StringBuilder();
                for (RetrievedChunk chunk : answer.getCitations()) {
                    citationsText.append("  📄 ").append(chunk.getSourceDoc());
                    if (chunk.getPage() != null) {
                        citationsText.append(" (Page ").append(chunk.getPage()).append(")");
                    }
                    citationsText.append("\n");
                }
                appendMessage("引用", citationsText.toString());
            } else if (answer.isGeneralKnowledge()) {
                appendMessage("系统", "⚠️ 此回答基于通用知识，未找到相关课程材料。");
            }
        } catch (Exception e) {
            appendMessage("错误", "生成答案时出错：" + e.getMessage());
        }
    }
    
    private void appendMessage(String sender, String content) {
        String message = "[" + sender + "] " + content + "\n\n";
        messageArea.append(message);
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
    }
}

