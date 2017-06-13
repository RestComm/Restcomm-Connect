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

/**
 * @author muhammad.bilal19@gmail.com (Muhammad Bilal)
 */
@Immutable
public class ClientFilter {

    private final String accountSid;
    private final String friendlyName;
    private final String login;
    private final Integer limit;
    private final Integer offset;

    /**
     * @param accountSid
     * @param friendlyName
     * @param login
     */
    public ClientFilter(String accountSid, String friendlyName, String login, Integer limit, Integer offset) {
        super();
        this.accountSid = accountSid;
        this.friendlyName = friendlyName;
        this.login = login;
        this.limit = limit;
        this.offset = offset;
    }

    /**
     * @return the accountSid
     */
    public String getAccountSid() {
        return accountSid;
    }

    /**
     * @return the friendlyName
     */
    public String getFriendlyName() {
        return friendlyName;
    }

    /**
     * @return the login
     */
    public String getLogin() {
        return login;
    }

    /**
     * @return the limit
     */
    public Integer getLimit() {
        return limit;
    }

    /**
     * @return the offset
     */
    public Integer getOffset() {
        return offset;
    }

}
