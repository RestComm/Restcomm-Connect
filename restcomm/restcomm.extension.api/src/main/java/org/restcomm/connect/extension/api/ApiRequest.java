/*
 *  TeleStax, Open Source Cloud Communications
 *  Copyright 2011-2016, Telestax Inc and individual contributors
 *  by the @authors tag.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package org.restcomm.connect.extension.api;

import javax.ws.rs.core.MultivaluedMap;

/**
 * Created by gvagenas on 10/10/2016.
 */
public class ApiRequest {
    public static enum Type {
        AVAILABLEPHONENUMBER,INCOMINGPHONENUMBER, CREATE_SUBACCOUNT
    };

    final String requestedAccountSid;
    final MultivaluedMap<String, String> data;
    final Type type;

    public ApiRequest(final String requestedAccountSid, final MultivaluedMap<String, String> data, final Type type) {
        this.requestedAccountSid = requestedAccountSid;
        this.data = data;
        this.type = type;
    }

    public MultivaluedMap<String, String> getData() {
        return data;
    }

    public String getRequestedAccountSid() {
        return requestedAccountSid;
    }

    public Type getType() {
        return type;
    }
}
