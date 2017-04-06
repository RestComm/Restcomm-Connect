package org.restcomm.connect.sms.smpp;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.DaoManager;

import java.net.URI;

public class SmppInterpreterBuilder {

    private static Logger logger = Logger.getLogger(SmppInterpreterBuilder.class);
    private final ActorSystem system;
    private Configuration configuration;
    private ActorRef service;
    private DaoManager storage;
    private Sid accountId;
    private String version;
    private URI url;
    private String method;
    private URI fallbackUrl;
    private String fallbackMethod;

    public SmppInterpreterBuilder(final ActorSystem system) {
        super();
        this.system = system;
    }

    public ActorRef build() {
        final Props props = new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new SmppInterpreter(service, configuration, storage, accountId, version, url, method, fallbackUrl,
                        fallbackMethod);
            }
        });
        return system.actorOf(props);
    }

    public void setConfiguration(final Configuration configuration) {
        this.configuration = configuration;
    }

    public void setSmsService(final ActorRef service) {
        this.service = service;
    }

    public void setStorage(final DaoManager storage) {
        this.storage = storage;
    }

    public void setAccount(final Sid accountId) {
        this.accountId = accountId;
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

    public void setVersion(final String version) {
        this.version = version;
    }

}
