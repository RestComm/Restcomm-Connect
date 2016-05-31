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
package org.mobicents.servlet.restcomm.entities;

/**
 * @author muhammad.bilal19@gmail.com (Muhammad Bilal)
 */
public class QueueFilter {

    private String accountSid;
    private int offset;
    private int limit;

    public String getAccountSid() {
        return accountSid;
    }

    public QueueFilter(String accountSid, int offset, int limit) {
        super();
        this.accountSid = accountSid;
        this.offset = offset;
        this.limit = limit;
    }

    public void setAccountSid(String accountSid) {
        this.accountSid = accountSid;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

}
