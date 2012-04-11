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

import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterException;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Method;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class RedirectTagStrategy extends RcmlTagStrategy {
  public RedirectTagStrategy() {
    super();
  }

  @Override public void execute(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    // Redirect the interpreter to the new RCML resource.
    final String text = tag.getText();
    if(text != null) {
      final URI base = interpreter.getCurrentResourceUri();
      final URI uri = resolveIfNotAbsolute(base, text);
      final String method = tag.getAttribute(Method.NAME).getValue();
      try {
        interpreter.loadResource(uri, method);
        interpreter.redirect();
      } catch(final InterpreterException exception) {
        interpreter.failed();
        final StringBuilder buffer = new StringBuilder();
        buffer.append("There was an error while redirecting the interpreter to the RCML located @ ");
        buffer.append(uri.toString());
        throw new TagStrategyException(buffer.toString(), exception);
      }
    }
  }
}
