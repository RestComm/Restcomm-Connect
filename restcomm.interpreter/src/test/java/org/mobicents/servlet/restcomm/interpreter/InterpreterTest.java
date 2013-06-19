package org.mobicents.servlet.restcomm.interpreter;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public final class InterpreterTest {
  private static ActorSystem system;
  
  public InterpreterTest() {
    super();
  }
  
  @Before public void before() {
    system = ActorSystem.create();
    // Create the interpreter.
    final InterpreterBuilder builder = new InterpreterBuilder(system);
    // Create the configuration.
    final Configuration configuration = new PropertiesConfiguration();
    builder.setConfiguration(configuration);
    // Create the mock downloader.
    builder.setDownloader(system.actorOf(new Props(MockDownloader.class)));
  }
  
  @After public void after() {
    system.shutdown();
  }

  @Test public void testInitialization() {
    
  }
  
  private final class MockDownloader extends UntypedActor {
    public MockDownloader() {
      super();
    }

	@Override public void onReceive(final Object message) throws Exception {
	  
	}
  }
}
