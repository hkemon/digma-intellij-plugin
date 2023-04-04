package org.digma.intellij.plugin.ui.common

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.util.ui.JBUI
import org.digma.intellij.plugin.notifications.NotificationUtil
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.model.NOT_SUPPORTED_OBJECT_MSG
import org.digma.intellij.plugin.ui.model.PanelModel
import org.digma.intellij.plugin.ui.model.insights.InsightsModel
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import javax.swing.*


const val NO_DATA_YET_DETAIL_DESCRIPTION = "Trigger actions that call this code object to learn more about its runtime behavior"
const val NO_OBSERVABILITY_DETAIL_DESCRIPTION = "Add an annotation to observe this method and collect data about its runtime behavior"
const val NO_OBSERVABILITY_MISSING_DEPENDENCY_DESCRIPTION = "Before adding annotations, please add the following dependency:";


fun noCodeObjectWarningPanel(model: PanelModel): DialogPanel {
    return panel {
        row {
            icon(AllIcons.General.BalloonInformation)
                    .horizontalAlign(HorizontalAlign.CENTER)
        }.bottomGap(BottomGap.MEDIUM).topGap(TopGap.MEDIUM)
        row {
            label(getNoInfoMessage(model)).bind(
                    JLabel::getText, JLabel::setText, MutableProperty(
                    getter = { getNoInfoMessage(model) },
                    setter = {})
            ).bind(
                    JLabel::getToolTipText, JLabel::setToolTipText, MutableProperty(
                    getter = { getNoInfoMessage(model) },
                    setter = {})
            )
                    .horizontalAlign(HorizontalAlign.CENTER)
        }.bottomGap(BottomGap.MEDIUM).topGap(TopGap.MEDIUM)
    }.andTransparent().withBorder(JBUI.Borders.empty())
}

fun createPendingInsightsPanel(): DialogPanel {
    return panel {
        row {
            icon(Laf.Icons.Common.Mascot64)
                    .horizontalAlign(HorizontalAlign.CENTER)
        }.bottomGap(BottomGap.MEDIUM).topGap(TopGap.MEDIUM)
        row {
            label("Processing Insights...")
                    .horizontalAlign(HorizontalAlign.CENTER)
        }.bottomGap(BottomGap.MEDIUM).topGap(TopGap.MEDIUM)
    }.andTransparent().withBorder(JBUI.Borders.empty())
}

fun createLoadingInsightsPanel(): DialogPanel {
    return panel {
        row {
            icon(Laf.Icons.Common.Loading)
                    .horizontalAlign(HorizontalAlign.CENTER)
        }.bottomGap(BottomGap.MEDIUM).topGap(TopGap.MEDIUM)
        row {
            label("Loading...")
                    .horizontalAlign(HorizontalAlign.CENTER)
        }.bottomGap(BottomGap.MEDIUM).topGap(TopGap.MEDIUM)
    }.andTransparent().withBorder(JBUI.Borders.empty())
}

fun createNoDataYetPanel(): DialogPanel {
    return panel {
        row {
            icon(Laf.Icons.Common.NoDataYet)
                    .horizontalAlign(HorizontalAlign.CENTER)
        }.bottomGap(BottomGap.MEDIUM).topGap(TopGap.SMALL)
        row {
            label("No Data Yet")
                    .horizontalAlign(HorizontalAlign.CENTER)
        }.bottomGap(BottomGap.MEDIUM).topGap(TopGap.MEDIUM)
        row {
            label(asHtml(NO_DATA_YET_DETAIL_DESCRIPTION))
                    .horizontalAlign(HorizontalAlign.CENTER)
        }.bottomGap(BottomGap.MEDIUM).topGap(TopGap.MEDIUM)
    }.andTransparent().withBorder(JBUI.Borders.empty())
}

fun createNoObservabilityPanel(project: Project, insightsModel: InsightsModel): DigmaResettablePanel {

    return EmptyStatePanel(project, insightsModel)
//
//    return panel {
//        row {
//            icon(Laf.Icons.Common.NoObservability)
//                    .horizontalAlign(HorizontalAlign.CENTER)
//        }.bottomGap(BottomGap.MEDIUM).topGap(TopGap.SMALL)
//        row {
//            label("No Observability")
//                    .horizontalAlign(HorizontalAlign.CENTER)
//        }.bottomGap(BottomGap.MEDIUM).topGap(TopGap.MEDIUM)
//        row {
//            label(asHtml(NO_OBSERVABILITY_DETAIL_DESCRIPTION))
//                    .horizontalAlign(HorizontalAlign.CENTER)
//        }.bottomGap(BottomGap.MEDIUM).topGap(TopGap.MEDIUM)
//        missingDependencyPanel = row {
//             panel {
//                row {
//                    label(asHtml(NO_OBSERVABILITY_MISSING_DEPENDENCY_DESCRIPTION))
//                }
//                row {
//                    dependencyName = textArea()
//                    dependencyName.component.isEditable = false
//                    dependencyName.component.background = Laf.Colors.EDITOR_BACKGROUND
//                    dependencyName.component.lineWrap = true
//                    dependencyName.horizontalAlign(HorizontalAlign.FILL)
//                }
//            }
//        }.visible(false)
//        row {
//            addButton = button("Add Annotation"){
//                val succeeded = model.instrumentMethod()
//                if(succeeded){
//                    addButton.enabled(false)
//                }
//                else{
//                    NotificationUtil.notifyError(project, "Failed to add annotation")
//                }
//            }.horizontalAlign(HorizontalAlign.CENTER)
//        }
//        onReset {
//            model.update((insightsModel.scope as? MethodScope)?.getMethodInfo()?.id)
//            if(model.canInstrumentMethod){
//                addButton.component.isEnabled = true
//                missingDependencyPanel.visible(false)
//            }
//            else {
//                addButton.component.isEnabled = false
//                missingDependencyPanel.visible(model.cannotBecauseMissingDependency)
//                dependencyName.text(model.missingDependency ?: "")
//            }
//        }
//    }.andTransparent().withBorder(JBUI.Borders.empty())
}



private fun getNoInfoMessage(model: PanelModel): String {
    var msg = if (model is InsightsModel) "No insights" else "No errors"

    if (model.getScope().isNotBlank() && !model.getScope().contains(NOT_SUPPORTED_OBJECT_MSG)) {
        msg += " for " + model.getScope()
    }
    return msg
}

fun wrapWithNoConnectionWrapper(project: Project, panel: DigmaTabPanel): DigmaTabPanel {
    return NoConnectionWrapper(project, panel)
}