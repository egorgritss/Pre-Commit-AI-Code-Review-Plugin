package my.plugin.demo.ui;

import com.intellij.configurationStore.StoreReloadManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.vcs.VcsConfiguration
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserNodeRenderer
import com.intellij.openapi.vcs.changes.ui.ChangesGroupingSupport
import com.intellij.openapi.vcs.changes.ui.NoneChangesGroupingFactory
import com.intellij.openapi.vcs.changes.ui.TreeModelBuilder
import com.intellij.openapi.vcs.changes.ui.VcsTreeModelData
import com.intellij.openapi.vcs.merge.MergeConflictsTreeTable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.DoubleClickListener
import com.intellij.ui.TableSpeedSearch
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns
import com.intellij.ui.treeStructure.treetable.TreeTable
import com.intellij.ui.treeStructure.treetable.TreeTableModel
import com.intellij.util.EditSourceOnDoubleClickHandler
import com.intellij.util.containers.Convertor
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.tree.TreeUtil
import my.plugin.demo.ai.interfaces.FileReviewResult
import java.awt.event.ActionEvent
import java.awt.event.MouseEvent
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.table.AbstractTableModel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeNode

open class AICodeReviewDialog(
    private val project: Project?,
    private val reviewResult: Map<VirtualFile, FileReviewResult>,
) : DialogWrapper(project) {
    private val table: TreeTable
    private val tableModel = ListTreeTableModelOnColumns(DefaultMutableTreeNode(), createColumns())

    private lateinit var viewButton: JButton
    private lateinit var descriptionLabel: JLabel

    private var groupByDirectory: Boolean = false
        get() = when {
            project != null -> VcsConfiguration.getInstance(project).GROUP_MULTIFILE_MERGE_BY_DIRECTORY
            else -> field
        }
        set(value) = when {
            project != null -> VcsConfiguration.getInstance(project).GROUP_MULTIFILE_MERGE_BY_DIRECTORY = value
            else -> field = value
        }


    private val virtualFileRenderer = object : ChangesBrowserNodeRenderer(project, { !groupByDirectory }, false) {
        override fun calcFocusedState() = UIUtil.isAncestor(this@AICodeReviewDialog.peer.window,
            IdeFocusManager.getInstance(project).focusOwner)
    }

    init {
        project?.let { StoreReloadManager.getInstance(project).blockReloadingProjectOnExternalChanges() }
        title = TITLE
        table = MergeConflictsTreeTable(tableModel)
        table.setTreeCellRenderer(virtualFileRenderer)
        table.rowHeight = virtualFileRenderer.preferredSize.height
        table.preferredScrollableViewportSize = JBUI.size(600, 300)

        @Suppress("LeakingThis")
        init()

        updateTree()
        table.tree.selectionModel.addTreeSelectionListener { updateButtonState() }
        updateButtonState()
        selectFirstFile()
        object : DoubleClickListener() {
            override fun onDoubleClick(event: MouseEvent): Boolean {
                if (EditSourceOnDoubleClickHandler.isToggleEvent(table.tree, event)) return false
                showReviewDiffDialog()
                return true
            }
        }.installOn(table.tree)

        TableSpeedSearch.installOn(table, Convertor { (it as? VirtualFile)?.name })
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                descriptionLabel = label(VcsBundle.message(DESCRIPTION_TEXT)).component
            }

            row {
                scrollCell(table)
                    .align(Align.FILL)
                    .resizableColumn()

                panel {
                    row {
                        val viewAction = object : AbstractAction("View") {
                            override fun actionPerformed(e: ActionEvent) {
                                showReviewDiffDialog()
                            }
                        }
                        viewAction.putValue(DEFAULT_ACTION, true)
                        viewButton = createJButtonForAction(viewAction)
                        cell(viewButton)
                            .align(AlignX.FILL)
                    }
                }.align(AlignY.TOP)
            }.resizableRow()

            if (project != null) {
                row {
                    checkBox(VcsBundle.message("multiple.file.merge.group.by.directory.checkbox"))
                        .selected(groupByDirectory)
                        .applyToComponent {
                            addChangeListener { toggleGroupByDirectory(isSelected) }
                        }
                }
            }
        }.also {
            // Temporary workaround for IDEA-302779
            it.minimumSize = JBUI.size(200, 150)
        }
    }

    private fun showReviewDiffDialog() {
        val files = getSelectedFiles()
        if (files.isEmpty()) return
        // TODO replace with proper handling of unreviewed files.
        val reviewResultText = reviewResult[files.first()]?.codeTextWithReviewChanges ?: "OOOPS, something went wrong. But i don't care."
        AiCodeReviewDiffDialog(project, files.first(), reviewResultText).show()
    }

    private fun saveContentToFile(file: VirtualFile, content: String) {
        file.setBinaryContent(content.toByteArray())
    }

    private fun createColumns(): Array<ColumnInfo<*, *>> {
        val columns = ArrayList<ColumnInfo<*, *>>()
        columns.add(object : ColumnInfo<DefaultMutableTreeNode, Any>(COLUMN_FILE_NAME) {
            override fun valueOf(node: DefaultMutableTreeNode) = node.userObject
            override fun getColumnClass(): Class<*> = TreeTableModel::class.java
        })

        return columns.toTypedArray()
    }

    private fun toggleGroupByDirectory(state: Boolean) {
        if (groupByDirectory == state) return
        groupByDirectory = state
        val firstSelectedFile = getSelectedFiles().firstOrNull()
        updateTree()
        if (firstSelectedFile != null) {
            val node = TreeUtil.findNodeWithObject(tableModel.root as DefaultMutableTreeNode, firstSelectedFile)
            node?.let { TreeUtil.selectNode(table.tree, node) }
        }
    }

    private fun updateTree() {
        val factory = when {
            project != null && groupByDirectory -> ChangesGroupingSupport.getFactory(ChangesGroupingSupport.DIRECTORY_GROUPING)
            else -> NoneChangesGroupingFactory
        }
        val model = TreeModelBuilder.buildFromVirtualFiles(project, factory, reviewResult.keys.toList())
        tableModel.setRoot(model.root as TreeNode)
        TreeUtil.expandAll(table.tree)
        (table.model as? AbstractTableModel)?.fireTableDataChanged()
    }

    private fun updateButtonState() {
        val selectedFiles = getSelectedFiles()
        val haveSelection = selectedFiles.any()
        viewButton.isEnabled = haveSelection
    }

    private fun getSelectedFiles(): List<VirtualFile> {
        return VcsTreeModelData.selected(table.tree).userObjects(VirtualFile::class.java)
    }

    private fun selectFirstFile() {
        if (!groupByDirectory) {
            table.selectionModel.setSelectionInterval(0, 0)
        }
        else {
            TreeUtil.promiseSelectFirstLeaf(table.tree)
        }
    }

    companion object {
        const val TITLE = "AI Code Review"
        const val DESCRIPTION_TEXT = "AI Review Result"
        const val COLUMN_FILE_NAME = "File Name"
    }

}
