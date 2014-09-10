package org.mobicents.servlet.restcomm.rvd;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.model.StepJsonDeserializer;
import org.mobicents.servlet.restcomm.rvd.model.StepJsonSerializer;
import org.mobicents.servlet.restcomm.rvd.model.client.Node;
import org.mobicents.servlet.restcomm.rvd.model.client.ProjectState;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;
import org.mobicents.servlet.restcomm.rvd.model.server.NodeName;
import org.mobicents.servlet.restcomm.rvd.model.server.ProjectOptions;
import org.mobicents.servlet.restcomm.rvd.storage.FsProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.WorkspaceStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * This class is responsible for breaking the project state from a big JSON object to separate files per node/step. The
 * resulting files will be easily processed from the interpreter when the application is run.
 *
 */
public class BuildService {

    static final Logger logger = Logger.getLogger(BuildService.class.getName());

    protected Gson gson;
    private WorkspaceStorage workspaceStorage;

    public BuildService(WorkspaceStorage workspaceStorage) {
        this.workspaceStorage = workspaceStorage;
        // Parse the big project state object into a nice dto model
        gson = new GsonBuilder()
                .registerTypeAdapter(Step.class, new StepJsonDeserializer())
                .registerTypeAdapter(Step.class, new StepJsonSerializer())
                .create();
    }

    /**
     * Breaks the project state from a big JSON object to separate files per node/step. The resulting files will be easily
     * processed from the interpreter when the application is run.
     *
     * @param projectStateJson string representation of a big JSON object representing the project's state in the client
     * @param projectPath absolute filesystem path of the project. This is where the generated files will be stored
     * @throws IOException
     * @throws StorageException
     */
    public void buildProject(String projectName, ProjectState projectState) throws StorageException {
        ProjectOptions projectOptions = new ProjectOptions();

        // Save general purpose project information
        // Use the start node name as a default target. We could use a more specialized target too here

        // Build the nodes one by one
        for (Node node : projectState.getNodes()) {
            buildNode(node, projectName);
            NodeName nodeName = new NodeName();
            nodeName.setName(node.getName());
            nodeName.setLabel(node.getLabel());
            projectOptions.getNodeNames().add( nodeName );
        }

        projectOptions.setDefaultTarget(projectState.getHeader().getStartNodeName());
        if ( projectState.getHeader().getLogging() != null )
            projectOptions.setLogging(true);
        // Save the nodename-node-label mapping
        FsProjectStorage.storeProjectOptions(projectOptions, projectName, workspaceStorage);
    }

    /**
     *
     * @param node
     * @param projectPath
     * @throws StorageException
     * @throws IOException
     */
    private void buildNode(Node node, String projectName) throws StorageException {
        logger.debug("Building module " + node.getName() );

        // TODO sanitize node name!

        FsProjectStorage.storeNodeStepnames(node, projectName, workspaceStorage);
        // process the steps one-by-one
        for (Step step : node.getSteps()) {
            logger.debug("Building step " + step.getKind() + " - " + step.getName() );
            FsProjectStorage.storeNodeStep(step, node, projectName, workspaceStorage);
        }
    }
}
