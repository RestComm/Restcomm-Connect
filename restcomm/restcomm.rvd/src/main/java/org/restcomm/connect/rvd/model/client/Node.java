package org.restcomm.connect.rvd.model.client;

import java.util.ArrayList;
import java.util.List;

public class Node {

    private String name;
    private String label;
    private String kind;
    private List<Step> steps;

    public Node() {
        // TODO Auto-generated constructor stub
    }

    public static Node createDefault(String kind, String name, String label) {
        Node node = new Node();
        node.setName(name);
        node.setLabel(label);
        node.setKind(kind);
        List<Step> steps = new ArrayList<Step>();
        node.setSteps(steps);

        return node;
    }

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

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }
}
