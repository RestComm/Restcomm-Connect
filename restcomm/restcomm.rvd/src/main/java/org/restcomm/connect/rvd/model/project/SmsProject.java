package org.restcomm.connect.rvd.model.project;

import org.restcomm.connect.rvd.model.client.ProjectState;

public class SmsProject extends RvdProject {

    public SmsProject(String name, ProjectState projectState) {
        super(name, projectState);
    }

    @Override
    public boolean supportsWavs() {
        return false;
    }

}
