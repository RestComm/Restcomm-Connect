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
public final class VoiceInterpreterParams {

    private Configuration configuration;
    private DaoManager storage;
    private ActorRef callManager;
    private ActorRef conferenceCenter;
    private ActorRef bridgeManager;
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
    private String referTarget;
    private String emailAddress;
    private ActorRef monitoring;
    private String rcml;

    // IMS authentication
    private boolean asImsUa;
    private String imsUaLogin;
    private String imsUaPassword;
    private String transferor;
    private String transferee;

    private VoiceInterpreterParams(Configuration configuration, DaoManager storage, ActorRef callManager, ActorRef conferences, ActorRef bridgeManager, ActorRef smsService, Sid account, Sid phone, String version, URI url, String method, URI fallbackUrl, String fallbackMethod, URI statusCallback, String statusCallbackMethod, String referTarget, String emailAddress, ActorRef monitoring, String rcml, boolean asImsUa, String imsUaLogin, String imsUaPassword, String transferor, String transferee) {
        this.configuration = configuration;
        this.storage = storage;
        this.callManager = callManager;
        this.conferenceCenter = conferences;
        this.bridgeManager = bridgeManager;
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
        this.referTarget = referTarget;
        this.emailAddress = emailAddress;
        this.monitoring = monitoring;
        this.rcml = rcml;
        this.asImsUa = asImsUa;
        this.imsUaLogin = imsUaLogin;
        this.imsUaPassword = imsUaPassword;
        this.transferor = transferor;
        this.transferee = transferee;
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

    public ActorRef getBridgeManager() {
        return bridgeManager;
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

    public String getReferTarget() {
        return referTarget;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public ActorRef getMonitoring() {
        return monitoring;
    }

    public String getRcml() {
        return rcml;
    }

    public boolean isAsImsUa() {
        return asImsUa;
    }

    public String getImsUaLogin() {
        return imsUaLogin;
    }

    public String getImsUaPassword() {
        return imsUaPassword;
    }

    public String getTransferor() {
        return transferor;
    }

    public String getTransferee() {
        return transferee;
    }

    public static final class Builder {
        private Configuration configuration;
        private DaoManager storage;
        private ActorRef callManager;
        private ActorRef conferenceCenter;
        private ActorRef bridgeManager;
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
        private String referTarget;
        private String emailAddress;
        private ActorRef monitoring;
        private String rcml;
        private boolean asImsUa;
        private String imsUaLogin;
        private String imsUaPassword;
        private String transferor;
        private String transferee;

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

        public Builder setBridgeManager(ActorRef bridgeManager) {
            this.bridgeManager = bridgeManager;
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

        public Builder setReferTarget(String referTarget) {
            this.referTarget = referTarget;
            return this;
        }

        public Builder setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
            return this;
        }

        public Builder setMonitoring(ActorRef monitoring) {
            this.monitoring = monitoring;
            return this;
        }

        public Builder setRcml(String rcml) {
            this.rcml = rcml;
            return this;
        }

        public Builder setAsImsUa(boolean asImsUa) {
            this.asImsUa = asImsUa;
            return this;
        }

        public Builder setImsUaLogin(String imsUaLogin) {
            this.imsUaLogin = imsUaLogin;
            return this;
        }

        public Builder setImsUaPassword(String imsUaPassword) {
            this.imsUaPassword = imsUaPassword;
            return this;
        }

        public Builder setTransferor(String transferor) {
            this.transferor = transferor;
            return this;
        }

        public Builder setTransferee(String transferee) {
            this.transferee = transferee;
            return this;
        }

        public VoiceInterpreterParams build() {
            return new VoiceInterpreterParams(configuration, storage, callManager, conferenceCenter, bridgeManager, smsService, account, phone, version, url, method, fallbackUrl, fallbackMethod, statusCallback, statusCallbackMethod, referTarget, emailAddress, monitoring, rcml, asImsUa, imsUaLogin, imsUaPassword, transferor, transferee);
        }
    }
}
