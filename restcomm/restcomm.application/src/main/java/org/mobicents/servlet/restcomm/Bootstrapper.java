package org.mobicents.servlet.restcomm;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Properties;

import javax.media.mscontrol.MsControlException;
import javax.media.mscontrol.MsControlFactory;
import javax.media.mscontrol.spi.Driver;
import javax.media.mscontrol.spi.DriverManager;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletContextEvent;
import javax.servlet.sip.SipServletListener;
import javax.servlet.sip.SipURI;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.interpol.ConfigurationInterpolator;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.configuration.RestcommConfiguration;
import org.mobicents.servlet.restcomm.configuration.sets.IdentityMigrationConfigurationSet;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.entities.InstanceId;
import org.mobicents.servlet.restcomm.entities.shiro.ShiroResources;
import org.mobicents.servlet.restcomm.http.RestcommRoles;
import org.mobicents.servlet.restcomm.identity.RestcommIdentityApi;
import org.mobicents.servlet.restcomm.identity.configuration.DbIdentityConfigurationSource;
import org.mobicents.servlet.restcomm.identity.configuration.IdentityConfigurationSource;
import org.mobicents.servlet.restcomm.identity.configuration.IdentityConfigurator;
import org.mobicents.servlet.restcomm.identity.migration.IdentityMigrationTool;
import org.mobicents.servlet.restcomm.loader.ObjectFactory;
import org.mobicents.servlet.restcomm.loader.ObjectInstantiationException;
import org.mobicents.servlet.restcomm.mgcp.PowerOnMediaGateway;
import org.mobicents.servlet.restcomm.mscontrol.MediaServerControllerFactory;
import org.mobicents.servlet.restcomm.mscontrol.MediaServerInfo;
import org.mobicents.servlet.restcomm.mscontrol.jsr309.Jsr309ControllerFactory;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsControllerFactory;
import org.mobicents.servlet.restcomm.telephony.config.ConfigurationStringLookup;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;

import com.telestax.servlet.MonitoringService;

/**
 *
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */

public final class Bootstrapper extends SipServlet implements SipServletListener {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(Bootstrapper.class);

    private ActorSystem system;

    public Bootstrapper() {
        super();
    }

    @Override
    public void destroy() {
        system.shutdown();
        system.awaitTermination();
    }

    private MediaServerControllerFactory mediaServerControllerFactory(final Configuration configuration, ClassLoader loader)
            throws ServletException {
        Configuration settings ;
        String compatibility = configuration.subset("mscontrol").getString("compatibility", "mms");

        MediaServerControllerFactory factory;
        switch (compatibility) {
            case "mms":
                ActorRef gateway;
                try {
                    settings = configuration.subset("media-server-manager");
                    gateway = gateway(settings, loader);
                    factory = new MmsControllerFactory(this.system, gateway);
                } catch (UnknownHostException e) {
                    throw new ServletException(e);
                }
                break;

            case "xms":
                try {
                    settings = configuration.subset("mscontrol");
                    // Load JSR 309 driver
                    final String driverName = settings.getString("media-server[@class]");
                    Driver driver = DriverManager.getDriver(driverName);
                    DriverManager.registerDriver(driver);

                    // Configure properties
                    Properties properties = getDialogicXmsProperties(settings);

                    // Create JSR 309 factory
                    MsControlFactory msControlFactory = driver.getFactory(properties);
                    MediaServerInfo mediaServerInfo = mediaServerInfo(settings);
                    factory = new Jsr309ControllerFactory(system, mediaServerInfo, msControlFactory);
                } catch (UnknownHostException | MsControlException e) {
                    throw new ServletException(e);
                }
                break;

            default:
                throw new IllegalArgumentException("MSControl unknown compatibility mode: " + compatibility);
        }
        return factory;
    }

    private MediaServerInfo mediaServerInfo(final Configuration configuration) throws UnknownHostException {
        final String name = configuration.getString("media-server[@name]");
        final String address = configuration.getString("media-server.address");
        final int port = configuration.getInt("media-server.port");
        final int timeout = configuration.getInt("media-server.timeout", 5);
        return new MediaServerInfo(name, InetAddress.getByName(address), port, timeout);
    }

