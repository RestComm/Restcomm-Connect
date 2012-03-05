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
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.sms.SmsAggregator;
import org.mobicents.servlet.sip.restcomm.xml.Attribute;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.From;
import org.mobicents.servlet.sip.restcomm.xml.rcml.To;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class SmsTagStrategy extends RcmlTagStrategy {
  private static final SmsAggregator smsAggregator = ServiceLocator.getInstance().get(SmsAggregator.class);
	  
  public SmsTagStrategy() {
    super();
  }

  @Override public void execute(final RcmlInterpreter interpreter,
      final RcmlInterpreterContext context, final Tag tag) throws TagStrategyException {
	// Try to answer the call if it hasn't been done so already.
	final Call call = context.getCall();
	try {
	  answer(call);
	} catch(final InterruptedException ignored) { return; }
	// Send the text message.
	final String body = tag.getText();
	if(body != null) {
	  Attribute attribute = tag.getAttribute(From.NAME);
	  String from = null;
	  if(attribute != null) {
	    from = attribute.getValue();
	  }
	  attribute = tag.getAttribute(To.NAME);
	  String to = null;
	  if(attribute != null) {
	    to = attribute.getValue();
	  }
	  // Send the text message.
	  try {
		smsAggregator.send(from, to, body);
	  } catch(final Exception exception) {
		interpreter.failed();
	    throw new TagStrategyException(exception);
	  }
	}
  }
}
