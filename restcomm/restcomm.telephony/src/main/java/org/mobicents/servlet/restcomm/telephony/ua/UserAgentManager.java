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
package org.mobicents.servlet.restcomm.telephony.ua;

import akka.actor.ActorContext;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.io.IOException;
import static java.lang.Integer.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import static javax.servlet.sip.SipServlet.*;
import javax.servlet.sip.Address;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import static javax.servlet.sip.SipServletResponse.*;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

import org.joda.time.DateTime;

import org.mobicents.servlet.restcomm.dao.ClientsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.RegistrationsDao;
import org.mobicents.servlet.restcomm.entities.Client;
import org.mobicents.servlet.restcomm.entities.Registration;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.util.DigestAuthentication;
import static org.mobicents.servlet.restcomm.util.HexadecimalUtils.*;
import static org.mobicents.servlet.restcomm.util.IPUtils.*;

import scala.concurrent.duration.Duration;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author jean.deruelle@telestax.com
 */
public final class UserAgentManager extends UntypedActor {
  private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
  private final ServletConfig configuration;
  private final SipFactory factory;
  private final DaoManager storage;
  
  public UserAgentManager(final ServletConfig configuration,
      final SipFactory factory, final DaoManager storage) {
    super();
    this.configuration = configuration;
    this.factory = factory;
    this.storage = storage;
    final ActorContext context = context();
    context.setReceiveTimeout(Duration.create(60, TimeUnit.SECONDS));
  }
  
  private void clean() {
    final RegistrationsDao registrations = storage.getRegistrationsDao();
    final List<Registration> results = registrations.getRegistrations();
    for(final Registration result : results) {
      final DateTime expires = result.getDateExpires();
      if(expires.isBeforeNow() || expires.isEqualNow()) {
        registrations.removeRegistration(result);
      }
    }
  }
  
  private String header(final String nonce, final String realm, final String scheme) {
	final StringBuilder buffer = new StringBuilder();
	buffer.append(scheme).append(" ");
	buffer.append("realm=\"").append(realm).append("\", ");
	buffer.append("nonce=\"").append(nonce).append("\"");
    return buffer.toString();
  }
  
  private void authenticate(final Object message) throws IOException {
    final SipServletRequest request = (SipServletRequest)message;
    final SipServletResponse response = request.createResponse(SC_PROXY_AUTHENTICATION_REQUIRED);
    final String nonce = nonce();
    final SipURI uri = (SipURI)request.getTo().getURI();
    final String realm = uri.getHost();
    final String header = header(nonce, realm, "Digest");
    response.addHeader("Proxy-Authenticate", header);
    response.send();
  }
  
  private void keepAlive() throws Exception {
    final RegistrationsDao registrations = storage.getRegistrationsDao();
    final List<Registration> results = registrations.getRegistrations();
    for(final Registration result : results) {
      final String to = result.getLocation();
      ping(to);
    }
  }
  
  private String nonce() {
    final byte[] uuid = UUID.randomUUID().toString().getBytes();
    final char[] hex = toHex(uuid);
	return new String(hex).substring(0, 31);
  }

  @Override public void onReceive(final Object message) throws Exception {
    if(message instanceof ReceiveTimeout) {
      clean();
      keepAlive();
    } else if(message instanceof SipServletRequest) {
      final SipServletRequest request = (SipServletRequest)message;
      final String method = request.getMethod();
      if("REGISTER".equalsIgnoreCase(method)) {
        final String authorization = request.getHeader("Proxy-Authorization");
        if(authorization != null && permitted(authorization, method)) {
          register(message);
        } else {
          authenticate(message);
        }
      }
    } else if(message instanceof SipServletResponse) {
      pong(message);
    }
  }
  
  private void patch(final SipURI uri, final String address, final int port)
      throws UnknownHostException {
    final InetAddress host = InetAddress.getByName(uri.getHost());
	final String ip = host.getHostAddress();
	if(!isRoutableAddress(ip)) {
	  uri.setHost(address);
	} else {
	  uri.setHost(ip);
	}
	uri.setPort(port);
  }
  
  private boolean permitted(final String authorization, final String method) {
  	final Map<String, String> map = toMap(authorization);
  	final String user = map.get("username");
    final String algorithm = map.get("algorithm");
    final String realm = map.get("realm");
    final String uri = map.get("uri");
    final String nonce = map.get("nonce");
    final String nc = map.get("nc");
    final String cnonce = map.get("cnonce");
    final String qop = map.get("qop");
    final String response = map.get("response");
    final ClientsDao clients = storage.getClientsDao();
    final Client client = clients.getClient(user);
    if(client != null && Client.ENABLED == client.getStatus()) {
      final String password = client.getPassword();
      final String result =  DigestAuthentication.response(algorithm, user, realm, password, nonce, nc,
          cnonce, method, uri, null, qop);
      return result.equals(response);
    } else {
      return false;
    }
  }
  
