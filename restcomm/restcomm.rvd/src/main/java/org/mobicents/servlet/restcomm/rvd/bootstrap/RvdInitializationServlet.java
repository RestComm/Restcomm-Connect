package org.mobicents.servlet.restcomm.rvd.bootstrap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.ApplicationContext;
import org.mobicents.servlet.restcomm.rvd.ApplicationContextBuilder;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.commons.http.CustomHttpClientBuilder;
import org.mobicents.servlet.restcomm.rvd.identity.AccountProvider;
import org.mobicents.servlet.restcomm.rvd.model.ModelMarshaler;
import org.mobicents.servlet.restcomm.rvd.storage.WorkspaceStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;
import org.mobicents.servlet.restcomm.rvd.upgrade.UpgradeService;

public class RvdInitializationServlet extends HttpServlet {

    static final Logger logger = Logger.getLogger(RvdInitializationServlet.class.getName());

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config) ;
        if(logger.isInfoEnabled()) {
            logger.info("Initializing RVD. Project version: " + RvdConfiguration.getRvdProjectVersion());
        }
        // Create application context and store in ServletContext
        ServletContext servletContext = config.getServletContext();
        RvdConfiguration rvdConfiguration = new RvdConfiguration(servletContext);
        CustomHttpClientBuilder httpClientBuilder = new CustomHttpClientBuilder(rvdConfiguration);
        AccountProvider accountProvider = new AccountProvider(rvdConfiguration.getRestcommBaseUri().toString(), httpClientBuilder);
        ApplicationContext appContext = new ApplicationContextBuilder()
                .setConfiguration(rvdConfiguration)
                .setHttpClientBuilder(httpClientBuilder)
                .setAccountProvider(accountProvider).build();
        servletContext.setAttribute(ApplicationContext.class.getName(), appContext);

        WorkspaceBootstrapper workspaceBootstrapper = new WorkspaceBootstrapper(rvdConfiguration.getWorkspaceBasePath());
        workspaceBootstrapper.run();

        ModelMarshaler marshaler = new ModelMarshaler();
        WorkspaceStorage workspaceStorage = new WorkspaceStorage(rvdConfiguration.getWorkspaceBasePath(), marshaler);
        UpgradeService upgradeService = new UpgradeService(workspaceStorage);
        try {
            upgradeService.upgradeWorkspace();
        } catch (StorageException e) {
            logger.error("Error upgrading workspace at " + rvdConfiguration.getWorkspaceBasePath(), e);
        }
    }

    public RvdInitializationServlet() {
        // TODO Auto-generated constructor stub
    }

}
