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
package org.restcomm.connect.dao.entities;

import org.joda.time.DateTime;
import org.restcomm.connect.commons.dao.Sid;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public class InstanceId {

    private Sid instanceId;
    private final DateTime dateCreated;
    private final DateTime dateUpdated;
    private final String host;

    public InstanceId(final Sid instanceId, final String host, final DateTime dateCreated, final DateTime dateUpdated) {
        this.instanceId = instanceId;
        this.host = host;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
    }

    public Sid getId() {
        return instanceId;
    }

    public String getHost() { return host; }

    public DateTime getDateCreated() {
        return dateCreated;
    }

    public DateTime getDateUpdated() {
        return dateUpdated;
    }

    public InstanceId setInstanceId(final Sid instanceId, final String host) {
        return new InstanceId(instanceId, host, this.dateCreated, DateTime.now());
    }

    @Override
    public String toString() {
        return this.instanceId+"/"+this.host;
    }
}
