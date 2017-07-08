package org.restcomm.connect.telephony.api;

/**
 * Created by gvagenas on 15/05/2017.
 */
public class GetStatistics {
    private final boolean withLiveCallDetails;
    private final String accountSid;
    public GetStatistics (final boolean withLiveCallDetails, final String accountSid) {
        this.withLiveCallDetails = withLiveCallDetails;
        this.accountSid = accountSid;
    }
    public boolean isWithLiveCallDetails () {
        return withLiveCallDetails;
    }

    public String getAccountSid () {
        return accountSid;
    }
}
