package org.restcomm.connect.commons.faulttolerance;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.ChildRestartStats;
import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.actor.SupervisorStrategyConfigurator;
import akka.japi.Function;
import org.apache.log4j.Logger;
import scala.collection.Iterable;
import scala.concurrent.duration.Duration;

import static akka.actor.SupervisorStrategy.resume;

/**
 * Created by gvagenas on 01/04/2017.
 */
public class RestcommSupervisorStrategy implements SupervisorStrategyConfigurator {

    private static Logger logger = Logger.getLogger(RestcommSupervisorStrategy.class);

    static final SupervisorStrategy.Directive strategy = resume();

    static RestcommFaultToleranceStrategy defaultStrategy = new RestcommFaultToleranceStrategy(10, Duration.create("1 minute"),
            new RestcommFaultToleranceDecider());

    @Override
    public SupervisorStrategy create() {
        return defaultStrategy;
    }

    public static SupervisorStrategy getStrategy() {
        return defaultStrategy;
    }

    private static class RestcommFaultToleranceStrategy extends OneForOneStrategy {

        public RestcommFaultToleranceStrategy(int maxNrOfRetries, Duration withinTimeRange, Function<Throwable, Directive> function) {
            super(maxNrOfRetries, withinTimeRange, function);
        }

        @Override
        public boolean handleFailure(ActorContext context, ActorRef child, Throwable cause, ChildRestartStats stats, Iterable<ChildRestartStats> children) {
            String msg = String.format("RestcommSupervisorStrategy, actor exception handling. Actor path %s, exception cause %s, default exception handling strategy %s", child.path().toString(), cause, strategy.toString());
            logger.error(msg);
            return super.handleFailure(context, child, cause, stats, children);
        }

//        @Override // - 3rd the Supervisor Strategy will execute processFailure() method. Useful for cleanup or logging
//        public void processFailure(ActorContext context, boolean restart, ActorRef child, Throwable cause, ChildRestartStats stats, Iterable<ChildRestartStats> children) {
//            String msg = String.format("RestcommSupervisor, actor exception handling. Restart %s, actor path %s, cause %s,", restart, child.path().toString(), cause);
//            logger.error(msg);
//            super.processFailure(context, restart, child, cause, stats, children);
//        }
    }

    private static class RestcommFaultToleranceDecider implements Function<Throwable, SupervisorStrategy.Directive> {

        @Override
        // - 2nd the Supervisor strategy will execute the Decider apply() to check what to do with the exception
        public SupervisorStrategy.Directive apply(Throwable t) throws Exception {
//            String msg = String.format("Handling exception %s with default strategy to %s", t.getClass().getName(), strategy.toString());
//            logger.error(msg);
            return strategy;
//            return resume();
//            return restart();
//            return stop();
//            return escalate();
        }
    }
}
