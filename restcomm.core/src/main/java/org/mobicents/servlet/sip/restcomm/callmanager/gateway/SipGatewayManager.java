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
package org.mobicents.servlet.sip.restcomm.callmanager.gateway;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.sip.Address;
import javax.servlet.sip.AuthInfo;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipApplicationSession;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;
import javax.servlet.sip.TimerService;

import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.entities.Gateway;
import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.dao.DaoManager;
import org.mobicents.servlet.sip.restcomm.dao.GatewaysDao;
import org.mobicents.servlet.sip.restcomm.util.TimeUtils;
import org.mobicents.servlet.sip.restcomm.xml.rcml.attributes.From;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class SipGatewayManager extends SipServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger logger = Logger.getLogger(SipGatewayManager.class);
	private static final String userAgent = "RestComm/1.0";

	public static final int defaultRegistrationTtl = 1800;

	private TimerService clock;
	private SipFactory sipFactory;
	private SipURI outboundInterface;
	private List<Gateway> gateways;

	public SipGatewayManager() {
		super();
	}

	private Address createContactHeader(final Gateway gateway, final int expires) throws ServletParseException {
		final StringBuilder buffer = new StringBuilder();
		buffer.append("sip:").append(gateway.getUser()).append("@").append(outboundInterface.getHost());
		final Address contact = sipFactory.createAddress(buffer.toString());
		contact.setExpires(expires);
		return contact;
	}

	@Override public void destroy() {
		for(final Gateway gateway : gateways) {
			if(gateway.register()) {
				register(gateway, 0);
			}
		}
	}

	@Override protected void doErrorResponse(final SipServletResponse response) throws ServletException, IOException {
		final String method = response.getRequest().getMethod();
		if("REGISTER".equalsIgnoreCase(method)) {
			final int status = response.getStatus();
			if(status == SipServletResponse.SC_UNAUTHORIZED || status == SipServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED) {
				final SipApplicationSession application = response.getApplicationSession();
				final Gateway gateway = (Gateway)application.getAttribute(Gateway.class.getName());
				final AuthInfo authentication = sipFactory.createAuthInfo();
				final String realm = response.getChallengeRealms().next(); 
				authentication.addAuthInfo(status, realm, gateway.getUser(), gateway.getPassword());
				register(gateway, defaultRegistrationTtl, authentication, response);
			}
		}
	}

	@Override protected void doSuccessResponse(final SipServletResponse response) throws ServletException, IOException {
		final String method = response.getRequest().getMethod();
		if("REGISTER".equalsIgnoreCase(method)) {
			final int status = response.getStatus();
			if(status == SipServletResponse.SC_OK) {
				final SipApplicationSession application = response.getApplicationSession();
				final Gateway gateway = (Gateway)application.getAttribute(Gateway.class.getName());
				final Address contact = response.getAddressHeader("Contact");
				final long expires = contact.getExpires() * 1000;
				clock.createTimer(application, expires, false, "REGISTER");
				application.setExpires(TimeUtils.millisToMinutes(expires));
				if(logger.isDebugEnabled()) {
					final StringBuilder buffer = new StringBuilder();
					buffer.append("Successfully registered\n");
					buffer.append(gateway.toString()).append("\n");
					buffer.append("for a duration of ").append(expires).append(" seconds.");
					logger.debug(buffer.toString());
				}
			}
		}
	}

	private SipURI getOutboundInterface(final ServletConfig config) {
		final ServletContext context = config.getServletContext();
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

	@Override public void init(ServletConfig config) throws ServletException {
		final ServletContext context = config.getServletContext();
		final ServiceLocator services = ServiceLocator.getInstance();
		final DaoManager daos = services.get(DaoManager.class);
		final GatewaysDao dao = daos.getGatewaysDao();
		gateways = dao.getGateways();
		clock = (TimerService)config.getServletContext().getAttribute(TIMER_SERVICE);
		outboundInterface = getOutboundInterface(config);
		sipFactory = (SipFactory)context.getAttribute(SIP_FACTORY);
	}

	public void register(final Gateway gateway, final int expires) {
		register(gateway, expires, null, null);
	}

	private void register(final Gateway gateway, final int expires, final AuthInfo authentication,
			final SipServletResponse response) {
		try {
			final SipApplicationSession application = sipFactory.createApplicationSession();
			application.setAttribute(Gateway.class.getName(), gateway);
			application.setAttribute(SipGatewayManager.class.getName(), this);
			final StringBuilder buffer = new StringBuilder();
			buffer.append("sip:").append(gateway.getUser()).append("@").append(gateway.getProxy());
			final String aor = buffer.toString();
			//Issue http://code.google.com/p/restcomm/issues/detail?id=65
			SipServletRequest register = null;
			if (response != null){
				register = response.getSession().createRequest(response.getRequest().getMethod());
			} else {
				register = sipFactory.createRequest(application, "REGISTER", aor, aor);
			}

			if(authentication != null && response != null) {
				register.addAuthHeader(response, authentication);
			}
			register.addAddressHeader("Contact", createContactHeader(gateway, expires), false);
			register.addHeader("User-Agent", userAgent);
			final SipURI uri = sipFactory.createSipURI(null, gateway.getProxy());
			register.pushRoute(uri);
			register.setRequestURI(uri);
			final SipSession session = register.getSession();
			session.setHandler("SipGatewayManager");
			register.send();
		} catch(final Exception exception) {
			logger.error(exception);
		}
	}

	public void start() {
		for(final Gateway gateway : gateways) {
			if(gateway.register()) {
				register(gateway, defaultRegistrationTtl);
			}
		}
	}
}
