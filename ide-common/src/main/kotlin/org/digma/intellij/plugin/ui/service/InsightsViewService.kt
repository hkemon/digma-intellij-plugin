package org.digma.intellij.plugin.ui.service

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.apache.commons.lang3.builder.EqualsBuilder
import org.digma.intellij.plugin.common.Backgroundable
import org.digma.intellij.plugin.common.IDEUtilsService
import org.digma.intellij.plugin.document.DocumentInfoContainer
import org.digma.intellij.plugin.insights.CodeLessSpanInsightsProvider
import org.digma.intellij.plugin.insights.CodelessSpanInsightsContainer
import org.digma.intellij.plugin.insights.InsightsListContainer
import org.digma.intellij.plugin.insights.InsightsProvider
import org.digma.intellij.plugin.log.Log
import org.digma.intellij.plugin.model.ModelChangeListener
import org.digma.intellij.plugin.model.discovery.CodeLessSpan
import org.digma.intellij.plugin.model.discovery.EndpointInfo
import org.digma.intellij.plugin.model.discovery.MethodInfo
import org.digma.intellij.plugin.model.rest.insights.InsightStatus
import org.digma.intellij.plugin.ui.MainToolWindowCardsController
import org.digma.intellij.plugin.ui.model.CodeLessSpanScope
import org.digma.intellij.plugin.ui.model.DocumentScope
import org.digma.intellij.plugin.ui.model.EmptyScope
import org.digma.intellij.plugin.ui.model.EndpointScope
import org.digma.intellij.plugin.ui.model.MethodScope
import org.digma.intellij.plugin.ui.model.UIInsightsStatus
import org.digma.intellij.plugin.ui.model.insights.InsightsModel
import org.digma.intellij.plugin.ui.model.insights.InsightsPreviewListItem
import org.digma.intellij.plugin.ui.model.insights.InsightsTabCard
import org.jetbrains.annotations.VisibleForTesting
import java.util.Collections
import java.util.concurrent.locks.ReentrantLock

//todo: delete
class InsightsViewService(project: Project) : AbstractViewService(project) {

    private val logger: Logger = Logger.getInstance(InsightsViewService::class.java)
    private val lock: ReentrantLock = ReentrantLock()

    //the model is single per the life of an open project in intellij. it shouldn't be created
    //elsewhere in the program. it can not be singleton.
    val model = InsightsModel()


    override fun getViewDisplayName(): String {
        return "Insights" + if (model.insightsCount > 0) " (${model.count()})" else ""
    }


    companion object {
        @JvmStatic
        fun getInstance(project: Project): InsightsViewService {
            return project.getService(InsightsViewService::class.java)
        }
    }


    //todo: quick and dirty prevent race condition with refresh task until we have time to re-write it
    fun updateInsightsModelFromRefresh(codeLessSpan: CodeLessSpan) {
        lock.lock()
        try {
            //don't let the refresh task update if it's not the same CodeLessSpan
            if ( model.scope is CodeLessSpanScope && codeLessSpan == (model.scope as CodeLessSpanScope).getSpan()) {
                updateInsightsModelImpl(codeLessSpan)
            }
        }finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }

    //todo: quick and dirty prevent race condition with refresh task until we have time to re-write it
    fun updateInsightsModel(codeLessSpan: CodeLessSpan) {
        lock.lock()
        try {
            updateInsightsModelImpl(codeLessSpan)
        }finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }

    private fun updateInsightsModelImpl(codeLessSpan: CodeLessSpan) {

        project.service<MainToolWindowCardsController>().showMainPanel()

        val codeLessInsightsProvider = CodeLessSpanInsightsProvider(codeLessSpan, project)

        Log.log(logger::debug, "updateInsightsModel to {}. ", codeLessSpan)

        val codelessSpanInsightsContainer: CodelessSpanInsightsContainer? = codeLessInsightsProvider.getInsights()

        val insightsContainer: InsightsListContainer? = codelessSpanInsightsContainer?.insightsContainer

        if (insightsContainer?.listViewItems.isNullOrEmpty()) {
            Log.log(logger::debug, project, "could not load insights for {}, see logs for details", codeLessSpan)
        }


        //todo: this is temporary, flickering happens because the UI is rebuilt on every refresh, when
        // UI components are changed to bind to models flickering should not happen and we can just
        // update the UI even with same data, it should be faster and more correct then deep equals of the data.
        //this is the way to prevent updating the UI if insights list didn't change between refresh
        // and by the way prevent flickering.
        //kotlin equality doesn't work for listViewItems because ListViewItem does not implement equals
        // so only the expensive reflectionEquals works here
        if (model.scope is CodeLessSpanScope &&
            (model.scope as CodeLessSpanScope).getSpan() == codeLessSpan &&
            EqualsBuilder.reflectionEquals(insightsContainer?.listViewItems, model.listViewItems)
        ) {
            return
        }


        model.listViewItems = insightsContainer?.listViewItems ?: listOf()
        model.previewListViewItems = ArrayList()
        model.scope = CodeLessSpanScope(codeLessSpan, codelessSpanInsightsContainer?.insightsResponse?.spanInfo)
        model.insightsCount = insightsContainer?.count ?: 0
        model.card = InsightsTabCard.INSIGHTS
        model.status = UIInsightsStatus.Default

        if (model.listViewItems.isEmpty()) {
            model.status = UIInsightsStatus.NoInsights
        }


        notifyModelChangedAndUpdateUi()

    }

