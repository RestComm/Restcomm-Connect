/*
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
package org.mobicents.servlet.restcomm.entities;

import java.net.URI;

import org.joda.time.DateTime;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

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
