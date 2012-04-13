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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;

import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.xml.Attribute;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Reason;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class RejectTagStrategy extends RcmlTagStrategy {
  private final List<URI> rejectAudioFile;
  
  private String reason;
  
  public RejectTagStrategy() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    final Configuration configuration = services.get(Configuration.class);
    final URI uri = URI.create("file://" + configuration.getString("reject-audio-file"));
    rejectAudioFile = new ArrayList<URI>();
    rejectAudioFile.add(uri);
  }
  
  @Override public void execute(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    final Call call = context.getCall();
    if(Call.Status.RINGING == call.getStatus()) {
      if("rejected".equalsIgnoreCase(reason)) {
    	try { call.play(rejectAudioFile, 1); }
    	catch(final Exception ignored) {  }
        call.hangup();
      } else if("busy".equalsIgnoreCase(reason)) {
        call.reject();
      }
      interpreter.finish();
    }
  }
  
  private String getReason(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) {
    final Attribute attribute = tag.getAttribute(Reason.NAME);
    if(attribute != null) {
      final String value = attribute.getValue();
      if("busy".equalsIgnoreCase(value) || "rejected".equalsIgnoreCase(value)) {
        return value;
      }
    }
    return "rejected";
  }
  
  @Override public void initialize(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    reason = getReason(interpreter, context, tag);
  }
}