    private Properties getDialogicXmsProperties(final Configuration configuration) {
        // New set of properties that will be used to configure the connector
        Properties properties = new Properties();

        // Tell the driver we are configuring it programmatically
        // properties.setProperty("connector.dynamic.configuration", "yes");

        // Configure the transport to be used by the connector
        final String mediaTransport = configuration.getString("media-server.transport", "udp");
        logger.info("JSR 309 - media-server.transport: udp");
        properties.setProperty("connector.sip.transport", mediaTransport);

        // Configure SIP connector using RestComm binding address
        SipURI sipURI = outboundInterface(getServletContext(), mediaTransport);
        properties.setProperty("connector.sip.address", sipURI.getHost());
        logger.info("JSR 309 - connector.sip.address: " + sipURI.getHost());
        properties.setProperty("connector.sip.port", String.valueOf(sipURI.getPort()));
        logger.info("JSR 309 - connector.sip.port: " + String.valueOf(sipURI.getPort()));

        // Configure Media Server address based on restcomm configuration file
        final String mediaAddress = configuration.getString("media-server.address", "127.0.0.1");
        properties.setProperty("mediaserver.sip.ipaddress", mediaAddress);
        logger.info("JSR 309 - mediaserver.sip.ipaddress: " + mediaAddress);

        final String mediaPort = configuration.getString("media-server.port", "5060");
        properties.setProperty("mediaserver.sip.port", mediaPort);
        logger.info("JSR 309 - mediaserver.sip.port: " + mediaPort);

        // Let RestComm control call legs
        properties.setProperty("connector.conferenceControlLeg", "no");

        return properties;
    }

    @SuppressWarnings("unchecked")
    private SipURI outboundInterface(ServletContext context, String transport) {
        SipURI result = null;
        final List<SipURI> uris = (List<SipURI>) context.getAttribute(OUTBOUND_INTERFACES);
        for (final SipURI uri : uris) {
            final String interfaceTransport = uri.getTransportParam();
            if (transport.equalsIgnoreCase(interfaceTransport)) {
                result = uri;
            }
        }
        return result;
    }

