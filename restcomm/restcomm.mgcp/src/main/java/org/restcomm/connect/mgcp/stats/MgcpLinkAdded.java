package org.restcomm.connect.mgcp.stats;

import akka.actor.ActorRef;
import org.restcomm.connect.mgcp.MediaSession;

public class MgcpLinkAdded {
    private final MediaSession session;
    private final ActorRef connection;

    public MgcpLinkAdded (MediaSession session, ActorRef connection) {
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
