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
    private String restcommRestRAT;
    private DateTime restcommRestLastRegistrationDate;
    private Status restcommRestStatus;
    private String restcommRestClientSecret; // ++
    private String restcommUiRAT;
    private DateTime restcommUiLastRegistrationDate;
    private Status restcommUiStatus;
    private String rvdRestRAT;
    private DateTime rvdRestLastRegistrationDate;
    private Status rvdRestStatus;
    private String rvdUiRAT;
    private DateTime rvdUiLastRegistrationDate;
    private Status rvdUiStatus;

    public IdentityInstance(Sid sid, Sid organizationSid, String name, DateTime dateCreated, DateTime dateUpdated, String restcommRestRAT, DateTime restcommRestLastRegistrationDate, Status restcommRestStatus, String restcommUiRAT, DateTime restcommUiLastRegistrationDate, Status restcommUiStatus, String rvdRestRAT, DateTime rvdRestLastRegistrationDate, Status rvdRestStatus, String rvdUiRAT, DateTime rvdUiLastRegistrationDate, Status rvdUiStatus) {
        this.sid = sid;
        this.organizationSid = organizationSid;
        this.name = name;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.restcommRestRAT = restcommRestRAT;
        this.restcommRestLastRegistrationDate = restcommRestLastRegistrationDate;
        this.restcommRestStatus = restcommRestStatus;
        this.restcommUiRAT = restcommUiRAT;
        this.restcommUiLastRegistrationDate = restcommUiLastRegistrationDate;
        this.restcommUiStatus = restcommUiStatus;
        this.rvdRestRAT = rvdRestRAT;
        this.rvdRestLastRegistrationDate = rvdRestLastRegistrationDate;
        this.rvdRestStatus = rvdRestStatus;
        this.rvdUiRAT = rvdUiRAT;
        this.rvdUiLastRegistrationDate = rvdUiLastRegistrationDate;
        this.rvdUiStatus = rvdUiStatus;
    }

    public IdentityInstance() {
        sid = Sid.generate(Sid.Type.IDENTITY_INSTANCE);

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

    public String getRestcommRestRAT() {
        return restcommRestRAT;
    }

    public DateTime getRestcommRestLastRegistrationDate() {
        return restcommRestLastRegistrationDate;
    }

    public Status getRestcommRestStatus() {
        return restcommRestStatus;
    }

    public String getRestcommUiRAT() {
        return restcommUiRAT;
    }

    public DateTime getRestcommUiLastRegistrationDate() {
        return restcommUiLastRegistrationDate;
    }

    public Status getRestcommUiStatus() {
        return restcommUiStatus;
    }

    public String getRvdRestRAT() {
        return rvdRestRAT;
    }

    public DateTime getRvdRestLastRegistrationDate() {
        return rvdRestLastRegistrationDate;
    }

    public Status getRvdRestStatus() {
        return rvdRestStatus;
    }

    public String getRvdUiRAT() {
        return rvdUiRAT;
    }

    public DateTime getRvdUiLastRegistrationDate() {
        return rvdUiLastRegistrationDate;
    }

    public Status getRvdUiStatus() {
        return rvdUiStatus;
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

    public void setRestcommRestRAT(String restcommRestRAT) {
        this.restcommRestRAT = restcommRestRAT;
    }

    public void setRestcommRestLastRegistrationDate(DateTime restcommRestLastRegistrationDate) {
        this.restcommRestLastRegistrationDate = restcommRestLastRegistrationDate;
    }

    public void setRestcommRestStatus(Status restcommRestStatus) {
        this.restcommRestStatus = restcommRestStatus;
    }

    public void setRestcommUiRAT(String restcommUiRAT) {
        this.restcommUiRAT = restcommUiRAT;
    }

    public void setRestcommUiLastRegistrationDate(DateTime restcommUiLastRegistrationDate) {
        this.restcommUiLastRegistrationDate = restcommUiLastRegistrationDate;
    }

    public void setRestcommUiStatus(Status restcommUiStatus) {
        this.restcommUiStatus = restcommUiStatus;
    }

    public void setRvdRestRAT(String rvdRestRAT) {
        this.rvdRestRAT = rvdRestRAT;
    }

    public void setRvdRestLastRegistrationDate(DateTime rvdRestLastRegistrationDate) {
        this.rvdRestLastRegistrationDate = rvdRestLastRegistrationDate;
    }

    public void setRvdRestStatus(Status rvdRestStatus) {
        this.rvdRestStatus = rvdRestStatus;
    }

    public void setRvdUiRAT(String rvdUiRAT) {
        this.rvdUiRAT = rvdUiRAT;
    }

    public void setRvdUiLastRegistrationDate(DateTime rvdUiLastRegistrationDate) {
        this.rvdUiLastRegistrationDate = rvdUiLastRegistrationDate;
    }

    public void setRvdUiStatus(Status rvdUiStatus) {
        this.rvdUiStatus = rvdUiStatus;
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

    public String getRestcommRestClientSecret() {
        return restcommRestClientSecret;
    }

    public void setRestcommRestClientSecret(String restcommRestClientSecret) {
        this.restcommRestClientSecret = restcommRestClientSecret;
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
