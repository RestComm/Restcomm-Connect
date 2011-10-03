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
package org.mobicents.servlet.sip.restcomm.interpreter.tagstrategies;

import org.apache.log4j.Logger;

import org.mobicents.servlet.sip.restcomm.Environment;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.TwiMLInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.TwiMLInterpreterContext;
import org.mobicents.servlet.sip.restcomm.sms.SmsAggregator;
import org.mobicents.servlet.sip.restcomm.xml.Attribute;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.twiml.From;
import org.mobicents.servlet.sip.restcomm.xml.twiml.To;

public final class SmsTagStrategy extends TwiMLTagStrategy {
  private static final Logger LOGGER = Logger.getLogger(SmsTagStrategy.class);
	  
  public SmsTagStrategy() {
    super();
  }

  @Override public void execute(final TwiMLInterpreter interpreter,
      final TwiMLInterpreterContext context, final Tag tag) throws TagStrategyException {
	// Try to answer the call if it hasn't been done so already.
	final Call call = context.getCall();
	answer(call);
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
	  final Environment environment = Environment.getInstance();
	  try {
		final SmsAggregator aggregator = environment.getSmsAggregator();
		aggregator.send(from, to, body);
	  } catch(final Exception exception) {
		LOGGER.error(exception);
		interpreter.failed();
	    throw new TagStrategyException(exception);
	  }
	}
  }
}
