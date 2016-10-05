package org.restcomm.connect.rvd.model.server;

import java.util.ArrayList;
import java.util.List;

public class ProjectOptions {

    private String defaultTarget;
    private List<NodeName> nodeNames = new ArrayList<NodeName>();
    private Boolean logging;

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

    public Boolean getLogging() {
        return logging;
    }

    public void setLogging(Boolean logging) {
        this.logging = logging;
    }
}
