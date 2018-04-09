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
 */
package org.restcomm.connect.testsuite;

import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.cafesip.sipunit.SipStack;

/**
 * Utility class to hide the complexity of creating a SipUnit sipStack.
 *
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 *
 */
public class SipStackTool {

    boolean initialized;

    private String sipStackName;

    private SipStack sipStack;

    private final Logger logger = Logger.getLogger(SipStackTool.class.getName());

    public SipStackTool(String sipStackName) {
        this.setSipStackName(sipStackName);
    }

    // Return SipStack
    private Properties makeProperties(String myTransport, String myHost, String myPort, String outboundProxy, Boolean myAutoDialog,
            String threadPoolSize, String reentrantListener, Map<String, String> additionalProperties) throws Exception {

        Properties properties = new Properties();

        if (myHost == null) {
            myHost = "127.0.0.1";
        }
        if (myTransport == null) {
            myTransport = SipStack.PROTOCOL_UDP;
        }

        properties.setProperty("javax.sip.IP_ADDRESS", myHost);
        properties.setProperty("javax.sip.STACK_NAME", "UAC_" + myTransport + "_" + myPort);
        properties.setProperty("javax.sip.AUTOMATIC_DIALOG_SUPPORT", (myAutoDialog ? "on" : "off"));

        properties.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32");
        properties.setProperty("gov.nist.javax.sip.DEBUG_LOG", "logs/testAgent_debug_" + myPort + "_" + myTransport + ".txt");
        properties.setProperty("gov.nist.javax.sip.SERVER_LOG", "logs/testAgent_serverLog_" + myPort + "_" + myTransport + ".xml");
        properties.setProperty("gov.nist.javax.sip.READ_TIMEOUT", "1000");
        properties.setProperty("gov.nist.javax.sip.CACHE_SERVER_CONNECTIONS", "false");
        properties.setProperty("gov.nist.javax.sip.PASS_INVITE_NON_2XX_ACK_TO_LISTENER", "true");

        properties.setProperty("sipunit.trace", "true");
        properties.setProperty("sipunit.test.protocol", myTransport);
        properties.setProperty("sipunit.test.port", myPort);
        properties.setProperty("sipunit.BINDADDR", myHost);

        if (outboundProxy != null) {
            properties.setProperty("javax.sip.OUTBOUND_PROXY", outboundProxy + "/"
                    + myTransport);

            String proxyHost = outboundProxy.split(":")[0];
            String proxyPort = outboundProxy.split(":")[1];

            properties.setProperty("sipunit.proxy.host", proxyHost);
            properties.setProperty("sipunit.proxy.port", proxyPort);
        }

        properties.setProperty("gov.nist.javax.sip.THREAD_POOL_SIZE", threadPoolSize == null ? "1" : threadPoolSize);
        properties.setProperty("gov.nist.javax.sip.REENTRANT_LISTENER", reentrantListener == null ? "false" : reentrantListener);

        if (additionalProperties != null) {
            properties.putAll(additionalProperties);
        }

        System.setProperty("org.mobicents.testsuite.testhostaddr", myHost);

        return properties;
    }

    // Initialize SipStack for no proxy use
    public SipStack initializeSipStack(String myTransport, String myHost, String myPort) throws Exception {
        return initializeSipStack(myTransport, myHost, myPort, null, true, null, null, null);
    }

    // Initialize SipStack using outbound proxy
    public SipStack initializeSipStack(String myPort, String outboundProxy) throws Exception {
        return initializeSipStack(null, null, myPort, outboundProxy, true, null, null, null);
    }

    // Initialize SipStack using outbound proxy
    public SipStack initializeSipStack(String myTransport, String myHost, String myPort, String outboundProxy) throws Exception {
        return initializeSipStack(myTransport, myHost, myPort, outboundProxy, true, null, null, null);
    }

    public SipStack initializeSipStack(String myTransport, String myHost, String myPort, String outboundProxy, Boolean myAutoDialog,
            String threadPoolSize, String reentrantListener, Map<String, String> additionalProperties) throws Exception {

        /*
        * http://code.google.com/p/mobicents/issues/detail?id=3121
        * Reset sipStack when calling initializeSipStack method
         */
        tearDown();

        try {
            Properties myProperties = makeProperties(myTransport, myHost, myPort, outboundProxy, myAutoDialog, threadPoolSize,
                    reentrantListener, additionalProperties);
            sipStack = new SipStack(myTransport, Integer.valueOf(myPort), myProperties);
            logger.info("SipStack - " + sipStackName + " - created!");
        } catch (Exception ex) {
            logger.info("Exception: " + ex.getClass().getName() + ": "
                    + ex.getMessage());
            throw ex;
        }

        initialized = true;
        return sipStack;
    }

    // Initialize SipStack using provided properties
    public SipStack initializeSipStack(String transport, String myPort, Properties myProperties) throws Exception {

        /*
        * http://code.google.com/p/mobicents/issues/detail?id=3121
        * Reset sipStack when calling initializeSipStack method
         */
        tearDown();

        try {
            sipStack = new SipStack(transport, Integer.valueOf(myPort), myProperties);
            logger.info("SipStack - " + sipStackName + " - created!");
        } catch (Exception ex) {
            logger.info("Exception: " + ex.getClass().getName() + ": "
                    + ex.getMessage());
            throw ex;
        }

        initialized = true;
        return sipStack;
    }

    public String getSipStackName() {
        return sipStackName;
    }

    public void setSipStackName(String sipStackName) {
        this.sipStackName = sipStackName;
    }

    public void tearDown() {
        if (sipStack != null && sipStack.getSipProvider().getListeningPoints().length > 0) {
            //SipStack.dispose() will cause previously reserved port to be released
            sipStack.dispose();
            sipStack = null;
        }
    }
}
