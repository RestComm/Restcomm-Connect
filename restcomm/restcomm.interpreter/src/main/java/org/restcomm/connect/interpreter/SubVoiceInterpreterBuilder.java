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
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.DaoManager;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class SubVoiceInterpreterBuilder {
    private final ActorRef supervisor;
    private Configuration configuration;
    private DaoManager storage;
    private ActorRef calls;
    private ActorRef conferences;
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

    private Boolean hangupOnEnd = false;

    /**
     * @author thomas.quintana@telestax.com (Thomas Quintana)
     */
    public SubVoiceInterpreterBuilder(final ActorRef supervisor) {
        super();
        this.supervisor = supervisor;
    }

    public ActorRef build() {
        final Props props = new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new SubVoiceInterpreter(supervisor, configuration, account, phone, version, url, method, fallbackUrl,
                        fallbackMethod, statusCallback, statusCallbackMethod, emailAddress, calls, conferences, sms, storage,
                        hangupOnEnd);
            }
        });
        ActorRef subVoiceInterpreter = null;
        try {
            subVoiceInterpreter = (ActorRef) Await.result(ask(supervisor, props, 5000), Duration.create(10, TimeUnit.SECONDS));
        } catch (Exception e) {

        }
        return subVoiceInterpreter;
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

    public void setHangupOnEnd(final Boolean hangupOnEnd) {
        this.hangupOnEnd = hangupOnEnd;
    }
}
