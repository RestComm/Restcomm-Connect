package org.restcomm.connect.rvd;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.restcomm.connect.rvd.exceptions.RvdException;
import org.restcomm.connect.rvd.exceptions.packaging.PackagingException;
import org.restcomm.connect.rvd.exceptions.project.UnsupportedProjectVersion;
import org.restcomm.connect.rvd.exceptions.ras.RestcommAppAlreadyExists;
import org.restcomm.connect.rvd.exceptions.ras.UnsupportedRasApplicationVersion;
import org.restcomm.connect.rvd.model.ModelMarshaler;
import org.restcomm.connect.rvd.model.RappItem;
import org.restcomm.connect.rvd.model.client.WavItem;
import org.restcomm.connect.rvd.model.packaging.Rapp;
import org.restcomm.connect.rvd.model.packaging.RappBinaryInfo;
import org.restcomm.connect.rvd.model.packaging.RappConfig;
import org.restcomm.connect.rvd.model.packaging.RappInfo;
import org.restcomm.connect.rvd.model.project.RvdProject;
import org.restcomm.connect.rvd.storage.FsPackagingStorage;
import org.restcomm.connect.rvd.storage.FsProjectStorage;
import org.restcomm.connect.rvd.storage.WorkspaceStorage;
import org.restcomm.connect.rvd.storage.exceptions.StorageException;
import org.restcomm.connect.rvd.upgrade.UpgradeService;
import org.restcomm.connect.rvd.utils.RvdUtils;
import org.restcomm.connect.rvd.utils.Unzipper;
import org.restcomm.connect.rvd.utils.Zipper;
import org.restcomm.connect.rvd.validation.ValidationReport;
import org.restcomm.connect.rvd.validation.exceptions.RvdValidationException;

import com.google.gson.Gson;
import com.thoughtworks.xstream.XStream;

import java.util.List;
import java.util.UUID;
/**
 * Functionality for importing and setting up an app from the app store
 * @author "Tsakiridis Orestis"
 *
 */
public class RasService {

    static final Logger logger = Logger.getLogger(RasService.class.getName());

    ModelMarshaler marshaler;
    WorkspaceStorage workspaceStorage;

    public RasService(RvdContext rvdContext, WorkspaceStorage workspaceStorage) {
        this.marshaler = rvdContext.getMarshaler();
        this.workspaceStorage = workspaceStorage;
    }

    /**
     * Builds a RappConfig object out of its JSON representation
     *
     */
    public RappConfig toModel(Class<RappConfig> clazz, String data) {
        Gson gson = new Gson();
        RappConfig rappConfig = gson.fromJson(data, RappConfig.class);
        return rappConfig;
    }


    public InputStream createZipPackage(RvdProject project) throws RvdException {
        if(logger.isDebugEnabled()) {
            logger.debug("Creating zip package for project " + project.getName());
        }
        String projectName = project.getName();

        // extract info and config parts of a Rapp
        Gson gson = new Gson();
        XStream xstream = marshaler.getXStream();
        xstream.alias("restcommApplication", RappInfo.class);

        Rapp rapp = FsPackagingStorage.loadRapp(projectName, workspaceStorage);
        String configData = gson.toJson(rapp.getConfig());
        //String infoData = gson.toJson(rapp.getInfo());
        String infoData = xstream.toXML(rapp.getInfo());

        // create the zip bundle
        try {
            File tempFile = File.createTempFile("rapp",".tmp");

            Zipper zipper = new Zipper(tempFile);
            try {
                zipper.addDirectory("/app/");
                zipper.addFileContent("/app/info.xml", infoData );
                zipper.addFileContent("/app/config", configData );
                zipper.addDirectory("/app/rvd/");
                zipper.addFileContent("/app/rvd/state", marshaler.toData(project.getState()) );

                if ( project.supportsWavs() ) {
                    zipper.addDirectory("/app/rvd/wavs/");
                    for ( WavItem wavItem : FsProjectStorage.listWavs(projectName, workspaceStorage) ) {
                        InputStream wavStream = FsProjectStorage.getWav(projectName, wavItem.getFilename(), workspaceStorage);
                        try {
                            zipper.addFile("app/rvd/wavs/" + wavItem.getFilename(), wavStream );
                        } finally {
                            wavStream.close();
                        }
                    }
                }


            } finally {
                zipper.finish();
            }

            FsPackagingStorage.storeRappBinary(tempFile, projectName, workspaceStorage);
            // TODO - if FsProjectStorage  is not used, the temporaty file should still be removed (in this case it is not moved) !!!
            if(logger.isDebugEnabled()) {
                logger.debug("Zip package created for project " + projectName);
            }

            return FsPackagingStorage.getRappBinary(projectName, workspaceStorage);
        } catch (IOException e) {
            throw new PackagingException("Error creating temporaty zip file ", e);
        }

    }


