package com.github.ericufo.jedai.mod

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.contents.DiffContent
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtil
import java.io.File

/**
 * Diff查看器辅助类
 * 用于在IntelliJ IDEA中显示代码修改的Diff预览
 */
object DiffViewerHelper {
    
    /**
     * 显示代码修改提案的Diff预览
     * @param project 项目
     * @param proposal 代码修改提案
     */
    fun showDiff(project: Project, proposal: CodeChangeProposal) {
        if (proposal.diffEntries.isEmpty()) {
            return
        }
        
        // TODO: 成员C需要完善：
        // 1. 支持多文件Diff（目前只处理第一个文件）
        // 2. 添加"Apply"按钮，点击后调用proposal.apply(project)
        // 3. 更好的Diff展示（使用DiffRequestFactory）
        
        val firstEntry = proposal.diffEntries.first()
        
        // 尝试获取原始文件
        val virtualFile = findVirtualFile(project, firstEntry.filePath)
        
        // 使用 DiffContentFactory 创建 DiffContent
        val contentFactory = DiffContentFactory.getInstance()
        
        val beforeContent = if (virtualFile != null) {
            // 如果找到文件，使用文件内容（但用提供的文本覆盖）
            contentFactory.create(project, firstEntry.beforeText, virtualFile.fileType)
        } else {
            // 如果找不到文件，使用纯文本
            contentFactory.create(project, firstEntry.beforeText)
        }
        
        val afterContent = if (virtualFile != null) {
            contentFactory.create(project, firstEntry.afterText, virtualFile.fileType)
        } else {
            contentFactory.create(project, firstEntry.afterText)
        }
        
        val request = SimpleDiffRequest(
            proposal.summary,
            beforeContent,
            afterContent,
            "Before",
            "After"
        )
        
        DiffManager.getInstance().showDiff(project, request)
    }
    
    private fun findVirtualFile(project: Project, filePath: String): VirtualFile? {
        // 先尝试相对路径
        project.baseDir?.findFileByRelativePath(filePath)?.let { return it }
        
        // 再尝试绝对路径
        val file = File(filePath)
        if (file.exists()) {
            return VfsUtil.findFileByIoFile(file, false)
        }
        
        return null
    }
}
