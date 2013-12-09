/*
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
package org.mobicents.servlet.restcomm.telephony;

import javax.servlet.sip.SipURI;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.dao.CallDetailRecordsDao;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author jean.deruelle@telestax.com
 */
@Immutable
public final class InitializeOutbound {
    private final String name;
    private final SipURI from;
    private final SipURI to;
    private final String username;
    private final String password;
    private final long timeout;
    private final boolean isFromApi;
    private final String apiVersion;
    private final Sid accountId;
    private final CreateCall.Type type;
    private final CallDetailRecordsDao recordsDao;

    public InitializeOutbound(final String name, final SipURI from, final SipURI to, final String username,
            final String password, final long timeout, final boolean isFromApi, final String apiVersion, final Sid accountId,
            final CreateCall.Type type, final CallDetailRecordsDao recordsDao) {
        super();
        this.name = name;
        this.from = from;
        this.to = to;
        this.username = username;
        this.password = password;
        this.timeout = timeout;
        this.isFromApi = isFromApi;
        this.apiVersion = apiVersion;
        this.accountId = accountId;
        this.type = type;
        this.recordsDao = recordsDao;
    }

    public String name() {
        return name;
    }

    public SipURI from() {
        return from;
    }

    public SipURI to() {
        return to;
    }

    public long timeout() {
        return timeout;
    }

    public boolean isFromApi() {
        return isFromApi;
    }

    public String apiVersion() {
        return apiVersion;
    }

    public Sid accountId() {
        return accountId;
    }

    public String username() {
        return username;
    }

    public String password() {
        return password;
    }

    public CreateCall.Type type() {
        return type;
    }

    public CallDetailRecordsDao recordsDao(){
        return recordsDao;
    }
}
