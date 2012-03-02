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
import org.mobicents.servlet.sip.restcomm.callmanager.CallException;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.Reason;

public final class RejectTagStrategy extends RcmlTagStrategy {
  private final List<URI> rejectAudioFile;
  
  public RejectTagStrategy() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    final Configuration configuration = services.get(Configuration.class);
    final URI uri = URI.create("file://" + configuration.getString("reject-audio-file"));
    rejectAudioFile = new ArrayList<URI>();
    rejectAudioFile.add(uri);
  }
  
  @Override public void execute(final RcmlInterpreter interpreter,
      final RcmlInterpreterContext context, final Tag tag) throws TagStrategyException {
    final Call call = context.getCall();
    if(Call.Status.RINGING == call.getStatus()) {
      final String reason = tag.getAttribute(Reason.NAME).getValue();
      if(reason.equals(Reason.REJECTED)) {
        answer(call);
        try {
          call.play(rejectAudioFile, 1);
        } catch(final CallException exception) {
          interpreter.failed();
          final StringBuilder buffer = new StringBuilder();
          buffer.append("There was an error while playing the rejection announcement. ");
          buffer.append("The announcement is located @ ").append(rejectAudioFile.toString());
          throw new TagStrategyException(buffer.toString(), exception);
        } catch(final InterruptedException ignored) { return; }
        call.hangup();
      } else if(reason.equals(Reason.BUSY)) {
        call.reject();
      }
      interpreter.finish();
    }
  }
}
