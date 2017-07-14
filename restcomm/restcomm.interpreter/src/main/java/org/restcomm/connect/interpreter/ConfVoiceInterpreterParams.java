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
import org.restcomm.connect.telephony.api.CallInfo;

import java.net.URI;

/**
 * @author oleg.agafonov@telestax.com (Oleg Agafonov)
 */
public final class ConfVoiceInterpreterParams {

    private Configuration configuration;
    private Sid account;
    private String version;
    private URI url;
    private String method;
    private String emailAddress;
    private ActorRef conference;
    private DaoManager storage;
    private CallInfo callInfo;

    private ConfVoiceInterpreterParams(Configuration configuration, Sid account, String version, URI url, String method, String emailAddress, ActorRef conference, DaoManager storage, CallInfo callInfo) {
        this.configuration = configuration;
        this.account = account;
        this.version = version;
        this.url = url;
        this.method = method;
        this.emailAddress = emailAddress;
        this.conference = conference;
        this.storage = storage;
        this.callInfo = callInfo;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public Sid getAccount() {
        return account;
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

    public String getEmailAddress() {
        return emailAddress;
    }

    public ActorRef getConference() {
        return conference;
    }

    public DaoManager getStorage() {
        return storage;
    }

    public CallInfo getCallInfo() {
        return callInfo;
    }

    public static class ConfVoiceInterpreterParamsBuilder {
        private Configuration configuration;
        private Sid account;
        private String version;
        private URI url;
        private String method;
        private String emailAddress;
        private ActorRef conference;
        private DaoManager storage;
        private CallInfo callInfo;

        public ConfVoiceInterpreterParamsBuilder setConfiguration(Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        public ConfVoiceInterpreterParamsBuilder setAccount(Sid account) {
            this.account = account;
            return this;
        }

        public ConfVoiceInterpreterParamsBuilder setVersion(String version) {
            this.version = version;
            return this;
        }

        public ConfVoiceInterpreterParamsBuilder setUrl(URI url) {
            this.url = url;
            return this;
        }

        public ConfVoiceInterpreterParamsBuilder setMethod(String method) {
            this.method = method;
            return this;
        }

        public ConfVoiceInterpreterParamsBuilder setEmailAddress(String emailAddress) {
            this.emailAddress = emailAddress;
            return this;
        }

        public ConfVoiceInterpreterParamsBuilder setConference(ActorRef conference) {
            this.conference = conference;
            return this;
        }

        public ConfVoiceInterpreterParamsBuilder setStorage(DaoManager storage) {
            this.storage = storage;
            return this;
        }

        public ConfVoiceInterpreterParamsBuilder setCallInfo(CallInfo callInfo) {
            this.callInfo = callInfo;
            return this;
        }

        public ConfVoiceInterpreterParams biuld() {
            return new ConfVoiceInterpreterParams(configuration, account, version, url, method, emailAddress, conference, storage, callInfo);
        }
    }
}
