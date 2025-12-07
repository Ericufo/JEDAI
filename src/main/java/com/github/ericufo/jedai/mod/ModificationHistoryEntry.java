package com.github.ericufo.jedai.mod;

import java.util.Date;

/**
 * 修改历史记录条目（成员C负责）
 */
public class ModificationHistoryEntry {
    private final String instruction;
    private final String filePath;
    private final String originalCode;
    private final String modifiedCode;
    private final Date timestamp;
    private final String language;
    
    public ModificationHistoryEntry(String instruction, String filePath, String originalCode, 
                                   String modifiedCode, String language) {
        this.instruction = instruction;
        this.filePath = filePath;
        this.originalCode = originalCode;
        this.modifiedCode = modifiedCode;
        this.timestamp = new Date();
        this.language = language;
    }
    
    public String getInstruction() {
        return instruction;
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public String getOriginalCode() {
        return originalCode;
    }
    
    public String getModifiedCode() {
        return modifiedCode;
    }
    
    public Date getTimestamp() {
        return timestamp;
    }
    
    public String getLanguage() {
        return language;
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s - %s", 
            timestamp.toString(), 
            instruction.substring(0, Math.min(50, instruction.length())),
            getFileName());
    }
    
    public String getFileName() {
        if (filePath == null) return "Unknown";
        int lastSlash = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        return lastSlash >= 0 ? filePath.substring(lastSlash + 1) : filePath;
    }
}

