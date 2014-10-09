package org.mobicents.servlet.restcomm.rvd.project;

import org.mobicents.servlet.restcomm.rvd.model.client.ProjectState;

public class SmsProject extends RvdProject {

    public SmsProject(String name, ProjectState projectState) {
        super(name, projectState);
    }

    @Override
    public boolean supportsWavs() {
        return false;
    }

}
