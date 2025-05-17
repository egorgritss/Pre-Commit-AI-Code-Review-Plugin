package my.plugin.demo.ui

import com.intellij.diff.DiffManager
import com.intellij.diff.DiffRequestPanel
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.diff.DiffContentFactory
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.Messages
import javax.swing.JComponent

class AiCodeReviewDiffDialog(
    project: Project?,
    rightFile: VirtualFile,
    leftString: String,
) : DialogWrapper(project), Disposable {

    private val diffPanel: DiffRequestPanel = DiffManager.getInstance().createRequestPanel(project, this, null)

    init {
        title = "Custom Diff"
        // Prepare contents
        val factory = DiffContentFactory.getInstance()
        val leftVirtualFile = LightVirtualFile("ProposedChanges.txt", leftString)
        val leftContent = factory.createDocument(project, leftVirtualFile)
        val rightContent = factory.createDocument(project, rightFile)

        if (leftContent == null || rightContent == null) {
            Messages.showErrorDialog("Cannot create diff content", "Error")
            throw throw IllegalStateException("Failed to create diff content :(")
        }

        // Mark left as read-only
        leftContent.document.setReadOnly(true)

        // Create diff request
        val request = SimpleDiffRequest(
            "Custom Diff",
            leftContent,
            rightContent,
            "Proposed Changes (Read-Only)",
            "Current File (Editable)"
        )

        diffPanel.setRequest(request)
        init()
    }

    override fun createCenterPanel(): JComponent = diffPanel.component

    override fun dispose() {
        super.dispose()
    }
}
