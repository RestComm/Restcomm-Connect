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

import java.net.URI;

import org.apache.log4j.Logger;
import org.mobicents.servlet.sip.restcomm.annotations.concurrency.ThreadSafe;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe public final class ConferenceRcmlInterpreter extends RcmlInterpreter {
  private static final Logger logger = Logger.getLogger(ConferenceRcmlInterpreter.class);
  
  private final ConferenceRcmlInterpreterContext context;
  private final InterpreterFactory factory;

  public ConferenceRcmlInterpreter(final ConferenceRcmlInterpreterContext context,
      final InterpreterFactory factory) {
    super(context, new ConferenceTagStrategyFactory());
    this.context = context;
    this.factory = factory;
  }

  @Override protected void cleanup() {
    factory.remove(context.getConference().getSid());
  }

  @Override protected void initialize() {
    URI url = context.getWaitUrl();
    String method = context.getWaitMethod();
    if(url != null && method != null && !method.isEmpty()) {
      try {
	    load(url, method, context.getRcmlRequestParameters());
	    setState(READY);
	  } catch(final InterpreterException exception) {
	    logger.warn(exception);
	  }
    }
  }
}
