package org.mobicents.servlet.restcomm.rvd.model.client;

import java.util.List;

public class ProjectState {

    private Integer lastStepId;
    private List<Node> nodes;
    private Integer activeNode;
    private Integer lastNodeId;
    private StateHeader header;


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

    public StateHeader getHeader() {
        return header;
    }

}
