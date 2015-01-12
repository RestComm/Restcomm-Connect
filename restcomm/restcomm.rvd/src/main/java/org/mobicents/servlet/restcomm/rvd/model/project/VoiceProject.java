package org.mobicents.servlet.restcomm.rvd.model.project;

import org.mobicents.servlet.restcomm.rvd.model.client.ProjectState;

public class VoiceProject extends RvdProject{

    public VoiceProject(String name, ProjectState projectState) {
        super(name, projectState);
    }

    @Override
    public boolean supportsWavs() {
        return true;
    }

}
