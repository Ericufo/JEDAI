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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 聊天面板
 * 支持主题切换（深色/浅色）和字体大小调整
 */
public class JedaiChatPanel extends SimpleToolWindowPanel {

    private final Project project;

    // RAG 和 Chat 模块
    private final RagRetriever ragRetriever = new SimpleRagRetriever();
    private final AnswerOrchestrator answerOrchestrator = new SimpleAnswerOrchestrator();

    // 主题和字体配置
    private final ThemeConfig themeConfig = new ThemeConfig();
    private final FontSizePreset fontSizePreset = new FontSizePreset();

    // 使用 JTextPane 支持富文本样式
    private final JTextPane chatDisplayArea;
    private final JTextField inputField;
    private final ModernButton sendButton;
    private final ModernButton clearButton;

    // 需要应用主题的面板
    private JPanel toolbarPanel;
    private JPanel inputPanel;

    // 消息历史（用于主题/字体切换时重新渲染）
    private final List<ChatMessage> messageHistory = new ArrayList<>();

    // 流式答案相关
    private final StringBuilder streamingAnswerBuffer = new StringBuilder();
    private javax.swing.Timer streamingUpdateTimer;

    /**
     * 聊天消息内部类
     */
    private static class ChatMessage {
        enum Type {
            USER, // 用户消息
            ASSISTANT, // 助手消息
            SYSTEM, // 系统消息
            CITATION, // 引用信息
            CODE_CONTEXT // 代码上下文
        }

        Type type;
        String content;
        long timestamp;
        List<RetrievedChunk> citations; // 仅用于助手消息
        IdeContext ideContext; // 仅用于代码上下文

        ChatMessage(Type type, String content, long timestamp) {
            this.type = type;
            this.content = content;
            this.timestamp = timestamp;
        }

        ChatMessage(Type type, String content, long timestamp, List<RetrievedChunk> citations) {
            this(type, content, timestamp);
            this.citations = citations;
        }

        ChatMessage(Type type, String content, IdeContext ideContext) {
            this.type = type;
            this.content = content;
            this.ideContext = ideContext;
            this.timestamp = System.currentTimeMillis();
        }
    }

    public JedaiChatPanel(Project project) {
        super(false, true);
        this.project = project;

        setLayout(new BorderLayout());

        // 创建顶部工具栏（主题和字体选择器）
        toolbarPanel = createToolbarPanel();

        // 创建聊天显示区域（使用JTextPane替代JTextArea）
        chatDisplayArea = new JTextPane();
        chatDisplayArea.setEditable(false);
        chatDisplayArea.setBackground(themeConfig.getBackgroundColor());
        chatDisplayArea.setFont(new Font("Microsoft YaHei", Font.PLAIN, fontSizePreset.getBaseSize()));

        // 显示欢迎消息
        showWelcomeMessage();

        JBScrollPane scrollPane = new JBScrollPane(chatDisplayArea);
        scrollPane.setPreferredSize(new Dimension(400, 300));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // 创建输入面板
        inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(JBUI.Borders.empty(8));
        inputPanel.setBackground(themeConfig.getBackgroundColor());

        inputField = new JTextField();
        inputField.setPreferredSize(new Dimension(0, 35));
        inputField.setFont(new Font("Microsoft YaHei", Font.PLAIN, fontSizePreset.getBaseSize()));
        inputField.setToolTipText("输入问题并按Enter发送");
        inputField.setBackground(themeConfig.getInputBackgroundColor());
        inputField.setForeground(themeConfig.getTextColor());
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(themeConfig.getSeparatorColor(), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));

        // 创建按钮面板（包含发送和清空按钮）
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setBackground(themeConfig.getBackgroundColor());

        clearButton = new ModernButton("清空对话");
        clearButton.setPreferredSize(new Dimension(90, 35));
        clearButton.setToolTipText("清空对话历史，开始新的对话");
        clearButton.addActionListener(e -> clearConversation());
        applyButtonTheme(clearButton);

