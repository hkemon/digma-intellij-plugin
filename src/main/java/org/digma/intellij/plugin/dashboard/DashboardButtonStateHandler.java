package org.digma.intellij.plugin.dashboard;

import org.digma.intellij.plugin.analytics.DashboardButtonStateChanged;

/**
 * The central handler of DashboardButtonStateChanged events.
 * it will perform the necessary actions that are common to all languages or IDEs.
 */
public class DashboardButtonStateHandler implements DashboardButtonStateChanged {

    @Override
    public void dashboardButtonStateChanged(Boolean isEnabled) {
        //nothing to do here
    }
}
