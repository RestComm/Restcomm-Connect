package org.restcomm.connect.mgcp.stats;

public class MgcpConnectionDeleted {
    private final String connId;

    public MgcpConnectionDeleted (String connId) {
        this.connId = connId;
    }

    public String getConnId () {
        return connId;
    }
}
