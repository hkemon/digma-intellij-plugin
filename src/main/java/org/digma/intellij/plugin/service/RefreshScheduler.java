package org.digma.intellij.plugin.service;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.util.messages.MessageBusConnection;
import org.apache.commons.collections4.CollectionUtils;
import org.digma.intellij.plugin.document.DocumentInfoContainer;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.digma.intellij.plugin.model.rest.insights.CodeObjectInsight;
import org.digma.intellij.plugin.toolwindow.ToolWindowId;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RefreshScheduler {

    private final Logger LOGGER = Logger.getInstance(RefreshScheduler.class);

    private final Project project;

    private final DocumentInfoService documentInfoService;

    private final MessageBusConnection toolWindowConnection;

    private ScheduledExecutorService scheduledExecutorService;

    private ScheduledFuture currentTask;

    public RefreshScheduler(Project project) {
        this.project = project;
        this.documentInfoService = project.getService(DocumentInfoService.class);

        toolWindowConnection = project.getMessageBus().connect();
        toolWindowConnection.subscribe(ToolWindowManagerListener.TOPIC, new ToolWindowManagerListener() {
            @Override
            public void toolWindowShown(@NotNull ToolWindow toolWindow) {
                if (toolWindow.getId().equals(ToolWindowId.MAIN_TOOL_WINDOW_ID)) {
                    startScheduler();
                }
            }

            @Override
            public void stateChanged(@NotNull ToolWindowManager toolWindowManager, @NotNull ToolWindowManagerEventType changeType) {

                var ourToolWindow = toolWindowManager.getToolWindow(ToolWindowId.MAIN_TOOL_WINDOW_ID);
                if (ourToolWindow == null) {
                    return;
                }

                switch (changeType) {
                    case HideToolWindow:
                    case UnregisterToolWindow: {
                        stopScheduler();
                        break;
                    }
                    case ActivateToolWindow:
                    case RegisterToolWindow:
                    case ShowToolWindow: {
                        startScheduler();
                        break;
                    }
                    default: {
                        Log.log(LOGGER::debug, "Unhandled change type {}", changeType);
                    }
                }
            }
        });
    }


    private synchronized void stopScheduler() {
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdown();
            try {
                var terminated = scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS);

                if (!terminated) {
                    scheduledExecutorService.shutdownNow();
                }

            } catch (InterruptedException ignored) { //no need here to re-interrupt
                if (!scheduledExecutorService.isTerminated()) {
                    scheduledExecutorService.shutdownNow();
                }
            }
            scheduledExecutorService = null;
        }
    }

    private synchronized void startScheduler() {

        if (scheduledExecutorService == null) {
            scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(getThreadFactory());
        }
    }


    public ScheduledFuture addRefreshTask(MethodUnderCaret methodUnderCaret, Runnable refreshAction) {
        Objects.requireNonNull(scheduledExecutorService, "scheduledExecutorService should not be null");

        ScheduledFuture previousTask = null;
        if (currentTask != null) {
            previousTask = currentTask;
            currentTask.cancel(true);
        }

        //if MethodInfo not found then there is no need to add a refresh task. it is probably a non-supported file
        // or empty methodUnderCaret
        MethodInfo methodInfo = documentInfoService.getMethodInfo(methodUnderCaret);
        if (methodInfo == null) {
            return previousTask;
        }

        currentTask = scheduledExecutorService.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {


                DocumentInfoContainer documentInfoContainer = documentInfoService.getDocumentInfoByMethodInfo(methodInfo);
                var oldInsights = documentInfoContainer.getAllInsights();
                if (documentInfoContainer != null) {
                    if (Thread.interrupted()) {
                        return;
                    }
                    documentInfoContainer.updateCache();
                    if (Thread.interrupted()) {
                        return;
                    }
                    var newInsights = documentInfoContainer.getAllInsights();
                    //todo: the comparison is disabled for the demonstration
//                    if (dataChanged(oldInsights,newInsights)) {

                        if (Thread.interrupted()) {
                            return;
                        }
                        refreshAction.run();
//                    }
                }
            }
        }, 5, 5, TimeUnit.SECONDS);

        return previousTask;
    }

    private boolean dataChanged(List<CodeObjectInsight> oldInsights, List<CodeObjectInsight> newInsights) {

        //No need for comparator because CodeObjectInsight are kotlin data classes, they have hashCode and equals
        return !CollectionUtils.isEqualCollection(oldInsights, newInsights);
    }



    private ThreadFactory getThreadFactory() {
        return new MyThreadFactory();
    }

    public void dispose() {
        toolWindowConnection.dispose();
    }


    private static class MyThreadFactory implements ThreadFactory {
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        MyThreadFactory() {
            group = Thread.currentThread().getThreadGroup();
            namePrefix = "digma-refresh-thread-";
        }

        public Thread newThread(@NotNull Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            t.setDaemon(true);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }









}
