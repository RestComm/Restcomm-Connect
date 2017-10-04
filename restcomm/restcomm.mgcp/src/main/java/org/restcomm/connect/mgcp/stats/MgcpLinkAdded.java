package org.restcomm.connect.mgcp.stats;

import akka.actor.ActorRef;
import org.restcomm.connect.mgcp.MediaSession;

public class MgcpLinkAdded {
    private final MediaSession session;
    private final ActorRef link;

    public MgcpLinkAdded (MediaSession session, ActorRef link) {
        this.session = session;
        this.link = link;
    }

    public MediaSession getSession () {
        return session;
    }

    public ActorRef getLink () {
        return link;
    }
}
