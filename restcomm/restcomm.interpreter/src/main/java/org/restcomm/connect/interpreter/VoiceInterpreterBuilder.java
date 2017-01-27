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
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;

import java.net.URI;

import org.apache.commons.configuration.Configuration;

import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.commons.dao.Sid;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author gvagenas@gmail.com (George Vagenas)
 */
public final class VoiceInterpreterBuilder {
    private final ActorSystem system;
    private Configuration configuration;
    private DaoManager storage;
    private ActorRef calls;
    private ActorRef conferences;
    private ActorRef bridges;
    private ActorRef sms;
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
    private ActorRef monitoring;
    private String rcml;

    // IMS authentication
    private boolean asImsUa;
    private String imsUaLogin;
    private String imsUaPassword;

    /**
     * @author thomas.quintana@telestax.com (Thomas Quintana)
     */
    public VoiceInterpreterBuilder(final ActorSystem system) {
        super();
        this.system = system;
    }

    public ActorRef build() {
        return system.actorOf(new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new VoiceInterpreter(configuration, account, phone, version, url, method, fallbackUrl, fallbackMethod,
                        statusCallback, statusCallbackMethod, emailAddress, calls, conferences, bridges, sms, storage, monitoring, rcml,
                        asImsUa, imsUaLogin, imsUaPassword);
            }
        }));
    }

    public void setConfiguration(final Configuration configuration) {
        this.configuration = configuration;
    }

    public void setStorage(final DaoManager storage) {
        this.storage = storage;
    }

    public void setCallManager(final ActorRef calls) {
        this.calls = calls;
    }

    public void setConferenceManager(final ActorRef conferences) {
        this.conferences = conferences;
    }

    public void setBridgeManager(final ActorRef bridges) {
        this.bridges = bridges;
    }

    public void setSmsService(final ActorRef sms) {
        this.sms = sms;
    }

    public void setAccount(final Sid account) {
        this.account = account;
    }

    public void setPhone(final Sid phone) {
        this.phone = phone;
    }

    public void setUrl(final URI url) {
        this.url = url;
    }

    public void setMethod(final String method) {
        this.method = method;
    }

    public void setFallbackUrl(final URI fallbackUrl) {
        this.fallbackUrl = fallbackUrl;
    }

    public void setFallbackMethod(final String fallbackMethod) {
        this.fallbackMethod = fallbackMethod;
    }

    public void setStatusCallback(final URI statusCallback) {
        this.statusCallback = statusCallback;
    }

    public void setStatusCallbackMethod(final String statusCallbackMethod) {
        this.statusCallbackMethod = statusCallbackMethod;
    }

    public void setEmailAddress(final String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public void setVersion(final String version) {
        this.version = version;
    }

    public void setMonitoring(ActorRef monitoring) {
        this.monitoring = monitoring;
    }

    public void setRcml(final String rcml) { this.rcml = rcml; }

    public void setAsImsUa(boolean asImsUa) {
        this.asImsUa = asImsUa;
    }

    public void setImsUaLogin(String imsUaLogin) {
        this.imsUaLogin = imsUaLogin;
    }

    public void setImsUaPassword(String imsUaPassword) {
        this.imsUaPassword = imsUaPassword;
    }
}