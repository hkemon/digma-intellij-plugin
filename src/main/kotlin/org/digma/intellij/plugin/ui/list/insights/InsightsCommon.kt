package org.digma.intellij.plugin.ui.list.insights

import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.hint.HintUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.ActionLink
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBUI.Borders.empty
import io.ktor.util.reflect.instanceOf
import org.digma.intellij.plugin.analytics.AnalyticsService
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.model.InsightType
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight
import org.digma.intellij.plugin.notifications.NotificationUtil
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.posthog.MonitoredPanel
import org.digma.intellij.plugin.refreshInsightsTask.RefreshService
import org.digma.intellij.plugin.ui.common.Laf
import org.digma.intellij.plugin.instrumentation.MethodInstrumentationPresenter
import org.digma.intellij.plugin.ui.common.OtelDependencyButton
import org.digma.intellij.plugin.ui.common.Text
import org.digma.intellij.plugin.ui.common.Text.NO_OBSERVABILITY_DETAIL_DESCRIPTION
import org.digma.intellij.plugin.ui.common.asHtml
import org.digma.intellij.plugin.ui.common.buildBoldTitleGrayedComment
import org.digma.intellij.plugin.ui.common.span
import org.digma.intellij.plugin.ui.common.spanBold
import org.digma.intellij.plugin.ui.common.spanGrayed
import org.digma.intellij.plugin.ui.list.ListItemActionButton
import org.digma.intellij.plugin.ui.list.PanelsLayoutHelper
import org.digma.intellij.plugin.ui.list.commonListItemPanel
import org.digma.intellij.plugin.ui.model.insights.NoObservability
import org.digma.intellij.plugin.ui.panels.DigmaResettablePanel
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import javax.swing.Box
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import kotlin.math.max

private const val RECALCULATE = "Recalculate"
private const val REFRESH = "Refresh"

fun insightTitlePanel(panel: JPanel): JPanel {
    panel.isOpaque = false
    panel.border = empty(0, 5)
    return panel
}

fun insightItemPanel(panel: JPanel): JPanel {
    return commonListItemPanel(panel)
}

fun createInsightPanel(
    insight: CodeObjectInsight?,
    project: Project,
    title: String,
    description: String,
    iconsList: List<Icon>?,
    bodyPanel: JComponent?,
    buttons: List<JButton?>?,
    paginationComponent: JComponent?,
): JPanel {

    val resultInsightPanel = buildInsightPanel(
        insight = insight,
        project = project,
        title = title,
        description = description,
        iconsList = iconsList,
        bodyPanel = bodyPanel,
        buttons = buttons,
        paginationComponent = paginationComponent,
    )

    return insightItemPanel(resultInsightPanel as DigmaResettablePanel)
}

private fun buildInsightPanel(
    insight: CodeObjectInsight?,
    project: Project,
    title: String,
    description: String,
    iconsList: List<Icon>?,
    bodyPanel: JComponent?,
    buttons: List<JButton?>?,
    paginationComponent: JComponent?,
): JPanel {
    val insightPanel = object : DigmaResettablePanel() {
        override fun reset() {
            rebuildPanel(
                insightPanel = this,
                insight = insight,
                project = project,
                title = title,
                description = description,
                iconsList = iconsList,
                bodyPanel = bodyPanel,
                buttons = buttons,
                paginationComponent = paginationComponent,
                isRecalculateButtonPressed = true
            )
        }
    }
    return rebuildPanel(
        insightPanel = insightPanel,
        insight = insight,
        project = project,
        title = title,
        description = description,
        iconsList = iconsList,
        bodyPanel = bodyPanel,
        buttons = buttons,
        paginationComponent = paginationComponent,
        isRecalculateButtonPressed = false
    )
}

