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
package org.mobicents.servlet.sip.restcomm.callmanager.presence;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;

import org.mobicents.servlet.sip.restcomm.Client;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.sip.restcomm.dao.ClientsDao;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.util.DigestAuthentication;
import org.mobicents.servlet.sip.restcomm.util.HexadecimalUtils;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class PresenceManager extends SipServlet {
  private static final long serialVersionUID = 1L;
  private final DaoManager daos;
  
  public PresenceManager() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    daos = services.get(DaoManager.class);
  }
  
  private String createNonce() {
    final byte[] uuid = UUID.randomUUID().toString().getBytes();
    final char[] hex = HexadecimalUtils.toHex(uuid);
	return new String(hex).substring(0, 31);
  }
  
  private String createAuthenticateHeader(final String nonce, final String realm, final String scheme) {
	final StringBuilder buffer = new StringBuilder();
	buffer.append(scheme).append(" ");
	buffer.append("realm=\"").append(realm).append("\", ");
	buffer.append("nonce=\"").append(nonce).append("\"");
    return buffer.toString();
  }
  
  private SipServletResponse createAuthenticateResponse(final SipServletRequest request) {
    final SipServletResponse response = request.createResponse(SipServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED);
    final String nonce = createNonce();
    final SipURI uri = (SipURI)request.getTo().getURI();
    final String realm = uri.getHost();
    final String header = createAuthenticateHeader(nonce, realm, "Digest");
    response.addHeader("Proxy-Authenticate", header);
    return response;
  }

  @Override protected void doRegister(final SipServletRequest request) throws ServletException, IOException {
    
  }

  @Override public void destroy() {
    
  }

  @Override public void init(final ServletConfig configuration) throws ServletException {
    
  }
  
  private boolean isPermitted(final String header, final String method) {
  	final Map<String, String> authorization = toMap(header);
  	final String user = authorization.get("username");
    final String algorithm = authorization.get("algorithm");
    final String realm = authorization.get("realm");
    final String uri = authorization.get("uri");
    final String nonce = authorization.get("nonce");
    final String nc = authorization.get("nc");
    final String cnonce = authorization.get("cnonce");
    final String qop = authorization.get("qop");
    final String response = authorization.get("response");
    final ClientsDao dao = daos.getClientsDao();
    final Client client = dao.getClient(user);
    final String password = client.getPassword();
    final String result =  DigestAuthentication.response(algorithm, user, realm, password, nonce, nc,
        cnonce, method, uri, null, qop);
    return result.equals(response);
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
