/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2016, Telestax Inc and individual contributors
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

package org.mobicents.servlet.restcomm.entities;

import org.joda.time.DateTime;

/**
 * Identity instance is the logical entity that represents a restcomm instance (actually an organization)
 * secured by a Keycloak authorization server. The 'name' property is the prefix used when creating the
 * respective keycloak Clients.
 *
 * @author Orestis Tsakiridis
 */
public class IdentityInstance {

    private Sid sid;
    private Sid organizationSid;
    private String name;
    private DateTime dateCreated;
    private DateTime dateUpdated;
    private String restcommRAT;
    private DateTime restcommLastRegistrationDate;
    private Status restcommStatus;
    private String restcommClientSecret; // ++

    public IdentityInstance(Sid sid, Sid organizationSid, String name, DateTime dateCreated, DateTime dateUpdated, String restcommRAT, DateTime restcommLastRegistrationDate, Status restcommStatus, String restcommClientSecret) {
        this.sid = sid;
        this.organizationSid = organizationSid;
        this.name = name;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.restcommRAT = restcommRAT;
        this.restcommLastRegistrationDate = restcommLastRegistrationDate;
        this.restcommStatus = restcommStatus;
        this.restcommClientSecret = restcommClientSecret;
    }

    public IdentityInstance() {
        sid = Sid.generate(Sid.Type.IDENTITY_INSTANCE);
    }

    public IdentityInstance(Sid sid) {
        this.sid = sid;
    }

    public Sid getSid() {
        return sid;
    }

    public Sid getOrganizationSid() {
        return organizationSid;
    }

    public String getName() {
        return name;
    }

    public String getRestcommRAT() {
        return restcommRAT;
    }

    public DateTime getRestcommLastRegistrationDate() {
        return restcommLastRegistrationDate;
    }

    public Status getRestcommStatus() {
        return restcommStatus;
    }

    public void setSid(Sid sid) {
        this.sid = sid;
    }

    public void setOrganizationSid(Sid organizationSid) {
        this.organizationSid = organizationSid;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRestcommRAT(String restcommRAT) {
        this.restcommRAT = restcommRAT;
    }

    public void setRestcommLastRegistrationDate(DateTime restcommLastRegistrationDate) {
        this.restcommLastRegistrationDate = restcommLastRegistrationDate;
    }

    public void setRestcommStatus(Status restcommStatus) {
        this.restcommStatus = restcommStatus;
    }

    public DateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(DateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public DateTime getDateUpdated() {
        return dateUpdated;
    }

    public void setDateUpdated(DateTime dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    public String getRestcommClientSecret() {
        return restcommClientSecret;
    }

    public void setRestcommClientSecret(String restcommClientSecret) {
        this.restcommClientSecret = restcommClientSecret;
    }

    public enum Status {
        success, fail;

        // Use this instead of direct valueOf(). It handles nulls too
        public static Status getValueOf(String value) {
            if ( value == null )
                return null;
            else
                return Status.valueOf(value);
        }
    }
}
