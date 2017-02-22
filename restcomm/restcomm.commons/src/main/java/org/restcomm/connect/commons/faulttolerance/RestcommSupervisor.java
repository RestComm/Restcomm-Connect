package org.restcomm.connect.commons.faulttolerance;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.StopChild;
import akka.actor.SupervisorStrategy;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import scala.concurrent.duration.Duration;

import java.util.concurrent.ConcurrentHashMap;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.restart;
import static akka.actor.SupervisorStrategy.resume;
import static akka.actor.SupervisorStrategy.stop;

/**
 * Created by gvagenas on 22/02/2017.
 */
public class RestcommSupervisor extends UntypedActor {

    private LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
    private final ConcurrentHashMap<ActorPath, ActorRef> liveActors;
    private final ConcurrentHashMap<ActorPath, ActorRef> terminatedActors;

    public RestcommSupervisor() {
        liveActors = new ConcurrentHashMap<ActorPath, ActorRef>();
        terminatedActors = new ConcurrentHashMap<ActorPath, ActorRef>();
    }

    public SupervisorStrategy strategy =
            new OneForOneStrategy(10, Duration.create("1 minute"),
                    new Function<Throwable, akka.actor.SupervisorStrategy.Directive>() {
                        @Override
                        public akka.actor.SupervisorStrategy.Directive apply(Throwable t) {
                            if (t instanceof ArithmeticException) {
                                logger.info("ArithmeticExceptio, will resume actor");
                                return resume();
                            } else if (t instanceof NullPointerException) {
                                logger.info("NullPointerException, will restart actor");
                                return restart();
                            } else if (t instanceof IllegalArgumentException) {
                                logger.info("IllegalArgumentException, will stop actor");
                                return stop();
                            } else {
                                logger.info("Will escalate");
                                return escalate();
                            }
                        }
                    });

    @Override
    public void onReceive(Object msg) throws Exception {
        final Class<?> klass = msg.getClass();
        final ActorRef sender = getSender();

        logger.info(" ********** RestcommSupervisor " + self().path() + " Processing Message: " + klass.getName());
        if (msg instanceof Props) {
            final ActorRef actor = getContext().actorOf((Props) msg);
            sender.tell(actor, getSelf());
            getContext().watch(actor);
            logger.info("Created and watching actor: "+actor.path().toString());
            liveActors.put(actor.path(), actor);
        } else if (msg instanceof Terminated || msg instanceof StopChild || msg.equals("kill")) {
            logger.info("Received Terminated message");
            final Terminated t = (Terminated) msg;
            final ActorRef actor = t.actor();
            liveActors.remove(t.actor().path());
        } else if (msg instanceof ActorPath) {
            final ActorPath actorPath = (ActorPath) msg;
            if (liveActors!= null && liveActors.containsKey(actorPath)) {
                //Actor is still in the list, so its not restarted
                sender.tell(false, getSelf());
            } else {
                //Actor is not in the list, so its restarted
                sender.tell(true, getSelf());
            }
        } else {
            unhandled(msg);
        }
    }

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }
}
