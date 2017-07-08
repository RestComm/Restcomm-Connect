package org.restcomm.connect.commons.faulttolerance.tool;

import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActorFactory;
import akka.testkit.JavaTestKit;

/**
 * @author mariafarooq
 *
 */
public class ActorCreatingThread implements Runnable {
	private final ActorSystem system;

	public ActorCreatingThread(final ActorSystem system){
		this.system = system;
	}

	@Override
	public void run() {
		new JavaTestKit(system) {
        {
        	final ActorRef self = getRef();

        	ActorRef actorCreator = system.actorOf(new Props(new UntypedActorFactory() {
                private static final long serialVersionUID = 1L;

                @Override
                public Actor create() throws Exception {
                    return new MyUntypedActor(system);
                }
            }));

        	actorCreator.tell(new String(), self);
        }};
	}

}
