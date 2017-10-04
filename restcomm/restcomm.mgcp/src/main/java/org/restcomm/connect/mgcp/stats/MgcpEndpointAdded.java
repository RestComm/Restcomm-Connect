package org.restcomm.connect.mgcp.stats;

import akka.actor.ActorRef;
import org.restcomm.connect.mgcp.MediaSession;

public class MgcpEndpointAdded {
    public enum Type {
        BRIDGE, IVR, PACKETRELAY, CONFERENCE
    };
    private final MediaSession session;
    private final ActorRef endpoint;
    private final Type type;

    public MgcpEndpointAdded (MediaSession session, ActorRef endpoint, Type type) {
        this.session = session;
        this.endpoint = endpoint;
        this.type = type;
    }

    public MediaSession getSession () {
        return session;
    }

    public ActorRef getEndpoint () {
        return endpoint;
    }

    public Type getType () {
        return type;
    }
}
