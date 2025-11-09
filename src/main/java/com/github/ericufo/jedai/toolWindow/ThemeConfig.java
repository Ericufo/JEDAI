package com.github.ericufo.jedai.toolWindow;

import java.awt.*;

/**
 * 主题配置类
 * 支持深色和浅色两种主题
 */
public class ThemeConfig {

    public enum Theme {
        LIGHT("浅色"),
        DARK("深色");

        private final String displayName;

        Theme(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // 当前主题
    private Theme currentTheme = Theme.LIGHT;

    // ===== 浅色主题配色 =====
    private static final Color LIGHT_BG = new Color(255, 255, 255);
    private static final Color LIGHT_TEXT = new Color(33, 33, 33);
    private static final Color LIGHT_USER_NAME = new Color(13, 71, 161);
    private static final Color LIGHT_ASSISTANT_NAME = new Color(56, 142, 60);
    private static final Color LIGHT_SYSTEM_TEXT = new Color(255, 152, 0);
    private static final Color LIGHT_TIMESTAMP = new Color(117, 117, 117);
    private static final Color LIGHT_TITLE = new Color(25, 118, 210);
    private static final Color LIGHT_SUBTITLE = new Color(97, 97, 97);
    private static final Color LIGHT_CODE_BG = new Color(245, 245, 245);
    private static final Color LIGHT_CODE_TEXT = new Color(60, 60, 60);
    private static final Color LIGHT_INLINE_CODE_BG = new Color(240, 240, 240);
    private static final Color LIGHT_INLINE_CODE_TEXT = new Color(200, 0, 0);
    private static final Color LIGHT_CITATION = new Color(100, 100, 100);
    private static final Color LIGHT_THINKING = new Color(150, 150, 150);
    private static final Color LIGHT_INPUT_BG = Color.WHITE;
    private static final Color LIGHT_BUTTON_BG = new Color(245, 245, 245);
    private static final Color LIGHT_BUTTON_HOVER = new Color(230, 230, 230);
    private static final Color LIGHT_SEPARATOR = new Color(224, 224, 224);

    // ===== 深色主题配色 =====
    private static final Color DARK_BG = new Color(30, 30, 30);
    private static final Color DARK_TEXT = new Color(220, 220, 220);
    private static final Color DARK_USER_NAME = new Color(100, 181, 246);
    private static final Color DARK_ASSISTANT_NAME = new Color(129, 199, 132);
    private static final Color DARK_SYSTEM_TEXT = new Color(255, 183, 77);
    private static final Color DARK_TIMESTAMP = new Color(158, 158, 158);
    private static final Color DARK_TITLE = new Color(66, 165, 245);
    private static final Color DARK_SUBTITLE = new Color(189, 189, 189);
    private static final Color DARK_CODE_BG = new Color(45, 45, 45);
    private static final Color DARK_CODE_TEXT = new Color(200, 200, 200);
    private static final Color DARK_INLINE_CODE_BG = new Color(50, 50, 50);
    private static final Color DARK_INLINE_CODE_TEXT = new Color(255, 138, 128);
    private static final Color DARK_CITATION = new Color(158, 158, 158);
    private static final Color DARK_THINKING = new Color(158, 158, 158);
    private static final Color DARK_INPUT_BG = new Color(45, 45, 45);
    private static final Color DARK_BUTTON_BG = new Color(55, 55, 55);
    private static final Color DARK_BUTTON_HOVER = new Color(70, 70, 70);
    private static final Color DARK_SEPARATOR = new Color(66, 66, 66);

    public ThemeConfig() {
    }

    public Theme getCurrentTheme() {
        return currentTheme;
    }

    public void setTheme(Theme theme) {
        this.currentTheme = theme;
    }

    public void toggleTheme() {
        currentTheme = (currentTheme == Theme.LIGHT) ? Theme.DARK : Theme.LIGHT;
    }

    // ===== 获取当前主题的颜色 =====

    public Color getBackgroundColor() {
        return currentTheme == Theme.LIGHT ? LIGHT_BG : DARK_BG;
    }

    public Color getTextColor() {
        return currentTheme == Theme.LIGHT ? LIGHT_TEXT : DARK_TEXT;
    }

    public Color getUserNameColor() {
        return currentTheme == Theme.LIGHT ? LIGHT_USER_NAME : DARK_USER_NAME;
    }

    public Color getAssistantNameColor() {
        return currentTheme == Theme.LIGHT ? LIGHT_ASSISTANT_NAME : DARK_ASSISTANT_NAME;
    }

    public Color getSystemTextColor() {
        return currentTheme == Theme.LIGHT ? LIGHT_SYSTEM_TEXT : DARK_SYSTEM_TEXT;
    }

    public Color getTimestampColor() {
        return currentTheme == Theme.LIGHT ? LIGHT_TIMESTAMP : DARK_TIMESTAMP;
    }

    public Color getTitleColor() {
        return currentTheme == Theme.LIGHT ? LIGHT_TITLE : DARK_TITLE;
    }

    public Color getSubtitleColor() {
        return currentTheme == Theme.LIGHT ? LIGHT_SUBTITLE : DARK_SUBTITLE;
    }

    public Color getCodeBackgroundColor() {
        return currentTheme == Theme.LIGHT ? LIGHT_CODE_BG : DARK_CODE_BG;
    }

    public Color getCodeTextColor() {
        return currentTheme == Theme.LIGHT ? LIGHT_CODE_TEXT : DARK_CODE_TEXT;
    }

    public Color getInlineCodeBackgroundColor() {
        return currentTheme == Theme.LIGHT ? LIGHT_INLINE_CODE_BG : DARK_INLINE_CODE_BG;
    }

    public Color getInlineCodeTextColor() {
        return currentTheme == Theme.LIGHT ? LIGHT_INLINE_CODE_TEXT : DARK_INLINE_CODE_TEXT;
    }

    public Color getCitationColor() {
        return currentTheme == Theme.LIGHT ? LIGHT_CITATION : DARK_CITATION;
    }

    public Color getThinkingColor() {
        return currentTheme == Theme.LIGHT ? LIGHT_THINKING : DARK_THINKING;
    }

    public Color getInputBackgroundColor() {
        return currentTheme == Theme.LIGHT ? LIGHT_INPUT_BG : DARK_INPUT_BG;
    }

    public Color getButtonBackgroundColor() {
        return currentTheme == Theme.LIGHT ? LIGHT_BUTTON_BG : DARK_BUTTON_BG;
    }

    public Color getButtonHoverColor() {
        return currentTheme == Theme.LIGHT ? LIGHT_BUTTON_HOVER : DARK_BUTTON_HOVER;
    }

    public Color getSeparatorColor() {
        return currentTheme == Theme.LIGHT ? LIGHT_SEPARATOR : DARK_SEPARATOR;
    }
}
