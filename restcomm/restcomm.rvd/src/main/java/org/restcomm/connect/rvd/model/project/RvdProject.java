package org.restcomm.connect.rvd.model.project;

import org.restcomm.connect.rvd.RvdConfiguration;
import org.restcomm.connect.rvd.exceptions.IncompatibleProjectVersion;
import org.restcomm.connect.rvd.exceptions.RvdException;
import org.restcomm.connect.rvd.exceptions.project.InvalidProjectKind;
import org.restcomm.connect.rvd.model.StepJsonDeserializer;
import org.restcomm.connect.rvd.model.StepJsonSerializer;
import org.restcomm.connect.rvd.model.client.ProjectState;
import org.restcomm.connect.rvd.model.client.StateHeader;
import org.restcomm.connect.rvd.model.client.Step;
import org.restcomm.connect.rvd.storage.exceptions.BadProjectHeader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public abstract class RvdProject {

    String name;
    ProjectState state;

    public RvdProject(String name, ProjectState projectState) {
        this.name = name;
        this.state = projectState;
    }

    public static RvdProject fromJson(String name, String projectJson) throws RvdException {
        ProjectState state = toModel(projectJson);
        String kind = state.getHeader().getProjectKind();
        RvdProject project = null;
        if ( "voice".equals(kind) ) {
            project = new VoiceProject(name, state);
        } else
        if ( "sms".equals(kind) ) {
            project = new SmsProject(name, state);
        } else
        if ( "ussd".equals(kind) ) {
            project = new UssdProject(name, state);
        } else {
            throw new InvalidProjectKind("Can't create project " + name +". Unknown project kind: " + kind);
        }
        return project;
    }

    public static ProjectState toModel(String projectJson) throws RvdException {
        Gson gson = new GsonBuilder()
        .registerTypeAdapter(Step.class, new StepJsonDeserializer())
        .registerTypeAdapter(Step.class, new StepJsonSerializer())
        .create();

        // Check header first
        JsonParser parser = new JsonParser();
        JsonElement header_element = parser.parse(projectJson).getAsJsonObject().get("header");
        if ( header_element == null )
            throw new BadProjectHeader("No header found. This is probably an old project");

        StateHeader header = gson.fromJson(header_element, StateHeader.class);
        if ( ! header.getVersion().equals(RvdConfiguration.getRvdProjectVersion()) )
                throw new IncompatibleProjectVersion("Error loading project. Project version: " + header.getVersion() + " - RVD project version: " + RvdConfiguration.getRvdProjectVersion() );
        // Looks like a good project. Make a ProjectState object out of it
        ProjectState projectState = gson.fromJson(projectJson, ProjectState.class);
        return projectState;
    }

    public abstract boolean supportsWavs();

    public String getName() {
        return name;
    }

    public ProjectState getState() {
        return state;
    }



}
