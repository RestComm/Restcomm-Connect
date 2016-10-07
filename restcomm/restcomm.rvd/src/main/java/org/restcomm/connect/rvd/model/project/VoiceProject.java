package org.restcomm.connect.rvd.model.project;

import org.restcomm.connect.rvd.model.client.ProjectState;

public class VoiceProject extends RvdProject{

    public VoiceProject(String name, ProjectState projectState) {
        super(name, projectState);
    }

    @Override
    public boolean supportsWavs() {
        return true;
    }

}
