/**
 *
 */
package org.mobicents.servlet.restcomm.telephony;

import org.mobicents.servlet.restcomm.annotations.concurrency.Immutable;

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
