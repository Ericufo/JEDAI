package com.github.ericufo.jedai.mod;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 指令模板管理器（成员C负责）
 * 用于保存和管理用户自定义的代码修改指令模板
 */
@State(
    name = "JedaiInstructionTemplates",
    storages = @Storage("jedai-instruction-templates.xml")
)
public class InstructionTemplateManager implements PersistentStateComponent<InstructionTemplateManager> {
    private static final Logger LOG = Logger.getInstance(InstructionTemplateManager.class);
    
    // 默认模板
    private static final List<String> DEFAULT_TEMPLATES = Arrays.asList(
        "Add comments to this code",
        "Refactor this method",
        "Fix potential bugs in this code",
        "Apply design pattern",
        "Optimize this code",
        "Add error handling",
        "Simplify this code"
    );
    
    // 用户自定义模板（持久化存储）
    public List<String> customTemplates = new ArrayList<>();
    
    /**
     * 获取所有模板（默认模板 + 自定义模板）
     */
    public List<String> getAllTemplates() {
        List<String> allTemplates = new ArrayList<>(DEFAULT_TEMPLATES);
        allTemplates.addAll(customTemplates);
        return allTemplates;
    }
    
    /**
     * 获取自定义模板
     */
    public List<String> getCustomTemplates() {
        return new ArrayList<>(customTemplates);
    }
    
    /**
     * 添加自定义模板
     */
    public void addCustomTemplate(String template) {
        if (template != null && !template.trim().isEmpty()) {
            String trimmed = template.trim();
            if (!customTemplates.contains(trimmed)) {
                customTemplates.add(trimmed);
                LOG.info("添加自定义模板：" + trimmed);
            }
        }
    }
    
    /**
     * 删除自定义模板
     */
    public void removeCustomTemplate(String template) {
        if (customTemplates.remove(template)) {
            LOG.info("删除自定义模板：" + template);
        }
    }
    
    /**
     * 清空所有自定义模板
     */
    public void clearCustomTemplates() {
        customTemplates.clear();
        LOG.info("清空所有自定义模板");
    }
    
    @Nullable
    @Override
    public InstructionTemplateManager getState() {
        return this;
    }
    
    @Override
    public void loadState(@NotNull InstructionTemplateManager state) {
        XmlSerializerUtil.copyBean(state, this);
        LOG.info("加载了 " + customTemplates.size() + " 个自定义模板");
    }
}

