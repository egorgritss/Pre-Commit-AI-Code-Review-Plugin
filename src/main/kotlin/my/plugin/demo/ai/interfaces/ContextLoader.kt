package my.plugin.demo.ai.interfaces

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import dev.langchain4j.data.document.Document

interface ContextLoader {
    fun loadDocuments(project: Project, excludedFiles: List<VirtualFile>?) : List<Document>
}