    /**
     * Unzips the package stream in a temporary directory and creates an app out of it. Since this action is initiated
     * from AdminUI there is no easy way to authenticate the request without forcing the user login in RVD too. So, all
     * requests are accepted and no loggedUser information is stored.
     * @param packageZipStream
     * @return The name (some sort of identifier) of the new project created
     * @throws RvdException
     */
    public String importAppToWorkspace(String applicationSid, InputStream packageZipStream, String loggedUser,
            ProjectService projectService) throws RvdException {
        File tempDir = RvdUtils.createTempDir();
        if(logger.isDebugEnabled()) {
            logger.debug("Unzipping ras package to temporary directory " + tempDir.getPath());
        }
        Unzipper unzipper = new Unzipper(tempDir);
        unzipper.unzip(packageZipStream);

        String newProjectName;
        try {
            RappInfo info = workspaceStorage.loadModelFromXMLFile(tempDir.getPath() + "/app/" + "info.xml", RappInfo.class);
            RappConfig config = workspaceStorage.loadModelFromFile(tempDir.getPath() + "/app/" + "config", RappConfig.class);
            // check project version from info.xml
            if ( ! UpgradeService.checkBackwardCompatible(info.getRvdAppVersion(), RvdConfiguration.getRvdProjectVersion()) && UpgradeService.checkUpgradability(info.getRvdAppVersion(), RvdConfiguration.getRvdProjectVersion()) != UpgradeService.UpgradabilityStatus.UPGRADABLE ) {
                throw new UnsupportedProjectVersion("Project version " + info.getRvdAppVersion() + " is not supported");
            }
            // check ras package version
            int effectivePackageVersion = 1;
            if (info.getRasVersion() != null) {
                try {
                    effectivePackageVersion = Integer.parseInt(info.getRasVersion());
                } catch (NumberFormatException e) {
                    //effectivePackageVersion = 1; // already done
                }
            }
            int runtimePackageVersion = Integer.parseInt(RvdConfiguration.getRasApplicationVersion());
            if (runtimePackageVersion < effectivePackageVersion)
                throw new UnsupportedRasApplicationVersion("Incompatible application package. Version " + effectivePackageVersion + " is not supported");
            // Make sure no such restcomm app already exists (single instance limitation)
            List<RappItem> rappItems = FsProjectStorage.listRapps(FsProjectStorage.listProjectNames(workspaceStorage), workspaceStorage, projectService);
            for (RappItem rappItem : rappItems)
                if (rappItem.getRappInfo() != null && rappItem.getRappInfo().getId() != null && rappItem.getRappInfo().getId().equals(info.getId()))
                    throw new RestcommAppAlreadyExists("A restcomm application with id " + rappItem.getRappInfo().getId() + "  already exists. Cannot import " + info.getName() + " app");
            // all seems fine, do import
            File tempProjectDir = new File(tempDir.getPath() + "/app/rvd");
            projectService.importProject(tempProjectDir, applicationSid, loggedUser);
            newProjectName = info.getName();

            // Store rapp for later usage
            Rapp rapp = new Rapp(info, config);
            FsProjectStorage.storeRapp(rapp, applicationSid, workspaceStorage);
        } finally {
            // now remove temporary directory
            FileUtils.deleteQuietly(tempDir);
        }
        return newProjectName;
    }

    /**
     * Updates packaging information for an app
     * @param rapp
     * @param projectName
     * @throws RvdValidationException
     * @throws StorageException
     */
    public void saveApp(Rapp rapp, String projectName) throws RvdValidationException, StorageException {
        ValidationReport report = rapp.validate();
        if ( ! report.isOk() )
            throw new RvdValidationException("Cannot validate rapp", report);

        // set version since they are affected from the current RVD runtime
        rapp.getInfo().setRasVersion(RvdConfiguration.getRasApplicationVersion());
        rapp.getInfo().setRvdAppVersion(RvdConfiguration.getRvdProjectVersion());

        // preserve the app's id
        Rapp existingRapp = FsPackagingStorage.loadRapp(projectName,workspaceStorage);
        rapp.getInfo().setId( existingRapp.getInfo().getId() );

        FsPackagingStorage.storeRapp(rapp, projectName, workspaceStorage);
    }

    /**
     * Creates packaging information for an app.
     * @param rapp
     * @param projectName
     * @throws RvdValidationException
     * @throws StorageException
     */
    public void createApp(Rapp rapp, String projectName) throws RvdValidationException, StorageException {
        ValidationReport report = rapp.validate();
        if ( ! report.isOk() )
            throw new RvdValidationException("Cannot validate rapp", report);

        // set version since they are affected from the current RVD runtime
        rapp.getInfo().setRasVersion(RvdConfiguration.getRasApplicationVersion());
        rapp.getInfo().setRvdAppVersion(RvdConfiguration.getRvdProjectVersion());

        // rapp.getInfo().setId(generateAppId(projectName)); // Let the RAS administrator choose an id for the app after submission
        FsPackagingStorage.storeRapp(rapp, projectName, workspaceStorage);
    }

    public Rapp getApp(String projectName) throws StorageException {
        return FsPackagingStorage.loadRapp(projectName, workspaceStorage);
    }

    public RappConfig getRappConfig(String projectName) throws StorageException {
        Rapp rapp = FsProjectStorage.loadRapp(projectName, workspaceStorage);
        return rapp.getConfig();
    }

    public RappBinaryInfo getBinaryInfo(String projectName) {
        RappBinaryInfo binaryInfo = new RappBinaryInfo();
        binaryInfo.setExists( FsPackagingStorage.binaryAvailable(projectName, workspaceStorage) );

        return binaryInfo;
    }

    protected String generateAppId(String projectName) {
        String id = UUID.randomUUID().toString();
        return id;
    }



}
