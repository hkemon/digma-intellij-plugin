package org.digma.intellij.plugin.toolwindow;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.jcef.*;
import org.apache.commons.io.IOUtils;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.callback.CefQueryCallback;
import org.cef.handler.CefMessageRouterHandlerAdapter;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor;
import org.digma.intellij.plugin.analytics.EnvironmentChanged;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.service.EditorService;
import org.digma.intellij.plugin.service.ErrorsActionsService;
import org.digma.intellij.plugin.ui.ToolWindowShower;
import org.digma.intellij.plugin.ui.errors.ErrorsTabKt;
import org.digma.intellij.plugin.ui.insights.InsightsTabKt;
import org.digma.intellij.plugin.ui.service.ErrorsViewService;
import org.digma.intellij.plugin.ui.service.InsightsViewService;
import org.digma.intellij.plugin.ui.service.SummaryViewService;
import org.digma.intellij.plugin.ui.service.ToolWindowTabsHelper;
import org.digma.intellij.plugin.ui.summary.SummaryTabKt;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Date;


/**
 * The main Digma tool window
 */
public class DigmaToolWindowFactory implements ToolWindowFactory {

    private static final Logger LOGGER = Logger.getInstance(DigmaToolWindowFactory.class);


    /**
     * this is the starting point of the plugin. this method is called when the tool window is opened.
     * before the window is opened there may be no reason to do anything, listen to events for example will be
     * a waste if the user didn't open the window. at least as much as possible, some extensions will be registered
     * but will do nothing if the plugin is not active.
     * after the plugin is active all listeners and extensions are installed and kicking until the IDE is closed.
     */
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        Log.log(LOGGER::debug, "createToolWindowContent for project  {}", project);

        var contentFactory = ContentFactory.getInstance();

        var toolWindowTabsHelper = project.getService(ToolWindowTabsHelper.class);
        toolWindowTabsHelper.setToolWindow(toolWindow);

        //initialize AnalyticsService early so the UI already can detect the connection status when created
        project.getService(AnalyticsService.class);


        Content contentToSelect = createInsightsTab(project, toolWindow, contentFactory, toolWindowTabsHelper);
        createErrorsTab(project, toolWindow, contentFactory, toolWindowTabsHelper);
        createSummaryTab(project, toolWindow, contentFactory);

        createJcefDemoTab(project, toolWindow, contentFactory);

        ErrorsActionsService errorsActionsService = project.getService(ErrorsActionsService.class);
        toolWindow.getContentManager().addContentManagerListener(errorsActionsService);


        project.getService(ToolWindowShower.class).setToolWindow(toolWindow);

        toolWindow.getContentManager().setSelectedContent(contentToSelect, true);


