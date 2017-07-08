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

import java.io.Serializable;
import java.net.URI;

import org.joda.time.DateTime;

import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.annotations.concurrency.NotThreadSafe;
import org.restcomm.connect.commons.dao.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class Gateway implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Sid sid;
    private final DateTime dateCreated;
    private final DateTime dateUpdated;
    private final String friendlyName;
    private final String password;
    private final String proxy;
    private final Boolean register;
    private final String userName;
    private final int timeToLive;
    private final URI uri;

    public Gateway(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated, final String friendlyName,
            final String password, final String proxy, final Boolean register, final String userName, final int timeToLive,
            final URI uri) {
        super();
        this.sid = sid;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.friendlyName = friendlyName;
        this.password = password;
        this.proxy = proxy;
        this.register = register;
        this.userName = userName;
        this.timeToLive = timeToLive;
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

    public String getPassword() {
        return password;
    }

    public String getProxy() {
        return proxy;
    }

    public String getUserName() {
        return userName;
    }

    public boolean register() {
        return register;
    }

    public int getTimeToLive() {
        return timeToLive;
    }

    public URI getUri() {
        return uri;
    }

    public Gateway setFriendlyName(final String friendlyName) {
        return new Gateway(sid, dateCreated, DateTime.now(), friendlyName, password, proxy, register, userName, timeToLive, uri);
    }

    public Gateway setPassword(final String password) {
        return new Gateway(sid, dateCreated, DateTime.now(), friendlyName, password, proxy, register, userName, timeToLive, uri);
    }

    public Gateway setProxy(final String proxy) {
        return new Gateway(sid, dateCreated, DateTime.now(), friendlyName, password, proxy, register, userName, timeToLive, uri);
    }

    public Gateway setRegister(final boolean register) {
        return new Gateway(sid, dateCreated, DateTime.now(), friendlyName, password, proxy, register, userName, timeToLive, uri);
    }

    public Gateway setUserName(final String userName) {
        return new Gateway(sid, dateCreated, DateTime.now(), friendlyName, password, proxy, register, userName, timeToLive, uri);
    }

    public Gateway setTimeToLive(final int timeToLive) {
        return new Gateway(sid, dateCreated, DateTime.now(), friendlyName, password, proxy, register, userName, timeToLive, uri);
    }

    @NotThreadSafe
    public static final class Builder {
        private Sid sid;
        private String friendlyName;
        private String password;
        private String proxy;
        private Boolean register;
        private String userName;
        private int timeToLive;
        private URI uri;

        private Builder() {
            super();
        }

        public Gateway build() {
            final DateTime now = DateTime.now();
            return new Gateway(sid, now, now, friendlyName, password, proxy, register, userName, timeToLive, uri);
        }

        public void setSid(final Sid sid) {
            this.sid = sid;
        }

        public void setFriendlyName(final String friendlyName) {
            this.friendlyName = friendlyName;
        }

        public void setPassword(final String password) {
            this.password = password;
        }

        public void setProxy(final String proxy) {
            this.proxy = proxy;
        }

        public void setRegister(final boolean register) {
            this.register = register;
        }

        public void setUserName(final String userName) {
            this.userName = userName;
        }

        public void setTimeToLive(final int timeToLive) {
            this.timeToLive = timeToLive;
        }

        public void setUri(final URI uri) {
            this.uri = uri;
        }
    }
}
