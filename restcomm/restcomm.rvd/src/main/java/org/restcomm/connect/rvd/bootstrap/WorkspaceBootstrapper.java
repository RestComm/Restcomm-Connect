package org.restcomm.connect.rvd.bootstrap;

import org.restcomm.connect.rvd.RvdConfiguration;

import java.io.File;

/**
 * A class to encapsulate all startup tasks for workspace initialization for example create the @users directory
 * if it does not exist.
 *
 * @author Orestis Tsakiridis
 */
public class WorkspaceBootstrapper {

    private String rootLocation; // the root directory of the workspace that contains project directories as children

    public WorkspaceBootstrapper(String rootLocation) {
        File rootDir = new File(rootLocation);
        if (!rootDir.exists() || !rootDir.isDirectory() ) {
            String message = "Error bootstrapping RVD workspace at '" + rootLocation + "'. Location does not exist or is not a directory.";
            throw new RuntimeException(message);
        }

        this.rootLocation = rootLocation;
    }

    /**
     * Executes all operations for workspace bootstrapping
     */
    public void run() {
        createUsersDirectory();
    }

    /**
     * Creates users directory inside the workspace. That's where user-specific information is stored
     */
    void createUsersDirectory() {
        String dirName = rootLocation + "/" + RvdConfiguration.USERS_DIRECTORY_NAME;
        File usersDir = new File(dirName);
        if (!usersDir.exists()) {
            usersDir.mkdir();
        }
    }
}
