package org.restcomm.connect.mgcp.stats;

import akka.actor.ActorRef;

public class MgcpLinkDeleted {
    private final ActorRef connection;

    public MgcpLinkDeleted (ActorRef connection) {
        this.connection = connection;
    }

    public ActorRef getConnection () {
        return connection;
    }
}
