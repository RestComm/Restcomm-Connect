package org.restcomm.connect.mgcp.stats;

import akka.actor.ActorRef;

public class MgcpEndpointDeleted {
    private final ActorRef connection;

    public MgcpEndpointDeleted (ActorRef connection) {
        this.connection = connection;
    }

    public ActorRef getConnection () {
        return connection;
    }
}
