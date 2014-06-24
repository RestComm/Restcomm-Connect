package org.mobicents.servlet.restcomm.rvd.bootstrap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.RvdSettings;
import org.mobicents.servlet.restcomm.rvd.model.ModelMarshaler;
import org.mobicents.servlet.restcomm.rvd.storage.FsProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.ProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;
import org.mobicents.servlet.restcomm.rvd.upgrade.UpgradeService;

public class RvdInitializationServlet extends HttpServlet {

    static final Logger logger = Logger.getLogger(RvdInitializationServlet.class.getName());

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        logger.info("Initializing RVD");

        RvdSettings settings = RvdSettings.getInstance(config.getServletContext());
        ModelMarshaler marshaler = new ModelMarshaler();
        ProjectStorage projectStorage = new FsProjectStorage(settings,marshaler);
        UpgradeService upgradeService = new UpgradeService(projectStorage);

        try {
            upgradeService.upgradeWorkspace();
        } catch (StorageException e) {
            logger.error("Error upgrading workspace at " + settings.getWorkspaceBasePath(), e);
        }
    }

    public RvdInitializationServlet() {
        // TODO Auto-generated constructor stub
    }

}
