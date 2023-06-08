package org.digma.intellij.plugin.icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

import static org.digma.intellij.plugin.icons.IconsUtil.loadAndScaleIconObjectByWidth;

/**
 * put here icons that usually load before our tool window and UI is built
 */
public final class AppIcons {

    private AppIcons() {
    }

    public static final Icon LOGO = IconLoader.getIcon("/icons/digma.png",AppIcons.class.getClassLoader());
    public static final Icon TOOL_WINDOW = loadAndScaleIconObjectByWidth("/icons/digma.png", 13);


}
