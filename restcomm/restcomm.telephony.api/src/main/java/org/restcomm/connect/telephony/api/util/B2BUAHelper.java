/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */package org.restcomm.connect.telephony.api.util;

 import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Currency;
import java.util.Vector;

 import javax.sdp.Connection;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.sdp.SessionName;
import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

 import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.mobicents.javax.servlet.sip.SipSessionExt;
 import org.restcomm.connect.telephony.api.CallStateChanged;
 import org.restcomm.connect.commons.configuration.RestcommConfiguration;
import org.restcomm.connect.dao.CallDetailRecordsDao;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.RegistrationsDao;
import org.restcomm.connect.dao.entities.CallDetailRecord;
import org.restcomm.connect.dao.entities.Client;
import org.restcomm.connect.dao.entities.Registration;
import org.restcomm.connect.commons.dao.Sid;

/**
  * Helper methods for proxying SIP messages between Restcomm clients that are connecting in peer to peer mode
  *
  * @author ivelin.ivanov@telestax.com
  * @author jean.deruelle@telestax.com
  * @author gvagenas@telestax.com
  */
 public class B2BUAHelper {

     public static final String FROM_INET_URI = "fromInetURI";
     public static final String TO_INET_URI = "toInetURI";
     public static final String B2BUA_LAST_REQUEST = "lastRequest";
     public static final String B2BUA_LAST_RESPONSE = "lastResponse";
     public static final String B2BUA_LAST_FINAL_RESPONSE = "lastFinalResponse";
     private static final String B2BUA_LINKED_SESSION = "linkedSession";
     private static final String CDR_SID = "callDetailRecord_sid";

     private static final Logger logger = Logger.getLogger(B2BUAHelper.class);

     // private static CallDetailRecord callRecord = null;
     private static DaoManager daoManager;

     /**
      * @param request
      * @param client
      * @param toClient
      * @throws IOException
      */
     // This is used for redirect calls to Restcomm clients from Restcomm Clients
     public static boolean redirectToB2BUA(final SipServletRequest request, final Client client, Client toClient,
                                           DaoManager storage, SipFactory sipFactory, final boolean patchForNat) throws IOException {
         request.getSession().setAttribute("lastRequest", request);

         if (logger.isInfoEnabled()) {
             logger.info("B2BUA (p2p proxy): Got request:\n" + request.getMethod());
             logger.info(String.format("B2BUA: Proxying a session between %s and %s", client.getLogin(), toClient.getLogin()));
         }

         if (daoManager == null) {
             daoManager = storage;
         }

         String user = ((SipURI) request.getTo().getURI()).getUser();

         final RegistrationsDao registrations = daoManager.getRegistrationsDao();
         try {
             final Registration registration = registrations.getRegistration(user);
             if (registration != null) {
                 final String location = registration.getLocation();
                 final String aor = registration.getAddressOfRecord();
                 SipURI to;
                 SipURI from;
                 to = (SipURI) sipFactory.createURI(location);
                 from = (SipURI) sipFactory.createURI((registrations.getRegistration(client.getLogin())).getLocation());
                 final SipSession incomingSession = request.getSession();
                 // create and send the outgoing invite and do the session linking
                 incomingSession.setAttribute(B2BUA_LAST_REQUEST, request);
                 SipServletRequest outRequest = sipFactory.createRequest(request.getApplicationSession(), request.getMethod(),
                         from, to);
                 outRequest.setRequestURI(to);

                 if (request.getContent() != null) {
                     final byte[] sdp = request.getRawContent();
                     String offer = null;
                     if (request.getContentType().equalsIgnoreCase("application/sdp") && patchForNat) {
                         // Issue 308: https://telestax.atlassian.net/browse/RESTCOMM-308
                         String externalIp = request.getInitialRemoteAddr();
                         // Issue 306: https://telestax.atlassian.net/browse/RESTCOMM-306
                         final String initialIpBeforeLB = request.getHeader("X-Sip-Balancer-InitialRemoteAddr");
                         try {
                             if (initialIpBeforeLB != null && !initialIpBeforeLB.isEmpty()) {
                                 offer = patch(sdp, initialIpBeforeLB);
                             } else {
                                 offer = patch(sdp, externalIp);
                             }
                         } catch (SdpException e) {
                             logger.error("Unexpected exception while patching sdp ", e);
                         }
                     }
                     if (offer != null) {
                         outRequest.setContent(offer, request.getContentType());
                     } else {
                         outRequest.setContent(sdp, request.getContentType());
                     }
                 }

                 final SipSession outgoingSession = outRequest.getSession();
                 if (request.isInitial()) {
                     incomingSession.setAttribute(B2BUA_LINKED_SESSION, outgoingSession);
                     outgoingSession.setAttribute(B2BUA_LINKED_SESSION, incomingSession);
                 }
                 outgoingSession.setAttribute(B2BUA_LAST_REQUEST, outRequest);
                 request.createResponse(100).send();
                 // Issue #307: https://telestax.atlassian.net/browse/RESTCOMM-307
                 request.getSession().setAttribute(TO_INET_URI, to);
                 if (logger.isInfoEnabled())
                     logger.info("bypassLoadBalancer is set to: " + RestcommConfiguration.getInstance().getMain().getBypassLbForClients());
                 if (RestcommConfiguration.getInstance().getMain().getBypassLbForClients()) {
                     ((SipSessionExt) outRequest.getSession()).setBypassLoadBalancer(true);
                     ((SipSessionExt) outRequest.getSession()).setBypassProxy(true);
                 }
                 outRequest.send();
                 outRequest.getSession().setAttribute(FROM_INET_URI, from);

                 final CallDetailRecord.Builder builder = CallDetailRecord.builder();
                 builder.setSid(Sid.generate(Sid.Type.CALL));
                 builder.setInstanceId(RestcommConfiguration.getInstance().getMain().getInstanceId());
                 builder.setDateCreated(DateTime.now());
                 builder.setAccountSid(client.getAccountSid());
                 builder.setTo(toClient.getLogin());
                 builder.setCallerName(client.getFriendlyName());
                 builder.setFrom(client.getLogin());
                 // builder.setForwardedFrom(callInfo.forwardedFrom());
                 // builder.setPhoneNumberSid(phoneId);
                 builder.setStatus(CallStateChanged.State.QUEUED.name());
                 builder.setDirection("Client-To-Client");
                 builder.setApiVersion(client.getApiVersion());
                 builder.setPrice(new BigDecimal("0.00"));
                 // TODO implement currency property to be read from Configuration
                 builder.setPriceUnit(Currency.getInstance("USD"));
                 final StringBuilder buffer = new StringBuilder();
                 buffer.append("/").append(client.getApiVersion()).append("/Accounts/");
                 buffer.append(client.getAccountSid().toString()).append("/Calls/");
                 buffer.append(client.getSid().toString());
                 final URI uri = URI.create(buffer.toString());
                 builder.setUri(uri);

                 CallDetailRecordsDao records = daoManager.getCallDetailRecordsDao();
                 CallDetailRecord callRecord = builder.build();
                 records.addCallDetailRecord(callRecord);

                 incomingSession.setAttribute(CDR_SID, callRecord.getSid());
                 outgoingSession.setAttribute(CDR_SID, callRecord.getSid());

                 return true; // successfully proxied the SIP request between two registered clients
             }
         } catch (Exception e) {
             if (logger.isInfoEnabled()) {
//                 logger.info(String.format("B2BUA: Error parsing Client Contact URI: %s", location), badUriEx);
                 logger.info("Cannot proxy client to client call");
             }
         }
         return false;
     }

     /**
      * @param request
      * @param client
      * @param toClient
      * @throws IOException
      */
     // https://telestax.atlassian.net/browse/RESTCOMM-335
     // This is used for redirect calls to PSTN Numbers and SIP URIs from Restcomm Clients
     public static boolean redirectToB2BUA(final SipServletRequest request, final Client fromClient, final SipURI from,
             SipURI to, String proxyUsername, String proxyPassword, DaoManager storage, SipFactory sipFactory,
             boolean callToSipUri, final boolean patchForNat) {
         request.getSession().setAttribute("lastRequest", request);
         if (logger.isInfoEnabled()) {
             logger.info("B2BUA (p2p proxy for DID and SIP URIs) - : Got request:\n" + request.getRequestURI().toString());
             logger.info(String.format("B2BUA: Proxying a session from %s to %s", from, to));
         }

         if (daoManager == null) {
             daoManager = storage;
         }

         try {
             final SipSession incomingSession = request.getSession();
             // create and send the outgoing invite and do the session linking
             incomingSession.setAttribute(B2BUA_LAST_REQUEST, request);
             SipServletRequest outRequest = null;
             if (fromClient != null) {
                 Address fromAddress = sipFactory.createAddress(from, fromClient.getFriendlyName());
                 Address toAddress = sipFactory.createAddress(to, to.getUser());
                 outRequest = sipFactory.createRequest(request.getApplicationSession(), request.getMethod(), fromAddress,
                         toAddress);
             } else {
                 outRequest = sipFactory.createRequest(request.getApplicationSession(), request.getMethod(), from, to);
             }
             outRequest.setRequestURI(to);

             if(logger.isInfoEnabled()) {
                 logger.info("Request: " + request.getMethod() + " content exists: " + request.getContent() != null+ " content type: " + request.getContentType());
             }

             boolean isBehindLB = false;
             final String initialIpBeforeLB = request.getHeader("X-Sip-Balancer-InitialRemoteAddr");
             String initialPortBeforeLB = request.getHeader("X-Sip-Balancer-InitialRemotePort");
             if (initialIpBeforeLB != null) {
                 if (initialPortBeforeLB == null)
                     initialPortBeforeLB = "5060";
                 if (logger.isDebugEnabled()) {
                     logger.debug("We are behind load balancer, initial IP and Ports are  " + initialIpBeforeLB+":"+initialPortBeforeLB);
                 }
                 isBehindLB = true;
             }

             if (request.getContent() != null) {
                 final byte[] sdp = request.getRawContent();
                 String offer = null;
                 if (request.getContentType().equalsIgnoreCase("application/sdp") && patchForNat) {
                     // Issue 308: https://telestax.atlassian.net/browse/RESTCOMM-308
                     String externalIp = request.getInitialRemoteAddr();
                     // Issue 306: https://telestax.atlassian.net/browse/RESTCOMM-306
                     try {
                         if (initialIpBeforeLB != null && !initialIpBeforeLB.isEmpty()) {
                             offer = patch(sdp, initialIpBeforeLB);
                         } else {
                             offer = patch(sdp, externalIp);
                         }
                     } catch (SdpException e) {
                         logger.error("Unexpected exception while patching sdp ", e);
                     }
                 }
                 if (offer != null) {
                     outRequest.setContent(offer, request.getContentType());
                 } else {
                     outRequest.setContent(sdp, request.getContentType());
                 }
             }

             final SipSession outgoingSession = outRequest.getSession();
             if (request.isInitial()) {
                 incomingSession.setAttribute(B2BUA_LINKED_SESSION, outgoingSession);
                 outgoingSession.setAttribute(B2BUA_LINKED_SESSION, incomingSession);
             }
             outgoingSession.setAttribute(B2BUA_LAST_REQUEST, outRequest);
             request.createResponse(100).send();
             // Issue #307: https://telestax.atlassian.net/browse/RESTCOMM-307
             request.getSession().setAttribute(TO_INET_URI, to);

             if (callToSipUri) {
                 if (logger.isInfoEnabled())
                     logger.info("bypassLoadBalancer is set to: "+RestcommConfiguration.getInstance().getMain().getBypassLbForClients());
                 if (RestcommConfiguration.getInstance().getMain().getBypassLbForClients()) {
                     ((SipSessionExt) outRequest.getSession()).setBypassLoadBalancer(true);
                     ((SipSessionExt) outRequest.getSession()).setBypassProxy(true);
                 }
             }
             outRequest.send();
             Address originalFromAddress = request.getFrom();
             SipURI originalFromUri = (SipURI) originalFromAddress.getURI();
             int port = originalFromUri.getPort();
             if (port == -1) {
                 port = request.getRemotePort();
                 originalFromUri.setPort(port);
             }

             if(isBehindLB) {
                 // https://github.com/RestComm/Restcomm-Connect/issues/1357
                 String realIP = initialIpBeforeLB + ":" + initialPortBeforeLB;
                 SipURI uri = sipFactory.createSipURI(null, realIP);
                 if (logger.isDebugEnabled()) {
                     logger.debug("We are behind load balancer, storing initial IP and Ports " + uri);
                 }
                 outRequest.getSession().setAttribute(FROM_INET_URI, uri);
             } else {
                 if (logger.isDebugEnabled()) {
                     logger.debug("We are not behind load balancer, storing " + FROM_INET_URI +": " + to);
                 }
                 outRequest.getSession().setAttribute(FROM_INET_URI, originalFromUri);
             }

             final CallDetailRecord.Builder builder = CallDetailRecord.builder();
             builder.setSid(Sid.generate(Sid.Type.CALL));
             builder.setInstanceId(RestcommConfiguration.getInstance().getMain().getInstanceId());
             builder.setDateCreated(DateTime.now());
             builder.setAccountSid(fromClient.getAccountSid());
             builder.setTo(to.toString());
             builder.setCallerName(fromClient.getFriendlyName());
             builder.setFrom(fromClient.getFriendlyName());
             // builder.setForwardedFrom(callInfo.forwardedFrom());
             // builder.setPhoneNumberSid(phoneId);
             builder.setStatus(CallStateChanged.State.QUEUED.name());
             builder.setDirection("Client-To-Client");
             builder.setApiVersion(fromClient.getApiVersion());
             builder.setPrice(new BigDecimal("0.00"));
             // TODO implement currency property to be read from Configuration
             builder.setPriceUnit(Currency.getInstance("USD"));
             final StringBuilder buffer = new StringBuilder();
             buffer.append("/").append(fromClient.getApiVersion()).append("/Accounts/");
             buffer.append(fromClient.getAccountSid().toString()).append("/Calls/");
             buffer.append(fromClient.getSid().toString());
             final URI uri = URI.create(buffer.toString());
             builder.setUri(uri);

             CallDetailRecordsDao records = daoManager.getCallDetailRecordsDao();
             CallDetailRecord callRecord = builder.build();
             records.addCallDetailRecord(callRecord);

             incomingSession.setAttribute(CDR_SID, callRecord.getSid());
             outgoingSession.setAttribute(CDR_SID, callRecord.getSid());

             return true; // successfully proxied the SIP request
         } catch (IOException exception) {
             if (logger.isInfoEnabled()) {
                 logger.info(String.format("B2BUA: Error while trying to proxy request from %s to %s", from, to));
                 logger.info("Exception: " + exception);
             }
         }
         return false;
     }

     // Issue 308: https://telestax.atlassian.net/browse/RESTCOMM-308
     @SuppressWarnings("unchecked")
     private static String patch(final byte[] data, final String externalIp) throws UnknownHostException, SdpException {
         final String text = new String(data);
         if(logger.isInfoEnabled()){
             logger.info("About to patch ");
             logger.info("SDP :" + text);
             logger.info("Using externalIP: " + externalIp);
         }
         final SessionDescription sdp = SdpFactory.getInstance().createSessionDescription(text);
         SessionName sessionName = SdpFactory.getInstance().createSessionName("Restcomm B2BUA");
         sdp.setSessionName(sessionName);
         // Handle the connection at the session level.
         fix(sdp.getConnection(), externalIp);
         // Handle the connections at the media description level.
         final Vector<MediaDescription> descriptions = sdp.getMediaDescriptions(false);
         for (final MediaDescription description : descriptions) {
             fix(description.getConnection(), externalIp);
         }
         sdp.getOrigin().setAddress(externalIp);
         return sdp.toString();
     }

     // Issue 308: https://telestax.atlassian.net/browse/RESTCOMM-308
     @SuppressWarnings("unused")
     private static void fix(final Connection connection, final String externalIp) throws UnknownHostException, SdpException {
         if (connection != null) {
             if (Connection.IN.equals(connection.getNetworkType())) {
                 if (Connection.IP4.equals(connection.getAddressType())) {
                     final InetAddress address = InetAddress.getByName(connection.getAddress());
                     if (address.isSiteLocalAddress() || address.isAnyLocalAddress() || address.isLoopbackAddress()) {
                         final String ip = address.getHostAddress();
                         connection.setAddress(externalIp);
                     }
                 }
             }
         }
     }

     public static SipServletResponse getLinkedResponse(SipServletMessage message) {
         SipSession linkedB2BUASession = getLinkedSession(message);
         // if this is an ACK that belongs to a B2BUA session, then we proxy it to the other client
         if (linkedB2BUASession != null) {
             SipServletResponse response = (SipServletResponse) linkedB2BUASession.getAttribute(B2BUA_LAST_RESPONSE);
             return response;
         }
         return null;
     }

     public static SipServletRequest getLinkedRequest(SipServletMessage message) {
         SipSession linkedB2BUASession = getLinkedSession(message);
         if (linkedB2BUASession != null) {
             SipServletRequest linkedRequest = (SipServletRequest) linkedB2BUASession.getAttribute(B2BUA_LAST_REQUEST);
             return linkedRequest;
         }
         return null;
     }

     public static SipSession getLinkedSession(SipServletMessage message) {

         SipSession sipSession = null;
         if (message.getSession().isValid()) {
             sipSession = (SipSession) message.getSession().getAttribute(B2BUA_LINKED_SESSION);
         }
         if (sipSession == null) {
             if(logger.isInfoEnabled()) {
                 logger.info("SIP SESSION is NULL");
             }
         }
         return sipSession;
     }

     /**
      * @param response
      * @throws IOException
      */
     public static void forwardResponse(final SipServletResponse response, final boolean patchForNat) throws IOException {
         if (logger.isInfoEnabled()) {
             logger.info(String.format("B2BUA: Got response: \n %s", response));
         }
         if (response.getSession() == null || !response.getSession().isValid()) {
             if (logger.isDebugEnabled()) {
                 logger.debug("Response session is either null or invalidated");
             }
             return;
         }
         if (getLinkedSession(response) == null || !getLinkedSession(response).isValid()) {
             if (logger.isDebugEnabled()) {
                 logger.debug("Linked session of response is either null or invalidated");
             }
             return;
         }
         CallDetailRecordsDao records = daoManager.getCallDetailRecordsDao();

         if (response.getStatus() > 200)
             response.getSession().setAttribute(B2BUA_LAST_FINAL_RESPONSE, response);

         // container handles CANCEL related responses no need to forward them
         if (response.getStatus() == 487 || (response.getStatus() == 200 && response.getMethod().equalsIgnoreCase("CANCEL"))) {
             if (logger.isDebugEnabled()) {
                 logger.debug("response to CANCEL not forwarding");
             }
             // Update CallDetailRecord
             SipServletRequest request = (SipServletRequest) getLinkedSession(response).getAttribute(B2BUA_LAST_REQUEST);
             CallDetailRecord callRecord = records.getCallDetailRecord((Sid) request.getSession().getAttribute(CDR_SID));

             if (callRecord != null) {
                 if(logger.isInfoEnabled()) {
                     logger.info("CDR found! Updating");
                 }
                 callRecord = callRecord.setStatus(CallStateChanged.State.CANCELED.name());
                 final DateTime now = DateTime.now();
                 callRecord = callRecord.setEndTime(now);
                 int seconds;
                 if (callRecord.getStartTime() != null) {
                     seconds = (int) (DateTime.now().getMillis() - callRecord.getStartTime().getMillis()) / 1000;
                 } else {
                     seconds = 0;
                 }
                 callRecord = callRecord.setDuration(seconds);
                 records.updateCallDetailRecord(callRecord);
             }
             return;
         }

         // We don't forward 200 OK Response to BYE.
         if (response.getStatus() == 200 && response.getMethod().equalsIgnoreCase("BYE")) {
             if (logger.isDebugEnabled()) {
                 logger.debug("response to BYE not forwarding");
             }
             return;
         }
         // forward the response
         if (response.getSession() != null &&
                 (!response.getSession().getState().equals(SipSession.State.TERMINATED))) {
             response.getSession().setAttribute(B2BUA_LAST_RESPONSE, response);
         }
         SipServletRequest linkedRequest = (SipServletRequest) getLinkedSession(response).getAttribute(B2BUA_LAST_REQUEST);
         SipServletResponse clonedResponse = linkedRequest.createResponse(response.getStatus());
         SipURI originalURI = null;
         try {
             if(response.getAddressHeader("Contact") != null && response.getAddressHeader("Contact").getURI() != null){
                 originalURI = (SipURI) response.getAddressHeader("Contact").getURI();
                 if (originalURI != null && originalURI.getUser() != null && !originalURI.getUser().isEmpty()) {
                     ((SipURI) clonedResponse.getAddressHeader("Contact").getURI()).setUser(originalURI.getUser());
                 }
             }
         } catch (ServletParseException | NullPointerException e) {
            logger.error("Problem while trying to set User part on a clones response for a P2P call, "+e);
         }

         CallDetailRecord callRecord = records.getCallDetailRecord((Sid) linkedRequest.getSession().getAttribute(CDR_SID));

         if (response.getRawContent() != null && response.getRawContent().length > 0 ) {
             final byte[] sdp = response.getRawContent();
             String offer = null;
             if (response.getContentType().equalsIgnoreCase("application/sdp") && patchForNat) {
                 // Issue 306: https://telestax.atlassian.net/browse/RESTCOMM-306
                 Registration registration = daoManager.getRegistrationsDao().getRegistration(callRecord.getTo());
                 String externalIp;
                 if (registration != null) {
                     externalIp = registration.getLocation().split(":")[1].split("@")[1];
                 } else {
                     externalIp = callRecord.getTo().split(":")[1].split("@")[1];
                 }
                 try {
                     if(logger.isDebugEnabled()) {
                         logger.debug("Got original address from Registration :" + externalIp);
                     }
                     offer = patch(sdp, externalIp);
                 } catch (SdpException e) {
                     logger.error("Unexpected exception while patching sdp ", e);
                 }
             }
             if (offer != null) {
                 clonedResponse.setContent(offer, response.getContentType());
             } else {
                 clonedResponse.setContent(sdp, response.getContentType());
             }
         }
         clonedResponse.send();

         // CallDetailRecord callRecord = records.getCallDetailRecord((Sid) request.getSession().getAttribute(CDR_SID));
         if (callRecord != null) {
             if(logger.isInfoEnabled()) {
                 logger.info("CDR found! Updating");
             }
             if (!linkedRequest.getMethod().equalsIgnoreCase("BYE")) {
                 if (response.getStatus() == 100 || response.getStatus() == 180 || response.getStatus() == 183) {
                     callRecord = callRecord.setStatus(CallStateChanged.State.RINGING.name());
                 } else if (response.getStatus() == 200 || response.getStatus() == 202) {
                     callRecord = callRecord.setStatus(CallStateChanged.State.IN_PROGRESS.name());
                     callRecord = callRecord.setAnsweredBy(((SipURI) response.getTo().getURI()).getUser());
                     final DateTime now = DateTime.now();
                     callRecord = callRecord.setStartTime(now);

                 } else if (response.getStatus() == 486 || response.getStatus() == 600) {
                     callRecord = callRecord.setStatus(CallStateChanged.State.BUSY.name());
                 } else if (response.getStatus() > 400) {
                     callRecord = callRecord.setStatus(CallStateChanged.State.FAILED.name());
                 }
             } else {
                 callRecord = callRecord.setStatus(CallStateChanged.State.COMPLETED.name());
                 final DateTime now = DateTime.now();
                 callRecord = callRecord.setEndTime(now);
                 final int seconds = (int) ((DateTime.now().getMillis() - callRecord.getStartTime().getMillis()) / 1000);
                 callRecord = callRecord.setDuration(seconds);
             }

             records.updateCallDetailRecord(callRecord);
         }

     }

     public static void updateCDR(SipServletMessage message, CallStateChanged.State state) {
         CallDetailRecordsDao records = daoManager.getCallDetailRecordsDao();
         SipServletRequest request = null;

         // Update CallDetailRecord
         if (message instanceof SipServletResponse) {
             request = (SipServletRequest) getLinkedSession(message).getAttribute(B2BUA_LAST_REQUEST);
         } else if (message instanceof SipServletRequest) {
             request = (SipServletRequest) message;
         }
         CallDetailRecord callRecord = records.getCallDetailRecord((Sid) request.getSession().getAttribute(CDR_SID));

         if (callRecord != null) {
             if(logger.isInfoEnabled()) {
                 logger.info("CDR found! Updating");
             }
             callRecord = callRecord.setStatus(state.name());
             final DateTime now = DateTime.now();
             callRecord = callRecord.setEndTime(now);
             int seconds;
             if (callRecord.getStartTime() != null) {
                 seconds = (int) (DateTime.now().getMillis() - callRecord.getStartTime().getMillis()) / 1000;
             } else {
                 seconds = 0;
             }
             callRecord = callRecord.setDuration(seconds);
             records.updateCallDetailRecord(callRecord);
         }
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
