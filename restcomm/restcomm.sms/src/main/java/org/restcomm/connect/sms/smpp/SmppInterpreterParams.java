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

package org.restcomm.connect.sms.smpp;

import akka.actor.ActorRef;
import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.DaoManager;

import java.net.URI;

/**
 * @author oleg.agafonov@telestax.com (Oleg Agafonov)
 */
public final class SmppInterpreterParams {

    private Configuration configuration;
    private ActorRef smsService;
    private DaoManager storage;
    private Sid accountId;
    private String version;
    private URI url;
    private String method;
    private URI fallbackUrl;
    private String fallbackMethod;

    private SmppInterpreterParams(Configuration configuration, ActorRef smsService, DaoManager storage, Sid accountId, String version, URI url, String method, URI fallbackUrl, String fallbackMethod) {
        this.configuration = configuration;
        this.smsService = smsService;
        this.storage = storage;
        this.accountId = accountId;
        this.version = version;
        this.url = url;
        this.method = method;
        this.fallbackUrl = fallbackUrl;
        this.fallbackMethod = fallbackMethod;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public ActorRef getSmsService() {
        return smsService;
    }

    public DaoManager getStorage() {
        return storage;
    }

    public Sid getAccountId() {
        return accountId;
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

    public static final class Builder {
        private Configuration configuration;
        private ActorRef smsService;
        private DaoManager storage;
        private Sid accountId;
        private String version;
        private URI url;
        private String method;
        private URI fallbackUrl;
        private String fallbackMethod;

        public Builder setConfiguration(Configuration configuration) {
            this.configuration = configuration;
            return this;
        }

        public Builder setSmsService(ActorRef smsService) {
            this.smsService = smsService;
            return this;
        }

        public Builder setStorage(DaoManager storage) {
            this.storage = storage;
            return this;
        }

        public Builder setAccountId(Sid accountId) {
            this.accountId = accountId;
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

        public SmppInterpreterParams build() {
            return new SmppInterpreterParams(configuration, smsService, storage, accountId, version, url, method, fallbackUrl, fallbackMethod);
        }
    }
}
