package org.restcomm.connect.commons.faulttolerance;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.commons.configuration.ConfigurationException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.restcomm.connect.commons.faulttolerance.tool.ActorCreatingThread;

import akka.actor.ActorSystem;

/**
 * @author mariafarooq
 *
 */
public class SupervisorActorCreationStressTest {

	protected static ActorSystem system;

    //nThreads the number of threads in the pool
    private static final int nThreads 		= 1500;
    //we can increase decrease value of this to put less request to create actors from RestcommSupervisor
    // each of this thread will ask RestcommSupervisor to create a SimpleActor
    private static final int THREAD_COUNT = 3000;

	public static AtomicInteger actorSuccessCount;
	public static AtomicInteger actorFailureCount;

    @BeforeClass
    public static void beforeClass() throws Exception {
		Config config = ConfigFactory.load("akka_fault_tolerance_application.conf");
		system = ActorSystem.create("test", config );

        actorSuccessCount = new AtomicInteger();
        actorFailureCount = new AtomicInteger();
    }

	@Test
	public void testCreateSampleAkkaActor() throws ConfigurationException, MalformedURLException, UnknownHostException, InterruptedException {
    	ExecutorService executor = Executors.newFixedThreadPool(nThreads);
		for (int i = 0; i < THREAD_COUNT; i++) {
			Runnable worker = new ActorCreatingThread(system);
			executor.execute(worker);
		}
		executor.shutdown();
		// Wait until all threads are finish
		while (!executor.isTerminated()) {

		}
		Thread.sleep(THREAD_COUNT*2);
		System.out.println("\nFinished all threads: \n actorSuccessCount: "+actorSuccessCount+"\nactorFailureCount: "+actorFailureCount);
		assertTrue(actorFailureCount.get()==0);
		assertTrue(actorSuccessCount.get()==THREAD_COUNT);
	}

    @AfterClass
    public static void afterClass() throws Exception {
    	system.shutdown();
    }
}
