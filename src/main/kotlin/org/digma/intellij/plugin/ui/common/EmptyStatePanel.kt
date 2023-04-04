package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.text
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.notifications.NotificationUtil
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.model.insights.InsightsModel
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import javax.swing.*

class EmptyStatePanel(project: Project, private val insightsModel: InsightsModel) : DigmaResettablePanel() {

    private val addButton: JButton
    private val missingDependencyPanel: DialogPanel
    private lateinit var dependencyName: Cell<JBTextArea>
    private val model: MethodInstrumentationPresenter

    init {
        model = MethodInstrumentationPresenter(project)

        val iconPanel = CircledIcon(Laf.Icons.Common.NoObservability)
        iconPanel.background = Laf.Colors.EDITOR_BACKGROUND
        iconPanel.border = JBUI.Borders.emptyTop(100)

        val titleLabel = JLabel("No Observability")
        titleLabel.alignmentX = Component.CENTER_ALIGNMENT
        titleLabel.border = JBUI.Borders.empty(5)
        titleLabel.font = Font(titleLabel.font.name, Font.BOLD, titleLabel.font.size+2)

        val descriptionLabel = JLabel(asHtml(NO_OBSERVABILITY_DETAIL_DESCRIPTION))
        descriptionLabel.alignmentX = Component.CENTER_ALIGNMENT
        descriptionLabel.horizontalTextPosition = SwingConstants.CENTER
        descriptionLabel.border = JBUI.Borders.empty(2)
        descriptionLabel.maximumSize = Dimension(250, 500)
        descriptionLabel.foreground = Laf.getLabelGrayedColor()

        missingDependencyPanel = panel {
            row {
                label(asHtml(NO_OBSERVABILITY_MISSING_DEPENDENCY_DESCRIPTION))
            }
            row {
                dependencyName = textArea()
                dependencyName.component.isEditable = false
                dependencyName.component.background = Laf.Colors.EDITOR_BACKGROUND
                dependencyName.component.lineWrap = true
                dependencyName.horizontalAlign(HorizontalAlign.FILL)
            }
        }
        missingDependencyPanel.border

        addButton = JButton("Add Annotation")
        addButton.addActionListener {
            val succeeded = model.instrumentMethod()
            if(succeeded){
                addButton.isEnabled = false
            }
            else{
                NotificationUtil.notifyError(project, "Failed to add annotation")
            }
        }

        this.isOpaque = false
        this.layout = BoxLayout(this, BoxLayout.Y_AXIS)
        this.border = JBUI.Borders.empty()

        this.add(iconPanel)
        this.add(titleLabel)
        this.add(descriptionLabel)
        this.add(missingDependencyPanel)
        this.add(addButton)
    }

    override fun reset() {
        model.update((insightsModel.scope as? MethodScope)?.getMethodInfo()?.id)
        if(model.canInstrumentMethod){
            addButton.isEnabled = true
            missingDependencyPanel.isVisible = false
        }
        else {
            addButton.isEnabled = false
            missingDependencyPanel.isVisible = model.cannotBecauseMissingDependency
            dependencyName.text(model.missingDependency ?: "")
        }
    }
}