private fun rebuildPanel(
    insightPanel: JPanel?,
    insight: CodeObjectInsight?,
    project: Project,
    title: String,
    description: String,
    iconsList: List<Icon>?,
    bodyPanel: JComponent?,
    buttons: List<JButton?>?,
    paginationComponent: JComponent?,
    isRecalculateButtonPressed: Boolean,
): JPanel {

    // .-----------------------------------.
    // | icon |title                       |
    // | description                       |
    // |-----------------------------------|
    // | timeInfoMessagePanel              |
    // | bodyPanel                         |
    // | paginationPanel                   |
    // |-----------------------------------|
    // |                           buttons |
    // '-----------------------------------'

    insightPanel!!.layout = BorderLayout()

    val titleLabel = JLabel(asHtml(spanBold(title)), SwingConstants.LEFT)
    titleLabel.isOpaque = false
    titleLabel.verticalAlignment = SwingConstants.CENTER

    val descriptionLabel = JLabel(asHtml(spanGrayed(description)), SwingConstants.LEFT)
    descriptionLabel.isOpaque = false
    descriptionLabel.verticalAlignment = SwingConstants.TOP

    val icon = iconsList?.firstOrNull { icon -> !icon.instanceOf(ThreeDotsIcon::class) }
    var iconLabel = JLabel()
    if (icon != null) {
        iconLabel = JLabel(icon, SwingConstants.RIGHT)  
        iconLabel.horizontalAlignment = SwingConstants.RIGHT
        iconLabel.verticalAlignment = SwingConstants.TOP
        iconLabel.isOpaque = false
        iconLabel.border = empty(2, 2, 2, 4)
    }

    val leftIcons = getIconsListPanel(
        insight,
        project,
        iconsList?.filter { i -> i != icon }?.toList(),
        insightPanel as DigmaResettablePanel
    )

    val headerPanel = JPanel(BorderLayout())
    headerPanel.isOpaque = false
    headerPanel.add(iconLabel, BorderLayout.WEST)
    headerPanel.add(leftIcons, BorderLayout.EAST)
    headerPanel.add(titleLabel, BorderLayout.CENTER)
    headerPanel.add(descriptionLabel, BorderLayout.SOUTH)

    insightPanel.add(headerPanel, BorderLayout.CENTER)

    if (bodyPanel != null || buttons != null) {
        val bodyWrapper = createDefaultBoxLayoutYAxisPanel()
        bodyWrapper.isOpaque = false

        if (insight != null && (insight.customStartTime != null || isRecalculateButtonPressed))
            bodyWrapper.add(
                getTimeInfoMessagePanel(
                    customStartTime = insight.customStartTime,
                    actualStartTime = insight.actualStartTime,
                    isRecalculateButtonPressed = isRecalculateButtonPressed,
                    project = project,
                    insight.type
                )
            )

        if (bodyPanel != null)
            bodyWrapper.add(bodyPanel)

        if (buttons != null) {
            val buttonsListPanel = getBasicEmptyListPanel()
            buttonsListPanel.border = JBUI.Borders.emptyTop(5)
            buttons.filterNotNull().forEach {
                buttonsListPanel.add(Box.createHorizontalStrut(5))
                buttonsListPanel.add(it)
            }
            bodyWrapper.add(buttonsListPanel)
        }

        if (paginationComponent != null) {
            bodyWrapper.add(getPaginationPanel(paginationComponent))
        }

        insightPanel.add(bodyWrapper, BorderLayout.SOUTH)
    }

    return insightPanel
}

private fun getTimeInfoMessagePanel(
    customStartTime: Date?,
    actualStartTime: Date?,
    isRecalculateButtonPressed: Boolean,
    project: Project,
    insightType: InsightType,
): JPanel {
    val formattedActualStartTime = actualStartTime?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDateTime()
    val diff: Duration = Duration.between(formattedActualStartTime, LocalDateTime.now())

    var formattedStartTime = ""
    if (!diff.isNegative && !diff.isZero) {
        formattedStartTime = getFormattedTimeDifference(diff)
    }

    val identicalStartTimes = customStartTime != null && actualStartTime?.compareTo(customStartTime) == 0

    val timeInfoMessage = if (shouldShowApplyNewTimeFilterLabel(isRecalculateButtonPressed, identicalStartTimes)) {
        "Applying the new time filter. Wait a few minutes and then refresh."
    } else {
        "Data from: $formattedStartTime ago"
    }

    val timeInfoMessageLabel = JLabel(asHtml(timeInfoMessage))

    val timeInfoMessageLabelPanel = getDefaultSpanOneRecordPanel()
    timeInfoMessageLabelPanel.add(timeInfoMessageLabel, BorderLayout.NORTH)
    if (shouldShowApplyNewTimeFilterLabel(isRecalculateButtonPressed, identicalStartTimes)) {
        timeInfoMessageLabelPanel.add(getRefreshInsightButton(project, insightType), BorderLayout.SOUTH)
    }
    return timeInfoMessageLabelPanel
}

private fun shouldShowApplyNewTimeFilterLabel(isRecalculateButtonPressed: Boolean, identicalStartTimes: Boolean): Boolean {
    return isRecalculateButtonPressed || !identicalStartTimes
}

