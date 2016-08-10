/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.mobicents.servlet.restcomm.entities;

import java.net.URI;

import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.annotations.concurrency.NotThreadSafe;

/**
 * @author guilherme.jansen@telestax.com
 */
@Immutable
public class Organization {

    private Sid sid;
    private DateTime dateCreated;
    private DateTime dateUpdated;
    private String friendlyName;
    private String namespace;
    private String apiVersion;
    private URI uri;

    public Organization(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated, final String friendlyName,
                        final String namespace, final String apiVersion, final URI uri) {
        super();
        this.sid = sid;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.friendlyName = friendlyName;
        this.namespace = namespace;
        this.apiVersion = apiVersion;
        this.uri = uri;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Sid getSid() {
        return sid;
    }

    public DateTime getDateCreated() {
        return dateCreated;
    }

    public DateTime getDateUpdated() {
        return dateUpdated;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public URI getUri() {
        return uri;
    }

    public Organization setFriendlyName(String friendlyName) {
        return new Organization(sid, dateCreated, DateTime.now(), friendlyName, namespace, apiVersion, uri);
    }

    public Organization setNamespace(String namespace) {
        return new Organization(sid, dateCreated, DateTime.now(), friendlyName, namespace, apiVersion, uri);
    }

    public Organization setApiVersion(String apiVersion) {
        return new Organization(sid, dateCreated, DateTime.now(), friendlyName, namespace, apiVersion, uri);
    }

    public Organization setUri(URI uri) {
        return new Organization(sid, dateCreated, DateTime.now(), friendlyName, namespace, apiVersion, uri);
    }

    @NotThreadSafe
    public static final class Builder {
        private Sid sid;
        private String friendlyName;
        private String namespace;
        private String apiVersion;
        private URI uri;

        private Builder() {
            super();
        }

        public Organization build() {
            final DateTime now = DateTime.now();
            return new Organization(sid, now, now, friendlyName, namespace, apiVersion, uri);
        }

        public void setSid(final Sid sid) {
            this.sid = sid;
        }

        public void setFriendlyName(final String friendlyName) {
            this.friendlyName = friendlyName;
        }

        public void setNamespace(final String namespace) {
            this.namespace = namespace;
        }

        public void setApiVersion(final String apiVersion) {
            this.apiVersion = apiVersion;
        }

        public void setUri(final URI uri) {
            this.uri = uri;
        }
    }

}