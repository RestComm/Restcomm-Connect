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
package org.restcomm.connect.telephony.api;

import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.commons.telephony.CreateCallType;
import org.restcomm.connect.extension.api.IExtensionCreateCallRequest;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author jean.deruelle@telestax.com
 * @author gvagenas@telestax.com
 */
@Immutable
public final class CreateCall implements IExtensionCreateCallRequest{

    private final String from;
    private final String to;
    private String username;
    private String password;
    private final boolean isFromApi;
    private final int timeout;
    private final CreateCallType type;
    private final Sid accountId;
    private boolean createCDR = true;
    private final Sid parentCallSid;
    private final URI statusCallbackUrl;
    private final String statusCallbackMethod;
    private final List<String> statusCallbackEvent;
    private String outboundProxy;
    private String outboundProxyUsername;
    private String outboundProxyPassword;
    private Map<String,ArrayList<String>> outboundProxyHeaders;

    public CreateCall(final String from, final String to, final String username, final String password,
            final boolean isFromApi, final int timeout, final CreateCallType type, final Sid accountId, final Sid parentCallSid,
            final URI statusCallbackUrl, final String statusCallbackMethod, final List<String> statusCallbackEvent
            ) {
        this(from, to, username, password, isFromApi, timeout, type, accountId, parentCallSid, statusCallbackUrl, statusCallbackMethod, statusCallbackEvent, null,null,null,null);
    }
    public CreateCall(final String from, final String to, final String username, final String password,
            final boolean isFromApi, final int timeout, final CreateCallType type, final Sid accountId, final Sid parentCallSid,
            final URI statusCallbackUrl, final String statusCallbackMethod, final List<String> statusCallbackEvent,
            final String outboundProxy, final String outboundProxyUsername, final String outboundProxyPassword, final Map<String,ArrayList<String>> outboundProxyHeaders) {
        super();
        this.from = from;
        this.to = to;
        this.username = username;
        this.password = password;
        this.isFromApi = isFromApi;
        this.timeout = timeout;
        this.type = type;
        this.accountId = accountId;
        this.parentCallSid = parentCallSid;
        this.statusCallbackUrl = statusCallbackUrl;
        this.statusCallbackMethod = statusCallbackMethod;
        this.statusCallbackEvent = statusCallbackEvent;
        this.outboundProxy = outboundProxy;
        this.outboundProxyUsername = outboundProxyUsername;//FIXME:unused
        this.outboundProxyPassword = outboundProxyPassword;//FIXME:unused
        this.outboundProxyHeaders = outboundProxyHeaders;
    }

    public String from() {
        return from;
    }

    public String to() {
        return to;
    }

    public int timeout() {
        return timeout;
    }

    public CreateCallType type() {
        return type;
    }

    public Sid accountId() {
        return accountId;
    }

    public String username() {
        return username;
    }
    public String setUsername() {
        return username;
    }

    public String password() {
        return password;
    }

    public String setPassword() {
        return password;
    }

    public boolean isCreateCDR() {
        return createCDR;
    }

    public void setCreateCDR(boolean createCDR) {
        this.createCDR = createCDR;
    }

    public Sid parentCallSid() {
        return parentCallSid;
    }

    public URI statusCallback() { return statusCallbackUrl; }

    public String statusCallbackMethod() { return statusCallbackMethod; }

    public List<String> statusCallbackEvent() { return statusCallbackEvent; }

    /**
     * @return the outboundProxy
     */
    public String getOutboundProxy() {
        return outboundProxy;
    }
    /**
     * @param outboundProxy the outboundProxy to set
     */
    public void setOutboundProxy(String outboundProxy) {
        this.outboundProxy = outboundProxy;
    }
    /**
     * @return the outboundProxyUsername
     */
    public String getOutboundProxyUsername() {
        return username;
    }
    /**
     * @param outboundProxyUsername the outboundProxyUsername to set
     */
    public void setOutboundProxyUsername(String outboundProxyUsername) {
        this.username = outboundProxyUsername;
    }
    /**
     * @return the outboundProxyPassword
     */
    public String getOutboundProxyPassword() {
        return password;
    }
    /**
     * @param outboundProxyPassword the outboundProxyPassword to set
     */
    public void setOutboundProxyPassword(String outboundProxyPassword) {
        this.password = outboundProxyPassword;
    }
    /**
     * @return the outboundProxyHeaders
     */
    public Map<String,ArrayList<String>> getOutboundProxyHeaders() {
        return outboundProxyHeaders;
    }
    /**
     * @param outboundProxyHeaders the outboundProxyHeaders to set
     */
    public void setOutboundProxyHeaders(Map<String,ArrayList<String>> outboundProxyHeaders) {
        this.outboundProxyHeaders = outboundProxyHeaders;
    }
    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public Sid getAccountId() {
        return accountId;
    }

    public boolean isFromApi() {
        return isFromApi;
    }

    public boolean isParentCallSidExists() {
        return parentCallSid != null;
    }

    @Override
    public String toString() {
        return "From: "+from+", To: "+to+", Type: "+type.name()+", AccountId: "+accountId+", isFromApi: "+isFromApi+", parentCallSidExists: "+isParentCallSidExists();
    }
    @Override
    public CreateCallType getType() {
        return type;
    }
}
