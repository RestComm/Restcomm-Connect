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
package org.mobicents.servlet.restcomm.sms;

import java.util.concurrent.ConcurrentHashMap;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class SmsSessionRequest {
    private final String from;
    private final String to;
    private final String body;
    private final ConcurrentHashMap<String, String> customHeaders;

    //TODO need to check which is using the SmsSessionRequest and modify accordingly to include or not the custom headers
    public SmsSessionRequest(final String from, final String to, final String body, final ConcurrentHashMap<String, String> customHeaders) {
        super();
        this.from = from;
        this.to = to;
        this.body = body;
        this.customHeaders = customHeaders;
    }

    public String from() {
        return from;
    }

    public String to() {
        return to;
    }

    public String body() {
        return body;
    }

    public ConcurrentHashMap<String, String> headers() {
        return customHeaders;
    }
}
