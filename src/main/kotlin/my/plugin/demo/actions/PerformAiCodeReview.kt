package my.plugin.demo.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.AppIcon
import com.intellij.vcs.commit.AbstractCommitWorkflowHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.plugin.demo.ai.services.AiReviewService
import my.plugin.demo.ui.AICodeReviewDialog

class PerformAiCodeReview : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = getChangedFiles(e)

        val aiReviewService = project.getService(AiReviewService::class.java)
        aiReviewService.coroutineScope.launch {
            val result = aiReviewService.performReviewWithProgress(files)
            withContext(Dispatchers.EDT) {
                val aiCodeReviewDialog = AICodeReviewDialog(project, result)
                AppIcon.getInstance().requestAttention(project, true)
                aiCodeReviewDialog.show()
            }
        }
    }

    private fun getChangedFiles(e: AnActionEvent): List<VirtualFile> {
        val commitWorkflowHandler =
            e.getData(VcsDataKeys.COMMIT_WORKFLOW_HANDLER) as? AbstractCommitWorkflowHandler<*, *>
        val selectedVirtualFiles =
            commitWorkflowHandler?.ui?.getIncludedChanges()?.mapNotNull { it.virtualFile }?.toList() ?: emptyList()

        return selectedVirtualFiles
    }
}