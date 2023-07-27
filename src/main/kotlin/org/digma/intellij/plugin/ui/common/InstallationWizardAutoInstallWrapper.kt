package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor
import org.digma.intellij.plugin.analytics.BackendConnectionUtil
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.docker.DockerService
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.persistence.PersistenceService
import org.digma.intellij.plugin.posthog.ActivityMonitor
import org.digma.intellij.plugin.ui.panels.DisposablePanel
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.GridBagLayout
import javax.swing.JLabel
import javax.swing.JPanel


private val logger: Logger =
    Logger.getInstance("org.digma.intellij.plugin.ui.common.InstallationWizardWrapper")


private const val startingCardName = "starting"
private const val wizardCardName = "wizard"

fun createInstallationWizardWrapper(project: Project, wizardSkipInstallationStep: Boolean): DisposablePanel? {

    val isServerConnectedAlready = BackendConnectionUtil.getInstance(project).testConnectionToBackend()
    val dockerService = service<DockerService>()

    if (wizardSkipInstallationStep || isServerConnectedAlready || dockerService.isEngineInstalled()) {
        return createInstallationWizardSidePanelWindowPanel(project, wizardSkipInstallationStep)
    }

    PersistenceService.getInstance().firstWizardLaunchDone()

    val cardLayout = CardLayout()
    val cardsPanel = JPanel(cardLayout)
    cardsPanel.setOpaque(false)
    cardsPanel.setBorder(JBUI.Borders.empty())

    val startingPanel = createStartingPanel(project, dockerService) {
        cardLayout.show(cardsPanel, wizardCardName)
    }
    val wizardPanel = createInstallationWizardSidePanelWindowPanel(project, true)

    cardsPanel.add(startingPanel, startingCardName)
    cardsPanel.add(wizardPanel, wizardCardName)

    cardLayout.addLayoutComponent(startingPanel, startingCardName)
    cardLayout.addLayoutComponent(wizardPanel, wizardCardName)

    cardLayout.show(cardsPanel, startingCardName)

    val result = object : DisposablePanel() {
        override fun dispose() {
            wizardPanel?.dispose()
        }
    }
    result.layout = BorderLayout()
    result.add(cardsPanel, BorderLayout.CENTER)
    return result
}

private fun createStartingPanel(project: Project, dockerService: DockerService, showWizardCard: () -> Unit): JPanel {

    val panel = JPanel(GridBagLayout())
    panel.isOpaque = false
    panel.border = JBUI.Borders.empty()
    val startingLabel = JLabel("Starting")
    panel.add(startingLabel)

    Backgroundable.runInNewBackgroundThread(project, "starting digma") {
        dockerService.installEngine(project) { exitValue ->

            runBlocking {

                val success = exitValue == "0"

                if (success) {
                    var i = 0
                    while (!BackendConnectionMonitor.getInstance(project).isConnectionOk() && i < 8) {
                        Log.log(logger::warn, "waiting for connection")
                        BackendConnectionUtil.getInstance(project).testConnectionToBackend()
                        delay(5000)
                        i++
                    }
                }


                val connectionOk = BackendConnectionMonitor.getInstance(project).isConnectionOk()
                if (!connectionOk) {
                    Log.log(logger::warn, "no connection after engine installation")
                    if (success) {
                        ActivityMonitor.getInstance(project)
                            .registerDigmaEngineEventError("installEngine", "No connection after successful engine install")
                    }
                }
                val isEngineUp = connectionOk && success
                if (isEngineUp) {
                    Log.log(logger::warn, "engine is up, {}", exitValue)

                    showWizardCard()

                } else {
                    Log.log(logger::warn, "error installing engine, {}", exitValue)

                    showWizardCard()

                    //start remove if install failed. wait a second to let the installEngine finish and
                    // report installEngine.end to posthog
                    Backgroundable.runInNewBackgroundThread(project, "removing engine") {
                        try {
                            Thread.sleep(1000)
                        } catch (e: Exception) {
                            //ignore
                        }
                        Log.log(logger::warn, "removing engine after installation failed")
                        service<DockerService>().removeEngine(project) { exitValue ->
                            if (exitValue != "0") {
                                Log.log(logger::warn, "error removing engine after failure {}", exitValue)
                            }
                        }
                    }

                }
            }
        }
    }


    return panel
}
