package org.digma.intellij.plugin.persistence

data class PersistenceData(
    var currentEnv: String? = null,
    var isWorkspaceOnly: Boolean = false,
    var isAutoOtel: Boolean = true,
    var alreadyPassedTheInstallationWizardForIdeaIDE: Boolean = false,
    var alreadyPassedTheInstallationWizardForRiderIDE: Boolean = false,
    var alreadyPassedTheInstallationWizardForPyCharmIDE: Boolean = false,
    var firstTimeConnectionEstablished: Boolean = false,
    var firstTimeConnectionEstablishedTimestamp: String? = null,
    var firstWizardLaunch: Boolean = true,
    var firstTimeInsightReceived: Boolean = false,
    var firstTimeAssetsReceived: Boolean = false,
    var firstTimeRecentActivityReceived: Boolean = false,
    var userEmail: String? = null,
    var userRegistrationEmail: String? = null,
    var isLocalEngineInstalled: Boolean? = null,
    var isFirstTimePluginLoaded: Boolean = false,
    var userId: String? = null,
    var firstTimePerformanceMetrics: Boolean = false,
    var lastInsightsEventTime: String? = null,
    var noInsightsYetNotificationPassed: Boolean = false,
    var notificationsStartDate: String? = null,

    var pendingEnvironment: String? = null,
    var selectedServices: MutableMap<String, Array<String>> = HashMap(),
)
