package org.digma.intellij.plugin.analytics;

import com.intellij.util.messages.Topic;

public interface DashboardButtonStateChanged {

    @Topic.ProjectLevel
    Topic<DashboardButtonStateChanged> DASHBOARD_BUTTON_STATE_CHANGED_TOPIC = Topic.create("DASHBOARD_BUTTON_STATE_CHANGED_TOPIC", DashboardButtonStateChanged.class);

    void dashboardButtonStateChanged(Boolean isEnabled);

}
