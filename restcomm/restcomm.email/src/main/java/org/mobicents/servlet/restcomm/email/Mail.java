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
package org.mobicents.servlet.restcomm.email;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

/**
 * @author liblefty@gmail.com (Lefteris Banos)
 */
@Immutable
public final class Mail {
    private final String from;
    private final String to;
    private final String cc;
    private final String subject;
    private final String body;

    public Mail(final String from, final String to, final String subject, final String body) {
        super();
        this.from = from;
        this.to = to;
        this.cc = "";
        this.subject = subject;
        this.body = body;
    }

    public Mail(final String from, final String to, final String subject, final String body,final String cc) {
        super();
        this.from = from;
        this.to = to;
        this.cc = cc;
        this.subject = subject;
        this.body = body;
    }

    public String from() {
        return from;
    }

    public String to() {
        return to;
    }

    public String cc() {
        return cc;
    }

    public String subject() {
        return subject;
    }

    public String body() {
        return body;
    }
}
