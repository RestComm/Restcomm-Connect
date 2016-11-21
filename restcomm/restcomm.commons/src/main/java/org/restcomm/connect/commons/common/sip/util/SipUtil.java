/*
 *  TeleStax, Open Source Cloud Communications
 *  Copyright 2011-2016, Telestax Inc and individual contributors
 *  by the @authors tag.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package org.restcomm.connect.commons.common.sip.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ListIterator;

import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipURI;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
public class SipUtil {

    public static SipURI getInitialIpAddressPort(SipServletMessage message, final SipFactory factory) throws ServletParseException, UnknownHostException {
        // Issue #268 - https://bitbucket.org/telestax/telscale-restcomm/issue/268
        // First get the Initial Remote Address (real address that the request came from)
        // Then check the following:
        // 1. If contact header address is private network address
        // 2. If there are no "Record-Route" headers (there is no proxy in the call)
        // 3. If contact header address != real ip address
        // Finally, if all of the above are true, create a SIP URI using the realIP address and the SIP port
        // and store it to the sip session to be used as request uri later
        SipURI uri = null;

        String realIP = message.getInitialRemoteAddr();
        Integer realPort = message.getInitialRemotePort();
        if (realPort == null || realPort == -1) {
            realPort = 5060;
        }

        if (realPort == 0) {
            realPort = message.getRemotePort();
        }

        final ListIterator<String> recordRouteHeaders = message.getHeaders("Record-Route");
        final Address contactAddr = factory.createAddress(message.getHeader("Contact"));

        InetAddress contactInetAddress = InetAddress.getByName(((SipURI) contactAddr.getURI()).getHost());
        InetAddress inetAddress = InetAddress.getByName(realIP);

        //int remotePort = message.getRemotePort();
        //int contactPort = ((SipURI) contactAddr.getURI()).getPort();
        //String remoteAddress = message.getRemoteAddr();

        // Issue #332: https://telestax.atlassian.net/browse/RESTCOMM-332
        final String initialIpBeforeLB = message.getHeader("X-Sip-Balancer-InitialRemoteAddr");
        String initialPortBeforeLB = message.getHeader("X-Sip-Balancer-InitialRemotePort");
        //String contactAddress = ((SipURI) contactAddr.getURI()).getHost();

        if (initialIpBeforeLB != null) {
            if (initialPortBeforeLB == null)
                initialPortBeforeLB = "5060";
            /*if(logger.isInfoEnabled()) {
                logger.info("We are behind load balancer, storing Initial Remote Address " + initialIpBeforeLB + ":"
                    + initialPortBeforeLB + " to the session for later use");
            }*/
            realIP = initialIpBeforeLB + ":" + initialPortBeforeLB;
            uri = factory.createSipURI(null, realIP);
        } else if (contactInetAddress.isSiteLocalAddress() && !recordRouteHeaders.hasNext()
                && !contactInetAddress.toString().equalsIgnoreCase(inetAddress.toString())) {
            /*if(logger.isInfoEnabled()) {
                logger.info("Contact header address " + contactAddr.toString()
                    + " is a private network ip address, storing Initial Remote Address " + realIP + ":" + realPort
                    + " to the session for later use");
            }*/
            realIP = realIP + ":" + realPort;
            uri = factory.createSipURI(null, realIP);
        }
        return uri;
    }
}