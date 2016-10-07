package org.restcomm.connect.rvd.model.client;

// Used for transferring the current project information (only name is used) back to the client
public class ActiveProjectInfo {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
