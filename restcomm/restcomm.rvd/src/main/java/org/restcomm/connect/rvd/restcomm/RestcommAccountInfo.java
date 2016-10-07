
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

package org.restcomm.connect.rvd.restcomm;

/**
 * A reduced entity for a Restcomm Account. It contains only the properties that make
 * sense and are used by RVD.
 *
 */
public class RestcommAccountInfo {
    String sid;
    String friendly_name;
    String email_address;
    String status;
    String role;

    public RestcommAccountInfo() {
        // TODO Auto-generated constructor stub
    }

    public String getSid() {
        return sid;
    }

    public String getFriendly_name() {
        return friendly_name;
    }

    public String getEmail_address() {
        return email_address;
    }

    public String getStatus() {
        return status;
    }

    public String getRole() { return role; }
}
