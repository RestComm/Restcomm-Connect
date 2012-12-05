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
package org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.voice;

import org.mobicents.servlet.sip.restcomm.annotations.concurrency.NotThreadSafe;
import org.mobicents.servlet.sip.restcomm.entities.Notification;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.VoiceRcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.interpreter.tagstrategy.RcmlTagStrategy;
import org.mobicents.servlet.sip.restcomm.media.api.Call;
import org.mobicents.servlet.sip.restcomm.media.api.CallException;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@NotThreadSafe public abstract class VoiceRcmlTagStrategy extends RcmlTagStrategy {
  public VoiceRcmlTagStrategy() {
    super();
  }
  
  @Override public synchronized void initialize(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    final VoiceRcmlInterpreterContext voiceContext = (VoiceRcmlInterpreterContext)context;
    final Call call = voiceContext.getCall();
    Call.Status status = call.getStatus();
    try {
      if(Call.Status.RINGING == status) {
        call.answer();
      } else if(Call.Status.QUEUED == status) {
        call.addObserver(this);
        call.dial();
        //Issue 95: http://code.google.com/p/restcomm/issues/detail?id=95
        try {
          // Wait for state change to ringing before starting the timeout period.
          wait();
          // If the call is ringing on the remote side then wait for timeout period.
          status = call.getStatus();
          if(Call.Status.RINGING.equals(status)) {
            interpreter.sendStatusCallback();
            wait(voiceContext.getTimeout() * 1000);
          }
        }
        catch(final InterruptedException ignored) { }
        call.removeObserver(this);
        status = call.getStatus();
        if(Call.Status.IN_PROGRESS != status && Call.Status.FAILED != status) {
          call.cancel();
        }
      }
    } catch(final CallException exception) {
	  interpreter.failed();
	  final Notification notification = interpreter.notify(voiceContext, Notification.ERROR, 12400); 
	  interpreter.save(notification);
	  throw new TagStrategyException(exception);
	}
  }
}
