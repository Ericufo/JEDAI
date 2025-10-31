package com.github.ericufo.jedai.chat;

/**
 * IDE上下文信息
 */
public class IdeContext {
    private final String projectName;
    private final String filePath;
    private final String selectedCode;
    private final String language;
    private final Integer lineNumber;

    public IdeContext(String projectName, String filePath, String selectedCode, String language, Integer lineNumber) {
        this.projectName = projectName;
        this.filePath = filePath;
        this.selectedCode = selectedCode;
        this.language = language;
        this.lineNumber = lineNumber;
    }

    public IdeContext(String projectName) {
        this(projectName, null, null, null, null);
    }

    public String getProjectName() {
        return projectName;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getSelectedCode() {
        return selectedCode;
    }

    public String getLanguage() {
        return language;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }
}

