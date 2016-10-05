package org.restcomm.connect.rvd.model.client;

import java.util.ArrayList;
import java.util.List;

import org.restcomm.connect.rvd.RvdConfiguration;
import org.restcomm.connect.rvd.model.steps.say.SayStep;
import org.restcomm.connect.rvd.model.steps.sms.SmsStep;
import org.restcomm.connect.rvd.model.steps.ussdsay.UssdSayStep;

public class ProjectState {

    private Integer lastStepId;
    private List<Node> nodes;
    private Integer activeNode;
    private Integer lastNodeId;
    private StateHeader header;
    private ExceptionHandlingInfo exceptionHandlingInfo;


    public ProjectState() {
        super();
    }

    public static ProjectState createEmptyVoice(String owner) {
        String kind = "voice";
        ProjectState state = new ProjectState();

        StateHeader header = new StateHeader();
        header.owner = owner;
        header.projectKind = kind;
        header.version = RvdConfiguration.getRvdProjectVersion();
        header.startNodeName = "start";
        state.setHeader(header);

        List<Node> nodes = new ArrayList<Node>();
        Node node = Node.createDefault("voice", "start", "Welcome");
        SayStep step = SayStep.createDefault("step1", "Welcome to Telestax Restcom Visual Designer Demo");
        node.getSteps().add(step);
        nodes.add(node);
        state.setNodes(nodes);

        state.setLastStepId(1);
        state.setLastNodeId(0);

        return state;
    }

    public static ProjectState createEmptySms(String owner) {
        String kind = "sms";
        ProjectState state = new ProjectState();

        StateHeader header = new StateHeader();
        header.owner = owner;
        header.projectKind = kind;
        header.version = RvdConfiguration.getRvdProjectVersion();
        header.startNodeName = "start";
        state.setHeader(header);

        List<Node> nodes = new ArrayList<Node>();
        Node node = Node.createDefault("sms", "start", "Welcome");
        SmsStep step = SmsStep.createDefault("step1", "Welcome to Telestax Restcom Visual Designer");
        node.getSteps().add(step);
        nodes.add(node);
        state.setNodes(nodes);

        state.setLastStepId(1);
        state.setLastNodeId(0);

        return state;
    }

    public static ProjectState createEmptyUssd(String owner) {
        String kind = "ussd";
        ProjectState state = new ProjectState();

        StateHeader header = new StateHeader();
        header.owner = owner;
        header.projectKind = kind;
        header.version = RvdConfiguration.getRvdProjectVersion();
        header.startNodeName = "start";
        state.setHeader(header);

        List<Node> nodes = new ArrayList<Node>();
        Node node = Node.createDefault("ussd", "start", "Welcome");
        UssdSayStep step = UssdSayStep.createDefault("step1", "Welcome to Telestax Restcom Visual Designer");
        node.getSteps().add(step);
        nodes.add(node);
        state.setNodes(nodes);

        state.setLastStepId(1);
        state.setLastNodeId(0);

        return state;
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

    public StateHeader getHeader() {
        return header;
    }

    public void setHeader(StateHeader header) {
        this.header = header;
    }

    public ExceptionHandlingInfo getExceptionHandlingInfo() {
        return exceptionHandlingInfo;
    }



}
