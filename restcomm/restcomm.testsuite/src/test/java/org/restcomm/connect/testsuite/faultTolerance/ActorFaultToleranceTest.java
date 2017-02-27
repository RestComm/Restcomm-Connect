package org.restcomm.connect.testsuite.faultTolerance;

import akka.actor.ActorPath;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.OneForOneStrategy;
import akka.actor.Props;
import akka.actor.SupervisorStrategy;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Function;
import akka.testkit.JavaTestKit;
import akka.testkit.TestProbe;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.connect.commons.faulttolerance.RestcommSupervisor;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

import static akka.actor.SupervisorStrategy.escalate;
import static akka.actor.SupervisorStrategy.resume;
import static akka.actor.SupervisorStrategy.stop;
import static akka.pattern.Patterns.ask;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by gvagenas on 22/02/2017.
 */
public class ActorFaultToleranceTest {

	static ActorSystem system;

	@BeforeClass
	public static void setup () {
		system = ActorSystem.create();
	}

	@AfterClass
	public static void teardown () {
		system.shutdown();
	}

	@Test
	public void testIt () {
	/*
     * Wrap the whole test procedure within a testkit constructor
     * if you want to receive actor replies or use Within(), etc.
     */
		new JavaTestKit(system) {{
			final Props props = new Props(TestActor.class);
			final ActorRef subject = system.actorOf(props);

			// can also use JavaTestKit “from the outside”
			final JavaTestKit probe = new JavaTestKit(system);
			// “inject” the probe by passing it to the test subject
			// like a real resource would be passed in production
			subject.tell(probe.getRef(), getRef());
			// await the correct response
			expectMsgEquals(duration("1 second"), "done");

			// the run() method needs to finish within 3 seconds
			new Within(duration("3 seconds")) {
				protected void run () {

					subject.tell("hello", getRef());

					// This is a demo: would normally use expectMsgEquals().
					// Wait time is bounded by 3-second deadline above.
					new AwaitCond() {
						protected boolean cond () {
							return probe.msgAvailable();
						}
					};

					// response must have been enqueued to us before probe
					expectMsgEquals(Duration.Zero(), "world");
					// check that the probe we injected earlier got the msg
					probe.expectMsgEquals(Duration.Zero(), "hello");
					assertEquals(getRef(), probe.getLastSender());

					// Will wait for the rest of the 3 seconds
					expectNoMsg();
				}
			};
		}};
	}

	@Test
	public void testException () throws Exception {
		new JavaTestKit(system) {{
			LoggingAdapter logger = Logging.getLogger(system, this);

			Props superprops = new Props(RestcommSupervisor.class);
			ActorRef supervisor = system.actorOf(superprops, "supervisor");

			final ActorRef subject = (ActorRef) Await.result(ask(supervisor,
					new Props(TestActor.class), 5000), Duration.create(10, TimeUnit.SECONDS));


			ActorPath subjectPath = subject.path();
			int subjectHashCode = subject.hashCode();
			subject.tell("exception", getRef());
			expectMsgEquals(duration("1 second"), "I don't stop on exceptions");
			Thread.sleep(5000);

			boolean actorRestarted = (boolean) Await.result(ask(supervisor,
					subjectPath, 5000), Duration.create(10, TimeUnit.SECONDS));

			assertTrue(actorRestarted);
//			logger.info("Subject original path: "+subjectPath+", new subject path: "+subject.path().toString());
		}};
	}

	public static class TestActor extends UntypedActor {

		public SupervisorStrategy strategy;
		ActorRef target = null;
		private LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

		public TestActor() {
			logger.info("At constructor, will create strategy.");
			strategy = new OneForOneStrategy(10, Duration.create("1 minute"),
					new Function<Throwable, SupervisorStrategy.Directive>() {
						@Override
						public akka.actor.SupervisorStrategy.Directive apply(Throwable t) {
							if (t instanceof ArithmeticException) {
								logger.info("ArithmeticExceptio, will resume actor");
								return resume();
							} else if (t instanceof NullPointerException) {
								logger.info("NullPointerException");
								return resume();
							} else if (t instanceof IllegalArgumentException) {
								logger.info("IllegalArgumentException, will stop actor");
								return stop();
							} else {
								logger.info("Will escalate");
								return escalate();
							}
						}
					});
		}

		public void onReceive (Object msg) {
			final Class<?> klass = msg.getClass();
			logger.info(" ********** TestActor " + self().path() + " Processing Message: " + klass.getName());

			if (msg.equals("hello")) {
				getSender().tell("world", getSelf());
				if (target != null) target.forward(msg, getContext());

			} else if (msg.equals("exception")) {
				getSender().tell("I don't stop on exceptions", getSelf());
				String s = null;
				s.equalsIgnoreCase("blabla");
			} else if (msg instanceof ActorRef) {
				target = (ActorRef) msg;
				getSender().tell("done", getSelf());
			}
		}

		@Override
		public SupervisorStrategy supervisorStrategy() {
			logger.info("Will return strategy");
			return strategy;
		}

		@Override
		public void postStop () {
			logger.info("At postStop method");
			super.postStop();
		}

		@Override
		public void postRestart (Throwable reason) {
			logger.info("At postRestart method");
			super.postRestart(reason);
		}
	}
}
