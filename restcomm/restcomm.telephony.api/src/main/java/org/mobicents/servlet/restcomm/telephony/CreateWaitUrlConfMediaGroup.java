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
public class CreateWaitUrlConfMediaGroup {

	private final ActorRef confVoiceInterpreter;

	/**
	 * 
	 */
	public CreateWaitUrlConfMediaGroup(final ActorRef confVoiceInterpreter) {
		this.confVoiceInterpreter = confVoiceInterpreter;
	}

	public ActorRef getConfVoiceInterpreter() {
		return confVoiceInterpreter;
	}

}
