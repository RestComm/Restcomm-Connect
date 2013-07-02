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
package org.mobicents.servlet.restcomm.telephony.proxy;

import akka.actor.ActorContext;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import static javax.servlet.sip.SipServlet.*;

import javax.servlet.sip.Address;
import javax.servlet.sip.AuthInfo;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import static javax.servlet.sip.SipServletResponse.*;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.GatewaysDao;
import org.mobicents.servlet.restcomm.entities.Gateway;
import org.mobicents.servlet.restcomm.telephony.Version;

import scala.concurrent.duration.Duration;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class ProxyManager extends UntypedActor {
  private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
  
  private static final String version = Version.getInstance().getRestCommVersion();
  private static final String ua = "RestComm/" + version;
  private static final int ttl = 1800;

  private final ServletConfig configuration;
  private final SipFactory factory;
  private final DaoManager storage;
  private final String address;

  public ProxyManager(final ServletConfig configuration, final SipFactory factory,
      final DaoManager storage, final String address) {
    super();
    this.configuration = configuration;
    this.factory = factory;
    this.storage = storage;
    this.address = address;
    final ActorContext context = context();
    context.setReceiveTimeout(Duration.create(60, TimeUnit.SECONDS));
    refresh();
  }
  
  private void authenticate(final Object message) {
    final SipServletResponse response = (SipServletResponse)message;
    final SipApplicationSession application = response.getApplicationSession();
	final Gateway gateway = (Gateway)application.getAttribute(Gateway.class.getName());
	final int status = response.getStatus();
	final AuthInfo authentication = factory.createAuthInfo();
	final String realm = response.getChallengeRealms().next(); 
	final String user = gateway.getUserName();
	final String password = gateway.getPassword();
	authentication.addAuthInfo(status, realm, user, password);
	register(gateway, authentication, response);
  }
  
  private Address contact(final Gateway gateway, final int expires)
      throws ServletParseException {
	SipURI outboundInterface = null;
	if(address != null && !address.isEmpty()) {
	  outboundInterface = (SipURI)factory.createSipURI(null, address);
	} else {
	  outboundInterface = outboundInterface();
	}
	final String user = gateway.getUserName();
	final String host = outboundInterface.getHost();
	final int port = outboundInterface.getPort();
	final StringBuilder buffer = new StringBuilder();
	buffer.append("sip:").append(user).append("@").append(host).append(":").append(port);
	final Address contact = factory.createAddress(buffer.toString());
	contact.setExpires(expires);
	return contact;
  }

  @Override public void onReceive(Object message) throws Exception {
    if(message instanceof ReceiveTimeout) {
      refresh();
    } else if(message instanceof SipServletResponse) {
      final SipServletResponse response = (SipServletResponse)message;
      final int status = response.getStatus();
      switch(status) {
        case SC_PROXY_AUTHENTICATION_REQUIRED:
        case SC_UNAUTHORIZED: {
          authenticate(message);
        }
        case SC_OK: {
          update(message);
        }
      }
    }
  }
  
  private SipURI outboundInterface() {
	final ServletContext context = configuration.getServletContext();
	SipURI result = null;
	@SuppressWarnings("unchecked")
	final List<SipURI> uris = (List<SipURI>)context.getAttribute(OUTBOUND_INTERFACES);
	for(final SipURI uri : uris) {
	  final String transport = uri.getTransportParam();
	  if("udp".equalsIgnoreCase(transport)) {
	    result = uri;
	  }
	}
	return result;
  }
  
  private void refresh() {
    final GatewaysDao gateways = storage.getGatewaysDao();
    final List<Gateway> results = gateways.getGateways();
    for(final Gateway result : results) {
      final DateTime lastUpdate = result.getDateUpdated();
      final DateTime expires = lastUpdate.plusSeconds(result.getTimeToLive());
      if(expires.isBeforeNow() || expires.isEqualNow()) {
        register(result);
      }
    }
  }
  
  private void register(final Gateway gateway) {
    register(gateway, null, null);
  }
  
  private void register(final Gateway gateway, final AuthInfo authentication,
      final SipServletResponse response) {
    try {
	  final SipApplicationSession application = factory.createApplicationSession();
	  application.setAttribute(Gateway.class.getName(), gateway);
	  final String user = gateway.getUserName();
	  final String proxy = gateway.getProxy();
	  final StringBuilder buffer = new StringBuilder();
	  buffer.append("sip:").append(user).append("@").append(proxy);
	  final String aor = buffer.toString();
      // Issue http://code.google.com/p/restcomm/issues/detail?id=65
	  SipServletRequest register = null;
	  if(response != null){
	    final String method = response.getRequest().getMethod();
	    register = response.getSession().createRequest(method);
	  } else {
	    register = factory.createRequest(application, "REGISTER", aor, aor);
	  }
	  if(authentication != null && response != null) {
	    register.addAuthHeader(response, authentication);
	  }
	  final int expires = (gateway.getTimeToLive() > 0 && gateway.getTimeToLive() < 3600) ?
	      gateway.getTimeToLive() : ttl;
	  final Address contact = contact(gateway, expires);
	  register.addAddressHeader("Contact", contact, false);
	  register.addHeader("User-Agent", ua);
	  final SipURI uri = factory.createSipURI(null, proxy);
	  register.pushRoute(uri);
	  register.setRequestURI(uri);
	  final SipSession session = register.getSession();
	  session.setHandler("ProxyManager" );
	  register.send();
    } catch(final Exception exception) {
      final String name = gateway.getFriendlyName();
      logger.error(exception, "Could not send a registration request to the proxy named " + name);
    }
  }
  
  private void update(final Object message) {
    final SipServletResponse response = (SipServletResponse)message;
    final SipApplicationSession application = response.getApplicationSession();
    Gateway gateway = (Gateway)application.getAttribute(Gateway.class.getName());
    // This will force the gateway's dateUpdated field to now so we can use it
    // to determine if we need to re-register.
    gateway = gateway.setTimeToLive(gateway.getTimeToLive());
    final GatewaysDao gateways = storage.getGatewaysDao();
    gateways.updateGateway(gateway);
  }
}
