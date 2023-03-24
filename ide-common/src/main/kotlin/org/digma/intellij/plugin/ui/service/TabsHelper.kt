package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.content.Content
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import org.digma.intellij.plugin.analytics.ErrorDetailsStateChanged
import org.digma.intellij.plugin.log.Log
import java.util.concurrent.ConcurrentHashMap

class TabsHelper(val project: Project) {
    private val logger: Logger = Logger.getInstance(TabsHelper::class.java)

    var currentTabIndex = 0
    // key - viewName (cardLayout card name),  value - tabIndex
    private val lastVisibleViewAndTab: ConcurrentHashMap<String, Int> = ConcurrentHashMap()

    private var visibleTabBeforeErrorDetails: Int? = null
    private var errorDetailsOn = false

    companion object {
        const val INSIGHTS_TAB_NAME = "Insights"
        const val DEFAULT_ERRORS_TAB_NAME = "Errors"
        const val DASHBOARD_TAB_NAME = "Dashboard"
        const val ASSETS_TAB_NAME = "Assets"

        @JvmStatic
        fun getInstance(project: Project): TabsHelper {
            return project.getService(TabsHelper::class.java)
        }
    }

    fun isInsightsTab(content: Content?): Boolean {
        return content != null && content.tabName.equals(INSIGHTS_TAB_NAME, ignoreCase = true)
    }

    fun isErrorsTab(content: Content?): Boolean {
        return content != null && content.tabName.equals(DEFAULT_ERRORS_TAB_NAME, ignoreCase = true)
    }

    fun isDashboardTab(content: Content?): Boolean {
        return content != null && content.tabName.equals(DASHBOARD_TAB_NAME, ignoreCase = true)
    }

    fun showingErrorDetails() {
        visibleTabBeforeErrorDetails = currentTabIndex
    }

    fun saveLastOpenedViewAndTab(cardName: String, tabIndex: Int) {
        lastVisibleViewAndTab[cardName] = tabIndex
    }

    fun errorDetailsClosed() {
        notifyErrorDetailsStateChanged(false)
//        visibleTabBeforeErrorDetails?.let { notifyErrorDetailsStateChanged(it) }
        visibleTabBeforeErrorDetails = null
    }

    private fun notifyErrorDetailsStateChanged(errorDetailsOn: Boolean) {
        Log.log(logger::info, "Firing ErrorDetailsStateChanged event for {}", errorDetailsOn)
        if (project.isDisposed) {
            return
        }
        val publisher = project.messageBus.syncPublisher(ErrorDetailsStateChanged.ERROR_DETAILS_STATE_TOPIC)
        publisher.errorDetailsStateChanged(errorDetailsOn)
    }

    fun errorDetailsOn() {
        errorDetailsOn = true
        notifyErrorDetailsStateChanged(true)
    }

    fun errorDetailsOff() {
        errorDetailsOn = false
    }

    fun isErrorDetailsOn(): Boolean {
        return errorDetailsOn
    }

}