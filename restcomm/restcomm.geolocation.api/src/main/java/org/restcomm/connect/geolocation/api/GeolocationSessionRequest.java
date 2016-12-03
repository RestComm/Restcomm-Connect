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

package org.restcomm.connect.geolocation.api;

import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.restcomm.connect.commons.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author <a href="mailto:fernando.mendioroz@telestax.com"> Fernando Mendioroz </a>
 *
 */
@Immutable
public final class GeolocationSessionRequest {

    /*****************************************************/
    /*** Phase II: internetworking with GMLC through SIP */
    /*****************************************************/
    private final String from;
    private final String to;
    private final String body;
    private final Encoding encoding;
    private final HttpServletRequest originHttpRequest;
    private final SipServletRequest originSipRequest;
    private final ConcurrentHashMap<String, String> customHeaders;

    public enum Encoding {
        UCS_2("UCS-2"),
        UTF_8("UTF-8"),
        GSM("GSM");

        private final String name;

        Encoding(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    //TODO need to check which is using the GeolocationSessionRequest and modify accordingly to include or not the custom headers
    public GeolocationSessionRequest(final String from, final String to, final String body, final Encoding encoding, final HttpServletRequest originHttpRequest,
                                     final SipServletRequest originSipRequest, final ConcurrentHashMap<String, String> customHeaders) {
        super();
        this.from = from;
        this.to = to;
        this.originHttpRequest = originHttpRequest;
        this.originSipRequest = originSipRequest;
        this.body = body;
        this.customHeaders = customHeaders;
        this.encoding = encoding;
    }

    public GeolocationSessionRequest(final String from, final String to, final String body, final HttpServletRequest originHttpRequest, final SipServletRequest originSipRequest, final ConcurrentHashMap<String, String> customHeaders) {
        this(from, to, body, Encoding.UTF_8, originHttpRequest, originSipRequest, customHeaders);
    }

    // FIXME
//    public GeolocationSessionRequest(final String from, final String to, final String body, final Encoding encoding, final ConcurrentHashMap<String, String> customHeaders) {
//        this(from, to, body, encoding, null, customHeaders);
//    }
//
//    public GeolocationSessionRequest(final String from, final String to, final String body, final ConcurrentHashMap<String, String> customHeaders) {
//        this(from, to, body, Encoding.GSM, null, customHeaders);
//    }

    public String from() {
        return from;
    }

    public String to() {
        return to;
    }

    public String body() {
        return body;
    }

    public Encoding encoding() {
        return encoding;
    }

    public HttpServletRequest getOriginHttpRequest() {
        return originHttpRequest;
    }

    public SipServletRequest getOriginSipRequest() {
        return originSipRequest;
    }

    public ConcurrentHashMap<String, String> headers() {
        return customHeaders;
    }
}