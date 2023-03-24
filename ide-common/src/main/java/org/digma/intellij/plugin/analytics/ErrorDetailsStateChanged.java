package org.digma.intellij.plugin.analytics;

import com.intellij.util.messages.Topic;

public interface ErrorDetailsStateChanged {

    @Topic.ProjectLevel
    Topic<ErrorDetailsStateChanged> ERROR_DETAILS_STATE_TOPIC = Topic.create("ERROR_DETAILS_STATE_TOPIC", ErrorDetailsStateChanged.class);

    void errorDetailsStateChanged(boolean errorDetailsOn);

}
