package org.mobicents.servlet.restcomm.rvd.upgrade;

import java.util.List;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.BuildService;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.exceptions.InvalidProjectVersion;
import org.mobicents.servlet.restcomm.rvd.model.client.ProjectState;
import org.mobicents.servlet.restcomm.rvd.model.client.StateHeader;
import org.mobicents.servlet.restcomm.rvd.storage.FsProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.WorkspaceStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.BadProjectHeader;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;
import org.mobicents.servlet.restcomm.rvd.upgrade.exceptions.NoUpgradePathException;
import org.mobicents.servlet.restcomm.rvd.upgrade.exceptions.UpgradeException;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class UpgradeService {
    static final Logger logger = Logger.getLogger(UpgradeService.class.getName());

    private WorkspaceStorage workspaceStorage;

    public UpgradeService(WorkspaceStorage workspaceStorage) {
        this.workspaceStorage = workspaceStorage;
    }

    /**
     * Checks whether a runtime able to handle (open without upgrading) referenceProjectVersion can also handle checkedProjectVersion
     * @param referenceProjectVersion
     * @param checkedProjectVesion
     * @return
     * @throws InvalidProjectVersion
     */
    public static boolean checkBackwardCompatible(String referenceProjectVersion, String checkedProjectVesion) throws InvalidProjectVersion {
        if ( "1.3".equals(referenceProjectVersion) ) {
            return "1.3".equals(checkedProjectVesion) || "1.2".equals(checkedProjectVesion) || "1.1".equals(checkedProjectVesion) || "1.0".equals(checkedProjectVesion);
        } else
        if ( "1.2".equals(referenceProjectVersion) ) {
            return "1.2".equals(checkedProjectVesion) || "1.1".equals(checkedProjectVesion) || "1.0".equals(checkedProjectVesion);
        } else
        if ( "1.1.1".equals(referenceProjectVersion) ) {
            return "1.1.1".equals(checkedProjectVesion) || "1.1".equals(checkedProjectVesion) || "1.0".equals(checkedProjectVesion);
        } else
        if ( "1.1".equals(referenceProjectVersion) ) {
            return "1.1".equals(checkedProjectVesion) || "1.0".equals(checkedProjectVesion);
        } else
        if ( "1.0".equals(referenceProjectVersion) ) {
            return "1.0".equals(checkedProjectVesion);
        } else
            throw new InvalidProjectVersion("Invalid version identifier: " + referenceProjectVersion);
    }

    /**
     * Upgrades a project to current RVD supported version
     * @param projectName
     * @return false for projects already upgraded or older supported projects. true for projects that were indeed upgraded
     * @throws StorageException
     * @throws UpgradeException
     */
    public boolean upgradeProject(String projectName) throws StorageException, UpgradeException {

        String[] versionPath = new String[] {"rvd714","1.0"};

        StateHeader header = null;
        String startVersion = null;
        try {
            header = FsProjectStorage.loadStateHeader(projectName,workspaceStorage);
            startVersion = header.getVersion();
        } catch (BadProjectHeader e) {
            // it looks like this is an old project.
            startVersion = "rvd714"; // assume this is an rvd714 project. It could be 713 as well...
        }

        if ( startVersion.equals(RvdConfiguration.getRvdProjectVersion()) )
            return false;
        if ( checkBackwardCompatible(RvdConfiguration.getRvdProjectVersion(), startVersion) ) {
            //logger.warn("Project '" + projectName + "' is old but compatible. No need to upgrade.");
            return false;
        }

        logger.info("Upgrading '" + projectName + "' from version " + startVersion);

        String version = startVersion;
        String source = FsProjectStorage.loadProjectString(projectName, workspaceStorage);
        JsonParser parser = new JsonParser();
        JsonElement root = parser.parse(source);

        for ( int i = 0; i < versionPath.length; i ++ ) {
            if ( versionPath[i].equals(version) ) {
                // we found the version to start the upgrade
                ProjectUpgrader upgrader = ProjectUpgraderFactory.create(version);
                root = upgrader.upgrade(root);
                version = upgrader.getResultingVersion();

                if (version.equals(versionPath[versionPath.length-1] ) )
                    break;

                // if we haven't reached the final version yet keep upgrading
            }
        }

        if ( ! version.equals(versionPath[versionPath.length-1]) ) {
            throw new NoUpgradePathException("No upgrade path for project " + projectName + "Best effort from version: " + startVersion + " - to version: " + version);
        }

        FsProjectStorage.backupProjectState(projectName,workspaceStorage);
        FsProjectStorage.updateProjectState(projectName, root.toString(), workspaceStorage);
        return true;
    }
    /**
     * Upgrades all projects inside the project workspace to the version supported by current RVD
     * @throws StorageException
     */
    public void upgradeWorkspace() throws StorageException {
        BuildService buildService = new BuildService(workspaceStorage);
        int upgradedCount = 0;
        int uptodateCount = 0;
        int failedCount = 0;

        List<String> projectNames = FsProjectStorage.listProjectNames(workspaceStorage);
        for ( String projectName : projectNames ) {
            try {
                if ( upgradeProject(projectName) ) {
                    upgradedCount ++;
                    logger.info("project '" + projectName + "' upgraded to version " + RvdConfiguration.getRvdProjectVersion() );
                    try {
                        ProjectState projectState = FsProjectStorage.loadProject(projectName, workspaceStorage);
                        buildService.buildProject(projectName, projectState);
                        logger.info("project '" + projectName + "' built");
                    } catch (StorageException e) {
                        logger.warn("error building upgraded project '" + projectName + "'", e);
                    }
                } else
                    uptodateCount ++;
            } catch (StorageException e) {
                failedCount ++;
                logger.error("error upgrading project '" + projectName + "' to version " + RvdConfiguration.getRvdProjectVersion(), e );
            } catch (UpgradeException e) {
                failedCount ++;
                logger.error("error upgrading project '" + projectName + "' to version " + RvdConfiguration.getRvdProjectVersion(), e );
            }
        }
        if ( failedCount > 0 )
            logger.info("" + failedCount + " RVD projects failed upgrade");
        if ( upgradedCount > 0 )
            logger.info("" + upgradedCount + " RVD projects upgraded");
        if ( projectNames.size() > 0 && failedCount == 0)
            logger.info("--- All RVD projects are up to date");
        //if ( upgradedCount  0 && projectNames.size() > 0 )
          //  logger.info("All RVD projects are up-to-date" );
    }


}
