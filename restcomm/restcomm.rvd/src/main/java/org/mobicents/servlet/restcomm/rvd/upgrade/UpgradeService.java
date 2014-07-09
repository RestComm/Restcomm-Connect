package org.mobicents.servlet.restcomm.rvd.upgrade;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.BuildService;
import org.mobicents.servlet.restcomm.rvd.RvdSettings;
import org.mobicents.servlet.restcomm.rvd.model.client.StateHeader;
import org.mobicents.servlet.restcomm.rvd.storage.FsProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.ProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.BadProjectHeader;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;
import org.mobicents.servlet.restcomm.rvd.upgrade.exceptions.NoUpgradePathException;
import org.mobicents.servlet.restcomm.rvd.upgrade.exceptions.UpgradeException;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class UpgradeService {
    static final Logger logger = Logger.getLogger(UpgradeService.class.getName());

    private ProjectStorage projectStorage;

    public UpgradeService(ProjectStorage projectStorage) {
        this.projectStorage = projectStorage;
    }

    public UpgradeService(String otherWorkspaceLocation) {
        this.projectStorage = new FsProjectStorage(otherWorkspaceLocation, null);
    }

    /**
     * Upgrades a project to current RVD supported version
     * @param projectName
     * @return Boolean value indicating if the project was upgraded or not. i.e. false for projects already upgraded, true for projects that were indeed upgraded
     * @throws StorageException
     * @throws UpgradeException
     */
    public boolean upgradeProject(String projectName) throws StorageException, UpgradeException {

        String[] versionPath = new String[] {"rvd714","1.0"};

        StateHeader header = null;
        String startVersion = null;
        try {
            header = projectStorage.loadStateHeader(projectName);
            startVersion = header.getVersion();
        } catch (BadProjectHeader e) {
            // it looks like this is an old project.
            startVersion = "rvd714"; // assume this is an rvd714 project. It could be 713 as well...
        }

        if ( startVersion.equals(RvdSettings.getRvdProjectVersion()) )
            return false;

        logger.info("Upgrading '" + projectName + "' from version " + startVersion);

        String version = startVersion;
        String source = projectStorage.loadProjectState(projectName);
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

        projectStorage.backupProjectState(projectName);
        projectStorage.updateProjectState(projectName, root.toString());
        return true;
    }
    /**
     * Upgrades all projects inside the project workspace to the version supported by current RVD
     * @throws StorageException
     */
    public void upgradeWorkspace() throws StorageException {
        BuildService buildService = new BuildService(projectStorage);
        int upgradedCount = 0;
        for ( String projectName : projectStorage.listProjectNames() ) {
            try {
                if ( upgradeProject(projectName) ) {
                    upgradedCount ++;
                    logger.info("project '" + projectName + "' upgraded to version " + RvdSettings.getRvdProjectVersion() );
                    try {
                        buildService.buildProject(projectName);
                        logger.info("project '" + projectName + "' built");
                    } catch (StorageException e) {
                        logger.warn("error building upgraded project '" + projectName + "'", e);
                    }
                }
            } catch (StorageException e) {
                logger.error("error upgrading project '" + projectName + "' to version " + RvdSettings.getRvdProjectVersion(), e );
            } catch (UpgradeException e) {
                logger.error("error upgrading project '" + projectName + "' to version " + RvdSettings.getRvdProjectVersion(), e );
            } catch (Exception e) {
                logger.error("error upgrading project '" + projectName + "' to version " + RvdSettings.getRvdProjectVersion(), e );
            }
        }
        if ( upgradedCount == 0 )
            logger.info("All RVD projects are up-to-date" );
        else
            logger.info("" + upgradedCount + " RVD project upgraded");
    }


}
