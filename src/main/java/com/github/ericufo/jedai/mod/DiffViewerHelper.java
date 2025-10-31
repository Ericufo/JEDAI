package com.github.ericufo.jedai.mod;

import com.intellij.diff.DiffContentFactory;
import com.intellij.diff.DiffManager;
import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.File;

/**
 * Diff查看器辅助类
 * 用于在IntelliJ IDEA中显示代码修改的Diff预览
 */
public class DiffViewerHelper {
    
    /**
     * 显示代码修改提案的Diff预览
     * @param project 项目
     * @param proposal 代码修改提案
     */
    public static void showDiff(Project project, CodeChangeProposal proposal) {
        if (proposal.getDiffEntries().isEmpty()) {
            return;
        }
        
        // TODO: 成员C需要完善：
        // 1. 支持多文件Diff（目前只处理第一个文件）
        // 2. 添加"Apply"按钮，点击后调用proposal.apply(project)
        // 3. 更好的Diff展示（使用DiffRequestFactory）
        
        DiffEntry firstEntry = proposal.getDiffEntries().get(0);
        
        // 尝试获取原始文件
        VirtualFile virtualFile = findVirtualFile(project, firstEntry.getFilePath());
        
        // 使用 DiffContentFactory 创建 DiffContent
        DiffContentFactory contentFactory = DiffContentFactory.getInstance();
        
        DiffContent beforeContent;
        DiffContent afterContent;
        
        if (virtualFile != null) {
            // 如果找到文件，使用文件内容（但用提供的文本覆盖）
            beforeContent = contentFactory.create(project, firstEntry.getBeforeText(), virtualFile.getFileType());
            afterContent = contentFactory.create(project, firstEntry.getAfterText(), virtualFile.getFileType());
        } else {
            // 如果找不到文件，使用纯文本
            beforeContent = contentFactory.create(project, firstEntry.getBeforeText());
            afterContent = contentFactory.create(project, firstEntry.getAfterText());
        }
        
        SimpleDiffRequest request = new SimpleDiffRequest(
            proposal.getSummary(),
            beforeContent,
            afterContent,
            "Before",
            "After"
        );
        
        DiffManager.getInstance().showDiff(project, request);
    }
    
    private static VirtualFile findVirtualFile(Project project, String filePath) {
        // 先尝试相对路径
        if (project.getBaseDir() != null) {
            VirtualFile file = project.getBaseDir().findFileByRelativePath(filePath);
            if (file != null) {
                return file;
            }
        }
        
        // 再尝试绝对路径
        File file = new File(filePath);
        if (file.exists()) {
            return VfsUtil.findFileByIoFile(file, false);
        }
        
        return null;
    }
}

