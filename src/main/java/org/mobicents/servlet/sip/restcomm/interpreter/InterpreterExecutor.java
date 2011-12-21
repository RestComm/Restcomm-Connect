package org.mobicents.servlet.sip.restcomm.interpreter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.mobicents.servlet.sip.restcomm.LifeCycle;

public final class InterpreterExecutor implements LifeCycle {
  private final ExecutorService executor;

  public InterpreterExecutor() {
    super();
    executor = Executors.newCachedThreadPool();
  }

  @Override public void start() throws RuntimeException {
    // Nothing to do.
  }

  @Override public void shutdown() {
    if(!executor.isShutdown()) {
      executor.shutdown();
      try {
		executor.awaitTermination(60, TimeUnit.SECONDS);
	  } catch(final InterruptedException ignored) { }
    }
  }
  
  public void submit(final InterpreterContext context) throws InterpreterException {
    final Interpreter interpreter = new Interpreter(context);
    interpreter.initialize();
    executor.submit(interpreter);
  }
}
