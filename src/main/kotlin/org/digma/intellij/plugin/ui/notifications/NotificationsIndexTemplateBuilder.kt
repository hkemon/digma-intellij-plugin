package org.digma.intellij.plugin.ui.notifications

import com.intellij.openapi.project.Project
import org.digma.intellij.plugin.ui.jcef.BaseIndexTemplateBuilder


private const val NOTIFICATION_VIEW_MODE_ENV = "notificationsViewMode"
private const val NOTIFICATION_REFRESH_INTERVAL_ENV = "notificationsRefreshInterval"

class NotificationsIndexTemplateBuilder(private val notificationsViewMode: NotificationViewMode) :
    BaseIndexTemplateBuilder(NOTIFICATIONS_RESOURCE_FOLDER_NAME, NOTIFICATIONS_INDEX_TEMPLATE_NAME) {

    override fun addAppSpecificEnvVariable(project: Project, data: HashMap<String, Any>) {
        data[NOTIFICATION_VIEW_MODE_ENV] = notificationsViewMode.name
        data[NOTIFICATION_REFRESH_INTERVAL_ENV] = 20000
    }
}