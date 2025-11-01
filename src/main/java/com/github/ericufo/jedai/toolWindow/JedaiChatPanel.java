package com.github.ericufo.jedai.toolWindow;

import com.github.ericufo.jedai.chat.Answer;
import com.github.ericufo.jedai.chat.AnswerOrchestrator;
import com.github.ericufo.jedai.chat.IdeContext;
import com.github.ericufo.jedai.chat.impl.SimpleAnswerOrchestrator;
import com.github.ericufo.jedai.rag.RagRetriever;
import com.github.ericufo.jedai.rag.RetrievedChunk;
import com.github.ericufo.jedai.rag.impl.SimpleRagRetriever;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 聊天面板
 */
public class JedaiChatPanel extends SimpleToolWindowPanel {

    // 字体大小配置常量
    /** 基础字体大小 - 聊天区域和输入框的默认字体 */
    private static final int BASE_FONT_SIZE = 16;
    /** 大标题字体大小 - 欢迎消息的主标题 */
    private static final int TITLE_FONT_SIZE = 20;
    /** 普通标题字体大小 - 欢迎消息的副标题和段落标题 */
    private static final int NORMAL_FONT_SIZE = 16;
    /** H2标题字体大小 - Markdown的二级标题 */
    private static final int H2_FONT_SIZE = 18;
    /** H3标题字体大小 - Markdown的三级标题 */
    private static final int H3_FONT_SIZE = 17;
    /** 代码字体大小 - 代码块和行内代码 */
    private static final int CODE_FONT_SIZE = 15;
    /** 小字体大小 - 提示文本和功能列表 */
    private static final int SMALL_FONT_SIZE = 14;
    /** 极小字体大小 - 时间戳 */
    private static final int TINY_FONT_SIZE = 12;

    // 字体大小预设方案：
    // 小 BASE=13, TITLE=16, CODE=12, TINY=10
    // 标准BASE=14, TITLE=18, CODE=13, TINY=10 (当前默认)
    // 大 BASE=16, TITLE=22, CODE=15, TINY=11
    // 最大BASE=18, TITLE=24, CODE=16, TINY=12
    // =============================================================

    private final Project project;

    // RAG 和 Chat 模块
    private final RagRetriever ragRetriever = new SimpleRagRetriever();
    private final AnswerOrchestrator answerOrchestrator = new SimpleAnswerOrchestrator();

    // 使用 JTextPane 支持富文本样式
    private final JTextPane chatDisplayArea;
    private final JTextField inputField;
    private final JButton sendButton;
    private final JButton clearButton;

    // 加载动画相关
    private javax.swing.Timer thinkingTimer;
    private int thinkingDots = 0;

