package org.mobicents.servlet.restcomm.rvd.ras;

import java.io.File;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;
import org.mobicents.servlet.restcomm.rvd.utils.RvdUtils;
import org.mobicents.servlet.restcomm.rvd.utils.Unzipper;

/**
 * Functionality for importing and setting up an app from the app store
 * @author "Tsakiridis Orestis"
 *
 */
public class RasService {

    static final Logger logger = Logger.getLogger(RasService.class.getName());

    public RasService() {
        // TODO Auto-generated constructor stub
    }


    /**
     * Unzips the package stream in a temporary directory and creates an app out of it
     * @param packageZipStream
     * @throws RvdException
     */
    public void importAppToWorkspace( InputStream packageZipStream ) throws RvdException {
        File tempDir = RvdUtils.createTempDir();
        logger.debug("Unzipping ras package to temporary directory " + tempDir.getPath());
        Unzipper unzipper = new Unzipper(tempDir);
        unzipper.unzip(packageZipStream);
    }

}
