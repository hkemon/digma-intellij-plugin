package org.digma.intellij.plugin.rider.env;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.jetbrains.rdclient.util.idea.LifetimedProjectComponent;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.rider.protocol.CodeObjectHost;
import org.digma.intellij.plugin.rider.protocol.DocumentCodeObjectsListener;
import org.digma.intellij.plugin.rider.protocol.ElementUnderCaretDetector;

public class RiderEnvironmentChangedHandler extends LifetimedProjectComponent {

    private final Logger LOGGER = Logger.getInstance(RiderEnvironmentChangedHandler.class);

    private final CodeObjectHost codeObjectHost;
    private final DocumentCodeObjectsListener documentCodeObjectsListener;
    private final ElementUnderCaretDetector elementUnderCaretDetector;

    public RiderEnvironmentChangedHandler(Project project) {
        super(project);
        codeObjectHost = project.getService(CodeObjectHost.class);
        documentCodeObjectsListener = project.getService(DocumentCodeObjectsListener.class);
        elementUnderCaretDetector = project.getService(ElementUnderCaretDetector.class);
    }

    public void environmentChanged(String newEnv) {
        Log.log(LOGGER::debug, "Got environmentChanged {}", newEnv);

        //codeObjectHost should mainly clear code lens
        codeObjectHost.environmentChanged();
        //documentCodeObjectsListener will fire documentCodeObjectsChanged for each documents
        //in the protocol, that will cause a refresh of the code objects,summaries etc. and will eventually
        //trigger a MethodUnderCaret event
        documentCodeObjectsListener.environmentChanged();

        //trigger a refresh here, its necessary when connection is lost and regained so that contextChange will be called
        //and the view update.
        elementUnderCaretDetector.refresh();
    }


}

