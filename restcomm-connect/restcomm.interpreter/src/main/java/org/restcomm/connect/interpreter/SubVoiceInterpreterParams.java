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

package org.restcomm.connect.interpreter;

import akka.actor.ActorRef;
import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.DaoManager;

import java.net.URI;

/**
 * @author oleg.agafonov@telestax.com (Oleg Agafonov)
 */
public final class SubVoiceInterpreterParams {

    private Configuration configuration;
    private DaoManager storage;
    private ActorRef callManager;
    private ActorRef conferenceCenter;
    private ActorRef smsService;
    private Sid account;
    private Sid phone;
    private String version;
    private URI url;
    private String method;
    private URI fallbackUrl;
    private String fallbackMethod;
    private URI statusCallback;
    private String statusCallbackMethod;
    private String emailAddress;

    private Boolean hangupOnEnd;

    private SubVoiceInterpreterParams(Configuration configuration, DaoManager storage, ActorRef callManager, ActorRef conferenceCenter, ActorRef smsService, Sid account, Sid phone, String version, URI url, String method, URI fallbackUrl, String fallbackMethod, URI statusCallback, String statusCallbackMethod, String emailAddress, Boolean hangupOnEnd) {
        this.configuration = configuration;
        this.storage = storage;
        this.callManager = callManager;
        this.conferenceCenter = conferenceCenter;
        this.smsService = smsService;
        this.account = account;
        this.phone = phone;
        this.version = version;
        this.url = url;
        this.method = method;
        this.fallbackUrl = fallbackUrl;
        this.fallbackMethod = fallbackMethod;
        this.statusCallback = statusCallback;
        this.statusCallbackMethod = statusCallbackMethod;
        this.emailAddress = emailAddress;
        this.hangupOnEnd = hangupOnEnd;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public DaoManager getStorage() {
        return storage;
    }

    public ActorRef getCallManager() {
        return callManager;
    }

    public ActorRef getConferenceCenter() {
        return conferenceCenter;
    }

    public ActorRef getSmsService() {
        return smsService;
    }

    public Sid getAccount() {
        return account;
    }

    public Sid getPhone() {
        return phone;
    }

    public String getVersion() {
        return version;
    }

    public URI getUrl() {
        return url;
    }

    public String getMethod() {
        return method;
    }

    public URI getFallbackUrl() {
        return fallbackUrl;
    }

    public String getFallbackMethod() {
        return fallbackMethod;
    }

    public URI getStatusCallback() {
        return statusCallback;
    }

    public String getStatusCallbackMethod() {
        return statusCallbackMethod;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public Boolean getHangupOnEnd() {
        return hangupOnEnd;
    }

    public static final class Builder {
        private Configuration configuration;
        private DaoManager storage;
        private ActorRef callManager;
        private ActorRef conferenceCenter;
        private ActorRef smsService;
        private Sid account;
        private Sid phone;
        private String version;
        private URI url;
        private String method;
        private URI fallbackUrl;
        private String fallbackMethod;
        private URI statusCallback;
        private String statusCallbackMethod;
        private String emailAddress;
        private Boolean hangupOnEnd = false;

        public Builder() {
        }

        public Builder setConfiguration(Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder setStorage(DaoManager storage) {
            this.storage = storage;
            return this;
        }

        public Builder setCallManager(ActorRef callManager) {
            this.callManager = callManager;
            return this;
        }

        public Builder setConferenceCenter(ActorRef conferenceCenter) {
            this.conferenceCenter = conferenceCenter;
            return this;
        }

        public Builder setSmsService(ActorRef smsService) {
            this.smsService = smsService;
            return this;
        }

        public Builder setAccount(Sid account) {
            this.account = account;
            return this;
        }

        public Builder setPhone(Sid phone) {
            this.phone = phone;
            return this;
        }

        public Builder setVersion(String version) {
            this.version = version;
            return this;
        }

        public Builder setUrl(URI url) {
            this.url = url;
            return this;
        }

        public Builder setMethod(String method) {
            this.method = method;
            return this;
        }

        public Builder setFallbackUrl(URI fallbackUrl) {
            this.fallbackUrl = fallbackUrl;
            return this;
        }

        public Builder setFallbackMethod(String fallbackMethod) {
            this.fallbackMethod = fallbackMethod;
            return this;
        }

        public Builder setStatusCallback(URI statusCallback) {
            this.statusCallback = statusCallback;
            return this;
        }

        public Builder setStatusCallbackMethod(String statusCallbackMethod) {
            this.statusCallbackMethod = statusCallbackMethod;
            return this;
        }

        public Builder setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
            return this;
        }

        public Builder setHangupOnEnd(Boolean hangupOnEnd) {
            this.hangupOnEnd = hangupOnEnd;
            return this;
        }

        public SubVoiceInterpreterParams build() {
            return new SubVoiceInterpreterParams(configuration, storage, callManager, conferenceCenter, smsService, account, phone, version, url, method, fallbackUrl, fallbackMethod, statusCallback, statusCallbackMethod, emailAddress, hangupOnEnd);
        }
    }
}