        sendButton = new ModernButton("发送");
        sendButton.setPreferredSize(new Dimension(80, 35));
        sendButton.addActionListener(e -> sendMessage());
        applyButtonTheme(sendButton);

        buttonPanel.add(clearButton);
        buttonPanel.add(sendButton);

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);

        // 添加回车键发送
        inputField.addActionListener(e -> sendMessage());

        // 组装界面
        add(toolbarPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);

        setBorder(JBUI.Borders.empty(0));
    }

    /**
     * 创建顶部工具栏
     */
    private JPanel createToolbarPanel() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        toolbar.setBackground(themeConfig.getBackgroundColor());
        toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, themeConfig.getSeparatorColor()));

        // 主题选择器
        JLabel themeLabel = new JLabel("主题:");
        themeLabel.setForeground(themeConfig.getTextColor());
        themeLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));

        JComboBox<ThemeConfig.Theme> themeBox = new JComboBox<>(ThemeConfig.Theme.values());
        themeBox.setSelectedItem(themeConfig.getCurrentTheme());
        themeBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ThemeConfig.Theme) {
                    setText(((ThemeConfig.Theme) value).getDisplayName());
                }
                return this;
            }
        });
        themeBox.addActionListener(e -> {
            ThemeConfig.Theme selected = (ThemeConfig.Theme) themeBox.getSelectedItem();
            if (selected != null) {
                themeConfig.setTheme(selected);
                applyTheme();
            }
        });

        // 字体大小选择器
        JLabel fontLabel = new JLabel("字体:");
        fontLabel.setForeground(themeConfig.getTextColor());
        fontLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));

        JComboBox<FontSizePreset.Preset> fontBox = new JComboBox<>(FontSizePreset.Preset.values());
        fontBox.setSelectedItem(fontSizePreset.getCurrentPreset());
        fontBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof FontSizePreset.Preset) {
                    setText(((FontSizePreset.Preset) value).getDisplayName());
                }
                return this;
            }
        });
        fontBox.addActionListener(e -> {
            FontSizePreset.Preset selected = (FontSizePreset.Preset) fontBox.getSelectedItem();
            if (selected != null) {
                fontSizePreset.setPreset(selected);
                applyFontSize();
            }
        });

        toolbar.add(themeLabel);
        toolbar.add(themeBox);
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(fontLabel);
        toolbar.add(fontBox);

        return toolbar;
    }

    /**
     * 应用按钮主题
     */
    private void applyButtonTheme(ModernButton button) {
        button.setColors(
                themeConfig.getButtonBackgroundColor(),
                themeConfig.getButtonHoverColor(),
                themeConfig.getTextColor());
    }

    /**
     * 切换主题
     */
    private void switchTheme() {
        applyTheme();
    }

    /**
     * 应用主题到所有组件
     */
    private void applyTheme() {
        // 应用到聊天显示区域
        chatDisplayArea.setBackground(themeConfig.getBackgroundColor());

        // 应用到顶部工具栏
        if (toolbarPanel != null) {
            toolbarPanel.setBackground(themeConfig.getBackgroundColor());
            toolbarPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, themeConfig.getSeparatorColor()));
            // 更新工具栏内的Label颜色
            for (Component comp : toolbarPanel.getComponents()) {
                if (comp instanceof JLabel) {
                    ((JLabel) comp).setForeground(themeConfig.getTextColor());
                }
            }
        }

        // 应用到底部输入面板
        if (inputPanel != null) {
            inputPanel.setBackground(themeConfig.getBackgroundColor());
            // 更新按钮面板背景色
            for (Component comp : inputPanel.getComponents()) {
                if (comp instanceof JPanel) {
                    ((JPanel) comp).setBackground(themeConfig.getBackgroundColor());
                }
            }
        }

        // 应用到输入框
        inputField.setBackground(themeConfig.getInputBackgroundColor());
        inputField.setForeground(themeConfig.getTextColor());
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(themeConfig.getSeparatorColor(), 1),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)));

        // 应用到按钮
        applyButtonTheme(sendButton);
        applyButtonTheme(clearButton);

        // 重新渲染所有消息以应用新主题
        reRenderAllMessages();
    }

    /**
     * 改变字体大小
     */
    private void changeFontSize() {
        applyFontSize();
    }

    /**
     * 应用字体大小到所有组件
     */
    private void applyFontSize() {
        // 应用到输入框
        inputField.setFont(new Font("Microsoft YaHei", Font.PLAIN, fontSizePreset.getBaseSize()));

        // 重新渲染所有消息以应用新字体大小
        reRenderAllMessages();
    }

    /**
     * 重新渲染所有消息（主题/字体切换时调用）
     */
    private void reRenderAllMessages() {
        // 清空显示区域
        chatDisplayArea.setText("");

        // 重新显示欢迎消息
        showWelcomeMessage();

        // 重新渲染所有历史消息
        for (ChatMessage msg : messageHistory) {
            switch (msg.type) {
                case USER:
                case ASSISTANT:
                    appendMessageDirect(msg.content, msg.type == ChatMessage.Type.USER, msg.timestamp);
                    break;
                case CITATION:
                    appendCitationDirect(msg.content);
                    break;
                case SYSTEM:
                    appendSystemMessageDirect(msg.content);
                    break;
                case CODE_CONTEXT:
                    appendCodeContextDirect(msg.ideContext);
                    break;
            }
        }
    }

    /**
     * 刷新聊天显示（清空并重新显示欢迎消息）
     * 注意：此方法会清空所有对话历史和消息历史
     */
    private void refreshChatDisplay() {
        // 清空消息历史
        messageHistory.clear();

        // 清空并重新显示欢迎消息
        chatDisplayArea.setText("");
        showWelcomeMessage();
    }

    /**
     * 显示欢迎消息
     */
    private void showWelcomeMessage() {
        StyledDocument doc = chatDisplayArea.getStyledDocument();

        // 标题样式
        SimpleAttributeSet titleStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(titleStyle, fontSizePreset.getTitleSize());
        StyleConstants.setBold(titleStyle, true);
        StyleConstants.setForeground(titleStyle, themeConfig.getTitleColor());

        // 普通文本样式
        SimpleAttributeSet normalStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(normalStyle, fontSizePreset.getNormalSize());
        StyleConstants.setForeground(normalStyle, themeConfig.getTextColor());

        // 副标题样式
        SimpleAttributeSet subtitleStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(subtitleStyle, fontSizePreset.getSmallSize());
        StyleConstants.setForeground(subtitleStyle, themeConfig.getSubtitleColor());

        // 功能项样式
        SimpleAttributeSet featureStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(featureStyle, fontSizePreset.getNormalSize());
        StyleConstants.setForeground(featureStyle, themeConfig.getTextColor());

        try {
            doc.insertString(doc.getLength(), "JEDAI Teaching Assistant\n\n", titleStyle);
            doc.insertString(doc.getLength(), "欢迎使用 Java Enterprise Application Development 课程助教插件！\n\n",
                    subtitleStyle);
            doc.insertString(doc.getLength(), "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n", subtitleStyle);
            doc.insertString(doc.getLength(), "功能介绍\n\n", normalStyle);
            doc.insertString(doc.getLength(), "- 课程知识问答：基于课程slides的智能问答系统\n", featureStyle);
            doc.insertString(doc.getLength(), "- 上下文感知：选中代码右键即可快速提问\n", featureStyle);
            doc.insertString(doc.getLength(), "- AI代码助手：代码重构、Bug修复等智能建议\n", featureStyle);
            doc.insertString(doc.getLength(), "- 来源追溯：回答会标注具体的课程资料来源\n\n", featureStyle);
            doc.insertString(doc.getLength(), "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n", subtitleStyle);
            doc.insertString(doc.getLength(), "提示：您可以直接在下方输入框中提问，或右键选中代码后选择 'Ask JEDAI about Selection'。\n\n",
                    subtitleStyle);
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

        // 构建IDE上下文（可以从当前编辑器获取）
        IdeContext ideContext = new IdeContext(project.getName());

        // 真正的异步处理：在后台线程执行LLM调用
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                // 调用RAG检索（在后台线程）
                List<RetrievedChunk> retrievedChunks = ragRetriever.search(question, 5);

                // 准备流式显示
                SwingUtilities.invokeLater(() -> {
                    // 添加Assistant标题和时间戳
                    StyledDocument doc = chatDisplayArea.getStyledDocument();
                    String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());

                    SimpleAttributeSet senderStyle = new SimpleAttributeSet();
                    StyleConstants.setBold(senderStyle, true);
                    StyleConstants.setFontSize(senderStyle, fontSizePreset.getNormalSize());
                    StyleConstants.setForeground(senderStyle, themeConfig.getAssistantNameColor());

                    SimpleAttributeSet timestampStyle = new SimpleAttributeSet();
                    StyleConstants.setFontSize(timestampStyle, fontSizePreset.getTinySize());
                    StyleConstants.setForeground(timestampStyle, themeConfig.getTimestampColor());

                    try {
                        doc.insertString(doc.getLength(), "\n", null);
                        doc.insertString(doc.getLength(), "Assistant ", senderStyle);
                        doc.insertString(doc.getLength(), timestamp + "\n", timestampStyle);
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }

                    // 开始流式答案
                    startStreamingAnswer();
                });

                // 调用流式LLM生成答案
                answerOrchestrator.generateAnswerStreaming(question, ideContext, retrievedChunks,
                        new com.github.ericufo.jedai.chat.StreamingAnswerHandler() {
                            @Override
                            public void onNext(String token) {
                                // 追加token到缓冲区
                                appendStreamingToken(token);
                            }

                            @Override
                            public void onComplete(Answer answer) {
                                // 结束流式答案，传入回调在刷新完成后执行后续操作
                                finishStreamingAnswer(answer.getContent(), () -> {
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
                            }

                            @Override
                            public void onError(Throwable error) {
                                finishStreamingAnswer("", () -> {
                                    appendSystemMessage("[错误] 生成答案时出错：" + error.getMessage());
                                    setInputEnabled(true);
                                    inputField.requestFocus();
                                });
                                error.printStackTrace();
                            }
                        });

            } catch (Exception e) {
                // 切换回UI线程显示错误
                SwingUtilities.invokeLater(() -> {
                    appendSystemMessage("[错误] 生成答案时出错：" + e.getMessage());
                    setInputEnabled(true);
                    inputField.requestFocus();
                });
                e.printStackTrace();
            }
        });
    }

    /**
     * 添加消息到聊天显示区（支持简单的Markdown解析）
     * 会保存到消息历史
     */
    private void appendMessage(String message, boolean isUser) {
        long timestamp = System.currentTimeMillis();

        // 保存到消息历史
        ChatMessage.Type type = isUser ? ChatMessage.Type.USER : ChatMessage.Type.ASSISTANT;
        messageHistory.add(new ChatMessage(type, message, timestamp));

        // 直接渲染消息
        appendMessageDirect(message, isUser, timestamp);
    }

    /**
     * 直接添加消息到聊天显示区（不保存到历史）
     */
    private void appendMessageDirect(String message, boolean isUser, long timestamp) {
        StyledDocument doc = chatDisplayArea.getStyledDocument();
        String timestampStr = new SimpleDateFormat("HH:mm:ss").format(new Date(timestamp));

        // 消息发送者样式
        SimpleAttributeSet senderStyle = new SimpleAttributeSet();
        StyleConstants.setBold(senderStyle, true);
        StyleConstants.setFontSize(senderStyle, fontSizePreset.getNormalSize());
        StyleConstants.setForeground(senderStyle,
                isUser ? themeConfig.getUserNameColor() : themeConfig.getAssistantNameColor());

        // 时间戳样式
        SimpleAttributeSet timestampStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(timestampStyle, fontSizePreset.getTinySize());
        StyleConstants.setForeground(timestampStyle, themeConfig.getTimestampColor());

        try {
            // 添加分隔线
            doc.insertString(doc.getLength(), "\n", null);

            // 添加发送者和时间戳
            String sender = isUser ? "User" : "Assistant";
            doc.insertString(doc.getLength(), sender + " ", senderStyle);
            doc.insertString(doc.getLength(), timestampStr + "\n", timestampStyle);

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
                StyleConstants.setFontSize(headingStyle, fontSizePreset.getH2Size());
                StyleConstants.setForeground(headingStyle, themeConfig.getTextColor());
                doc.insertString(doc.getLength(), line.substring(3) + "\n", headingStyle);
                continue;
            } else if (line.startsWith("### ")) {
                SimpleAttributeSet headingStyle = new SimpleAttributeSet();
                StyleConstants.setBold(headingStyle, true);
                StyleConstants.setFontSize(headingStyle, fontSizePreset.getH3Size());
                StyleConstants.setForeground(headingStyle, themeConfig.getTextColor());
                doc.insertString(doc.getLength(), line.substring(4) + "\n", headingStyle);
                continue;
            }

            // 列表项（- 开头）
            if (line.trim().startsWith("- ")) {
                SimpleAttributeSet listStyle = new SimpleAttributeSet();
                StyleConstants.setFontSize(listStyle, fontSizePreset.getNormalSize());
                StyleConstants.setForeground(listStyle, themeConfig.getTextColor());
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
                StyleConstants.setFontSize(codeStyle, fontSizePreset.getCodeSize());
                StyleConstants.setBackground(codeStyle, themeConfig.getCodeBackgroundColor());
                StyleConstants.setForeground(codeStyle, themeConfig.getCodeTextColor());

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
        StyleConstants.setFontSize(normalStyle, fontSizePreset.getNormalSize());
        StyleConstants.setForeground(normalStyle, themeConfig.getTextColor());

        // 加粗样式
        SimpleAttributeSet boldStyle = new SimpleAttributeSet();
        StyleConstants.setBold(boldStyle, true);
        StyleConstants.setFontSize(boldStyle, fontSizePreset.getNormalSize());
        StyleConstants.setForeground(boldStyle, themeConfig.getTextColor());

        // 行内代码样式
        SimpleAttributeSet inlineCodeStyle = new SimpleAttributeSet();
        StyleConstants.setFontFamily(inlineCodeStyle, "Consolas");
        StyleConstants.setFontSize(inlineCodeStyle, fontSizePreset.getCodeSize());
        StyleConstants.setBackground(inlineCodeStyle, themeConfig.getInlineCodeBackgroundColor());
        StyleConstants.setForeground(inlineCodeStyle, themeConfig.getInlineCodeTextColor());

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
     * 添加引用信息（会保存到消息历史）
     */
    private void appendCitation(String citationText) {
        // 保存到消息历史
        messageHistory.add(new ChatMessage(ChatMessage.Type.CITATION, citationText, System.currentTimeMillis()));

        // 直接渲染
        appendCitationDirect(citationText);
    }

    /**
     * 直接添加引用信息（不保存到历史）
     */
    private void appendCitationDirect(String citationText) {
        StyledDocument doc = chatDisplayArea.getStyledDocument();

        SimpleAttributeSet citationStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(citationStyle, fontSizePreset.getSmallSize());
        StyleConstants.setForeground(citationStyle, themeConfig.getCitationColor());
        StyleConstants.setItalic(citationStyle, true);

        try {
            doc.insertString(doc.getLength(), citationText, citationStyle);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    /**
     * 添加系统消息（会保存到消息历史）
     */
    private void appendSystemMessage(String message) {
        // 保存到消息历史
        messageHistory.add(new ChatMessage(ChatMessage.Type.SYSTEM, message, System.currentTimeMillis()));

        // 直接渲染
        appendSystemMessageDirect(message);
    }

    /**
     * 直接添加系统消息（不保存到历史）
     */
    private void appendSystemMessageDirect(String message) {
        StyledDocument doc = chatDisplayArea.getStyledDocument();

        SimpleAttributeSet systemStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(systemStyle, fontSizePreset.getSmallSize());
        StyleConstants.setForeground(systemStyle, themeConfig.getSystemTextColor());

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

            // 刷新聊天显示（会清空消息历史）
            refreshChatDisplay();

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

        // 真正的异步处理：在后台线程执行LLM调用
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                // 调用RAG检索（在后台线程）
                List<RetrievedChunk> retrievedChunks = ragRetriever.search(question, 5);

                // 准备流式显示
                SwingUtilities.invokeLater(() -> {
                    // 添加Assistant标题和时间戳
                    StyledDocument doc = chatDisplayArea.getStyledDocument();
                    String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());

                    SimpleAttributeSet senderStyle = new SimpleAttributeSet();
                    StyleConstants.setBold(senderStyle, true);
                    StyleConstants.setFontSize(senderStyle, fontSizePreset.getNormalSize());
                    StyleConstants.setForeground(senderStyle, themeConfig.getAssistantNameColor());

                    SimpleAttributeSet timestampStyle = new SimpleAttributeSet();
                    StyleConstants.setFontSize(timestampStyle, fontSizePreset.getTinySize());
                    StyleConstants.setForeground(timestampStyle, themeConfig.getTimestampColor());

                    try {
                        doc.insertString(doc.getLength(), "\n", null);
                        doc.insertString(doc.getLength(), "Assistant ", senderStyle);
                        doc.insertString(doc.getLength(), timestamp + "\n", timestampStyle);
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }

                    // 开始流式答案
                    startStreamingAnswer();
                });

                // 调用流式LLM生成答案
                answerOrchestrator.generateAnswerStreaming(question, ideContext, retrievedChunks,
                        new com.github.ericufo.jedai.chat.StreamingAnswerHandler() {
                            @Override
                            public void onNext(String token) {
                                // 追加token到缓冲区
                                appendStreamingToken(token);
                            }

                            @Override
                            public void onComplete(Answer answer) {
                                // 结束流式答案，传入回调在刷新完成后执行后续操作
                                finishStreamingAnswer(answer.getContent(), () -> {
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
                            }

                            @Override
                            public void onError(Throwable error) {
                                finishStreamingAnswer("", () -> {
                                    appendSystemMessage("[错误] 生成答案时出错：" + error.getMessage());
                                    setInputEnabled(true);
                                    inputField.requestFocus();
                                });
                                error.printStackTrace();
                            }
                        });

            } catch (Exception e) {
                // 切换回UI线程显示错误
                SwingUtilities.invokeLater(() -> {
                    appendSystemMessage("[错误] 生成答案时出错：" + e.getMessage());
                    setInputEnabled(true);
                    inputField.requestFocus();
                });
                e.printStackTrace();
            }
        });
    }

    /**
     * 显示代码上下文信息（会保存到消息历史）
     */
    private void appendCodeContext(IdeContext ideContext) {
        // 保存到消息历史
        messageHistory.add(new ChatMessage(ChatMessage.Type.CODE_CONTEXT, "", ideContext));

        // 直接渲染
        appendCodeContextDirect(ideContext);
    }

    /**
     * 直接显示代码上下文信息（不保存到历史）
     */
    private void appendCodeContextDirect(IdeContext ideContext) {
        StyledDocument doc = chatDisplayArea.getStyledDocument();

        SimpleAttributeSet contextStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(contextStyle, fontSizePreset.getSmallSize());
        StyleConstants.setForeground(contextStyle, themeConfig.getSubtitleColor());
        StyleConstants.setItalic(contextStyle, true);

        SimpleAttributeSet codeStyle = new SimpleAttributeSet();
        StyleConstants.setFontSize(codeStyle, fontSizePreset.getCodeSize());
        StyleConstants.setForeground(codeStyle, themeConfig.getCodeTextColor());
        StyleConstants.setFontFamily(codeStyle, "Consolas");
        StyleConstants.setBackground(codeStyle, themeConfig.getCodeBackgroundColor());

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

    /**
     * 开始流式答案显示
     */
    private void startStreamingAnswer() {
        // 清空缓冲区
        streamingAnswerBuffer.setLength(0);

        // 启动定时器，每50ms批量更新一次UI（提高性能）
        streamingUpdateTimer = new javax.swing.Timer(50, e -> flushStreamingBuffer());
        streamingUpdateTimer.start();
    }

    /**
     * 追加流式答案token
     */
    private void appendStreamingToken(String token) {
        streamingAnswerBuffer.append(token);
    }

    /**
     * 刷新流式答案缓冲区到UI
     */
    private void flushStreamingBuffer() {
        if (streamingAnswerBuffer.length() == 0) {
            return;
        }

        String tokensToAdd = streamingAnswerBuffer.toString();
        streamingAnswerBuffer.setLength(0);

        SwingUtilities.invokeLater(() -> {
            StyledDocument doc = chatDisplayArea.getStyledDocument();

            // 普通文本样式
            SimpleAttributeSet normalStyle = new SimpleAttributeSet();
            StyleConstants.setFontSize(normalStyle, fontSizePreset.getNormalSize());
            StyleConstants.setForeground(normalStyle, themeConfig.getTextColor());

            try {
                doc.insertString(doc.getLength(), tokensToAdd, normalStyle);

                // 滚动到底部
                chatDisplayArea.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 结束流式答案显示
     * 
     * @param fullAnswer 完整答案内容
     * @param onFinished 完成后的回调（在EDT线程中执行）
     */
    private void finishStreamingAnswer(String fullAnswer, Runnable onFinished) {
        // 停止定时器
        if (streamingUpdateTimer != null && streamingUpdateTimer.isRunning()) {
            streamingUpdateTimer.stop();
        }

        // 先刷新剩余缓冲区，然后在EDT中执行回调
        if (streamingAnswerBuffer.length() == 0) {
            // 缓冲区为空，直接执行回调
            SwingUtilities.invokeLater(() -> {
                // 保存到消息历史
                long timestamp = System.currentTimeMillis();
                messageHistory.add(new ChatMessage(ChatMessage.Type.ASSISTANT, fullAnswer, timestamp));

                // 执行后续操作
                if (onFinished != null) {
                    onFinished.run();
                }
            });
        } else {
            // 缓冲区有内容，先刷新，再执行回调
            String tokensToAdd = streamingAnswerBuffer.toString();
            streamingAnswerBuffer.setLength(0);

            SwingUtilities.invokeLater(() -> {
                StyledDocument doc = chatDisplayArea.getStyledDocument();

                // 普通文本样式
                SimpleAttributeSet normalStyle = new SimpleAttributeSet();
                StyleConstants.setFontSize(normalStyle, fontSizePreset.getNormalSize());
                StyleConstants.setForeground(normalStyle, themeConfig.getTextColor());

                try {
                    doc.insertString(doc.getLength(), tokensToAdd, normalStyle);

                    // 滚动到底部
                    chatDisplayArea.setCaretPosition(doc.getLength());
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }

                // 保存到消息历史
                long timestamp = System.currentTimeMillis();
                messageHistory.add(new ChatMessage(ChatMessage.Type.ASSISTANT, fullAnswer, timestamp));

                // 执行后续操作（显示引用信息等）
                if (onFinished != null) {
                    onFinished.run();
                }
            });
        }
    }
}
