package org.digma.intellij.plugin.ui.notifications

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cef.browser.CefBrowser
import org.digma.intellij.plugin.analytics.AnalyticsServiceException
import org.digma.intellij.plugin.analytics.NoSelectedEnvironmentException
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.EDT
import org.digma.intellij.plugin.errorreporting.ErrorReporter
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.posthog.MonitoredPanel
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.jcef.BaseMessageRouterHandler
import org.digma.intellij.plugin.ui.jcef.executeWindowPostMessageJavaScript
import org.digma.intellij.plugin.ui.jcef.model.ErrorPayload
import org.digma.intellij.plugin.ui.jcef.model.Payload
import org.digma.intellij.plugin.ui.notifications.model.SetNotificationsMessage


abstract class NotificationsMessageRouterHandler(project: Project) : BaseMessageRouterHandler(project) {

    abstract fun doClose()

    abstract fun getNotifications(project: Project, pageNumber: Int, pageSize: Int, isRead: Boolean): String


    /**
     * do the query action. this method is executed on a pooled thread
     */
    override fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String) {

        //exceptions are handles in BaseMessageRouterHandler.onQuery

        when (action) {

            "NOTIFICATIONS/GET_DATA" -> {
                Log.log(logger::trace, project, "got NOTIFICATIONS/GET_DATA message {}", requestJsonNode)

                Backgroundable.executeOnPooledThread {

                    Log.log(logger::trace, project, "updating notifications data in background")

                    try {

                        //note that GET_DATA always expect SET_DATA in return even if there is an error , that a contract with the react app

                        val pageNumber: String = objectMapper.readTree(requestJsonNode.get("payload").toString()).get("pageNumber").asText()
                        val pageSize: String = objectMapper.readTree(requestJsonNode.get("payload").toString()).get("pageSize").asText()
                        val isRead: String = objectMapper.readTree(requestJsonNode.get("payload").toString()).get("isRead").asText()
                        val notificationsJson = getNotifications(project, pageNumber.toInt(), pageSize.toInt(), isRead.toBoolean())
                        Log.log(logger::trace, project, "got notifications {}", notificationsJson)
                        val payload = objectMapper.readTree(notificationsJson)
                        val message = SetNotificationsMessage("digma", "NOTIFICATIONS/SET_DATA", Payload(payload))
                        Log.log(logger::trace, project, "sending NOTIFICATIONS/SET_DATA message")
                        executeWindowPostMessageJavaScript(browser, objectMapper.writeValueAsString(message))

                    } catch (e: NoSelectedEnvironmentException) {
                        Log.warnWithException(logger, e, "error setting notifications data")
                        val jsonNode = createEmptyNotifications()
                        val message =
                            SetNotificationsMessage("digma", "NOTIFICATIONS/SET_DATA", Payload(jsonNode))
                        Log.log(logger::trace, project, "sending NOTIFICATIONS/SET_DATA message with error")
                        executeWindowPostMessageJavaScript(browser, objectMapper.writeValueAsString(message))
                        ErrorReporter.getInstance().reportError(project, "NotificationsMessageRouterHandler.SET_DATA", e)
                    } catch (e: AnalyticsServiceException) {
                        Log.warnWithException(logger, e, "error setting notifications data")
                        val message =
                            SetNotificationsMessage("digma", "NOTIFICATIONS/SET_DATA", Payload(null, ErrorPayload(e.getMeaningfulMessage())))
                        Log.log(logger::trace, project, "sending NOTIFICATIONS/SET_DATA message with error")
                        executeWindowPostMessageJavaScript(browser, objectMapper.writeValueAsString(message))
                        ErrorReporter.getInstance().reportError(project, "NotificationsMessageRouterHandler.SET_DATA", e)
                    } catch (e: Exception) {
                        Log.warnWithException(logger, e, "error setting notifications data")
                        val message = SetNotificationsMessage("digma", "NOTIFICATIONS/SET_DATA", Payload(null, ErrorPayload(e.toString())))
                        Log.log(logger::trace, project, "sending NOTIFICATIONS/SET_DATA message with error")
                        executeWindowPostMessageJavaScript(browser, objectMapper.writeValueAsString(message))
                        ErrorReporter.getInstance().reportError(project, "NotificationsMessageRouterHandler.SET_DATA", e)
                        //let BaseMessageRouterHandler handle the exception too in case it does something meaningful, worst case it will just log
                        // the error again
                        throw e
                    }
                }
            }

            "NOTIFICATIONS/GO_TO_INSIGHTS" -> {
                project.service<ActivityMonitor>().registerSpanLinkClicked(MonitoredPanel.Notifications)
                ActivityMonitor.getInstance(project).registerNotificationCenterEvent("${javaClass.simpleName}SpanClicked", mapOf())
                ActivityMonitor.getInstance(project).registerUserAction("Notification link clicked")

                project.service<NotificationsService>().markAllRead()

                Log.log(logger::trace, project, "got NOTIFICATIONS/GO_TO_INSIGHTS message")
                doClose()
                val spanCodeObjectId: String? = try {
                    objectMapper.readTree(requestJsonNode.get("payload").toString()).get("spanCodeObjectId").asText()
                } catch (e: Exception) {
                    null
                }

                val methodCodeObjectId: String? = try {
                    objectMapper.readTree(requestJsonNode.get("payload").toString()).get("methodCodeObjectId").asText()
                } catch (e: Exception) {
                    null
                }


                project.service<NotificationsService>().goToInsight(spanCodeObjectId, methodCodeObjectId)
            }

            "NOTIFICATIONS/CLOSE" -> {
                ActivityMonitor.getInstance(project).registerNotificationCenterEvent("${javaClass.simpleName}Closed", mapOf())
                Log.log(logger::trace, project, "got NOTIFICATIONS/CLOSE message")
                project.service<NotificationsService>().markAllRead()
                doClose()
            }
        }

    }

    private fun createEmptyNotifications(): JsonNode? {

        return try {
            val objectMapper = ObjectMapper()
            val notificationRoot: ObjectNode = objectMapper.createObjectNode()
            val notificationsArray: ArrayNode = objectMapper.createArrayNode()
            notificationRoot.set<ArrayNode>("notifications", notificationsArray)

            notificationRoot.set<IntNode>("totalCount", IntNode(0))
            notificationRoot.set<IntNode>("unreadCount", IntNode(0))

            notificationRoot

        } catch (e: Exception) {
            null
        }

    }
}


