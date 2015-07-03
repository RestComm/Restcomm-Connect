/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
 * by the @authors tag.
 *
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

package org.mobicents.servlet.restcomm.smpp;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;

import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.SmppSessionHandler;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.impl.DefaultSmppSession;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.EnquireLinkResp;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

/**
 * @author amit bhayani
 *
 */
public class SmppClientOpsThread implements Runnable {

    private static final Logger logger = Logger.getLogger(SmppClientOpsThread.class);

    private static final long SCHEDULE_CONNECT_DELAY = 1000 * 30; // 30 sec

    private List<ChangeRequest> pendingChanges = new CopyOnWriteArrayList<ChangeRequest>();

    private Object waitObject = new Object();

    private final DefaultSmppClient clientBootstrap;

    protected volatile boolean started = true;

    /**
     *
     */
    public SmppClientOpsThread(DefaultSmppClient clientBootstrap) {
        this.clientBootstrap = clientBootstrap;
    }

    /**
     * @param started the started to set
     */
    protected void setStarted(boolean started) {
        this.started = started;

        synchronized (this.waitObject) {
            this.waitObject.notify();
        }
    }

    protected void scheduleConnect(Smpp esme) {
        synchronized (this.pendingChanges) {
            this.pendingChanges.add(new ChangeRequest(esme, ChangeRequest.CONNECT, System.currentTimeMillis()
                    + SCHEDULE_CONNECT_DELAY));
        }

        synchronized (this.waitObject) {
            this.waitObject.notify();
        }

    }

    protected void scheduleEnquireLink(Smpp esme) {
        synchronized (this.pendingChanges) {
            this.pendingChanges.add(new ChangeRequest(esme, ChangeRequest.ENQUIRE_LINK, System.currentTimeMillis()
                    + esme.getEnquireLinkDelay()));
        }

        synchronized (this.waitObject) {
            this.waitObject.notify();
        }
    }

    @Override
    public void run() {
        if (logger.isInfoEnabled()) {
            logger.info("SmppClientOpsThread started.");
        }

        while (this.started) {

            try {
                synchronized (this.pendingChanges) {
                    Iterator<ChangeRequest> changes = pendingChanges.iterator();
                    while (changes.hasNext()) {
                        ChangeRequest change = changes.next();
                        switch (change.getType()) {
                            case ChangeRequest.CONNECT:
                                if (!change.getSmpp().isStarted()) {
                                    pendingChanges.remove(change);
                                    //changes.remove();
                                } else {
                                    if (change.getExecutionTime() <= System.currentTimeMillis()) {
                                        pendingChanges.remove(change);
                                        //changes.remove();
                                        initiateConnection(change.getSmpp());
                                    }
                                }
                                break;
                            case ChangeRequest.ENQUIRE_LINK:
                                if (!change.getSmpp().isStarted()) {
                                    pendingChanges.remove(change);
                                    //changes.remove();
                                } else {
                                    if (change.getExecutionTime() <= System.currentTimeMillis()) {
                                        pendingChanges.remove(change);
                                        //changes.remove();
                                        enquireLink(change.getSmpp());
                                    }
                                }
                                break;
                        }
                    }
                }

                synchronized (this.waitObject) {
                    this.waitObject.wait(5000);
                }

            } catch (InterruptedException e) {
                logger.error("Error while looping SmppClientOpsThread thread", e);
            }
        }// while

        if (logger.isInfoEnabled()) {
            logger.info("SmppClientOpsThread for stopped.");
        }
    }

    private void enquireLink(Smpp esme) {
        SmppSession smppSession = esme.getSmppSession();

        if (!esme.isStarted()) {
            return;
        }

        if (smppSession != null && smppSession.isBound()) {
            try {
                EnquireLinkResp enquireLinkResp1 = smppSession.enquireLink(new EnquireLink(), 10000);

                // all ok lets scehdule another ENQUIRE_LINK
                this.scheduleEnquireLink(esme);
                return;

            } catch (RecoverablePduException e) {
                logger.warn(
                        String.format("RecoverablePduException while sending the ENQURE_LINK for ESME SystemId=%s",
                                esme.getSystemId()), e);

                // Recoverabel exception is ok
                // all ok lets schedule another ENQUIRE_LINK
                this.scheduleEnquireLink(esme);
                return;

            } catch (Exception e) {

                logger.error(
                        String.format("Exception while trying to send ENQUIRE_LINK for ESME SystemId=%s", esme.getSystemId()),
                        e);
                // For all other exceptions lets close session and re-try
                // connect
                smppSession.close();
                this.scheduleConnect(esme);
            }

        } else {
            // This should never happen
            logger.warn(String.format("Sending ENQURE_LINK fialed for ESME SystemId=%s as SmppSession is =%s !",
                    esme.getSystemId(), (smppSession == null ? null : smppSession.getStateName())));

            if (smppSession != null) {
                smppSession.close();
            }
            this.scheduleConnect(esme);
        }
    }

