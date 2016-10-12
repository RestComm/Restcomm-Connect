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
package org.restcomm.connect.dao.entities;

import java.net.URI;

import org.joda.time.DateTime;

import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.dao.Sid;

/**
 * @author <a href="mailto:gvagenas@gmail.com">George Vagenas</a>
 */
@Immutable
public final class Announcement {
    private Sid sid;
    private DateTime dateCreated;
    private Sid accountSid;
    private String gender;
    private String language;
    private String text;
    private URI uri;

    public Announcement(final Sid sid, final Sid accountSid, final String gender, final String language, final String text,
            final URI uri) {
        this.sid = sid;
        this.dateCreated = DateTime.now();
        this.accountSid = accountSid;
        this.gender = gender;
        this.language = language;
        this.text = text;
        this.uri = uri;
    }

    public Announcement(final Sid sid, final DateTime dateCreated, final Sid accountSid, final String gender,
            final String language, final String text, final URI uri) {
        this.sid = sid;
        this.dateCreated = dateCreated;
        this.accountSid = accountSid;
        this.gender = gender;
        this.language = language;
        this.text = text;
        this.uri = uri;
    }

    public Sid getSid() {
        return sid;
    }

    public DateTime getDateCreated() {
        return dateCreated;
    }

    public Sid getAccountSid() {
        return accountSid;
    }

    public String getGender() {
        return gender;
    }

    public String getLanguage() {
        return language;
    }

    public String getText() {
        return text;
    }

    public URI getUri() {
        return uri;
    }
}