class TopNotificationsMessageRouterHandler(project: Project, private val topNotificationsPanel: TopNotificationsPanel) :
    NotificationsMessageRouterHandler(project) {


    override fun doOnQuery(project: Project, browser: CefBrowser, requestJsonNode: JsonNode, rawRequest: String, action: String) {


        when (action) {

            "NOTIFICATIONS/GO_TO_NOTIFICATIONS" -> {
                ActivityMonitor.getInstance(project).registerNotificationCenterEvent("NotificationsViewAllClicked", mapOf())
                ActivityMonitor.getInstance(project).registerUserAction("Notification view-all clicked")

                project.service<NotificationsService>().resetLatestNotificationTime()

                Log.log(logger::trace, project, "got NOTIFICATIONS/GO_TO_NOTIFICATIONS message")
                doClose()
                EDT.ensureEDT {
                    MainToolWindowCardsController.getInstance(project).showAllNotifications()
                }

            }

            else -> {
                super.doOnQuery(project, browser, requestJsonNode, rawRequest, action)
            }

        }
    }

    override fun getOriginForTroubleshootingEvent(): String {
        return "top notifications"
    }

    override fun doClose() {

        EDT.ensureEDT {
            topNotificationsPanel.close()
        }
    }

    override fun getNotifications(project: Project, pageNumber: Int, pageSize: Int, isRead: Boolean): String {
        //top notifications ignores the query parameters and always loads top 3
        return project.service<NotificationsService>().getNotifications(1, 3, false)
    }
}


class AllNotificationsMessageRouterHandler(project: Project) :
    NotificationsMessageRouterHandler(project) {

    override fun getOriginForTroubleshootingEvent(): String {
        return "all notifications"
    }

    override fun doClose() {
        EDT.ensureEDT {
            MainToolWindowCardsController.getInstance(project).closeAllNotifications()
        }
    }

    override fun getNotifications(project: Project, pageNumber: Int, pageSize: Int, isRead: Boolean): String {

        return project.service<NotificationsService>().getNotifications(pageNumber, pageSize, isRead)
    }
}


