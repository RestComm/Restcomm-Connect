package org.mobicents.servlet.restcomm.rvd.model.server;

import java.util.ArrayList;
import java.util.List;

public class ProjectOptions {

    private String defaultTarget;
    private List<NodeName> nodeNames = new ArrayList<NodeName>();

    public String getDefaultTarget() {
        return defaultTarget;
    }

    public void setDefaultTarget(String defaultTarget) {
        this.defaultTarget = defaultTarget;
    }
    public List<NodeName> getNodeNames() {
        return nodeNames;
    }

    public void setNodeNames(List<NodeName> nodeNames) {
        this.nodeNames = nodeNames;
    }
}
