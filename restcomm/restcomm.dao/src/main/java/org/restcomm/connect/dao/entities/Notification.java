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

import java.net.URI;

import org.joda.time.DateTime;

import org.restcomm.connect.commons.annotations.concurrency.Immutable;
import org.restcomm.connect.commons.annotations.concurrency.NotThreadSafe;
import org.restcomm.connect.commons.dao.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Immutable
public final class Notification {
    public static final int ERROR = 0;
    public static final int WARNING = 1;

    private final Sid sid;
    private final DateTime dateCreated;
    private final DateTime dateUpdated;
    private final Sid accountSid;
    private final Sid callSid;
    private final String apiVersion;
    private final Integer log;
    private final Integer errorCode;
    private final URI moreInfo;
    private final String messageText;
    private final DateTime messageDate;
    private final URI requestUrl;
    private final String requestMethod;
    private final String requestVariables;
    private final String responseHeaders;
    private final String responseBody;
    private final URI uri;

    public Notification(final Sid sid, final DateTime dateCreated, final DateTime dateUpdated, final Sid accountSid,
            final Sid callSid, final String apiVersion, final Integer log, final Integer errorCode, final URI moreInfo,
            String messageText, final DateTime messageDate, final URI requestUrl, final String requestMethod,
            final String requestVariables, final String responseHeaders, final String responseBody, final URI uri) {
        super();
        this.sid = sid;
        this.dateCreated = dateCreated;
        this.dateUpdated = dateUpdated;
        this.accountSid = accountSid;
        this.callSid = callSid;
        this.apiVersion = apiVersion;
        this.log = log;
        this.errorCode = errorCode;
        this.moreInfo = moreInfo;
        this.messageText = messageText;
        this.messageDate = messageDate;
        this.requestUrl = requestUrl;
        this.requestMethod = requestMethod;
        this.requestVariables = requestVariables;
        this.responseHeaders = responseHeaders;
        this.responseBody = responseBody;
        this.uri = uri;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Sid getSid() {
        return sid;
    }

    public DateTime getDateCreated() {
        return dateCreated;
    }

    public DateTime getDateUpdated() {
        return dateUpdated;
    }

    public Sid getAccountSid() {
        return accountSid;
    }

    public Sid getCallSid() {
        return callSid;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public Integer getLog() {
        return log;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public URI getMoreInfo() {
        return moreInfo;
    }

    public String getMessageText() {
        return messageText;
    }

    public DateTime getMessageDate() {
        return messageDate;
    }

    public URI getRequestUrl() {
        return requestUrl;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public String getRequestVariables() {
        return requestVariables;
    }

    public String getResponseHeaders() {
        return responseHeaders;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public URI getUri() {
        return uri;
    }

    @NotThreadSafe
    public static final class Builder {
        private Sid sid;
        private Sid accountSid;
        private Sid callSid;
        private String apiVersion;
        private Integer log;
        private Integer errorCode;
        private URI moreInfo;
        private String messageText;
        private DateTime messageDate;
        private URI requestUrl;
        private String requestMethod;
        private String requestVariables;
        private String responseHeaders;
        private String responseBody;
        private URI uri;

        private Builder() {
            super();
        }

        public Notification build() {
            final DateTime now = DateTime.now();
            return new Notification(sid, now, now, accountSid, callSid, apiVersion, log, errorCode, moreInfo, messageText,
                    messageDate, requestUrl, requestMethod, requestVariables, responseHeaders, responseBody, uri);
        }

        public void setSid(final Sid sid) {
            this.sid = sid;
        }

        public void setAccountSid(final Sid accountSid) {
            this.accountSid = accountSid;
        }

        public void setCallSid(final Sid callSid) {
            this.callSid = callSid;
        }

        public void setApiVersion(final String apiVersion) {
            this.apiVersion = apiVersion;
        }

        public void setLog(final int log) {
            this.log = log;
        }

        public void setErrorCode(final int errorCode) {
            this.errorCode = errorCode;
        }

        public void setMoreInfo(final URI moreInfo) {
            this.moreInfo = moreInfo;
        }

        public void setMessageText(final String messageText) {
            this.messageText = messageText;
        }

        public void setMessageDate(final DateTime messageDate) {
            this.messageDate = messageDate;
        }

        public void setRequestUrl(final URI requestUrl) {
            this.requestUrl = requestUrl;
        }

        public void setRequestMethod(final String requestMethod) {
            this.requestMethod = requestMethod;
        }

        public void setRequestVariables(final String requestVariables) {
            this.requestVariables = requestVariables;
        }

        public void setResponseHeaders(final String responseHeaders) {
            this.responseHeaders = responseHeaders;
        }

        public void setResponseBody(final String responseBody) {
            this.responseBody = responseBody;
        }

        public void setUri(final URI uri) {
            this.uri = uri;
        }
    }
}
