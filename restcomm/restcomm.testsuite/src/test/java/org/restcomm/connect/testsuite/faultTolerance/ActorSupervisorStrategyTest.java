package org.restcomm.connect.testsuite.faultTolerance;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import akka.util.Timeout;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.connect.commons.faulttolerance.RestcommUntypedActor;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

import static akka.pattern.Patterns.ask;
import static org.junit.Assert.assertTrue;

/**
 * @author oleg.agafonov@telestax.com (Oleg Agafonov)
 */
public class ActorSupervisorStrategyTest {

    static ActorSystem system;

    @BeforeClass
    public static void setup() {
        Config config = ConfigFactory.load("akka_fault_tolerance_application.conf");
        system = ActorSystem.create("test", config);
    }

    @AfterClass
    public static void teardown() {
        system.shutdown();
    }

    @Test
    public void parentActorThrowsExceptionTest() throws Exception {
        new JavaTestKit(system) {{
            final ActorRef parent = system.actorOf(new Props(Parent.class));
            parent.tell("check exception", getRef());
            expectMsgEquals(duration("1 second"), false);
            parent.tell("throw exception", getRef());
            Thread.sleep(5000);
            parent.tell("check exception", getRef());
            expectMsgEquals(duration("1 second"), true);
        }};
    }

    @Test
    public void childActorThrowsExceptionTest() throws Exception {
        new JavaTestKit(system) {{
            final ActorRef parent = system.actorOf(new Props(Parent.class));
            final Future<Object> future = ask(parent, "create child", new Timeout(Duration.create(5, TimeUnit.SECONDS)));
            final ActorRef child = (ActorRef) Await.result(future, Duration.create(10, TimeUnit.SECONDS));

            child.tell("check exception", getRef());
            expectMsgEquals(duration("1 second"), false);
            child.tell("throw exception", getRef());
            Thread.sleep(5000);
            child.tell("check exception", getRef());
            expectMsgEquals(duration("1 second"), true);

            system.stop(parent);
            Thread.sleep(500);
            assertTrue(parent.isTerminated());
            assertTrue(child.isTerminated());
        }};

    }

    public static class Parent extends RestcommUntypedActor {

        private boolean receivedThrowException;

        @SuppressWarnings("Duplicates")
        @Override
        public void onReceive(Object message) throws Exception {
            if ("create child".equals(message)) {
                ActorRef child = getContext().actorOf(new Props(Child.class));
                sender().tell(child, self());
            } else if ("throw exception".equals(message)) {
                this.receivedThrowException = true;
                throw new RuntimeException();
            } else if ("check exception".equals(message)) {
                sender().tell(receivedThrowException, self());
            }
        }
    }

    public static class Child extends RestcommUntypedActor {

        private boolean receivedThrowException;

        @SuppressWarnings("Duplicates")
        @Override
        public void onReceive(Object message) throws Exception {
            if ("throw exception".equals(message)) {
                this.receivedThrowException = true;
                throw new RuntimeException();
            } else if ("check exception".equals(message)) {
                sender().tell(receivedThrowException, self());
            }
        }
    }
}
