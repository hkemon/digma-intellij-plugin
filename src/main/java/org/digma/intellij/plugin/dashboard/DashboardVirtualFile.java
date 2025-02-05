package org.digma.intellij.plugin.dashboard;

import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightVirtualFile;
import org.digma.intellij.plugin.common.DigmaVirtualFileMarker;
import org.jetbrains.annotations.NotNull;

public class DashboardVirtualFile extends LightVirtualFile implements DigmaVirtualFileMarker {

    public static final Key<String> DASHBOARD_EDITOR_KEY = Key.create("Digma.DASHBOARD_EDITOR_KEY");
    private String dashboardEnvId;
    public DashboardVirtualFile(String myTitle) {
        super(myTitle);
        setFileType(DashboardFileType.INSTANCE);
        setWritable(false);
        putUserData(FileEditorManagerImpl.FORBID_PREVIEW_TAB, true);
    }

    public static boolean isDashboardVirtualFile(@NotNull VirtualFile file) {
        return file instanceof DashboardVirtualFile;
    }

    @NotNull
    public static VirtualFile createVirtualFile(@NotNull String dashboardName) {
        var file = new DashboardVirtualFile(dashboardName);
        file.setDashboardEnvId(dashboardName);
        DASHBOARD_EDITOR_KEY.set(file, DashboardFileEditorProvider.DASHBOARD_EDITOR_TYPE);
        return file;
    }

    public void setDashboardEnvId(String documentationPage) {
        this.dashboardEnvId = documentationPage;
    }

    public String getDashboardEnvId() {
        return dashboardEnvId;
    }
}
