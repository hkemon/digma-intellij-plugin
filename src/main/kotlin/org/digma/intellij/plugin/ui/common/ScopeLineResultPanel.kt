package org.digma.intellij.plugin.ui.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.launchBackground
import com.intellij.openapi.ui.DialogPanel
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.ui.JBUI
import com.jetbrains.rd.util.lifetime.LifetimeDefinition
import org.digma.intellij.plugin.common.modelChangeListener.ModelChangeListener
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.ui.model.PanelModel
import org.digma.intellij.plugin.ui.model.errors.ErrorsModel
import org.digma.intellij.plugin.ui.model.insights.InsightsModel
import org.digma.intellij.plugin.ui.panels.DigmaTabPanel
import org.digma.intellij.plugin.ui.service.TabsHelper
import java.util.concurrent.locks.ReentrantLock
import javax.swing.BoxLayout
import javax.swing.JComponent

class ScopeLineResultPanel(
        project: Project,
        model: PanelModel,
): DigmaTabPanel(), Disposable {
    private val logger: Logger = Logger.getInstance(ScopeLineResultPanel::class.java)

    private val modelChangeConnection: MessageBusConnection = project.messageBus.connect()
    private val project: Project
    private val model: PanelModel
    private val rebuildPanelLock = ReentrantLock()
    private var scopeLine: DialogPanel? = null
    private val tabsHelper: TabsHelper

    init {
        modelChangeConnection.subscribe(
                ModelChangeListener.MODEL_CHANGED_TOPIC,
                ModelChangeListener { newModel -> rebuildInBackground(newModel) }
        )
        this.project = project
        this.tabsHelper = project.getService(TabsHelper::class.java)
        this.model = model
        this.layout = BoxLayout(this, BoxLayout.LINE_AXIS)
        this.border = JBUI.Borders.emptyLeft(7)
        this.background = Laf.Colors.EDITOR_BACKGROUND
        this.isOpaque = true

        rebuildInBackground(model)
    }

    override fun dispose() {
        modelChangeConnection.dispose()
    }

    override fun getPreferredFocusableComponent(): JComponent {
        return this
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return this
    }

    override fun reset() {
        rebuildInBackground(model)
    }

    private fun rebuildInBackground(model: PanelModel) {
        val lifetimeDefinition = LifetimeDefinition()
        lifetimeDefinition.lifetime.launchBackground {
            rebuildPanelLock.lock()
            Log.log(logger::debug, "Lock acquired for rebuild ScopeLineResultPanel process.")
            try {
                rebuild(model)
            } finally {
                rebuildPanelLock.unlock()
                Log.log(logger::debug, "Lock released for rebuild ScopeLineResultPanel process.")
                lifetimeDefinition.terminate()
            }
        }
    }

    private fun rebuild(model: PanelModel) {
        ApplicationManager.getApplication().invokeLater {
            removeExistingComponentsIfPresent()
            buildScopeLineResultPanelComponents(model)
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

    private fun buildScopeLineResultPanelComponents(model: PanelModel) {
        if (project.isDisposed) {
            return
        }
        if (model is InsightsModel || model is ErrorsModel) {
            scopeLine = scopeLine({ model.getScope() }, { model.getScopeTooltip() }, ScopeLineIconProducer(model))
            scopeLine!!.isOpaque = false
            scopeLine!!.border = JBUI.Borders.empty(2, 4)
            this.add(scopeLine)
        }
    }
}