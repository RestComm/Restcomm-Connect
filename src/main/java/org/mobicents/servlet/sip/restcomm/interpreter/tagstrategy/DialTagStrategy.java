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

import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.CallException;
import org.mobicents.servlet.sip.restcomm.callmanager.CallManager;
import org.mobicents.servlet.sip.restcomm.callmanager.CallManagerException;
import org.mobicents.servlet.sip.restcomm.callmanager.ConferenceCenter;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.xml.Attribute;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.CallerId;

public final class DialTagStrategy extends RcmlTagStrategy {
  private final CallManager callManager;
  private final ConferenceCenter conferenceCenter;
  
  public DialTagStrategy() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    callManager = services.get(CallManager.class);
    conferenceCenter = services.get(ConferenceCenter.class);
  }
  
  @Override public void execute(final RcmlInterpreter interpreter,
      final RcmlInterpreterContext context, final Tag tag) throws TagStrategyException {
    final Call call = context.getCall();
    Attribute attribute = tag.getAttribute(CallerId.NAME);
    final String from = attribute != null ? attribute.getValue() : call.getOriginator();
    final String to = tag.getText();
    try {
      answer(call);
      // Dial out.
      final Call outboundCall = callManager.createCall(from, to);
      outboundCall.dial();
      
      outboundCall.hangup();
    } catch(final CallException exception) {
      final StringBuilder buffer = new StringBuilder();
      buffer.append("There was an error while bridging a call from ");
      buffer.append(call.getOriginator()).append(" to ").append(to);
      throw new TagStrategyException(buffer.toString(), exception);
    } catch(final CallManagerException exception) {
      final StringBuilder buffer = new StringBuilder();
      buffer.append("There was an error creating a connection from ");
      buffer.append(from).append(" to ").append(to);
      throw new TagStrategyException(buffer.toString(), exception);
    } catch(final InterruptedException ignored) { return; }
  }
}
