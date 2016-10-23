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
public class CreateWaitUrlConfMediaGroup {

    private final ActorRef confVoiceInterpreter;


    public CreateWaitUrlConfMediaGroup(final ActorRef confVoiceInterpreter) {
        this.confVoiceInterpreter = confVoiceInterpreter;
    }

    public ActorRef getConfVoiceInterpreter() {
        return confVoiceInterpreter;
    }

}
