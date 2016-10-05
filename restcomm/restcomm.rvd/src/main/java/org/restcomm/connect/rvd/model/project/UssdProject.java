package org.restcomm.connect.rvd.model.project;

import org.restcomm.connect.rvd.model.client.ProjectState;

public class UssdProject extends RvdProject {

    public UssdProject(String name, ProjectState projectState) {
        super(name, projectState);
    }

    @Override
    public boolean supportsWavs() {
        return false;
    }

}
