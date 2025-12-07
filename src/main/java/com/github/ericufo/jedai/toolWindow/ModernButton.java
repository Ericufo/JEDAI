package com.github.ericufo.jedai.toolWindow;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 现代化圆角按钮
 * 支持悬停效果和主题适配
 */
public class ModernButton extends JButton {

    private Color normalBackgroundColor;
    private Color hoverBackgroundColor;
    private boolean isHovered = false;
    private int cornerRadius = 8; // 圆角半径

    public ModernButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);

        // 默认颜色
        normalBackgroundColor = new Color(245, 245, 245);
        hoverBackgroundColor = new Color(230, 230, 230);
        setForeground(Color.BLACK);

        // 添加鼠标监听器实现悬停效果
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }
        });
    }

    public void setColors(Color normalBg, Color hoverBg, Color textColor) {
        this.normalBackgroundColor = normalBg;
        this.hoverBackgroundColor = hoverBg;
        setForeground(textColor);
        repaint();
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        // 启用抗锯齿
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 绘制圆角背景
        Color bgColor = isHovered ? hoverBackgroundColor : normalBackgroundColor;
        g2.setColor(bgColor);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);

        // 如果按钮被按下，绘制轻微的阴影效果
        if (getModel().isPressed()) {
            g2.setColor(new Color(0, 0, 0, 30));
            g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, cornerRadius, cornerRadius);
        }

        g2.dispose();

        // 绘制文本
        super.paintComponent(g);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        // 确保按钮有足够的内边距
        size.width += 20;
        size.height += 10;
        return size;
    }
}
