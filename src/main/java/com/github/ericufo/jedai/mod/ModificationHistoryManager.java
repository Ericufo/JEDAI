package com.github.ericufo.jedai.mod;

import com.intellij.openapi.diagnostic.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 修改历史管理器（成员C负责）
 * 用于记录和管理代码修改历史
 */
public class ModificationHistoryManager {
    private static final Logger LOG = Logger.getInstance(ModificationHistoryManager.class);
    private static final int MAX_HISTORY_SIZE = 100; // 最多保存100条历史记录
    
    private static ModificationHistoryManager instance;
    private final List<ModificationHistoryEntry> history = new ArrayList<>();
    
    private ModificationHistoryManager() {
    }
    
    public static ModificationHistoryManager getInstance() {
        if (instance == null) {
            instance = new ModificationHistoryManager();
        }
        return instance;
    }
    
    /**
     * 添加历史记录
     */
    public void addEntry(ModificationHistoryEntry entry) {
        history.add(0, entry); // 添加到列表开头（最新的在前）
        
        // 限制历史记录大小
        if (history.size() > MAX_HISTORY_SIZE) {
            history.remove(history.size() - 1);
        }
        
        LOG.info("添加修改历史：" + entry.getInstruction());
    }
    
    /**
     * 获取所有历史记录
     */
    public List<ModificationHistoryEntry> getHistory() {
        return Collections.unmodifiableList(history);
    }
    
    /**
     * 清空历史记录
     */
    public void clearHistory() {
        history.clear();
        LOG.info("清空修改历史");
    }
    
    /**
     * 获取最近的N条历史记录
     */
    public List<ModificationHistoryEntry> getRecentHistory(int count) {
        int size = Math.min(count, history.size());
        return Collections.unmodifiableList(history.subList(0, size));
    }
    
    /**
     * 搜索历史记录（按指令或文件名）
     */
    public List<ModificationHistoryEntry> searchHistory(String keyword) {
        List<ModificationHistoryEntry> results = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();
        
        for (ModificationHistoryEntry entry : history) {
            if (entry.getInstruction().toLowerCase().contains(lowerKeyword) ||
                entry.getFilePath().toLowerCase().contains(lowerKeyword)) {
                results.add(entry);
            }
        }
        
        return results;
    }
}

