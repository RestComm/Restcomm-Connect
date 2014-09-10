package org.mobicents.servlet.restcomm.rvd.project.exceptions;

import org.mobicents.servlet.restcomm.rvd.model.client.ProjectState;
import org.mobicents.servlet.restcomm.rvd.project.RvdProject;

public class UssdProject extends RvdProject {

    public UssdProject(String name, ProjectState projectState) {
        super(name, projectState);
    }

    @Override
    public boolean supportsWavs() {
        return false;
    }

}
