package org.mobicents.servlet.sip.restcomm.interpreter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.mobicents.servlet.sip.restcomm.Application;
import org.mobicents.servlet.sip.restcomm.IncomingPhoneNumber;
import org.mobicents.servlet.sip.restcomm.LifeCycle;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;

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
  
  public void submit(final Application application, final IncomingPhoneNumber incomingPhoneNumber, final Call call)
      throws InterpreterException {
	final RcmlInterpreterContext context = new RcmlInterpreterContext(application, incomingPhoneNumber, call);
    final RcmlInterpreter interpreter = new RcmlInterpreter(context);
    interpreter.initialize();
    executor.submit(interpreter);
  }
}
