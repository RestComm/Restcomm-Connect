/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.restcomm.telephony;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.sip.SipApplicationSessionEvent;
import javax.servlet.sip.SipApplicationSessionListener;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.interpol.ConfigurationInterpolator;
import org.apache.log4j.Logger;

import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.mgcp.PowerOnMediaGateway;
import org.mobicents.servlet.restcomm.telephony.config.ConfigurationStringLookup;
import org.mobicents.servlet.restcomm.telephony.config.ObjectFactory;
import org.mobicents.servlet.restcomm.telephony.config.ObjectInstantiationException;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class CallManagerProxy extends SipServlet implements SipApplicationSessionListener {
  private static final long serialVersionUID = 1L;
  private static final Logger logger = Logger.getLogger(CallManagerProxy.class);
  
  private ActorSystem system;
  private ActorRef manager;

  public CallManagerProxy() {
    super();
  }
  
  @Override public void destroy() {
    system.shutdown();
    system.awaitTermination();
  }
  
  @Override protected void doRequest(final SipServletRequest request)
      throws ServletException, IOException {
    manager.tell(request, null);
  }

  @Override protected void doResponse(final SipServletResponse response)
      throws ServletException, IOException {
    manager.tell(response, null);
  }
  
  private ActorRef gateway(final Configuration configuration,
      final ClassLoader loader) throws UnknownHostException {
    final Configuration settings = configuration.subset("media-server-manager");
    final ActorRef gateway = system.actorOf(new Props(new UntypedActorFactory() {
		private static final long serialVersionUID = 1L;
		@Override public UntypedActor create() throws Exception {
		  final String classpath = settings.getString("mgcp-server[@class]");
          return (UntypedActor)new ObjectFactory(loader).getObjectInstance(classpath);
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
	if(address != null) {
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
    if(path.endsWith("/")) {
      return path.substring(0, path.length() - 1);
    } else {
      return path;
    }
  }
  
  @Override public void init(final ServletConfig config) throws ServletException {
    final ServletContext context = config.getServletContext();
    final String path = context.getRealPath("WEB-INF/conf/restcomm.xml");
    // Initialize the configuration interpolator.
    final ConfigurationStringLookup strings = new ConfigurationStringLookup();
    strings.addProperty("home", home(config));
    strings.addProperty("uri", uri(config));
    ConfigurationInterpolator.registerGlobalLookup("restcomm", strings);
    // Load the RestComm configuration file.
    XMLConfiguration xml = null;
    try {
	  xml = new XMLConfiguration(path);
	} catch(final ConfigurationException exception) {
      logger.error(exception);
	}
    xml.setProperty("runtime-settings.home-directory", home(config));
    xml.setProperty("runtime-settings.root-uri", uri(config));
    context.setAttribute(Configuration.class.getName(), xml);
    // Initialize global dependencies.
    DaoManager storage = null;
    final ClassLoader loader = context.getClass().getClassLoader();
    try {
      storage = storage(xml, loader);
    } catch(final ObjectInstantiationException exception) {
      throw new ServletException(exception);
    }
    context.setAttribute(DaoManager.class.getName(), storage);
    // Create the actor system.
    final Config settings = ConfigFactory.load();
    system = ActorSystem.create("RestComm", settings, loader);
    // Share the actor system with other servlets.
    context.setAttribute(ActorSystem.class.getName(), system);
    // Create the media gateway.
    ActorRef gateway = null;
    try {
      gateway = gateway(xml, loader);
    } catch(final UnknownHostException exception) {
      throw new ServletException(exception);
    }
    // Create the call manager.
    final SipFactory factory = (SipFactory)context.getAttribute(SIP_FACTORY);
    manager = manager(xml.subset("runtime-settings"), gateway, factory, storage);
  }
  
  private ActorRef manager(final Configuration configuration, final ActorRef gateway,
      final SipFactory factory, final DaoManager storage) {
    return system.actorOf(new Props(new UntypedActorFactory() {
		private static final long serialVersionUID = 1L;
		@Override public UntypedActor create() throws Exception {
          return new CallManager(configuration, system, gateway, factory, storage);
		}
    }));
  }
  
  private DaoManager storage(final Configuration configuration,
      final ClassLoader loader) throws ObjectInstantiationException {
    final String classpath = configuration.getString("dao-manager[@class]");
    final DaoManager daoManager = (DaoManager)new ObjectFactory(loader)
        .getObjectInstance(classpath);
    daoManager.configure(configuration.subset("dao-manager"));
    daoManager.start();
    return daoManager;
  }

  @Override public void sessionCreated(final SipApplicationSessionEvent event) {
    // Nothing to do.
  }

  @Override public void sessionDestroyed(final SipApplicationSessionEvent event) {
	// Nothing to do.
  }

  @Override public void sessionExpired(final SipApplicationSessionEvent event) {
    manager.tell(event, null);
  }

  @Override public void sessionReadyToInvalidate(final SipApplicationSessionEvent event) {
	// Nothing to do.
  }
  
  private String uri(final ServletConfig config) {
	return config.getServletContext().getContextPath();
  }
}
