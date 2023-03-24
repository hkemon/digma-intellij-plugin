package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.launchBackground
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import org.digma.intellij.plugin.analytics.DashboardButtonStateChanged
import org.digma.intellij.plugin.analytics.ErrorDetailsStateChanged
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.common.modelChangeListener.ModelChangeListener
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.errors.errorsPanel
import org.digma.intellij.plugin.ui.insights.insightsPanel
import org.digma.intellij.plugin.ui.model.PanelModel
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import org.digma.intellij.plugin.ui.service.*
import org.digma.intellij.plugin.ui.summary.summaryPanel
import java.awt.CardLayout
import java.util.concurrent.locks.ReentrantLock
import javax.swing.BorderFactory
import javax.swing.JComponent

class TabsPanel(
        project: Project
//        model: PanelModel
) : DigmaTabPanel(), Disposable {
    private val logger: Logger = Logger.getInstance(TabsPanel::class.java)

    private val project: Project
    private val rebuildPanelLock = ReentrantLock()
    private val tabsHelper: TabsHelper
//    private val model: PanelModel
    private val homeButtonHelper = HomeButtonHelper.getInstance(project)
    private val messageBusConnection: MessageBusConnection = project.messageBus.connect()
    private var errorDetailsOn = false
    private var lastOpenedCardName: String = DASHBOARD_DISABLED
//    private var previousCardName: String

    companion object {
        const val DASHBOARD_IS_ACTIVE = "DASHBOARD_IS_ACTIVE"
        const val DASHBOARD_DISABLED = "DASHBOARD_DISABLED"
        const val DASHBOARD_DETAILED_ERROR = "DASHBOARD_DETAILED_ERROR"
    }

    init {
        this.project = project
        this.tabsHelper = TabsHelper.getInstance(project)
//        this.model = model
        layout = CardLayout()
        isOpaque = true
//        previousCardName = DASHBOARD_DISABLED

        rebuildInBackground()

        messageBusConnection.subscribe(ErrorDetailsStateChanged.ERROR_DETAILS_STATE_TOPIC,
                ErrorDetailsStateChanged { errorDetailsOn ->
                    this@TabsPanel.errorDetailsOn = errorDetailsOn
                    showCardBasedClickedButton()
                })

        messageBusConnection.subscribe(DashboardButtonStateChanged.DASHBOARD_BUTTON_STATE_CHANGED_TOPIC,
                DashboardButtonStateChanged {
                    EDT.ensureEDT {
                        showCardBasedClickedButton()
                    }
                })
    }

    override fun getPreferredFocusableComponent(): JComponent {
        return this@TabsPanel
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return this@TabsPanel
    }

    override fun reset() {
        showCardBasedClickedButton()
    }

    private fun rebuildInBackground() {
        val lifetimeDefinition = LifetimeDefinition()
        lifetimeDefinition.lifetime.launchBackground {
            rebuildPanelLock.lock()
            Log.log(logger::debug, "Lock acquired for rebuild Tabs panel process.")
            try {
                rebuild()
            } finally {
                rebuildPanelLock.unlock()
                Log.log(logger::debug, "Lock released for rebuild Tabs panel process.")
                lifetimeDefinition.terminate()
            }
        }
    }

    private fun rebuild() {
        ApplicationManager.getApplication().invokeLater {
            removeExistingComponentsIfPresent()
            buildTabsPanelComponents()
            revalidate()
        }
    }

    private fun removeExistingComponentsIfPresent() {
        if (components.isNotEmpty()) {
            this.components.forEach {
                this.remove(it)
            }
        }
    }

    private fun buildTabsPanelComponents() {
        val dashboardPanel = getDashboardTabsPanel()
        val errorsPanel = createErrorsPanel(project)
        val generalTabsPanel = getGeneralTabsPanel()

        add(dashboardPanel, DASHBOARD_IS_ACTIVE)
        add(generalTabsPanel, DASHBOARD_DISABLED)
        add(errorsPanel, DASHBOARD_DETAILED_ERROR)
        (layout as CardLayout).addLayoutComponent(dashboardPanel, DASHBOARD_IS_ACTIVE)
        (layout as CardLayout).addLayoutComponent(generalTabsPanel, DASHBOARD_DISABLED)
        (layout as CardLayout).addLayoutComponent(errorsPanel, DASHBOARD_DETAILED_ERROR)

        showCardBasedClickedButton()
    }

    private fun showCardBasedClickedButton() {
         if (errorDetailsOn) {
            lastOpenedCardName = DASHBOARD_DETAILED_ERROR
            (layout as CardLayout).show(this, DASHBOARD_DETAILED_ERROR)
        } else if (homeButtonHelper.isHomeButtonEnabled()) {
            lastOpenedCardName = DASHBOARD_IS_ACTIVE
            (layout as CardLayout).show(this, DASHBOARD_IS_ACTIVE)
        } else {
            lastOpenedCardName = DASHBOARD_DISABLED
            (layout as CardLayout).show(this, DASHBOARD_DISABLED)
        }
    }
//    private fun showCardBasedClickedButton() {
//        val model: PanelModel = if (errorDetailsOn) {
//            (layout as CardLayout).show(this, DASHBOARD_DETAILED_ERROR)
//            ErrorsViewService.getInstance(project).model
//        } else if (homeButtonHelper.isHomeButtonEnabled()) {
//            (layout as CardLayout).show(this, DASHBOARD_IS_ACTIVE)
//            SummaryViewService.getInstance(project).model
//        } else {
//            (layout as CardLayout).show(this, DASHBOARD_DISABLED)
//            InsightsViewService.getInstance(project).model
//        }
//        notifyModelChanged(model)
//    }

    private fun notifyModelChanged(newModel: PanelModel) {
        Log.log(logger::debug, "Firing ModelChange event for new model = {}", newModel)
        if (project.isDisposed) {
            return
        }
        val publisher = project.messageBus.syncPublisher(ModelChangeListener.MODEL_CHANGED_TOPIC)
        publisher.modelChanged(newModel)
    }

    private fun getGeneralTabsPanel(): JBTabbedPane {
        val tabbedPane = JBTabbedPane()
        tabbedPane.isOpaque = false
        tabbedPane.border = JBUI.Borders.empty()

        val insightsPanel = createInsightsPanel(project)
        insightsPanel.border = BorderFactory.createEmptyBorder(0, 0, 0, 0) // Set the border of the panel to empty
        tabbedPane.addTab(TabsHelper.INSIGHTS_TAB_NAME, insightsPanel)

        val errorsPanel = createErrorsPanel(project)
        tabbedPane.addTab(TabsHelper.DEFAULT_ERRORS_TAB_NAME, errorsPanel)
        tabbedPane.border = BorderFactory.createEmptyBorder(); // Set the border of the tabbed pane to empty

        var currentTabIndex: Int

        // Add a listener to the JBTabbedPane to track tab changes
        tabbedPane.addChangeListener {
            // Get the index of the currently selected tab
            currentTabIndex = tabbedPane.selectedIndex
            // Do something with the tab indexes, such as update UI or perform logic
            tabsHelper.currentTabIndex = currentTabIndex
            // remember previous opened card and tab
            tabsHelper.saveLastOpenedViewAndTab(lastOpenedCardName, tabbedPane.selectedIndex)
            getActiveTabName(tabbedPane)
        }

        return tabbedPane
    }

    private fun getActiveTabName(tabbedPane: JBTabbedPane): String {
        val activeTabIndex = tabbedPane.selectedIndex
        return tabbedPane.getTitleAt(activeTabIndex)
    }

    private fun getDashboardTabsPanel(): JBTabbedPane {
        val tabbedPane = JBTabbedPane()
        tabbedPane.isOpaque = false
        tabbedPane.border = JBUI.Borders.empty()

        val dashboardTabPanel = createDashboardTabPanel(project)
        dashboardTabPanel.border = BorderFactory.createEmptyBorder(0, 0, 0, 0) // Set the border of the panel to empty
        tabbedPane.addTab(TabsHelper.DASHBOARD_TAB_NAME, dashboardTabPanel)
//        tabbedPane.addTab(TabsHelper.ASSETS_TAB_NAME, getAssetsTabPanel())  // will be used later

        tabbedPane.border = BorderFactory.createEmptyBorder()
        return tabbedPane
    }

    private fun createDashboardTabPanel(project: Project): DigmaTabPanel {
        val summaryPanel = summaryPanel(project)
        val summaryViewService = project.getService(SummaryViewService::class.java)
        summaryViewService.panel = summaryPanel
        return summaryPanel
    }

    private fun createErrorsPanel(project: Project): DigmaTabPanel {
        val errorsPanel = errorsPanel(project)
        val errorsViewService = project.getService(ErrorsViewService::class.java)
        errorsViewService.panel = errorsPanel
        return errorsPanel
    }

    private fun createInsightsPanel(project: Project): DigmaTabPanel {
        val insightsPanel = insightsPanel(project)
        val insightsViewService = project.getService(InsightsViewService::class.java)
        insightsViewService.panel = insightsPanel
        return insightsPanel
    }

    override fun dispose() {
        messageBusConnection.dispose()
    }
}