private fun getFormattedTimeDifference(diff: Duration): String {
    val builder = StringBuilder()
    if (diff.toDays() > 0) {
        builder.append(diff.toDays(), " days ")
    } else if (diff.toHoursPart() > 0) {
        builder.append(diff.toHoursPart(), " hours ")
    } else if (diff.toMinutesPart() > 0) {
        builder.append(diff.toMinutesPart(), " minutes ")
    } else {
        builder.append(1, " minute ")
    }
    return builder.toString()
}

private fun getBasicEmptyListPanel(): JPanel {
    val listPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0))
    listPanel.isOpaque = false
    listPanel.border = empty()
    return listPanel
}

private fun getPaginationPanel(paginationComponent: JComponent?): JPanel {
    val paginationPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
    paginationPanel.isOpaque = false
    paginationPanel.border = empty()

    paginationPanel.add(Box.createHorizontalStrut(5))
    paginationPanel.add(paginationComponent)
    return paginationPanel
}

private fun getMessageLabel(title: String, description: String): JLabel {
    val messageLabel = JLabel(buildBoldTitleGrayedComment(title, description), SwingConstants.LEFT)
    messageLabel.isOpaque = false
    messageLabel.verticalAlignment = SwingConstants.TOP
    return messageLabel
}

private fun getIconsListPanel(
    insight: CodeObjectInsight?,
    project: Project,
    iconsList: List<Icon>?,
    insightPanel: DigmaResettablePanel,
): JPanel {
    val icons = ArrayList<Icon>()
    if (iconsList != null) {
        icons.addAll(iconsList)
    }
    if (insight?.isRecalculateEnabled == true) {
        icons.add(Laf.Icons.Insight.THREE_DOTS)
    }

    val iconsResultListPanel = getBasicEmptyListPanel()
    icons.forEach {
        iconsResultListPanel.add(Box.createHorizontalStrut(5))
        val iconLabel = JLabel(it, SwingConstants.RIGHT)
        iconLabel.horizontalAlignment = SwingConstants.RIGHT
        iconLabel.verticalAlignment = SwingConstants.TOP
        iconLabel.isOpaque = false
        iconLabel.border = empty(2, 2, 2, 4)

        if (it.instanceOf(ThreeDotsIcon::class)) {
            iconLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            iconLabel.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    showHintMessage(
                        threeDotsIcon = iconLabel,
                        insightPanel = insightPanel,
                        codeObjectId = insight!!.prefixedCodeObjectId!!,
                        insightType = insight.type,
                        project = project
                    )
                }

                override fun mouseEntered(e: MouseEvent?) {
                    showHintMessage(
                        threeDotsIcon = iconLabel,
                        insightPanel = insightPanel,
                        codeObjectId = insight!!.prefixedCodeObjectId!!,
                        insightType = insight.type,
                        project = project
                    )
                }

                override fun mouseExited(e: MouseEvent?) {}
                override fun mousePressed(e: MouseEvent?) {}
            })
        }

        iconsResultListPanel.add(iconLabel)
    }
    return iconsResultListPanel
}

private fun showHintMessage(
    threeDotsIcon: JComponent,
    insightPanel: DigmaResettablePanel,
    codeObjectId: String,
    insightType: InsightType,
    project: Project,
) {
    val analyticsService = AnalyticsService.getInstance(project)
    val recalculateAction = ActionLink(RECALCULATE)
    recalculateAction.addActionListener {

        Backgroundable.executeOnPooledThread {
            analyticsService.setInsightCustomStartTime(codeObjectId, insightType)
        };

        rebuildInsightPanel(insightPanel)
        ActivityMonitor.getInstance(project).registerButtonClicked("recalculate", insightType)
    }
    recalculateAction.border = HintUtil.createHintBorder()
    recalculateAction.background = HintUtil.getInformationColor()
    recalculateAction.isOpaque = true
    HintManager.getInstance().showHint(recalculateAction, RelativePoint.getSouthWestOf(threeDotsIcon), HintManager.HIDE_BY_ESCAPE, 2000)
}

private fun getRefreshInsightButton(project: Project, insightType: InsightType): ActionLink {
    val refreshAction = ActionLink(REFRESH)
    refreshAction.addActionListener {
        val refreshService: RefreshService = project.getService(RefreshService::class.java)
        refreshService.refreshAllInBackground()
        ActivityMonitor.getInstance(project).registerButtonClicked("refresh", insightType)
    }
    refreshAction.border = empty()
    refreshAction.isOpaque = false
    return refreshAction
}

private fun rebuildInsightPanel(insightPanel: DigmaResettablePanel) {
    insightPanel.removeAll()
    insightPanel.reset()
}

