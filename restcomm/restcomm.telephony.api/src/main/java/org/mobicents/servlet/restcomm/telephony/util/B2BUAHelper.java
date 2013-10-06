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
 */package org.mobicents.servlet.restcomm.telephony.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.RegistrationsDao;
import org.mobicents.servlet.restcomm.entities.Client;
import org.mobicents.servlet.restcomm.entities.Registration;

/**
 * 
 * Helper methods for proxying SIP messages between Restcomm clients 
 * that are connecting in peer to peer mode
 * 
 * @author ivelin.ivanov@telestax.com
 *
 */
public class B2BUAHelper {

	private static final String B2BUA_LAST_REQUEST = "lastRequest";
	private static final String B2BUA_LAST_RESPONSE = "lastResponse";
	private static final String B2BUA_LINKED_SESSION = "linkedSession";

    private static final Logger logger = Logger.getLogger(B2BUAHelper.class);
	  
	/**
	 * @param request
	 * @param client
	 * @param toClient
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 */
	public static boolean redirectToB2BUA(final SipServletRequest request,
			final Client client, Client toClient, DaoManager storage, SipFactory sipFactory) throws IOException,
			UnsupportedEncodingException {
		request.getSession().setAttribute("lastRequest", request);
		if(logger.isInfoEnabled()) {
		        logger.info("B2BUA (p2p proxy): Got request:\n"
		                        + request.getMethod());
		        logger.info(String.format("B2BUA: Proxying a session between %s and %s", client.getUri(), toClient.getUri()));
		}
		
		String user = ((SipURI) request.getTo().getURI()).getUser();
	
		final RegistrationsDao registrations = storage.getRegistrationsDao();
		final Registration registration = registrations.getRegistration(user);
		if(registration != null) {
		    final String location = registration.getLocation();
		    SipURI to;
			try {
				to = (SipURI)sipFactory.createURI(location);
	
				final SipSession incomingSession = request.getSession();
				//create and send the outgoing invite and do the session linking
				incomingSession.setAttribute(B2BUA_LAST_REQUEST, request); 
				SipServletRequest outRequest = sipFactory.createRequest(request.getApplicationSession(),
						request.getMethod(), request.getFrom().getURI(), request.getTo().getURI());
		        outRequest.setRequestURI(to);
		        if(request.getContent() != null) {
		                outRequest.setContent(request.getContent(), request.getContentType());
		        }
		        final SipSession outgoingSession = outRequest.getSession();
		        if(request.isInitial()) {
		        	incomingSession.setAttribute(B2BUA_LINKED_SESSION, outgoingSession);
		        	outgoingSession.setAttribute(B2BUA_LINKED_SESSION, incomingSession);
		        }
		        outgoingSession.setAttribute(B2BUA_LAST_REQUEST, outRequest);
		        request.createResponse(100).send();
		        outRequest.send();
		        return true; // successfully proxied the SIP request between two registered clients
			} catch (ServletParseException badUriEx) {
		        if(logger.isInfoEnabled()) {
		            logger.info(String.format("B2BUA: Error parsing Client Contact URI: %s", location), badUriEx);
		        }
			};
		}
		return false;
	}

	public static SipServletResponse getLinkedResponse(SipServletMessage message) {
	    SipSession linkedB2BUASession = getLinkedSession(message);
	    // if this is an ACK that belongs to a B2BUA session, then we proxy it to the other client
	    if (linkedB2BUASession != null) { 	  
	        SipServletResponse response = (SipServletResponse) linkedB2BUASession.
	      		  getAttribute(B2BUA_LAST_RESPONSE);
	        return response;
	    }
	    return null;
    }

	public static SipServletRequest getLinkedRequest(SipServletMessage message) {
		SipSession linkedB2BUASession = getLinkedSession(message);
		if (linkedB2BUASession != null) {
		    SipServletRequest linkedRequest = (SipServletRequest) linkedB2BUASession.getAttribute(B2BUA_LAST_REQUEST);
		    return linkedRequest;
		};
		return null;
    }	
	
	public static SipSession getLinkedSession(SipServletMessage message) {
		  return (SipSession)message.getSession().getAttribute(B2BUA_LINKED_SESSION);
	  }

	/**
	 * @param response
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 */
	public static void forwardResponse(final SipServletResponse response)
			throws IOException, UnsupportedEncodingException {
		if(logger.isInfoEnabled()) {
		    logger.info(String.format("B2BUA: Got response: \n %s", response));
		}
		// container handles CANCEL related responses no need to forward them
		if(response.getStatus() == 487 || (response.getStatus()==200 && response.getMethod().equalsIgnoreCase("CANCEL"))) {
			if(logger.isDebugEnabled()) {
				logger.debug("response to CANCEL not forwarding");
			}
			return;
		}
		// forward the response
		response.getSession().setAttribute(B2BUA_LAST_RESPONSE, response);
		SipServletRequest request = (SipServletRequest) getLinkedSession(response).getAttribute(B2BUA_LAST_REQUEST);	    
		SipServletResponse resp = request.createResponse(response.getStatus());
		if(response.getContent() != null) {
			resp.setContent(response.getContent(), response.getContentType());
		}
		resp.send();
	}

	/**
	 * Check whether a SIP request or response belongs to a peer to peer (B2BUA) session
	 *  
	 * @param sipMessage
	 * @return
	 */
	public static boolean isB2BUASession(SipServletMessage sipMessage) {
	    SipSession linkedB2BUASession = getLinkedSession(sipMessage);
	    return (linkedB2BUASession != null);
	}
	
}
