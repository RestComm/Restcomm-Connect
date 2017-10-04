package org.restcomm.connect.mgcp.stats;

import akka.actor.ActorRef;

public class MgcpEndpointDeleted {
    private final ActorRef endpoint;

    public MgcpEndpointDeleted (ActorRef endpoint) {
        this.endpoint = endpoint;
    }

    public ActorRef getEndpoint () {
        return endpoint;
    }
}
