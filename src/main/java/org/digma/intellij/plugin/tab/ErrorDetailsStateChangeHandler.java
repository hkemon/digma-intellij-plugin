package org.digma.intellij.plugin.tab;

import org.digma.intellij.plugin.analytics.ErrorDetailsStateChanged;

/**
 * The central handler of ErrorDetailsStateChanged events.
 * it will perform the necessary actions that are common to all languages or IDEs.
 */
public class ErrorDetailsStateChangeHandler implements ErrorDetailsStateChanged {

    @Override
    public void errorDetailsStateChanged(boolean errorDetailsOn) {
        //nothing to do here
    }
}
