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

import org.restcomm.connect.commons.annotations.concurrency.Immutable;

@Immutable
public class ApplicationFilter {
    private String accountSid;
    private String friendlyName;

    public ApplicationFilter ( ) {
    }

    public ApplicationFilter ( String accountSid, String friendlyName ) {
        this.accountSid = accountSid;
        this.friendlyName = friendlyName;
    }

    /**
     * @param accountSid - the new account Sid need to be set
     * @return void
     */
    public void setAccountSid ( String accountSid ) {
        this.accountSid = accountSid;
    }

    /**
     * @return account Sid
     */
    public String getAccountSid ( ) {
        return this.accountSid;
    }

    /**
     * @param friendlyName - the new application friendly name need to be set
     * @return void
     */
    public void setFriendlyName ( String friendlyName ) {
        this.friendlyName = friendlyName;
    }

    /**
     * @return the application friendly name
     */
    public String getFriendlyName ( ) {
        return this.friendlyName;
    }
}
