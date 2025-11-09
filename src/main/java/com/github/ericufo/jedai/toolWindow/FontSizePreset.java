package com.github.ericufo.jedai.toolWindow;

/**
 * 字体大小预设类
 * 提供5套字体大小方案供用户选择
 */
public class FontSizePreset {

    public enum Preset {
        EXTRA_SMALL("超小"),
        SMALL("小"),
        MEDIUM("标准"),
        LARGE("大"),
        EXTRA_LARGE("特大");

        private final String displayName;

        Preset(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // 当前字体大小预设
    private Preset currentPreset = Preset.MEDIUM;

    // ===== 超小字体方案 =====
    private static final int XS_BASE = 12;
    private static final int XS_TITLE = 15;
    private static final int XS_NORMAL = 12;
    private static final int XS_H2 = 14;
    private static final int XS_H3 = 13;
    private static final int XS_CODE = 11;
    private static final int XS_SMALL = 11;
    private static final int XS_TINY = 9;

    // ===== 小字体方案 =====
    private static final int S_BASE = 13;
    private static final int S_TITLE = 17;
    private static final int S_NORMAL = 13;
    private static final int S_H2 = 15;
    private static final int S_H3 = 14;
    private static final int S_CODE = 12;
    private static final int S_SMALL = 12;
    private static final int S_TINY = 10;

    // ===== 标准字体方案（默认）=====
    private static final int M_BASE = 14;
    private static final int M_TITLE = 20;
    private static final int M_NORMAL = 14;
    private static final int M_H2 = 17;
    private static final int M_H3 = 15;
    private static final int M_CODE = 13;
    private static final int M_SMALL = 13;
    private static final int M_TINY = 11;

    // ===== 大字体方案 =====
    private static final int L_BASE = 16;
    private static final int L_TITLE = 24;
    private static final int L_NORMAL = 16;
    private static final int L_H2 = 20;
    private static final int L_H3 = 18;
    private static final int L_CODE = 15;
    private static final int L_SMALL = 14;
    private static final int L_TINY = 12;

    // ===== 特大字体方案 =====
    private static final int XL_BASE = 18;
    private static final int XL_TITLE = 28;
    private static final int XL_NORMAL = 18;
    private static final int XL_H2 = 24;
    private static final int XL_H3 = 21;
    private static final int XL_CODE = 17;
    private static final int XL_SMALL = 16;
    private static final int XL_TINY = 14;

    public FontSizePreset() {
    }

    public Preset getCurrentPreset() {
        return currentPreset;
    }

    public void setPreset(Preset preset) {
        this.currentPreset = preset;
    }

    // ===== 获取当前预设的字体大小 =====

    public int getBaseSize() {
        switch (currentPreset) {
            case EXTRA_SMALL:
                return XS_BASE;
            case SMALL:
                return S_BASE;
            case MEDIUM:
                return M_BASE;
            case LARGE:
                return L_BASE;
            case EXTRA_LARGE:
                return XL_BASE;
            default:
                return M_BASE;
        }
    }

    public int getTitleSize() {
        switch (currentPreset) {
            case EXTRA_SMALL:
                return XS_TITLE;
            case SMALL:
                return S_TITLE;
            case MEDIUM:
                return M_TITLE;
            case LARGE:
                return L_TITLE;
            case EXTRA_LARGE:
                return XL_TITLE;
            default:
                return M_TITLE;
        }
    }

    public int getNormalSize() {
        switch (currentPreset) {
            case EXTRA_SMALL:
                return XS_NORMAL;
            case SMALL:
                return S_NORMAL;
            case MEDIUM:
                return M_NORMAL;
            case LARGE:
                return L_NORMAL;
            case EXTRA_LARGE:
                return XL_NORMAL;
            default:
                return M_NORMAL;
        }
    }

    public int getH2Size() {
        switch (currentPreset) {
            case EXTRA_SMALL:
                return XS_H2;
            case SMALL:
                return S_H2;
            case MEDIUM:
                return M_H2;
            case LARGE:
                return L_H2;
            case EXTRA_LARGE:
                return XL_H2;
            default:
                return M_H2;
        }
    }

    public int getH3Size() {
        switch (currentPreset) {
            case EXTRA_SMALL:
                return XS_H3;
            case SMALL:
                return S_H3;
            case MEDIUM:
                return M_H3;
            case LARGE:
                return L_H3;
            case EXTRA_LARGE:
                return XL_H3;
            default:
                return M_H3;
        }
    }

    public int getCodeSize() {
        switch (currentPreset) {
            case EXTRA_SMALL:
                return XS_CODE;
            case SMALL:
                return S_CODE;
            case MEDIUM:
                return M_CODE;
            case LARGE:
                return L_CODE;
            case EXTRA_LARGE:
                return XL_CODE;
            default:
                return M_CODE;
        }
    }

    public int getSmallSize() {
        switch (currentPreset) {
            case EXTRA_SMALL:
                return XS_SMALL;
            case SMALL:
                return S_SMALL;
            case MEDIUM:
                return M_SMALL;
            case LARGE:
                return L_SMALL;
            case EXTRA_LARGE:
                return XL_SMALL;
            default:
                return M_SMALL;
        }
    }

    public int getTinySize() {
        switch (currentPreset) {
            case EXTRA_SMALL:
                return XS_TINY;
            case SMALL:
                return S_TINY;
            case MEDIUM:
                return M_TINY;
            case LARGE:
                return L_TINY;
            case EXTRA_LARGE:
                return XL_TINY;
            default:
                return M_TINY;
        }
    }
}