        new Task.Backgroundable(project, "Digma: update views") {
            //sometimes the views models are updated before the tool window is initialized.
            //it happens when files are re-opened early before the tool window, and CaretContextService.contextChanged
            //is invoked and updates the models.
            //SummaryViewService is also initialized before the tool window is opened, it will get the event when
            // the environment is loaded and will update its model but will not update the ui because the panel is
            // not initialized yet.
            //only at this stage the panels are constructed already. just calling updateUi() for all view services
            // will actually update the UI.
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                project.getService(InsightsViewService.class).updateUi();
                project.getService(ErrorsViewService.class).updateUi();
                project.getService(SummaryViewService.class).updateUi();
            }
        }.queue();


        //sometimes there is a race condition on startup, a contextChange is fired before method info is available.
        //calling environmentChanged will fix it
        BackendConnectionMonitor backendConnectionMonitor = project.getService(BackendConnectionMonitor.class);
        if (backendConnectionMonitor.isConnectionOk()) {
            Backgroundable.ensureBackground(project, "change environment", () -> {
                EnvironmentChanged publisher = project.getMessageBus().syncPublisher(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC);
                publisher.environmentChanged(project.getService(AnalyticsService.class).getEnvironment().getCurrent());
            });
        }

    }

    private void createJcefDemoTab(Project project, ToolWindow toolWindow, ContentFactory contentFactory) {

        if (!JBCefApp.isSupported()) {
            // Fallback to an alternative browser-less solution
            return;
        }


//        JBCefBrowserBuilder jbCefBrowserBuilder = new JBCefBrowserBuilder();
//        jbCefBrowserBuilder.setOffScreenRendering(false);
//        JBCefBrowser jbCefBrowser = jbCefBrowserBuilder.build();
        JBCefBrowser jbCefBrowser = new JBCefBrowser();


//        jbCefBrowser.loadURL("https://en.wikipedia.org/wiki/Main_Page");
//        jbCefBrowser.loadHTML(Files.readString(Path.of(getClass().getResource("test.html").toURI())));
        try {
            jbCefBrowser.loadHTML(IOUtils.resourceToString("/test.html", null));
//            jbCefBrowser.loadHTML(IOUtils.resourceToString("/binding_test.html",null));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        JBCefClient jbCefClient = jbCefBrowser.getJBCefClient();

        CefMessageRouter msgRouter = CefMessageRouter.create();
        msgRouter.addHandler(new CefMessageRouterHandlerAdapter() {
            @Override
            public boolean onQuery(CefBrowser browser, CefFrame frame, long queryId, String request, boolean persistent, CefQueryCallback callback) {

                if (request.startsWith("BindingTest")) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        project.getService(EditorService.class).openTestFile();
                    });
                    callback.success("OK from java, opening class OwnerController at " + new Date());
                    return true;
                }

                return false;
            }

            @Override
            public void onQueryCanceled(CefBrowser browser, CefFrame frame, long queryId) {
                super.onQueryCanceled(browser, frame, queryId);
            }
        }, true);

        jbCefClient.getCefClient().addMessageRouter(msgRouter);



        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        JButton showAlert = new ActionLink("Show Alert");
        showAlert.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                jbCefBrowser.getCefBrowser().executeJavaScript("alert('ExecuteJavaScript works!');",jbCefBrowser.getCefBrowser().getURL(),0);
            }
        });
        topPanel.add(showAlert, BorderLayout.WEST);

        JButton resetLabel = new ActionLink("Reset Label");
        resetLabel.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String script = "document.getElementById('label1').innerHTML = 'Empty';";
                jbCefBrowser.getCefBrowser().executeJavaScript(script,jbCefBrowser.getCefBrowser().getURL(),0);
            }
        });
        topPanel.add(resetLabel, BorderLayout.EAST);


        JPanel browserPanel = new JPanel();
        browserPanel.setLayout(new BorderLayout());
        browserPanel.add(jbCefBrowser.getComponent(), BorderLayout.CENTER);


        JPanel jcefDemoPanel = new JPanel();
        jcefDemoPanel.setLayout(new BorderLayout());
        jcefDemoPanel.add(topPanel, BorderLayout.NORTH);
        jcefDemoPanel.add(browserPanel, BorderLayout.CENTER);

        var jcefContent = contentFactory.createContent(jcefDemoPanel, "Jcef", false);
        jcefContent.setTabName("Jcef");

        toolWindow.getContentManager().addContent(jcefContent);
    }


    private static void createSummaryTab(@NotNull Project project, @NotNull ToolWindow toolWindow, ContentFactory contentFactory) {
        var summaryPanel = SummaryTabKt.summaryPanel(project);
        var summaryViewService = project.getService(SummaryViewService.class);
        summaryViewService.setPanel(summaryPanel);
        var summaryContent = contentFactory.createContent(summaryPanel, "Summary", false);
        summaryContent.setTabName(ToolWindowTabsHelper.SUMMARY_TAB_NAME);
        summaryContent.setPreferredFocusedComponent(summaryPanel::getPreferredFocusedComponent);
        summaryContent.setPreferredFocusableComponent(summaryPanel.getPreferredFocusableComponent());
        toolWindow.getContentManager().addContent(summaryContent);
        summaryViewService.setContent(toolWindow, summaryContent);
    }

    private static void createErrorsTab(@NotNull Project project, @NotNull ToolWindow toolWindow, ContentFactory contentFactory, ToolWindowTabsHelper toolWindowTabsHelper) {
        var errorsPanel = ErrorsTabKt.errorsPanel(project);
        var errorsViewService = project.getService(ErrorsViewService.class);
        errorsViewService.setPanel(errorsPanel);
        var errorsContent = contentFactory.createContent(errorsPanel, "Errors", false);
        errorsContent.setTabName(ToolWindowTabsHelper.ERRORS_TAB_NAME); //we use tab name as a key , changing the name will break the plugin
        errorsContent.setPreferredFocusedComponent(errorsPanel::getPreferredFocusedComponent);
        errorsContent.setPreferredFocusableComponent(errorsPanel.getPreferredFocusableComponent());
        toolWindow.getContentManager().addContent(errorsContent);
        errorsViewService.setContent(toolWindow, errorsContent);
        toolWindowTabsHelper.setErrorsContent(errorsContent);
    }


    @NotNull
    private static Content createInsightsTab(@NotNull Project project, @NotNull ToolWindow toolWindow, ContentFactory contentFactory, ToolWindowTabsHelper toolWindowTabsHelper) {
        var insightsPanel = InsightsTabKt.insightsPanel(project);
        var insightsViewService = project.getService(InsightsViewService.class);
        insightsViewService.setPanel(insightsPanel);
        var insightsContent = contentFactory.createContent(insightsPanel, "Insights", false);
        insightsContent.setTabName(ToolWindowTabsHelper.INSIGHTS_TAB_NAME);//we use tab name as a key , changing the name will break the plugin
        insightsContent.setPreferredFocusedComponent(insightsPanel::getPreferredFocusedComponent);
        insightsContent.setPreferredFocusableComponent(insightsPanel.getPreferredFocusableComponent());
        toolWindow.getContentManager().addContent(insightsContent);
        insightsViewService.setContent(toolWindow, insightsContent);
        toolWindowTabsHelper.setInsightsContent(insightsContent);
        return insightsContent;
    }
}
