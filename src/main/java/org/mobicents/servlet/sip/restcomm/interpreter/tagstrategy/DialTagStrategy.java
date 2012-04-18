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

import java.util.List;

import org.mobicents.servlet.sip.restcomm.ServiceLocator;
import org.mobicents.servlet.sip.restcomm.callmanager.Call;
import org.mobicents.servlet.sip.restcomm.callmanager.CallException;
import org.mobicents.servlet.sip.restcomm.callmanager.CallManager;
import org.mobicents.servlet.sip.restcomm.callmanager.CallManagerException;
import org.mobicents.servlet.sip.restcomm.callmanager.CallObserver;
import org.mobicents.servlet.sip.restcomm.callmanager.Conference;
import org.mobicents.servlet.sip.restcomm.callmanager.ConferenceCenter;
import org.mobicents.servlet.sip.restcomm.interpreter.TagStrategyException;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreter;
import org.mobicents.servlet.sip.restcomm.interpreter.RcmlInterpreterContext;
import org.mobicents.servlet.sip.restcomm.xml.Attribute;
import org.mobicents.servlet.sip.restcomm.xml.Tag;
import org.mobicents.servlet.sip.restcomm.xml.rcml.CallerId;
import org.mobicents.servlet.sip.restcomm.xml.rcml.RcmlTag;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class DialTagStrategy extends RcmlTagStrategy implements CallObserver {
  private final CallManager callManager;
  private final ConferenceCenter conferenceCenter;
  
  public DialTagStrategy() {
    super();
    final ServiceLocator services = ServiceLocator.getInstance();
    callManager = services.get(CallManager.class);
    conferenceCenter = services.get(ConferenceCenter.class);
  }
  
  @Override public void execute(final RcmlInterpreter interpreter, final RcmlInterpreterContext context,
      final RcmlTag tag) throws TagStrategyException {
    final Call call = context.getCall();
    Attribute attribute = tag.getAttribute(CallerId.NAME);
    final String from = attribute != null ? attribute.getValue() : call.getOriginator();
    final String to = tag.getText();
    try {
      if(tag.hasChildren() && (to != null && !to.isEmpty())) {
        throw new TagStrategyException("The <Dial> tag can not contain text and child elements at the same time.");
      } else {
        if(to != null) {
          bridge(call, from, to);
        } else if(tag.hasChildren()) {
          handleChildren(call, from, tag.getChildren());
        }
      }
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
  
  private synchronized void handleChildren(final Call call, final String callerId, final List<Tag> children)
      throws InterruptedException, CallManagerException, CallException {
    final Tag child = children.get(0);
    if(org.mobicents.servlet.sip.restcomm.xml.rcml.Conference.NAME.equals(child.getName())) {
      handleConference(call, child.getText());
    } else {
      handleNumbersAndClients(call, callerId, children);
    }
  }
  
  private synchronized void handleConference(final Call call, final String room) throws InterruptedException {
    final Conference conference = conferenceCenter.getConference(room);
    call.addObserver(this);
    conference.addCall(call);
    wait();
    call.removeObserver(this);
    conference.removeCall(call);
  }
  
  private synchronized void handleNumbersAndClients(final Call call, final String callerId, final List<Tag> children)
      throws CallManagerException, CallException, InterruptedException {
    for(final Tag child : children) {
      final String to = child.getText();
      final Call outboundCall = callManager.createCall(callerId, to);
      outboundCall.dial();
      if(outboundCall.getStatus() == Call.Status.IN_PROGRESS) {
    	outboundCall.addObserver(this);
    	call.addObserver(this);
	    final String name = new StringBuilder().append(callerId).append(":").append(to).toString();
	    final Conference bridge = conferenceCenter.getConference(name);
	    bridge.addCall(outboundCall);
	    bridge.addCall(call);
	    wait();
	    call.removeObserver(this);
	    bridge.removeCall(call);
	    outboundCall.removeObserver(this);
	    bridge.removeCall(outboundCall);
	    conferenceCenter.removeConference(name);
	    break;
      }
    }
  }
  
  private synchronized void bridge(final Call call, final String callerId, final String to)
      throws CallManagerException, CallException, InterruptedException {
    final Call outboundCall = callManager.createCall(callerId, normalizeText(to));
    outboundCall.dial();
    outboundCall.addObserver(this);
    call.addObserver(this);
    // Bridge the call.
    final String name = new StringBuilder().append(callerId).append(":").append(to).toString();
    final Conference bridge = conferenceCenter.getConference(name);
    bridge.addCall(outboundCall);
    bridge.addCall(call);
    wait();
    call.removeObserver(this);
    bridge.removeCall(call);
    outboundCall.removeObserver(this);
    bridge.removeCall(outboundCall);
    conferenceCenter.removeConference(name);
  }
  
  private String normalizeText(final String phoneNumber) {
    return phoneNumber.replace("-", "");
  }
  
  @Override public synchronized void onStatusChanged(final Call call) {
    notify();
  }
}
