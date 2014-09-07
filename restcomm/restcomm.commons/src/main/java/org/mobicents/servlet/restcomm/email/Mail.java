package org.mobicents.servlet.restcomm.email;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

@Immutable
public final class Mail {
    private final String from;
    private final String to;
    private final String subject;
    private final String body;

    public Mail(final String from, final String to, final String subject, final String body) {
        super();
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    public String from() {
        return from;
    }

    public String to() {
        return to;
    }

    public String subject() {
        return subject;
    }

    public String body() {
        return body;
    }
}
