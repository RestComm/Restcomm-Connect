package org.restcomm.connect.mgcp.stats;

import akka.actor.ActorRef;

public class MgcpLinkDeleted {
    private final ActorRef link;

    public MgcpLinkDeleted (ActorRef link) {
        this.link = link;
    }

    public ActorRef getLink () {
        return link;
    }
}
