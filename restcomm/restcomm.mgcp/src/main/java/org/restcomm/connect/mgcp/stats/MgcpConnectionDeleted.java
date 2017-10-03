package org.restcomm.connect.mgcp.stats;

import akka.actor.ActorRef;

public class MgcpConnectionDeleted {
    private final ActorRef connection;

    public MgcpConnectionDeleted (ActorRef connection) {
        this.connection = connection;
    }

    public ActorRef getConnection () {
        return connection;
    }
}
