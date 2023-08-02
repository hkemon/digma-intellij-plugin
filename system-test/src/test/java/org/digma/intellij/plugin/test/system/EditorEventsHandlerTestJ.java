package org.digma.intellij.plugin.test.system;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.Backend;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ComponentManager;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.digma.intellij.plugin.analytics.AnalyticsProvider;
import org.digma.intellij.plugin.analytics.AnalyticsService;
import org.digma.intellij.plugin.analytics.BackendConnectionMonitor;
import org.digma.intellij.plugin.analytics.BackendConnectionUtil;
import org.digma.intellij.plugin.analytics.RestAnalyticsProvider;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.psi.LanguageService;
import org.digma.intellij.plugin.psi.LanguageServiceLocator;
import org.junit.Test;
import retrofit2.Retrofit;
import retrofit2.mock.MockRetrofit;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EditorEventsHandlerTestJ extends LightJavaCodeInsightFixtureTestCase {


    private static final Logger LOGGER = Logger.getInstance(EditorEventsHandlerTestJ.class);
    private static MockWebServer mockBackEnd = new MockWebServer();

    @Override
    protected String getTestDataPath() {
        return "src/test/resources";
    }

    private ObjectMapper objectMapper = new ObjectMapper();
    private String url;

    private Retrofit retrofit;
    private MockRetrofit mockRetrofit;


//    @BeforeClass
//    public void setupMocks(){
//        mockAnalyticsService = mock(AnalyticsService.class);
//
//        List<String> testEnvironments = List.of(new String[]{"env1", "env2"});
//        when(mockAnalyticsService.getEnvironments()).thenReturn(testEnvironments);
//    }


    @Override
    public void tearDown() throws Exception {
//        mockBackEnd.shutdown();
        super.tearDown();
    }

    @Override
    public void setUp() throws Exception {

//        mockBackEnd.start(5051);
//        initialize("");
////        enqueueGetEnvResponse();
//        mockBackEnd.setDispatcher(new DispatcherMock());
        super.setUp();

    }


    public void initialize(String extensionUrl) {
//        url = mockBackEnd.url("/" + extensionUrl).toString();
//        Log.log(LOGGER::warn, "url: {}", url);
    }

    private List<String> enqueueGetEnvResponse() throws JsonProcessingException {
        List<String> expectedEnvs = new ArrayList<>();
        expectedEnvs.add("env1");
        expectedEnvs.add("env2");
        MockResponse response = new MockResponse()
                .setBody(objectMapper.writeValueAsString(expectedEnvs))
                .addHeader("Content-Type", "application/json");
        mockBackEnd.enqueue(response);
        return expectedEnvs;
    }

    public void testGetEnv() throws JsonProcessingException {
        initialize("");
        List<String> expectedEnvs = new ArrayList<>();
        expectedEnvs.add("env1");
        expectedEnvs.add("env2");


//        Log.log(LOGGER::debug, "url: {}", url);
//        AnalyticsProvider restAnalyticsProvider = new RestAnalyticsProvider(url);
//        List<String> envsResult = restAnalyticsProvider.getEnvironments();

        AnalyticsService analyticsService1 = getProject().getService(AnalyticsService.class);
        List<String> envsResult = analyticsService1.getEnvironments();
        assertSize(expectedEnvs.size(), envsResult);
        assertTrue(envsResult.containsAll(expectedEnvs));
        assertIterableEquals(expectedEnvs, envsResult, "unexpected environments result");
    }

    public void testGetEnvAndGetInsights() {

    }

    public void testProjectPathFiles() {
        Project project = getProject();
        String basePath = project.getBasePath();
        System.out.println("Project path: " + basePath);
        String projectFilePath = project.getProjectFilePath();
        System.out.println("Project files: " + projectFilePath);

        assertNotNull(basePath);
        assertNotNull(projectFilePath);
    }

    @Test
    public void testPsiFileNotEmpty() {
        PsiFile psiFile = myFixture.configureByFile("EditorEventsHandler.java");
        String testDirFixture = myFixture.getTempDirFixture().getTempDirPath();

        Project project = getProject();
        LanguageServiceLocator languageServiceLocator = project.getService(LanguageServiceLocator.class);
//        project.setService(LanguageServiceLocator.class, languageServiceLocator);

        LanguageService languageService = languageServiceLocator.locate(psiFile.getLanguage());
        Log.log(LOGGER::debug, "Found language service {} for :{}", languageService, psiFile);

        DocumentInfo documentInfo = languageService.buildDocumentInfo(psiFile);
        Log.log(LOGGER::debug, "got DocumentInfo for :{}", psiFile);
    }

    public void testMockServiceRegistration() {
//        initialize();
//        Log.log(LOGGER::info, "local url: {}", url);
//        List<String> environments = analyticsService.getEnvironments();
//        Log.log(LOGGER::info, "environments: {}", environments);

//        assertEquals(analyticsService, FromProjectServiceContainer);
    }

}
