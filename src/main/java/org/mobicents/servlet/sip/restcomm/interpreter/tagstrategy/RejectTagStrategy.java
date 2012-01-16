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

import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.Interpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.InterpreterContext;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Reason;

public final class RejectTagStrategy extends TwiMLTagStrategy {
  public RejectTagStrategy() {
    super();
  }
  
  @Override public void execute(final Interpreter interpreter,
      final InterpreterContext context, final Tag tag) throws TagStrategyException {
    final Call call = context.getCall();
    final String reason = tag.getAttribute(Reason.NAME).getValue();
    if(call.getStatus().equals("ringing")) {
      if(reason.equals(Reason.REJECTED)) {
        answer(call);
        /* Fix Me: This should answer the call play a not-in-service message and hangup. */
        call.hangup();
      } else if(reason.equals(Reason.BUSY)) {
        call.reject();
      }
      interpreter.finish();
    }
  }
}
