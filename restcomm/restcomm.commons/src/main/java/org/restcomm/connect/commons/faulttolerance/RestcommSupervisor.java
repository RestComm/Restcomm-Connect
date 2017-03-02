package org.restcomm.connect.commons.faulttolerance;

import akka.actor.ActorContext;
import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ChildRestartStats;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.StopChild;
import akka.actor.SupervisorStrategy;
import akka.actor.Terminated;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import org.restcomm.connect.commons.fsm.TransitionFailedException;
import org.restcomm.connect.commons.fsm.TransitionNotFoundException;
import scala.collection.Iterable;
import scala.concurrent.duration.Duration;

import javax.servlet.sip.ServletParseException;
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

    public RestcommSupervisor() {
        liveActors = new ConcurrentHashMap<ActorPath, ActorRef>();
    }


    RestcommFaultToleranceStrategy defaultStrategy = new RestcommFaultToleranceStrategy(10, Duration.create("1 minute"),
            new RestcommFaultToleranceDecider());

    @Override
    public void onReceive(Object msg) throws Exception {
        final Class<?> klass = msg.getClass();
        final ActorRef sender = getSender();

        logger.info(" ********** RestcommSupervisor " + self().path() + " Processing Message: " + klass.getName());

        if (msg instanceof Props) {
            final ActorRef actor = getContext().actorOf((Props) msg);
            getContext().watch(actor);
            logger.info("Created and watching actor: " + actor.path().toString());
            liveActors.put(actor.path(), actor);
            sender.tell(actor, getSelf());
        } else if (msg instanceof Terminated) {
            logger.info("Received Terminated message");
            final Terminated t = (Terminated) msg;
            final ActorRef actor = t.actor();
            liveActors.remove(t.actor().path());
        } else if (msg instanceof ActorPath) {
            final ActorPath actorPath = (ActorPath) msg;
            if (liveActors != null && liveActors.containsKey(actorPath)) {
                //Actor is still in the list, so its not restarted
                sender.tell(false, getSelf());
            } else {
                //Actor is not in the list, so its restarted
                sender.tell(true, getSelf());
            }
        } else if (msg instanceof StopChild) {
            StopChild stop = (StopChild) msg;
            final ActorRef child = stop.child();
            getContext().unwatch(child);
            liveActors.remove(child.path());
            getContext().stop(child);
        } else {
            unhandled(msg);
        }
    }

    @Override // - 1st the actor will try to get the supervisor strategy
    public SupervisorStrategy supervisorStrategy() {
        ActorRef sender = getSender();
        return defaultStrategy;
    }

    private class RestcommFaultToleranceStrategy extends OneForOneStrategy {

        public RestcommFaultToleranceStrategy(int maxNrOfRetries, Duration withinTimeRange, Function<Throwable, Directive> function) {
            super(maxNrOfRetries, withinTimeRange, function);
        }

        @Override // - 3rd the Supervisor Strategy will execute processFailure() method. Useful for cleanup or logging
        public void processFailure(ActorContext context, boolean restart, ActorRef child, Throwable cause, ChildRestartStats stats, Iterable<ChildRestartStats> children) {
            String msg = String.format("RestcommSupervisor, actor exception handling. Restart %s, actor path %s, cause %s,", restart, child.path().toString(), cause);
            logger.error(msg);
            liveActors.remove(child.path());
            super.processFailure(context, restart, child, cause, stats, children);
        }
    }

    private class RestcommFaultToleranceDecider implements Function<Throwable, SupervisorStrategy.Directive> {

        @Override
        // - 2nd the Supervisor strategy will execute the Decider apply() to check what to do with the exception
        public SupervisorStrategy.Directive apply(Throwable t) throws Exception {
            if (t instanceof ServletParseException) {
                logger.info("ServletParseException, will resume actor");
                return resume();
            } else if (t instanceof NullPointerException) {
                logger.info("NullPointerException, will restart actor");
                return restart();
            } else if (t instanceof IllegalArgumentException) {
                logger.info("IllegalArgumentException, will stop actor");
                return stop();
            } else if (t instanceof TransitionFailedException || t instanceof TransitionNotFoundException) {
                logger.info("FSM Transition exception, will resume");
                return resume();
            } else {
                logger.info("Will escalate");
                return escalate();
            }
        }
    }

}
