package org.mobicents.servlet.restcomm.rvd.model.client;

import java.util.List;

public class Node {

    public Node() {
        // TODO Auto-generated constructor stub
    }

    private String name;
    private String label;
    private List<Step> steps;
    //private List<String> stepnames;

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

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }


    /*public List<String> getStepnames() {
        return stepnames;
    }

    public void setStepnames(List<String> stepnames) {
        this.stepnames = stepnames;
    }*/
}
