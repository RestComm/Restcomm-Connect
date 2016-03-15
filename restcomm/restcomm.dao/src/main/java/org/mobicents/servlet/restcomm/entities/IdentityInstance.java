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
    private final Sid sid;
    private final Sid organizationSid;
    private final String name;
    private final String restcommRestRAT;
    private final DateTime restcommRestLastRegistrationDate;
    private final String restcommRestStatus;
    private final String restcommUiRAT;
    private final DateTime restcommUiLastRegistrationDate;
    private final String restcommUiStatus;
    private final String rvdRestRAT;
    private final DateTime rvdRestLastRegistrationDate;
    private final String rvdRestStatus;
    private final String rvdUiRAT;
    private final DateTime rvdUiLastRegistrationDate;
    private final String rvdUiStatus;

    public IdentityInstance(Sid sid, Sid organization_sid, String name, String restcommRestRAT, DateTime restcommRestLastRegistrationDate, String restcommRestStatus, String restcommUiRAT, DateTime restcommUiLastRegistrationDate, String restcommUiStatus, String rvdRestRAT, DateTime rvdRestLastRegistrationDate, String rvdRestStatus, String rvdUiRAT, DateTime rvdUiLastRegistrationDate, String rvdUiStatus) {
        this.sid = sid;
        this.organizationSid = organization_sid;
        this.name = name;
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

    public String getRestcommRestStatus() {
        return restcommRestStatus;
    }

    public String getRestcommUiRAT() {
        return restcommUiRAT;
    }

    public DateTime getRestcommUiLastRegistrationDate() {
        return restcommUiLastRegistrationDate;
    }

    public String getRestcommUiStatus() {
        return restcommUiStatus;
    }

    public String getRvdRestRAT() {
        return rvdRestRAT;
    }

    public DateTime getRvdRestLastRegistrationDate() {
        return rvdRestLastRegistrationDate;
    }

    public String getRvdRestStatus() {
        return rvdRestStatus;
    }

    public String getRvdUiRAT() {
        return rvdUiRAT;
    }

    public DateTime getRvdUiLastRegistrationDate() {
        return rvdUiLastRegistrationDate;
    }

    public String getRvdUiStatus() {
        return rvdUiStatus;
    }
}