    //todo: quick and dirty prevent race condition with refresh task until we have time to re-write it
    fun updateInsightsModelFromRefresh(methodInfo: MethodInfo) {
        lock.lock()
        try {
            //don't let the refresh task update if it's not the same MethodInfo
            if ( model.scope is MethodScope && methodInfo == (model.scope as MethodScope).getMethodInfo()) {
                updateInsightsModelImpl(methodInfo, null)
            }
        }finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }

    //todo: quick and dirty prevent race condition with refresh task until we have time to re-write it
    fun updateInsightsModel(methodInfo: MethodInfo, endpointInfo: EndpointInfo? = null) {
        lock.lock()
        try {
            updateInsightsModelImpl(methodInfo, endpointInfo)
        }finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
        }
    }

    private fun updateInsightsModelImpl(methodInfo: MethodInfo, endpointInfo: EndpointInfo?) {
        val insightsProvider: InsightsProvider = project.getService(InsightsProvider::class.java)
        updateInsightsModelWithInsightsProvider(methodInfo, insightsProvider, endpointInfo)
    }

    private fun updateInsightsModelWithInsightsProvider(methodInfo: MethodInfo, insightsProvider: InsightsProvider, endpointInfo: EndpointInfo?) {
        lock.lock()
        Log.log(logger::trace, "Lock acquired for updateInsightsModel to {}. ", methodInfo)
        try {
            Log.log(logger::trace, "updateInsightsModel to {}. ", methodInfo)

            val insightsListContainer = insightsProvider.getCachedInsights(methodInfo)

            model.listViewItems = insightsListContainer.listViewItems ?: listOf()
            model.previewListViewItems = ArrayList()
            model.scope = if (endpointInfo == null) MethodScope(methodInfo) else EndpointScope(endpointInfo)
            model.insightsCount = insightsListContainer.count
            model.card = InsightsTabCard.INSIGHTS


            if (model.listViewItems.isNotEmpty()) {
                model.status = UIInsightsStatus.Default
            } else {
                model.status = UIInsightsStatus.Loading
                Log.log(logger::debug, "No insights for method {}, Starting background thread.", methodInfo.name)
                Backgroundable.runInNewBackgroundThread(project, "Fetching insights status for method ${methodInfo.name}") {

                    Log.log(logger::debug, "Loading backend status in background for method {}", methodInfo.name)
                    val insightStatus = insightsProvider.getInsightStatus(methodInfo)
                    Log.log(logger::debug, "Got status from backend {} for method {}", insightStatus, methodInfo.name)
                    model.status = toUiInsightStatus(insightStatus, methodInfo.hasRelatedCodeObjectIds())

                    Log.log(logger::debug, "UIInsightsStatus for method {} is {}", methodInfo.name, model.status)

                    notifyModelChangedAndUpdateUi()
                }
            }

            notifyModelChangedAndUpdateUi()

        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
                Log.log(logger::trace, "Lock released for updateInsightsModel to {}. ", methodInfo)
            }
        }
    }


    @VisibleForTesting
    fun toUiInsightStatus(status: InsightStatus?, methodHasRelatedCodeObjectIds: Boolean): UIInsightsStatus {
        //no need for else branch, all possible values are handled
        return when (status) {
            InsightStatus.InsightExist -> UIInsightsStatus.InsightPending
            InsightStatus.InsightPending -> UIInsightsStatus.InsightPending
            InsightStatus.NoSpanData,null -> {
                return if (methodHasRelatedCodeObjectIds) {
                    UIInsightsStatus.NoSpanData // the client(this plugin) is aware of code objects, but server is not (yet)
                } else {
                    if (IDEUtilsService.getInstance(project).isJavaProject) {
                        UIInsightsStatus.NoObservability
                    } else {
                        UIInsightsStatus.NoInsights
                    }
                }
            }
        }
    }

    fun contextChangeNoMethodInfo(dummy: MethodInfo) {

        Log.log(logger::trace, "contextChangeNoMethodInfo to {}. ", dummy)

        model.listViewItems = ArrayList()
        model.previewListViewItems = ArrayList()
        model.scope = MethodScope(dummy)
        model.insightsCount = 0
        model.card = InsightsTabCard.INSIGHTS
        model.status = UIInsightsStatus.NoInsights

        notifyModelChangedAndUpdateUi()

    }


    /**
     * empty should be called only when there is no file opened in the editor and not in
     * any other case.
     */
    @Deprecated("for removal")
    fun empty() {

        //Note: we do not empty the model anymore

//        Log.log(logger::debug, "empty called")
//
//        model.listViewItems = ArrayList()
//        model.previewListViewItems = ArrayList()
//        model.usageStatusResult = EmptyUsageStatusResult
//        model.scope = EmptyScope("")
//        model.insightsCount = 0
//        model.card = InsightsTabCard.INSIGHTS
//        //when empty set Default status, empty editor should be covered by MainToolWindowCardsController
//        model.status = UIInsightsStatus.Default
//
//        notifyModelChangedAndUpdateUi()

    }

    @Deprecated("for removal")
    fun emptyNonSupportedFile(fileUri: String) {

        //Note: we do not empty the model anymore

//        Log.log(logger::debug, "empty called")
//
//        model.listViewItems = ArrayList()
//        model.previewListViewItems = ArrayList()
//        model.usageStatusResult = EmptyUsageStatusResult
//        model.scope = EmptyScope(getNonSupportedFileScopeMessage(fileUri))
//        model.insightsCount = 0
//        model.card = InsightsTabCard.INSIGHTS
//        //when non supported file set Default status, non-supported file should be covered by MainToolWindowCardsController
//        model.status = UIInsightsStatus.Default
//
//        notifyModelChangedAndUpdateUi()

    }

    fun showDocumentPreviewList(
        documentInfoContainer: DocumentInfoContainer?,
        fileUri: String,
    ) {


        Log.log(logger::trace, "showDocumentPreviewList for {}. ", fileUri)

        if (documentInfoContainer == null) {
            model.previewListViewItems = ArrayList()
            model.listViewItems = ArrayList()
            model.scope = EmptyScope(fileUri.substringAfterLast('/'))
            model.insightsCount = 0
            model.card = InsightsTabCard.PREVIEW
            model.status = UIInsightsStatus.NoInsights
        } else {
            model.previewListViewItems = getDocumentPreviewItems(documentInfoContainer)
            model.listViewItems = ArrayList()
            model.scope = DocumentScope(documentInfoContainer.documentInfo)
            model.insightsCount = computeInsightsPreviewCount(documentInfoContainer)
            model.card = InsightsTabCard.PREVIEW
            model.status = UIInsightsStatus.Default
            if (!model.hasInsights()) {
                if (model.hasDiscoverableCodeObjects()) {
                    model.status = UIInsightsStatus.NoSpanData
                } else {
                    model.status = UIInsightsStatus.NoInsights
                }
            }
        }

        notifyModelChangedAndUpdateUi()

    }


    private fun computeInsightsPreviewCount(documentInfoContainer: DocumentInfoContainer): Int {
        return documentInfoContainer.insightsCount
    }

    private fun getDocumentPreviewItems(documentInfoContainer: DocumentInfoContainer): List<InsightsPreviewListItem> {

        val listViewItems = ArrayList<InsightsPreviewListItem>()
        documentInfoContainer.documentInfo.methods.forEach { (id, methodInfo) ->
            listViewItems.add(
                InsightsPreviewListItem(
                    methodInfo.id,
                    documentInfoContainer.hasInsights(id),
                    methodInfo.getRelatedCodeObjectIds().any()
                )
            )
        }

        //sort by name of the function, it will be sorted later by sortIndex when added to a PanelListModel, but
        // because they all have the same sortIndex then positions will not change
        Collections.sort(listViewItems, Comparator.comparing { it.name })
        return listViewItems

    }

    fun refreshInsightsModel() {
        val scope = model.scope
        if (scope is MethodScope) {
            Backgroundable.ensureBackground(project, "Refresh insights list") {
                updateInsightsModel(scope.getMethodInfo())
            }
        }
    }

    private fun notifyModelChanged() {
        Log.log(logger::trace, "Firing ModelChange event for {}", model)
        if (project.isDisposed) {
            return
        }
        val publisher = project.messageBus.syncPublisher(ModelChangeListener.MODEL_CHANGED_TOPIC)
        publisher.modelChanged(model)
    }

    fun notifyModelChangedAndUpdateUi() {
        notifyModelChanged()
        updateUi()
    }


    //the insights panel is not used anymore
    override fun canUpdateUI(): Boolean {
        return false
    }

    override fun updateUi() {
        //do nothing, not used anymore
    }

}