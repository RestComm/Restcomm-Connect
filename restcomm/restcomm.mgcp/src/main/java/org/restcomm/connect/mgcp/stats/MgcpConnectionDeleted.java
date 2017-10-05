package org.restcomm.connect.mgcp.stats;

public class MgcpConnectionDeleted {
    private final String connId;
    private final String endpoint;

    public MgcpConnectionDeleted (String connId, String endpoint) {
        this.connId = connId;
        this.endpoint = endpoint;
    }

    public String getConnId () {
        return connId;
    }

    public String getEndpoint () {
        return endpoint;
    }
}
