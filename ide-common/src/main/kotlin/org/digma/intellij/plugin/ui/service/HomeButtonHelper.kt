package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.analytics.DashboardButtonStateChanged
import org.digma.intellij.plugin.log.Log
import java.util.concurrent.atomic.AtomicBoolean

class HomeButtonHelper(val project: Project) {
    private val logger: Logger = Logger.getInstance(HomeButtonHelper::class.java)

    private var isHomeBooleanEnabled = AtomicBoolean(false)
    companion object {
        @JvmStatic
        fun getInstance(project: Project): HomeButtonHelper {
            return project.getService(HomeButtonHelper::class.java)
        }
    }

    fun enableHomeButton() {
        isHomeBooleanEnabled.set(true)
    }

    fun disableHomeButton() {
        isHomeBooleanEnabled.set(false)
    }

    fun isHomeButtonEnabled(): Boolean {
        return isHomeBooleanEnabled.get()
    }

    fun setHomeButtonEnabled(isEnabled: Boolean) {
        isHomeBooleanEnabled.set(isEnabled)
        notifyDashboardButtonClicked(isHomeBooleanEnabled.get())
    }

    private fun notifyDashboardButtonClicked(isEnabled: Boolean) {
        Log.log(logger::info, "Firing TabChanged event for {}", isEnabled)
        if (project.isDisposed) {
            return
        }
        val publisher = project.messageBus.syncPublisher(DashboardButtonStateChanged.DASHBOARD_BUTTON_STATE_CHANGED_TOPIC)
        publisher.dashboardButtonStateChanged(isEnabled)
    }
}