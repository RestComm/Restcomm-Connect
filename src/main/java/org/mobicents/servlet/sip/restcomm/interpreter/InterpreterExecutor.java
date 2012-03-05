/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.sip.restcomm.interpreter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.mobicents.servlet.sip.restcomm.Application;
import org.mobicents.servlet.sip.restcomm.IncomingPhoneNumber;
import org.mobicents.servlet.sip.restcomm.LifeCycle;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
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
