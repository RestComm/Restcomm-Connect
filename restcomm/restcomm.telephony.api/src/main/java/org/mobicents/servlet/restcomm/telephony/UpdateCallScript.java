package org.mobicents.servlet.restcomm.telephony;

import java.net.URI;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;
import org.mobicents.servlet.restcomm.entities.Sid;

import akka.actor.ActorRef;

@Immutable
public final class UpdateCallScript {
    private final ActorRef call;
    private final Sid account;
    private final String version;
    private final URI url;
    private final String method;
    private final URI fallbackUrl;
    private final String fallbackMethod;
    private final URI callback;
    private final String callbackMethod;

    public UpdateCallScript(final ActorRef call, final Sid account, final String version, final URI url, final String method,
            final URI fallbackUrl, final String fallbackMethod, final URI callback, final String callbackMethod) {
        super();
        this.call = call;
        this.account = account;
        this.version = version;
        this.url = url;
        this.method = method;
        this.fallbackUrl = fallbackUrl;
        this.fallbackMethod = fallbackMethod;
        this.callback = callback;
        this.callbackMethod = callbackMethod;
    }

    public ActorRef call() {
        return call;
    }

    public Sid account() {
        return account;
    }

    public String version() {
        return version;
    }

    public URI url() {
        return url;
    }

    public String method() {
        return method;
    }

    public URI fallbackUrl() {
        return fallbackUrl;
    }

    public String fallbackMethod() {
        return fallbackMethod;
    }

    public URI callback() {
        return callback;
    }

    public String callbackMethod() {
        return callbackMethod;
    }
}
