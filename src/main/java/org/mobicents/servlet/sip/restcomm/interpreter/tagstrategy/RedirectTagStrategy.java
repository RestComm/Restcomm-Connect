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
package org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy;

import java.net.URI;

import org.mobicents.servlet.sip.restcomm.Notification;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterException;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class RedirectTagStrategy extends RcmlTagStrategy {
  private String method;
  private URI uri;

  public RedirectTagStrategy() {
    super();
  }

  @Override public void execute(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    // Redirect the interpreter to the new RCML resource.
    if(uri != null) {
      try {
        interpreter.loadResource(uri, method);
        interpreter.redirect();
      } catch(final InterpreterException exception) {
        interpreter.failed();
        notify(interpreter, context, tag, Notification.ERROR, 12400);
        throw new TagStrategyException(exception);
      }
    }
  }
  
  @Override public void initialize(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    super.initialize(interpreter, context, tag);
    try {
      method = getMethod(interpreter, context, tag);
      if(!"GET".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
        notify(interpreter, context, tag, Notification.WARNING, 13710);
        method = "POST";
      }
      uri = getUri(interpreter, context, tag);
    } catch(final IllegalArgumentException ignored) {
      notify(interpreter, context, tag, Notification.ERROR, 11100);
    }
  }
}
