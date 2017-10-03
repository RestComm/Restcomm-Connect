package org.restcomm.connect.mgcp.stats;

import akka.actor.ActorRef;
import org.restcomm.connect.mgcp.MediaSession;

public class MgcpConnectionAdded {
    private final MediaSession session;
    private final ActorRef connection;

    public MgcpConnectionAdded (MediaSession session, ActorRef connection) {
        this.session = session;
        this.connection = connection;
    }

    public MediaSession getSession () {
        return session;
    }

    public ActorRef getConnection () {
        return connection;
    }
}
