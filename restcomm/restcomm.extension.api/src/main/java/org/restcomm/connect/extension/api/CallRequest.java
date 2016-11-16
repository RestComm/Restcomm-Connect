package org.restcomm.connect.extension.api;


import org.restcomm.connect.commons.dao.Sid;

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
    private final boolean isFromApi;
    private final boolean parentCallSidExists;

    public CallRequest(String from, String to, Type type, Sid accountId, boolean isFromApi, boolean parentCallSidExists) {
        this.from = from;
        this.to = to;
        this.type = type;
        this.accountId = accountId;
        this.isFromApi = isFromApi;
        this.parentCallSidExists = parentCallSidExists;
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

    public boolean isFromApi() {
        return isFromApi;
    }

    public boolean isParentCallSidExists() {
        return parentCallSidExists;
    }

    @Override
    public String toString() {
        return "From: "+from+", To: "+to+", Type: "+type.name()+", AccountId: "+accountId+", isFromApi: "+isFromApi+", parentCallSidExists: "+parentCallSidExists;
    }
}
