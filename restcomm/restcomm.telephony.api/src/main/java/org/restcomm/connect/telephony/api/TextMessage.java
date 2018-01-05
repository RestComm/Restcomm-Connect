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
package org.restcomm.connect.telephony.api;

import org.restcomm.connect.commons.stream.StreamEvent;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public class TextMessage implements StreamEvent {
    public static enum SmsState {INBOUND_TO_APP, INBOUND_TO_CLIENT, INBOUND_TO_PROXY_OUT, OUTBOUND, NOT_FOUND}
    private final String from;
    private final String to;
    private final SmsState state;

    public TextMessage(final String from, final String to, final SmsState state) {
        this.from = from;
        this.to = to;
        this.state = state;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public SmsState getState() {
        return state;
    }
}