fun genericPanelForSingleInsight(project: Project, modelObject: Any?): JPanel {

    return createInsightPanel(
        project = project,
        insight = modelObject as CodeObjectInsight,
        title = "Generic insight panel",
        description = "Insight named ${modelObject.javaClass.simpleName}",
        iconsList = listOf(Laf.Icons.Insight.QUESTION_MARK),
        bodyPanel = null,
        buttons = null,
        paginationComponent = null
    )
}


internal fun getInsightIconPanelRightBorderSize(): Int {
    return 5
}

internal fun getCurrentLargestWidthIconPanel(layoutHelper: PanelsLayoutHelper, width: Int): Int {
    //this method should never return null and never throw NPE
    val currentLargest: Int =
        (layoutHelper.getObjectAttribute("insightsIconPanelBorder", "largestWidth") ?: 0) as Int
    return max(width, currentLargest)
}

internal fun addCurrentLargestWidthIconPanel(layoutHelper: PanelsLayoutHelper, width: Int) {
    //this method should never throw NPE
    val currentLargest: Int =
        (layoutHelper.getObjectAttribute("insightsIconPanelBorder", "largestWidth") ?: 0) as Int
    layoutHelper.addObjectAttribute(
        "insightsIconPanelBorder", "largestWidth",
        max(currentLargest, width)
    )
}

private const val NoDataYetDescription = "No data received yet for this span, please trigger some actions using this code to see more insights."

fun noDataYetInsightPanel(): JPanel {

    val thePanel = object : DigmaResettablePanel() {
        override fun reset() {
        }
    }
    thePanel.layout = BorderLayout()
    thePanel.add(getMessageLabel("No Data Yet", ""), BorderLayout.WEST)
    thePanel.add(JLabel(asHtml(NoDataYetDescription)), BorderLayout.SOUTH)

    return insightItemPanel(thePanel as DigmaResettablePanel)
}

fun noObservabilityInsightPanel(project: Project, insight: NoObservability): JPanel {

    val methodId = insight.methodId
    val model = MethodInstrumentationPresenter(project)
    model.update(methodId)

    val body = JPanel(BorderLayout())
    body.isOpaque = false

    val autoFixLabel = JLabel(asHtml(span(Laf.Colors.RED_OF_MISSING, "missing dependency: " + model.missingDependency)))
    autoFixLabel.border = JBUI.Borders.emptyRight(10)
    body.add(autoFixLabel, BorderLayout.CENTER)

    val autoFixLink = OtelDependencyButton("Autofix", project, model)
    body.add(autoFixLink, BorderLayout.EAST)

    val workingOnItLabel = JLabel(asHtml(Text.NO_OBSERVABILITY_WORKING_ON_IT_DESCRIPTION))
    workingOnItLabel.isVisible = false
    workingOnItLabel.border = JBUI.Borders.emptyTop(10)
    body.add(workingOnItLabel, BorderLayout.SOUTH)

    val addAnnotationButton = ListItemActionButton("Add Annotation")
    addAnnotationButton.addActionListener {
        ActivityMonitor.getInstance(project).registerButtonClicked(MonitoredPanel.NoObservability, "add-annotation")
        val succeeded = model.instrumentMethod()
        if (succeeded) {
            addAnnotationButton.isEnabled = false
        } else {
            NotificationUtil.notifyError(project, "Failed to add annotation")
        }
    }

    if (model.canInstrumentMethod) {
        addAnnotationButton.isEnabled = true
        body.isVisible = false
    } else {
        addAnnotationButton.isEnabled = false
        body.isVisible = model.cannotBecauseMissingDependency
    }

    autoFixLink.defineTheAction(null, workingOnItLabel)

    return createInsightPanel(
        project = project,
        insight = null,
        title = "No Observability",
        description = NO_OBSERVABILITY_DETAIL_DESCRIPTION,
        iconsList = emptyList(),
        bodyPanel = body,
        buttons = listOf(addAnnotationButton),
        paginationComponent = null
    )
}

class InsightAlignedPanel(private val layoutHelper: PanelsLayoutHelper) : JPanel() {

    init {
        border = JBUI.Borders.emptyRight(getInsightIconPanelRightBorderSize())
    }

    override fun getPreferredSize(): Dimension {
        val ps = super.getPreferredSize()
        if (ps == null) {
            return ps
        }
        val h = ps.height
        val w = ps.width
        addCurrentLargestWidthIconPanel(layoutHelper, w)
        return Dimension(getCurrentLargestWidthIconPanel(layoutHelper, w), h)
    }
}