    public JedaiChatPanel(Project project) {
        super(false, true);
        this.project = project;

        setLayout(new BorderLayout());

        // 创建聊天显示区域（使用JTextPane替代JTextArea）
        chatDisplayArea = new JTextPane();
        chatDisplayArea.setEditable(false);
        chatDisplayArea.setBackground(Color.WHITE);
        chatDisplayArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, BASE_FONT_SIZE));

        // 显示欢迎消息
        showWelcomeMessage();

        JBScrollPane scrollPane = new JBScrollPane(chatDisplayArea);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // 创建输入面板
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(JBUI.Borders.empty(5));

        inputField = new JTextField();
        inputField.setPreferredSize(new Dimension(0, 30));
        inputField.setFont(new Font("Microsoft YaHei", Font.PLAIN, BASE_FONT_SIZE));
        inputField.setToolTipText("输入问题并按Enter发送");

        // 创建按钮面板（包含发送和清空按钮）
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));

        clearButton = new JButton("清空对话");
        clearButton.setPreferredSize(new Dimension(90, 30));
        clearButton.setToolTipText("清空对话历史，开始新的对话");
        clearButton.addActionListener(e -> clearConversation());

        sendButton = new JButton("发送");
        sendButton.setPreferredSize(new Dimension(80, 30));
        sendButton.addActionListener(e -> sendMessage());

        buttonPanel.add(clearButton);
        buttonPanel.add(sendButton);

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);

        // 添加回车键发送
        inputField.addActionListener(e -> sendMessage());

        // 组装界面
        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        setBorder(JBUI.Borders.empty(10));
    }

    /**
     * 显示欢迎消息
     */
    private void showWelcomeMessage() {
        StyledDocument doc = chatDisplayArea.getStyledDocument();

        // 标题样式
        SimpleAttributeSet titleStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(titleStyle, TITLE_FONT_SIZE);
        StyleConstants.setBold(titleStyle, true);
        StyleConstants.setForeground(titleStyle, new Color(25, 118, 210));

        // 普通文本样式
        SimpleAttributeSet normalStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(normalStyle, NORMAL_FONT_SIZE);
        StyleConstants.setForeground(normalStyle, Color.BLACK);

        // 灰色文本样式
        SimpleAttributeSet grayStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(grayStyle, SMALL_FONT_SIZE);
        StyleConstants.setForeground(grayStyle, Color.GRAY);

        // 功能项样式
        SimpleAttributeSet featureStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(featureStyle, CODE_FONT_SIZE);
        StyleConstants.setForeground(featureStyle, Color.DARK_GRAY);

        try {
            doc.insertString(doc.getLength(), "JEDAI Teaching Assistant\n\n", titleStyle);
            doc.insertString(doc.getLength(), "欢迎使用 Java Enterprise Application Development 课程助教插件！\n\n", grayStyle);
            doc.insertString(doc.getLength(), "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n", grayStyle);
            doc.insertString(doc.getLength(), "功能介绍\n\n", normalStyle);
            doc.insertString(doc.getLength(), "- 课程知识问答：基于课程slides的智能问答系统\n", featureStyle);
            doc.insertString(doc.getLength(), "- 上下文感知：选中代码右键即可快速提问\n", featureStyle);
            doc.insertString(doc.getLength(), "- AI代码助手：代码重构、Bug修复等智能建议\n", featureStyle);
            doc.insertString(doc.getLength(), "- 来源追溯：回答会标注具体的课程资料来源\n\n", featureStyle);
            doc.insertString(doc.getLength(), "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n", grayStyle);
            doc.insertString(doc.getLength(), "提示：您可以直接在下方输入框中提问，或右键选中代码后选择 'Ask JEDAI about Selection'。\n\n",
                    grayStyle);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送消息
     */
    private void sendMessage() {
        String question = inputField.getText().trim();
        if (question.isEmpty()) {
            return;
        }

        // 清空输入框
        inputField.setText("");

        // 显示用户消息
        appendMessage(question, true);

        // 禁用输入控件
        setInputEnabled(false);

        // 显示"正在思考"动画
        showThinkingAnimation();

        // 构建IDE上下文（可以从当前编辑器获取）
        IdeContext ideContext = new IdeContext(project.getName());

        // 真正的异步处理：在后台线程执行LLM调用
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                // 调用RAG检索（在后台线程）
                List<RetrievedChunk> retrievedChunks = ragRetriever.search(question, 5);

                // 调用LLM生成答案（在后台线程）
                Answer answer = answerOrchestrator.generateAnswer(question, ideContext, retrievedChunks);

                // 切换回UI线程更新界面
                SwingUtilities.invokeLater(() -> {
                    // 停止思考动画
                    stopThinkingAnimation();

                    // 显示答案
                    appendMessage(answer.getContent(), false);

                    // 显示引用信息
                    if (!answer.getCitations().isEmpty()) {
                        StringBuilder citationsText = new StringBuilder();
                        citationsText.append("\n[参考来源]\n");
                        for (RetrievedChunk chunk : answer.getCitations()) {
                            citationsText.append("  - ").append(chunk.getSourceDoc());
                            if (chunk.getPage() != null) {
                                citationsText.append(" (第 ").append(chunk.getPage()).append(" 页)");
                            }
                            citationsText.append("\n");
                        }
                        appendCitation(citationsText.toString());
                    } else if (answer.isGeneralKnowledge()) {
                        appendSystemMessage("[提示] 此回答基于通用知识，未找到相关课程材料。");
                    }

                    // 重新启用输入控件
                    setInputEnabled(true);
                    inputField.requestFocus();
                });

            } catch (Exception e) {
                // 切换回UI线程显示错误
                SwingUtilities.invokeLater(() -> {
                    stopThinkingAnimation();
                    appendSystemMessage("[错误] 生成答案时出错：" + e.getMessage());
                    setInputEnabled(true);
                    inputField.requestFocus();
                });
                e.printStackTrace();
            }
        });
    }

    /**
     * 显示"正在思考"动画
     */
    private void showThinkingAnimation() {
        thinkingDots = 0;

        // 添加初始的"正在思考"消息
        StyledDocument doc = chatDisplayArea.getStyledDocument();
        SimpleAttributeSet thinkingStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(thinkingStyle, new Color(150, 150, 150));
        StyleConstants.setItalic(thinkingStyle, true);

        try {
            doc.insertString(doc.getLength(), "\n[正在思考", thinkingStyle);
            int thinkingStartPos = doc.getLength() - "[正在思考".length();

            // 创建定时器，每500ms更新一次点数
            thinkingTimer = new javax.swing.Timer(500, e -> {
                try {
                    // 更新点数
                    thinkingDots = (thinkingDots % 3) + 1;
                    String dots = ".".repeat(thinkingDots);

                    // 删除旧的动画文本
                    int currentLength = doc.getLength();
                    int messageStart = currentLength;

                    // 查找"[正在思考"的位置
                    String text = doc.getText(0, doc.getLength());
                    int startIdx = text.lastIndexOf("[正在思考");
                    if (startIdx != -1) {
                        int endIdx = text.indexOf("]", startIdx);
                        if (endIdx == -1)
                            endIdx = doc.getLength();
                        else
                            endIdx++; // 包含 ]

                        // 替换整个思考消息
                        doc.remove(startIdx, endIdx - startIdx);
                        doc.insertString(startIdx, "[正在思考" + dots + "]", thinkingStyle);
                    }

                    // 滚动到底部
                    chatDisplayArea.setCaretPosition(doc.getLength());
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            });
            thinkingTimer.start();

            doc.insertString(doc.getLength(), "]\n", thinkingStyle);
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 停止"正在思考"动画并移除提示
     */
    private void stopThinkingAnimation() {
        if (thinkingTimer != null && thinkingTimer.isRunning()) {
            thinkingTimer.stop();
        }

        // 移除"正在思考"文本
        try {
            StyledDocument doc = chatDisplayArea.getStyledDocument();
            String text = doc.getText(0, doc.getLength());
            int startIdx = text.lastIndexOf("[正在思考");

            if (startIdx != -1) {
                int endIdx = text.indexOf("\n", startIdx);
                if (endIdx != -1) {
                    doc.remove(startIdx, endIdx - startIdx + 1);
                }
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * 添加消息到聊天显示区（支持简单的Markdown解析）
     */
    private void appendMessage(String message, boolean isUser) {
        StyledDocument doc = chatDisplayArea.getStyledDocument();
        String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());

        // 消息发送者样式
        SimpleAttributeSet senderStyle = new SimpleAttributeSet();
        StyleConstants.setBold(senderStyle, true);
        StyleConstants.setForeground(senderStyle, isUser ? new Color(13, 71, 161) : new Color(56, 142, 60));

        // 时间戳样式
        SimpleAttributeSet timestampStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(timestampStyle, TINY_FONT_SIZE);
        StyleConstants.setForeground(timestampStyle, Color.GRAY);

        try {
            // 添加分隔线
            doc.insertString(doc.getLength(), "\n", null);

            // 添加发送者和时间戳
            String sender = isUser ? "User" : "Assistant";
            doc.insertString(doc.getLength(), sender + " ", senderStyle);
            doc.insertString(doc.getLength(), timestamp + "\n", timestampStyle);

            // 解析并添加消息内容（支持简单的Markdown）
            appendMarkdownText(doc, message);

        } catch (BadLocationException e) {
            e.printStackTrace();
        }

        // 滚动到底部
        SwingUtilities.invokeLater(() -> chatDisplayArea.setCaretPosition(doc.getLength()));
    }

    /**
     * 简易的Markdown解析器
     * 支持：代码块、加粗、标题、列表
     */
    private void appendMarkdownText(StyledDocument doc, String text) throws BadLocationException {
        // 按行处理
        String[] lines = text.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // 标题（## 或 ###）
            if (line.startsWith("## ")) {
                SimpleAttributeSet headingStyle = new SimpleAttributeSet();
                StyleConstants.setBold(headingStyle, true);
                StyleConstants.setFontSize(headingStyle, H2_FONT_SIZE);
                StyleConstants.setForeground(headingStyle, new Color(0, 0, 0));
                doc.insertString(doc.getLength(), line.substring(3) + "\n", headingStyle);
                continue;
            } else if (line.startsWith("### ")) {
                SimpleAttributeSet headingStyle = new SimpleAttributeSet();
                StyleConstants.setBold(headingStyle, true);
                StyleConstants.setFontSize(headingStyle, H3_FONT_SIZE);
                StyleConstants.setForeground(headingStyle, new Color(50, 50, 50));
                doc.insertString(doc.getLength(), line.substring(4) + "\n", headingStyle);
                continue;
            }

            // 列表项（- 开头）
            if (line.trim().startsWith("- ")) {
                SimpleAttributeSet listStyle = new SimpleAttributeSet();
                StyleConstants.setForeground(listStyle, Color.BLACK);
                doc.insertString(doc.getLength(), "  • " + line.trim().substring(2) + "\n", listStyle);
                continue;
            }

            // 代码块（``` 开头或结尾）
            if (line.trim().startsWith("```")) {
                // 代码块标记，跳过
                continue;
            }

            // 检查是否在代码块内（简单判断：如果上一行是```）
            boolean inCodeBlock = false;
            if (i > 0 && lines[i - 1].trim().startsWith("```")) {
                inCodeBlock = true;
                // 查找代码块结束
                StringBuilder codeBlock = new StringBuilder();
                int j = i;
                while (j < lines.length && !lines[j].trim().startsWith("```")) {
                    codeBlock.append(lines[j]).append("\n");
                    j++;
                }

                SimpleAttributeSet codeStyle = new SimpleAttributeSet();
                StyleConstants.setFontFamily(codeStyle, "Consolas");
                StyleConstants.setFontSize(codeStyle, CODE_FONT_SIZE);
                StyleConstants.setBackground(codeStyle, new Color(245, 245, 245));
                StyleConstants.setForeground(codeStyle, new Color(60, 60, 60));

                doc.insertString(doc.getLength(), codeBlock.toString(), codeStyle);

                // 跳过已处理的行
                i = j;
                continue;
            }

            // 普通行：处理行内格式（加粗、代码）
            appendLineWithInlineFormatting(doc, line + "\n");
        }
    }

    /**
     * 处理行内格式（加粗 **text**、行内代码 `code`）
     */
    private void appendLineWithInlineFormatting(StyledDocument doc, String line) throws BadLocationException {
        // 普通文本样式
        SimpleAttributeSet normalStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(normalStyle, Color.BLACK);

        // 加粗样式
        SimpleAttributeSet boldStyle = new SimpleAttributeSet();
        StyleConstants.setBold(boldStyle, true);
        StyleConstants.setForeground(boldStyle, Color.BLACK);

        // 行内代码样式
        SimpleAttributeSet inlineCodeStyle = new SimpleAttributeSet();
        StyleConstants.setFontFamily(inlineCodeStyle, "Consolas");
        StyleConstants.setBackground(inlineCodeStyle, new Color(240, 240, 240));
        StyleConstants.setForeground(inlineCodeStyle, new Color(200, 0, 0));

        int pos = 0;
        while (pos < line.length()) {
            // 检查加粗 **text**
            if (line.startsWith("**", pos)) {
                int endPos = line.indexOf("**", pos + 2);
                if (endPos != -1) {
                    // 找到匹配的结束标记
                    String boldText = line.substring(pos + 2, endPos);
                    doc.insertString(doc.getLength(), boldText, boldStyle);
                    pos = endPos + 2;
                    continue;
                }
            }

            // 检查行内代码 `code`
            if (line.charAt(pos) == '`') {
                int endPos = line.indexOf('`', pos + 1);
                if (endPos != -1) {
                    // 找到匹配的结束标记
                    String codeText = line.substring(pos + 1, endPos);
                    doc.insertString(doc.getLength(), codeText, inlineCodeStyle);
                    pos = endPos + 1;
                    continue;
                }
            }

            // 普通字符
            doc.insertString(doc.getLength(), String.valueOf(line.charAt(pos)), normalStyle);
            pos++;
        }
    }

    /**
     * 添加引用信息
     */
    private void appendCitation(String citationText) {
        StyledDocument doc = chatDisplayArea.getStyledDocument();

        SimpleAttributeSet citationStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(citationStyle, SMALL_FONT_SIZE);
        StyleConstants.setForeground(citationStyle, new Color(100, 100, 100));
        StyleConstants.setItalic(citationStyle, true);

        try {
            doc.insertString(doc.getLength(), citationText, citationStyle);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * 添加系统消息
     */
    private void appendSystemMessage(String message) {
        StyledDocument doc = chatDisplayArea.getStyledDocument();

        SimpleAttributeSet systemStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(systemStyle, SMALL_FONT_SIZE);
        StyleConstants.setForeground(systemStyle, new Color(255, 152, 0));

        try {
            doc.insertString(doc.getLength(), "\n" + message + "\n", systemStyle);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置输入控件启用状态
     */
    private void setInputEnabled(boolean enabled) {
        inputField.setEnabled(enabled);
        sendButton.setEnabled(enabled);
        clearButton.setEnabled(enabled);
    }

    /**
     * 清空对话
     * 清空对话历史和显示区域，开始新的对话
     */
    private void clearConversation() {
        // 确认对话框（避免误操作）
        int result = JOptionPane.showConfirmDialog(
                this,
                "确定要清空对话历史吗？此操作无法撤销。",
                "清空对话",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            // 清空LLM对话记忆
            if (answerOrchestrator instanceof SimpleAnswerOrchestrator) {
                ((SimpleAnswerOrchestrator) answerOrchestrator).clearChatHistory();
            }

            // 清空聊天显示区域
            chatDisplayArea.setText("");

            // 重新显示欢迎消息
            showWelcomeMessage();

            // 显示系统提示
            appendSystemMessage("[提示] 对话历史已清空，已开始新的对话。");

            // 清空输入框
            inputField.setText("");
            inputField.requestFocus();
        }
    }

    /**
     * 公开方法：从Action调用，发送带有代码上下文的问题
     * 
     * @param question   用户问题
     * @param ideContext IDE上下文（包含选中的代码等信息）
     */
    public void askQuestionWithContext(String question, IdeContext ideContext) {
        // 显示用户问题
        appendMessage(question, true);

        // 如果有选中的代码，显示代码上下文
        if (ideContext != null && ideContext.getSelectedCode() != null
                && !ideContext.getSelectedCode().trim().isEmpty()) {
            appendCodeContext(ideContext);
        }

        // 禁用输入控件
        setInputEnabled(false);

        // 显示"正在思考"动画
        showThinkingAnimation();

        // 真正的异步处理：在后台线程执行LLM调用
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                // 调用RAG检索（在后台线程）
                List<RetrievedChunk> retrievedChunks = ragRetriever.search(question, 5);

                // 调用LLM生成答案（在后台线程）
                Answer answer = answerOrchestrator.generateAnswer(question, ideContext, retrievedChunks);

                // 切换回UI线程更新界面
                SwingUtilities.invokeLater(() -> {
                    // 停止思考动画
                    stopThinkingAnimation();

                    // 显示答案
                    appendMessage(answer.getContent(), false);

                    // 显示引用信息
                    if (!answer.getCitations().isEmpty()) {
                        StringBuilder citationsText = new StringBuilder();
                        citationsText.append("\n[参考来源]\n");
                        for (RetrievedChunk chunk : answer.getCitations()) {
                            citationsText.append("  - ").append(chunk.getSourceDoc());
                            if (chunk.getPage() != null) {
                                citationsText.append(" (第 ").append(chunk.getPage()).append(" 页)");
                            }
                            citationsText.append("\n");
                        }
                        appendCitation(citationsText.toString());
                    } else if (answer.isGeneralKnowledge()) {
                        appendSystemMessage("[提示] 此回答基于通用知识，未找到相关课程材料。");
                    }

                    // 重新启用输入控件
                    setInputEnabled(true);
                    inputField.requestFocus();
                });

            } catch (Exception e) {
                // 切换回UI线程显示错误
                SwingUtilities.invokeLater(() -> {
                    stopThinkingAnimation();
                    appendSystemMessage("[错误] 生成答案时出错：" + e.getMessage());
                    setInputEnabled(true);
                    inputField.requestFocus();
                });
                e.printStackTrace();
            }
        });
    }

    /**
     * 显示代码上下文信息
     */
    private void appendCodeContext(IdeContext ideContext) {
        StyledDocument doc = chatDisplayArea.getStyledDocument();

        SimpleAttributeSet contextStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(contextStyle, TINY_FONT_SIZE + 1); // 比时间戳稍大一点
        StyleConstants.setForeground(contextStyle, new Color(100, 100, 150));
        StyleConstants.setItalic(contextStyle, true);

        SimpleAttributeSet codeStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(codeStyle, SMALL_FONT_SIZE);
        StyleConstants.setForeground(codeStyle, new Color(60, 60, 60));
        StyleConstants.setFontFamily(codeStyle, "Consolas");

        try {
            doc.insertString(doc.getLength(), "\n[选中的代码]\n", contextStyle);

            if (ideContext.getFilePath() != null) {
                doc.insertString(doc.getLength(), "文件: " + ideContext.getFilePath() + "\n", contextStyle);
            }
            if (ideContext.getLanguage() != null) {
                doc.insertString(doc.getLength(), "语言: " + ideContext.getLanguage() + "\n", contextStyle);
            }

            // 显示代码片段（限制长度）
            String code = ideContext.getSelectedCode();
            if (code.length() > 500) {
                code = code.substring(0, 500) + "\n... (代码过长，已截断)";
            }
            doc.insertString(doc.getLength(), code + "\n\n", codeStyle);

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}