    private void initiateConnection(Smpp esme) {
        // If Esme is stopped, don't try to initiate connect
        if (!esme.isStarted()) {
            return;
        }

        SmppSession smppSession = esme.getSmppSession();
        if ((smppSession != null && smppSession.isBound()) || (smppSession != null && smppSession.isBinding())) {
            // If process has already begun lets not do it again
            return;
        }

        SmppSession session0 = null;
        try {

            SmppSessionConfiguration config0 = new SmppSessionConfiguration();
            config0.setWindowSize(esme.getWindowSize());
            config0.setName(esme.getSystemId());
            config0.setType(esme.getSmppBindType());
            config0.setHost(esme.getPeerIp());
            config0.setPort(esme.getPeerPort());
            config0.setConnectTimeout(esme.getConnectTimeout());
            config0.setSystemId(esme.getSystemId());
            config0.setPassword(esme.getPassword());
            config0.getLoggingOptions().setLogBytes(true);
            // to enable monitoring (request expiration)
            config0.setRequestExpiryTimeout(esme.getRequestExpiryTimeout());
            config0.setWindowMonitorInterval(esme.getWindowMonitorInterval());
            config0.setCountersEnabled(esme.isCountersEnabled());

            Address address = esme.getAddress();
            config0.setAddressRange(address);

            SmppSessionHandler sessionHandler = new ClientSmppSessionHandler(esme);

            session0 = clientBootstrap.bind(config0, sessionHandler);

            // Set in ESME
            esme.setSmppSession((DefaultSmppSession) session0);

            // Finally set Enquire Link schedule
            this.scheduleEnquireLink(esme);
        } catch (Exception e) {
            logger.error(
                    String.format("Exception when trying to bind client SMPP connection for ESME systemId=%s",
                            esme.getSystemId()), e);
            if (session0 != null) {
                session0.close();
            }
            this.scheduleConnect(esme);
        }
    }

    protected class ClientSmppSessionHandler implements SmppSessionHandler {

        private final Smpp esme;

        /**
         * @param esme
         */
        public ClientSmppSessionHandler(Smpp esme) {
            super();
            this.esme = esme;
        }

        @Override
        public String lookupResultMessage(int arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String lookupTlvTagName(short arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void fireChannelUnexpectedlyClosed() {
            logger.error("ChannelUnexpectedlyClosed for Smpp " + this.esme.getName()
                    + " Closing Smpp session and restrting BIND process again");
            this.esme.getSmppSession().close();

            // Schedule the connection again
            scheduleConnect(this.esme);
        }

        @Override
        public void fireExpectedPduResponseReceived(PduAsyncResponse pduAsyncResponse) {
            // TODO : SMPP Response received. Does RestComm need confirmation for this?
            logger.info("ExpectedPduResponseReceived received for Smpp " + this.esme.getName() + " PduAsyncResponse="
                    + pduAsyncResponse);
        }

        @Override
        public void firePduRequestExpired(PduRequest pduRequest) {
            // TODO : SMPP request Expired. RestComm needs to notify Application about SMS failure
            logger.warn("PduRequestExpired for Smpp " + this.esme.getName() + " PduRequest=" + pduRequest);
        }

        @Override
        public PduResponse firePduRequestReceived(PduRequest pduRequest) {
            // TODO : SMPP request received. Let RestComm know so it calls coresponding App
            logger.info("PduRequest received for Smpp " + this.esme.getName() + " PduRequest=" + pduRequest);

            PduResponse response = pduRequest.createResponse();

            // do any logic here

            return response;
        }

        @Override
        public void fireRecoverablePduException(RecoverablePduException e) {
            logger.warn("RecoverablePduException received for Smpp " + this.esme.getName(), e);
        }

        @Override
        public void fireUnexpectedPduResponseReceived(PduResponse pduResponse) {
            logger.warn("UnexpectedPduResponseReceived received for Smpp " + this.esme.getName() + " PduResponse="
                    + pduResponse);
        }

        @Override
        public void fireUnknownThrowable(Throwable e) {
            logger.error("UnknownThrowable for Smpp " + this.esme.getName()
                    + " Closing Smpp session and restrting BIND process again", e);
            // TODO is this ok?

            this.esme.getSmppSession().close();

            // Schedule the connection again
            scheduleConnect(this.esme);

        }

        @Override
        public void fireUnrecoverablePduException(UnrecoverablePduException e) {
            logger.error("UnrecoverablePduException for Smpp " + this.esme.getName()
                    + " Closing Smpp session and restrting BIND process again", e);

            this.esme.getSmppSession().close();

            // Schedule the connection again
            scheduleConnect(this.esme);
        }

    }

}
