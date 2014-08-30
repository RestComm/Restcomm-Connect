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
package org.mobicents.servlet.restcomm.telephony.util;

import static javax.servlet.sip.SipServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED;
import static org.mobicents.servlet.restcomm.util.HexadecimalUtils.toHex;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipURI;

import org.mobicents.servlet.restcomm.dao.ClientsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.entities.Client;
import org.mobicents.servlet.restcomm.util.DigestAuthentication;

/**
 *
 * Helper class for managing SIP interactions
 *
 *
 * @author ivelin.ivanov@teletax.com
 *
 */
public class CallControlHelper {

    static boolean permitted(final String authorization, final String method, DaoManager daoManager) {
        final Map<String, String> map = authHeaderToMap(authorization);
        final String user = map.get("username");
        final String algorithm = map.get("algorithm");
        final String realm = map.get("realm");
        final String uri = map.get("uri");
        final String nonce = map.get("nonce");
        final String nc = map.get("nc");
        final String cnonce = map.get("cnonce");
        final String qop = map.get("qop");
        final String response = map.get("response");
        final ClientsDao clients = daoManager.getClientsDao();
        final Client client = clients.getClient(user);
        if (client != null && Client.ENABLED == client.getStatus()) {
            final String password = client.getPassword();
            final String result = DigestAuthentication.response(algorithm, user, realm, password, nonce, nc, cnonce, method,
                    uri, null, qop);
            return result.equals(response);
        } else {
            return false;
        }
    }

    /**
     *
     * Check if a client is authenticated. If so, return true. Otherwise request authentication and return false;
     *
     * @return
     * @throws IOException
     */
    public static boolean checkAuthentication(SipServletRequest request, DaoManager storage) throws IOException {
        // Make sure we force clients to authenticate.
        final String authorization = request.getHeader("Proxy-Authorization");
        final String method = request.getMethod();
        if (authorization == null || !CallControlHelper.permitted(authorization, method, storage)) {
            authenticate(request);
            return false;
        } else {
            return true;
        }
    }

    static void authenticate(final SipServletRequest request) throws IOException {
        final SipServletResponse response = request.createResponse(SC_PROXY_AUTHENTICATION_REQUIRED);
        final String nonce = nonce();
        final SipURI uri = (SipURI) request.getTo().getURI();
        final String realm = uri.getHost();
        final String header = header(nonce, realm, "Digest");
        response.addHeader("Proxy-Authenticate", header);
        response.send();
    }

    private static Map<String, String> authHeaderToMap(final String header) {
        final Map<String, String> map = new HashMap<String, String>();
        final int endOfScheme = header.indexOf(" ");
        map.put("scheme", header.substring(0, endOfScheme).trim());
        final String[] tokens = header.substring(endOfScheme + 1).split(",");
        for (final String token : tokens) {
            final String[] values = token.trim().split("=");
            map.put(values[0].toLowerCase(), values[1].replace("\"", ""));
        }

        return map;
    }

    static String nonce() {
        final byte[] uuid = UUID.randomUUID().toString().getBytes();
        final char[] hex = toHex(uuid);
        return new String(hex).substring(0, 31);
    }

    static String header(final String nonce, final String realm, final String scheme) {
        final StringBuilder buffer = new StringBuilder();
        buffer.append(scheme).append(" ");
        buffer.append("realm=\"").append(realm).append("\", ");
        buffer.append("nonce=\"").append(nonce).append("\"");
        return buffer.toString();
    }

    /**
     *
     * Extracts the User SIP identity from a request header
     *
     * @param request
     * @param useTo Whether or not to use the To field in the SIP header
     * @return
     */
    public static String getUserSipId(final SipServletRequest request, boolean useTo) {
        final SipURI toUri;
        final String toUser;
        if (useTo) {
            toUri = (SipURI) request.getTo().getURI();
            toUser = toUri.getUser();
        } else {
            toUri = (SipURI) request.getRequestURI();
            toUser = toUri.getUser();
        }
        return toUser;
    }

}