  private void ping(final String to) throws Exception {
    final SipApplicationSession application = factory.createApplicationSession();
    String toTransport = ((SipURI)factory.createURI(to)).getTransportParam();
    if(toTransport.equalsIgnoreCase("ws") || toTransport.equalsIgnoreCase("wss")) {
        return ;
    }
	final SipURI outboundInterface = outboundInterface(toTransport);
	StringBuilder buffer = new StringBuilder();
	buffer.append("sip:restcomm").append("@").append(outboundInterface.getHost());
	final String from = buffer.toString();
	final SipServletRequest ping = factory.createRequest(application, "OPTIONS", from, to);
	final SipURI uri = (SipURI)factory.createURI(to);
	ping.pushRoute(uri);
	ping.setRequestURI(uri);
	final SipSession session = ping.getSession();
	session.setHandler("UserAgentManager");
	ping.send();
  }
  
  private void pong(final Object message) {
    final SipServletResponse response = (SipServletResponse)message;
//    if(response.getSession().isValid()) {
//        response.getSession().invalidate();
//    }
    if(response.getApplicationSession().isValid()) {
        response.getApplicationSession().invalidate();
    }
  }
  
  private SipURI outboundInterface(String toTransport) {
	final ServletContext context = configuration.getServletContext();
	SipURI result = null;
	@SuppressWarnings("unchecked")
	final List<SipURI> uris = (List<SipURI>)context.getAttribute(OUTBOUND_INTERFACES);
	for(final SipURI uri : uris) {
	  final String transport = uri.getTransportParam();
	  if(toTransport != null && toTransport.equalsIgnoreCase(transport)) {
	    result = uri;
	  }
	}
	return result;
  }
  
  private void register(final Object message) throws Exception {
    final SipServletRequest request = (SipServletRequest)message;
    final Address contact = request.getAddressHeader("Contact");
    // Get the expiration time.
    int ttl = contact.getExpires();
    if(ttl == -1) {
      final String expires = request.getHeader("Expires");
      if(expires != null) {
        ttl = parseInt(expires);
      } else {
        ttl = 3600;
      }
    }
    // Make sure registrations don't last more than 1 hour.
    if(ttl > 3600) {
      ttl = 3600;
    }
    // Get the rest of the information needed for a registration record.
    String name = contact.getDisplayName();
    String ua = request.getHeader("User-Agent");
    final SipURI to = (SipURI)request.getTo().getURI();
    final String aor = to.toString();
    final String user = to.getUser();
    final SipURI uri = (SipURI)contact.getURI();
    final String ip = request.getInitialRemoteAddr();
    final int port = request.getInitialRemotePort();
    final String transport = uri.getTransportParam();
    patch(uri, ip, port);
    final StringBuffer buffer = new StringBuffer();
    buffer.append("sip:").append(user).append("@")
        .append(uri.getHost()).append(":").append(uri.getPort());
    // https://bitbucket.org/telestax/telscale-restcomm/issue/142/restcomm-support-for-other-transports-than
    if(transport != null) {
        buffer.append(";transport=").append(transport);
    }
    final String address = buffer.toString();
    // Prepare the response.
    final SipServletResponse response = request.createResponse(SC_OK);
    // Update the data store.
    final Sid sid = Sid.generate(Sid.Type.REGISTRATION);
    final DateTime now = DateTime.now();
    
    //Issue 87 (http://www.google.com/url?q=https://bitbucket.org/telestax/telscale-restcomm/issue/87/verb-and-not-working-for-end-to-end-calls%23comment-5855486&usd=2&usg=ALhdy2_mIt4FU4Yb_EL-s0GZCpBG9BB8eQ)
    //if display name or UA are null, the hasRegistration returns 0 even if there is a registration
    if(name==null)
    	name = user;
    if(ua==null)
    	ua="GenericUA";
    
    final Registration registration = new Registration(sid, now, now, aor,
        name, user, ua, ttl, address);
    final RegistrationsDao registrations = storage.getRegistrationsDao();

	if(ttl == 0) {
      //Remove Registration if ttl=0
      registrations.removeRegistration(registration);
      response.setHeader("Expires", "0");
      logger.info("The user agent manager unregistered " + user);
    } else {

      if(registrations.hasRegistration(registration)) {
          //Update Registration if exists
        registrations.updateRegistration(registration);
        logger.info("The user agent manager updated " + user);
      } else {
        //Add registration since it doesn't exists on the DB
        registrations.addRegistration(registration);
        logger.info("The user agent manager registered " + user);
      }
      response.setHeader("Contact", contact(uri, ttl));
    }
    // Success
    response.send();
    // Cleanup
//    if(request.getSession().isValid()) {
//        request.getSession().invalidate();
//    }
    if(request.getApplicationSession().isValid()) {
        request.getApplicationSession().invalidate();
    }
  }
  
  private String contact(final SipURI uri, final int expires) {
    final Address contact = factory.createAddress(uri);
    contact.setExpires(expires);
    return contact.toString();
  }
  
  private Map<String, String> toMap(final String header) {
	final Map<String, String> map = new HashMap<String, String>();
	final int endOfScheme = header.indexOf(" ");
	map.put("scheme", header.substring(0, endOfScheme).trim());
	final String[] tokens = header.substring(endOfScheme + 1).split(",");
	for(final String token : tokens) {
	  final String[] values = token.trim().split("=");
	  map.put(values[0].toLowerCase(), values[1].replace("\"", ""));
	}
    return map;
  }
}
