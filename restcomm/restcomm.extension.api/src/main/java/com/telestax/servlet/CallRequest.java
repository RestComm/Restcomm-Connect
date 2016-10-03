package com.telestax.servlet;

import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * Created by gvagenas on 27/09/16.
 */
public class CallRequest {
    public static enum Type {
        CLIENT, PSTN, SIP, USSD
    };

    private final String from;
    private final String to;
    private final Type type;
    private final Sid accountId;

    public CallRequest(String from, String to, Type type, Sid accountId) {
        this.from = from;
        this.to = to;
        this.type = type;
        this.accountId = accountId;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public Type getType() {
        return type;
    }

    public Sid getAccountId() {
        return accountId;
    }

    @Override
    public String toString() {
        return "From: "+from+", To: "+to+", Type: "+type.name()+", AccountId: "+accountId;
    }
}
