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

package org.restcomm.connect.geolocation.api;

import org.restcomm.connect.commons.annotations.concurrency.Immutable;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author <a href="mailto:fernando.mendioroz@telestax.com"> Fernando Mendioroz </a>
 *
 */
@Immutable
public final class CreateGeolocationSession {

    /*****************************************************/
    /*** Phase II: internetworking with GMLC through SIP */
    /*****************************************************/
    private final String from;
    private final String to;
    private final String accountSid;
    private final boolean isFromApi;


    //This will be used to create GeolocationSession from
    // 1. REST API GeolocationEndpoint - Send Geolocation from REST API
    // 2. GeolocationInterpreter.CreatingGeolocationSession - Send Geolocation using "Geolocation" RCML verb in Geolocation application
    public CreateGeolocationSession(final String from, final String to, final String accountSid, final boolean isFromApi) {
        this.from = from;
        this.to = to;
        this.accountSid = accountSid;
        this.isFromApi = isFromApi;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getAccountSid() {
        return accountSid;
    }

    public boolean isFromApi() {
        return isFromApi;
    }

    @Override
    public String toString() {
        return "From: "+from+" , To: "+to+" , AccountSid: "+accountSid+" , isFromApi: "+isFromApi;
    }
}
