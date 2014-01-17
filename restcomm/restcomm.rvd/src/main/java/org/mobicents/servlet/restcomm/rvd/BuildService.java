package org.mobicents.servlet.restcomm.rvd;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.mobicents.servlet.restcomm.rvd.model.StepJsonDeserializer;
import org.mobicents.servlet.restcomm.rvd.model.StepJsonSerializer;
import org.mobicents.servlet.restcomm.rvd.model.client.ProjectState;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;
import org.mobicents.servlet.restcomm.rvd.model.server.ProjectOptions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * This class is responsible for breaking the project state from a big JSON object to separate files per node/step. The
 * resulting files will be easily processed from the interpreter when the application is run.
 *
 */
public class BuildService {

    private Gson gson;

    public BuildService() {
        // Parse the big project state object into a nice dto model
        gson = new GsonBuilder()
                .registerTypeAdapter(Step.class, new StepJsonDeserializer())
                .registerTypeAdapter(Step.class, new StepJsonSerializer())
                //.registerTypeAdapter(AccessRawOperation.class, new AccessRawOperationJsonDeserializer())
                .create();
    }

    /**
     * Breaks the project state from a big JSON object to separate files per node/step. The resulting files will be easily
     * processed from the interpreter when the application is run.
     *
     * @param projectStateJson string representation of a big JSON object representing the project's state in the client
     * @param projectPath absolute filesystem path of the project. This is where the generated files will be stored
     * @throws IOException
     */
    public void buildProject(String projectStateJson, String projectPath) throws IOException {
        ProjectState projectState = gson.fromJson(projectStateJson, ProjectState.class);

        ProjectOptions projectOptions = new ProjectOptions();

        // Save general purpose project information
        // Use the start node name as a default target. We could use a more specialized target too here
        projectOptions.setDefaultTarget(projectState.getStartNodeName());
        File outFile = new File(projectPath + "data/" + "project");
        FileUtils.writeStringToFile(outFile, gson.toJson(projectOptions), "UTF-8");

        // Build the nodes one by one
        for (ProjectState.Node node : projectState.getNodes()) {
            buildNode(node, projectPath);
        }
    }

    /**
     *
     * @param node
     * @param projectPath
     * @throws IOException
     */
    private void buildNode(ProjectState.Node node, String projectPath) throws IOException {
        System.out.println("building node " + node.getName());

        // TODO sanitize node name!
        File outFile = new File(projectPath + "data/" + node.getName() + ".node");
        FileUtils.writeStringToFile(outFile, gson.toJson(node.getStepnames()), "UTF-8");

        // process the steps one-by-one
        for (String stepname : node.getSteps().keySet()) {
            Step step = node.getSteps().get(stepname);
            System.out.println("building step " + step.getKind() + " - " + step.getName());
            FileUtils.writeStringToFile(new File(projectPath + "data/" + node.getName() + "." + step.getName()),
                    gson.toJson(step), "UTF-8");
        }
    }
}
