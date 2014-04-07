package org.mobicents.servlet.restcomm.rvd.model.client;

import java.util.List;
import java.util.Map;

public class ProjectState {

    private String version;
    private String projectKind;
    private String startNodeName;
    private Integer lastStepId;
    private List<Node> nodes;
    private Integer activeNode;
    private Integer lastNodeId;
    private StateHeader header;


    public static class Node {
        private String name;
        private String label;
        private Map<String, Step> steps;
        private List<String> stepnames;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Map<String, Step> getSteps() {
            return steps;
        }

        public void setSteps(Map<String, Step> steps) {
            this.steps = steps;
        }

        public List<String> getStepnames() {
            return stepnames;
        }

        public void setStepnames(List<String> stepnames) {
            this.stepnames = stepnames;
        }
    }


    public String getProjectKind() {
        return projectKind;
    }

    public void setProjectKind(String projectKind) {
        this.projectKind = projectKind;
    }

    public String getStartNodeName() {
        return startNodeName;
    }

    public void setStartNodeName(String startNodeName) {
        this.startNodeName = startNodeName;
    }

    public Integer getLastStepId() {
        return lastStepId;
    }

    public void setLastStepId(Integer lastStepId) {
        this.lastStepId = lastStepId;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

    public Integer getActiveNode() {
        return activeNode;
    }

    public void setActiveNode(Integer activeNode) {
        this.activeNode = activeNode;
    }

    public Integer getLastNodeId() {
        return lastNodeId;
    }

    public void setLastNodeId(Integer lastNodeId) {
        this.lastNodeId = lastNodeId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public StateHeader getHeader() {
        return header;
    }

}
