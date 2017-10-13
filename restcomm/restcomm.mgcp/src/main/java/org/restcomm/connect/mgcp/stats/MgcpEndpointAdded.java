package org.restcomm.connect.mgcp.stats;

public class MgcpEndpointAdded {
    private final String connId;
    private final String endpoint;

    public MgcpEndpointAdded (String connId, String endpoint) {
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
