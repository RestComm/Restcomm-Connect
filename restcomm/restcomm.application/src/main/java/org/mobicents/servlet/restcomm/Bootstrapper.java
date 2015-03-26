package org.mobicents.servlet.restcomm;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.sip.SipServlet;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.interpol.ConfigurationInterpolator;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.entities.shiro.ShiroResources;
import org.mobicents.servlet.restcomm.loader.ObjectFactory;
import org.mobicents.servlet.restcomm.loader.ObjectInstantiationException;
import org.mobicents.servlet.restcomm.mgcp.PowerOnMediaGateway;
import org.mobicents.servlet.restcomm.mscontrol.MediaServerControllerFactory;
import org.mobicents.servlet.restcomm.mscontrol.MediaServerInfo;
import org.mobicents.servlet.restcomm.mscontrol.mgcp.MmsControllerFactory;
import org.mobicents.servlet.restcomm.mscontrol.xms.XmsControllerFactory;
import org.mobicents.servlet.restcomm.telephony.config.ConfigurationStringLookup;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public final class Bootstrapper extends SipServlet {
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
        Configuration settings = configuration.subset("mscontrol");
        String compatibility = settings.getString("compatibility");

        MediaServerControllerFactory factory;
        switch (compatibility) {
            case "mms":
                ActorRef gateway;
                try {
                    gateway = gateway(configuration, loader);
                    factory = new MmsControllerFactory(this.system, gateway);
                } catch (UnknownHostException e) {
                    throw new ServletException(e);
                }
                break;

            case "xms":
                try {
                    MediaServerInfo mediaServerInfo = mediaServerInfo(settings);
                    factory = new XmsControllerFactory(system, mediaServerInfo);
                } catch (UnknownHostException e) {
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
        final int timeout = configuration.getInt("media-server.timeout");
        return new MediaServerInfo(name, InetAddress.getByName(address), port, timeout);
    }

    private ActorRef gateway(final Configuration configuration, final ClassLoader loader) throws UnknownHostException {
        final Configuration settings = configuration.subset("media-server-manager");
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

    private String home(final ServletConfig config) {
        final ServletContext context = config.getServletContext();
        final String path = context.getRealPath("/");
        if (path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        } else {
            return path;
        }
    }

    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);
        final ServletContext context = config.getServletContext();
        final String path = context.getRealPath("WEB-INF/conf/restcomm.xml");
        // Initialize the configuration interpolator.
        final ConfigurationStringLookup strings = new ConfigurationStringLookup();
        strings.addProperty("home", home(config));
        strings.addProperty("uri", uri(config));
        ConfigurationInterpolator.registerGlobalLookup("restcomm", strings);
        // Load the RestComm configuration file.
        Configuration xml = null;
        try {
            xml = new XMLConfiguration(path);
        } catch (final ConfigurationException exception) {
            logger.error(exception);
        }
        xml.setProperty("runtime-settings.home-directory", home(config));
        xml.setProperty("runtime-settings.root-uri", uri(config));
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
            throw new ServletException(exception);
        }
        context.setAttribute(DaoManager.class.getName(), storage);
        ShiroResources.getInstance().set(DaoManager.class, storage);
        ShiroResources.getInstance().set(Configuration.class, xml.subset("runtime-settings"));

        // Create the media server controller factory
        MediaServerControllerFactory mscontrollerFactory = mediaServerControllerFactory(xml, loader);
        context.setAttribute(MediaServerControllerFactory.class.getName(), mscontrollerFactory);

        Version.printVersion();
        Ping ping = new Ping(xml, context);
        ping.sendPing();
    }

    private DaoManager storage(final Configuration configuration, final ClassLoader loader) throws ObjectInstantiationException {
        final String classpath = configuration.getString("dao-manager[@class]");
        final DaoManager daoManager = (DaoManager) new ObjectFactory(loader).getObjectInstance(classpath);
        daoManager.configure(configuration);
        daoManager.start();
        return daoManager;
    }

    private String uri(final ServletConfig config) {
        return config.getServletContext().getContextPath();
    }
}
