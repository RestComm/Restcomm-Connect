package org.restcomm.connect.telephony.api;

/**
 * Created by gvagenas on 15/05/2017.
 */
public class GetStatistics {
    private final boolean withLiveCallDetails;
    private final boolean withMgcpStats;
    private final String accountSid;
    public GetStatistics (final boolean withLiveCallDetails, final boolean withMgcpStats, final String accountSid) {
        this.withLiveCallDetails = withLiveCallDetails;
        this.withMgcpStats = withMgcpStats;
        this.accountSid = accountSid;
    }
    public boolean isWithLiveCallDetails () {
        return withLiveCallDetails;
    }

    public boolean isWithMgcpStats () {
        return withMgcpStats;
    }

    public String getAccountSid () {
        return accountSid;
    }
}
