package my.plugin.demo.ai.services

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import dev.langchain4j.data.document.Document
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader
import my.plugin.demo.ai.interfaces.ContextLoader


class OpenFilesContextLoader : ContextLoader {
    override fun loadDocuments(project: Project, excludedFiles: List<VirtualFile>?): List<Document> {
        val openFiles = FileEditorManager.getInstance(project).openFiles.toList()
        val openFilesWithoutExcluded = excludedFiles?.let {
            openFiles.filterNot { it in excludedFiles }
        } ?: openFiles

        return openFilesWithoutExcluded.map { FileSystemDocumentLoader.loadDocument(it.path) }
    }
}