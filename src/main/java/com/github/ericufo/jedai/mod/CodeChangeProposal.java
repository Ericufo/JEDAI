package com.github.ericufo.jedai.mod;

import com.intellij.openapi.project.Project;

import java.util.List;
import java.util.function.Consumer;

/**
 * 代码修改提案
 * 包含差异信息和应用逻辑
 */
public class CodeChangeProposal {
    private final String summary;
    private final List<DiffEntry> diffEntries;
    private final Consumer<Project> applyFunction;

    public CodeChangeProposal(String summary, List<DiffEntry> diffEntries, Consumer<Project> applyFunction) {
        this.summary = summary;
        this.diffEntries = diffEntries;
        this.applyFunction = applyFunction;
    }

    public String getSummary() {
        return summary;
    }

    public List<DiffEntry> getDiffEntries() {
        return diffEntries;
    }

    public void apply(Project project) {
        applyFunction.accept(project);
    }
}

