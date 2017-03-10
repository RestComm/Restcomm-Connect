package org.restcomm.connect.sms.smpp;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.DaoManager;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.net.URI;
import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;

public class SmppInterpreterBuilder {

    private static Logger logger = Logger.getLogger(SmppInterpreterBuilder.class);
    private final ActorRef supervisor;
    private Configuration configuration;
    private ActorRef service;
    private DaoManager storage;
    private Sid accountId;
    private String version;
    private URI url;
    private String method;
    private URI fallbackUrl;
    private String fallbackMethod;

    public SmppInterpreterBuilder(final ActorRef supervisor) {
        super();
        this.supervisor = supervisor;
    }

    public ActorRef build() {
        final Props props = new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public UntypedActor create() throws Exception {
                return new SmppInterpreter(supervisor, service, configuration, storage, accountId, version, url, method, fallbackUrl,
                        fallbackMethod);
            }
        });
        ActorRef smppInterpreter = null;
        try {
            smppInterpreter = (ActorRef) Await.result(ask(supervisor, props, 500), Duration.create(500, TimeUnit.MILLISECONDS));
        } catch (Exception e) {
            logger.error("Problem during creation of actor: "+e);
        }
        return smppInterpreter;
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
