package org.restcomm.connect.mgcp.stats;

public class MgcpEndpointDeleted {
    private final String endpoint;

    public MgcpEndpointDeleted (String endpoint) {
        this.endpoint = endpoint;
    }

    public String getEndpoint () {
        return endpoint;
    }
}
