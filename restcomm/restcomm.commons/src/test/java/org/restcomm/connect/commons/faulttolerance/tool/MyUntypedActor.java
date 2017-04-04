package org.restcomm.connect.commons.faulttolerance.tool;

import static akka.pattern.Patterns.ask;

import java.util.concurrent.TimeUnit;

import akka.actor.ActorSystem;
import org.restcomm.connect.commons.faulttolerance.SupervisorActorCreationStressTest;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

/**
 * MyUntypedActor represent a restcomm-connect class that request supervisor to create actor for it
 *
 * @author mariafarooq
 *
 */
public class MyUntypedActor  extends UntypedActor {
    private LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

	private final ActorSystem system;

	public MyUntypedActor(final ActorSystem system){
		this.system = system;
	}

	@Override
	public void onReceive(Object message) throws Exception {
		final Class<?> klass = message.getClass();
        if (logger.isInfoEnabled()) {
            logger.info(" ********** MyUntypedActor " + self().path() + " Processing Message: " + klass.getName());
        }
        if (String.class.equals(klass)) {
        	if (logger.isInfoEnabled())
        		logger.debug("create");
        	final Props props = new Props(new UntypedActorFactory() {
                private static final long serialVersionUID = 1L;

                @Override
                public UntypedActor create() throws Exception {
                    return new SimpleActor();
                }
            });
            ActorRef actorToBeCreated = null;
            try {
            	actorToBeCreated = system.actorOf(props);
                if (logger.isInfoEnabled())
                	logger.debug("Actor created: "+actorToBeCreated.path());
            	SupervisorActorCreationStressTest.actorSuccessCount.incrementAndGet();
            } catch (Exception e) {
            	SupervisorActorCreationStressTest.actorFailureCount.incrementAndGet();
            	e.printStackTrace();
            	logger.error("Problem during creation of actor: "+e);
            }
    	} else {
        	unhandled(message);
        }
	}

}
