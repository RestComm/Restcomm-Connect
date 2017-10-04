package org.restcomm.connect.mgcp.stats;

import akka.actor.ActorRef;
import org.restcomm.connect.mgcp.MediaSession;

public class MgcpEndpointAdded {
    private final MediaSession session;
    private final ActorRef endpoint;

    public MgcpEndpointAdded (MediaSession session, ActorRef endpoint) {
        this.session = session;
        this.endpoint = endpoint;
    }

    public MediaSession getSession () {
        return session;
    }

    public ActorRef getEndpoint () {
        return endpoint;
    }
}
