package org.restcomm.connect.rvd;

import org.apache.log4j.Logger;
import org.restcomm.connect.rvd.model.StepJsonDeserializer;
import org.restcomm.connect.rvd.model.StepJsonSerializer;
import org.restcomm.connect.rvd.model.client.Node;
import org.restcomm.connect.rvd.model.client.ProjectState;
import org.restcomm.connect.rvd.model.client.Step;
import org.restcomm.connect.rvd.model.server.NodeName;
import org.restcomm.connect.rvd.model.server.ProjectOptions;
import org.restcomm.connect.rvd.storage.FsProjectStorage;
import org.restcomm.connect.rvd.storage.WorkspaceStorage;
import org.restcomm.connect.rvd.storage.exceptions.StorageException;

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
        //if ( projectState.getHeader().getLogging() != null )
        //    projectOptions.setLogging(true);
        // Save the nodename-node-label mapping
        FsProjectStorage.storeProjectOptions(projectOptions, projectName, workspaceStorage);
    }

    public void buildProject(String projectName) throws StorageException {
        ProjectState state = FsProjectStorage.loadProject(projectName, workspaceStorage);
        buildProject(projectName, state);
    }

    private void buildNode(Node node, String projectName) throws StorageException {
        if(logger.isDebugEnabled()) {
            logger.debug("Building module " + node.getName() );
        }

        // TODO sanitize node name!

        FsProjectStorage.storeNodeStepnames(node, projectName, workspaceStorage);
        // process the steps one-by-one
        for (Step step : node.getSteps()) {
            if(logger.isDebugEnabled()) {
                logger.debug("Building step " + step.getKind() + " - " + step.getName() );
            }
            FsProjectStorage.storeNodeStep(step, node, projectName, workspaceStorage);
        }
    }
}
