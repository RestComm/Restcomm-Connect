/**
 *
 */
package org.restcomm.connect.telephony.api;

import org.restcomm.connect.commons.annotations.concurrency.Immutable;

import akka.actor.ActorRef;

/**
 * @author Amit Bhayani
 *
 */
@Immutable
public class DestroyWaitUrlConfMediaGroup {

    private final ActorRef waitUrlConfMediaGroup;

    public DestroyWaitUrlConfMediaGroup(final ActorRef waitUrlConfMediaGroup) {
        super();
        this.waitUrlConfMediaGroup = waitUrlConfMediaGroup;
    }

    public ActorRef getWaitUrlConfMediaGroup() {
        return waitUrlConfMediaGroup;
    }
}