    private ActorRef gateway(final Configuration settings, final ClassLoader loader) throws UnknownHostException {
        final ActorRef gateway = system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                final String classpath = settings.getString("mgcp-server[@class]");
                return (UntypedActor) new ObjectFactory(loader).getObjectInstance(classpath);
            }
        }));
        final PowerOnMediaGateway.Builder builder = PowerOnMediaGateway.builder();
        builder.setName(settings.getString("mgcp-server[@name]"));
        String address = settings.getString("mgcp-server.local-address");
        builder.setLocalIP(InetAddress.getByName(address));
        String port = settings.getString("mgcp-server.local-port");
        builder.setLocalPort(Integer.parseInt(port));
        address = settings.getString("mgcp-server.remote-address");
        builder.setRemoteIP(InetAddress.getByName(address));
        port = settings.getString("mgcp-server.remote-port");
        builder.setRemotePort(Integer.parseInt(port));
        address = settings.getString("mgcp-server.external-address");
        if (address != null) {
            builder.setExternalIP(InetAddress.getByName(address));
            builder.setUseNat(true);
        } else {
            builder.setUseNat(false);
        }
        final String timeout = settings.getString("mgcp-server.response-timeout");
        builder.setTimeout(Long.parseLong(timeout));
        final PowerOnMediaGateway powerOn = builder.build();
        gateway.tell(powerOn, null);
        return gateway;
    }

    private String home(final ServletContext context) {
        final String path = context.getRealPath("/");
        if (path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        } else {
            return path;
        }
    }

    private DaoManager storage(final Configuration configuration, final ClassLoader loader) throws ObjectInstantiationException {
        final String classpath = configuration.getString("dao-manager[@class]");
        final DaoManager daoManager = (DaoManager) new ObjectFactory(loader).getObjectInstance(classpath);
        daoManager.configure(configuration);
        daoManager.start();
        return daoManager;
    }

    private ActorRef monitoringService(final Configuration configuration, final ClassLoader loader) {
        final ActorRef monitoring = system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return (UntypedActor) new ObjectFactory(loader).getObjectInstance(MonitoringService.class.getName());
            }
        }));
        return monitoring;

    }

    private String uri(final ServletContext context) {
        return context.getContextPath();
    }

    private void identityMigration(RestcommConfiguration config, DaoManager daos ) {
        // TODO - replace these hardcoded values with values from the actual configuration
        IdentityMigrationConfigurationSet identityConfig = config.getIdentityMigration();

        RestcommIdentityApi api = new RestcommIdentityApi(identityConfig.getAuthServerBaseUrl(), identityConfig.getUsername(), identityConfig.getPassword());
        IdentityMigrationTool migrationTool = new IdentityMigrationTool(daos.getAccountsDao(), api, identityConfig.getInviteExistingUsers());
        migrationTool.migrate();

        //String instanceId = api.createInstance("http://localhost", "my-secret").instanceId;
        //api.bindInstance(instanceId);
    }

    @Override
    public void servletInitialized(SipServletContextEvent event) {
        if (event.getSipServlet().getClass().equals(Bootstrapper.class)) {
            final ServletContext context = event.getServletContext();
            final String path = context.getRealPath("WEB-INF/conf/restcomm.xml");
            final String extensionConfigurationPath = context.getRealPath("WEB-INF/conf/extensions.xml");
            // Initialize the configuration interpolator.
            final ConfigurationStringLookup strings = new ConfigurationStringLookup();
            strings.addProperty("home", home(context));
            strings.addProperty("uri", uri(context));
            ConfigurationInterpolator.registerGlobalLookup("restcomm", strings);
            // Load the RestComm configuration file.
            Configuration xml = null;
            try {
                xml = new XMLConfiguration(path);
            } catch (final ConfigurationException exception) {
                logger.error(exception);
            }
            xml.setProperty("runtime-settings.home-directory", home(context));
            xml.setProperty("runtime-settings.root-uri", uri(context));
            context.setAttribute(Configuration.class.getName(), xml);
            // Initialize global dependencies.
            final ClassLoader loader = getClass().getClassLoader();
            // Create the actor system.
            final Config settings = ConfigFactory.load();
            system = ActorSystem.create("RestComm", settings, loader);
            // Share the actor system with other servlets.
            context.setAttribute(ActorSystem.class.getName(), system);
            // Create the storage system.
            DaoManager storage = null;
            try {
                storage = storage(xml, loader);
            } catch (final ObjectInstantiationException exception) {
                logger.error("ObjectInstantiationException during initialization: ", exception);
            }
            context.setAttribute(DaoManager.class.getName(), storage);
            ShiroResources.getInstance().set(DaoManager.class, storage);
            ShiroResources.getInstance().set(Configuration.class, xml.subset("runtime-settings"));
            // Create high-level restcomm configuration
            RestcommConfiguration.createOnce(xml);

            // Identity migration. Register instance to auth server and migrate users.
            identityMigration(RestcommConfiguration.getInstance(), storage);

            // Create directory of shiro based restcomm roles
            RestcommRoles restcommRoles = new RestcommRoles();
            context.setAttribute(RestcommRoles.class.getName(), restcommRoles);
            ShiroResources.getInstance().set(RestcommRoles.class, restcommRoles );
            //logger.info("RestcommRoles: " + ShiroResources.getInstance().get(RestcommRoles.class).toString() );

            // Initialize identity/keycloak configuration
            IdentityConfigurationSource identityConfSource = new DbIdentityConfigurationSource(storage.getConfigurationDao());
            IdentityConfigurator keycloakConfig = IdentityConfigurator.create(identityConfSource,context);
            context.setAttribute(IdentityConfigurator.class.getName(), keycloakConfig);

            // Create the media gateway.

            //Initialize Monitoring Service
            ActorRef monitoring = monitoringService(xml, loader);
            if (monitoring != null) {
                context.setAttribute(MonitoringService.class.getName(), monitoring);
                logger.info("Monitoring Service created and stored in the context");
            } else {
                logger.error("Monitoring Service is null");
            }

            //Initialize Extensions
            Configuration extensionConfiguration = null;
            try {
                extensionConfiguration = new XMLConfiguration(extensionConfigurationPath);
            } catch (final ConfigurationException exception) {
                logger.error(exception);
            }
            ExtensionScanner extensionScanner = new ExtensionScanner(extensionConfiguration);
            extensionScanner.start();

            // Create the media server controller factory
            MediaServerControllerFactory mscontrollerFactory = null;
            try {
                mscontrollerFactory = mediaServerControllerFactory(xml, loader);
            } catch (ServletException exception) {
                logger.error("ServletException during initialization: ", exception);
            }
            context.setAttribute(MediaServerControllerFactory.class.getName(), mscontrollerFactory);

            //Last, print Version and send PING if needed
            Version.printVersion();
            GenerateInstanceId generateInstanceId = new GenerateInstanceId(context);
            InstanceId instanceId = generateInstanceId.instanceId();
            context.setAttribute(InstanceId.class.getName(), instanceId);
            monitoring.tell(instanceId, null);
            Ping ping = new Ping(xml, context);
            ping.sendPing();
        }
    }
}
