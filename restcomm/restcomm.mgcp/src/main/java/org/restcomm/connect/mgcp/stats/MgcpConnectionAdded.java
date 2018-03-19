package org.restcomm.connect.mgcp.stats;

public class MgcpConnectionAdded {
    private final String connId;
    private final String endpointId;

    public MgcpConnectionAdded (String connId, String endpointId) {
        this.connId = connId;
        this.endpointId = endpointId;
    }

    public String getConnId () {
        return connId;
    }

    public String getEndpointId () {
        return endpointId;
    }
}
