package com.github.ericufo.jedai.mod;

/**
 * 单个文件的差异条目
 */
public class DiffEntry {
    private final String filePath;
    private final String beforeText;
    private final String afterText;

    public DiffEntry(String filePath, String beforeText, String afterText) {
        this.filePath = filePath;
        this.beforeText = beforeText;
        this.afterText = afterText;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getBeforeText() {
        return beforeText;
    }

    public String getAfterText() {
        return afterText;
    }
}

