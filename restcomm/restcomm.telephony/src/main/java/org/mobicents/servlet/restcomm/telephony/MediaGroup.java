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
package org.mobicents.servlet.restcomm.telephony;

import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mobicents.servlet.restcomm.fsm.Action;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.mgcp.CloseLink;
import org.mobicents.servlet.restcomm.mgcp.CreateIvrEndpoint;
import org.mobicents.servlet.restcomm.mgcp.CreateLink;
import org.mobicents.servlet.restcomm.mgcp.DestroyEndpoint;
import org.mobicents.servlet.restcomm.mgcp.DestroyLink;
import org.mobicents.servlet.restcomm.mgcp.InitializeLink;
import org.mobicents.servlet.restcomm.mgcp.IvrEndpointResponse;
import org.mobicents.servlet.restcomm.mgcp.LinkStateChanged;
import org.mobicents.servlet.restcomm.mgcp.MediaGatewayResponse;
import org.mobicents.servlet.restcomm.mgcp.MediaSession;
import org.mobicents.servlet.restcomm.mgcp.OpenLink;
import org.mobicents.servlet.restcomm.mgcp.PlayCollect;
import org.mobicents.servlet.restcomm.mgcp.PlayRecord;
import org.mobicents.servlet.restcomm.mgcp.StopEndpoint;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class MediaGroup extends UntypedActor {
  // Finite state machine stuff.
  private final State uninitialized;
  private final State active;
  private final State inactive;
  // Special intermediate states.
  private final State acquiringIvr;
  private final State acquiringLink;
  private final State initializingLink;
  private final State openingLink;
  private final State deactivating;
  // FSM.
  private final FiniteStateMachine fsm;
  // MGCP runtime stuff.
  private final ActorRef gateway;
  private final ActorRef endpoint;
  private final MediaSession session;
  private ActorRef link;
  private ActorRef ivr;
  private boolean ivrInUse;
  // Runtime stuff.
  private final List<ActorRef> observers;

  public MediaGroup(final ActorRef gateway, final MediaSession session,
      final ActorRef endpoint) {
    super();
    final ActorRef source = self();
    // Initialize the states for the FSM.
    uninitialized = new State("uninitialized", null, null);
    active = new State("active", new Active(source), null);
    inactive = new State("inactive", new Inactive(source), null);
    acquiringIvr = new State("acquiring ivr", new AcquiringIvr(source), null);
    acquiringLink = new State("acquiring link", new AcquiringLink(source), null);
    initializingLink = new State("initializing link", new InitializingLink(source), null);
    openingLink = new State("opening link", new OpeningLink(source), null);
    deactivating = new State("deactivating", new Deactivating(source), null);
    // Initialize the transitions for the FSM.
    final Set<Transition> transitions = new HashSet<Transition>();
    transitions.add(new Transition(uninitialized, acquiringIvr));
    transitions.add(new Transition(acquiringIvr, inactive));
    transitions.add(new Transition(acquiringIvr, acquiringLink));
    transitions.add(new Transition(acquiringLink, inactive));
    transitions.add(new Transition(acquiringLink, initializingLink));
    transitions.add(new Transition(initializingLink, inactive));
    transitions.add(new Transition(initializingLink, openingLink));
    transitions.add(new Transition(openingLink, inactive));
    transitions.add(new Transition(openingLink, deactivating));
    transitions.add(new Transition(openingLink, active));
    transitions.add(new Transition(active, deactivating));
    transitions.add(new Transition(deactivating, inactive));
    // Initialize the FSM.
    this.fsm = new FiniteStateMachine(uninitialized, transitions);
    // Initialize the MGCP state.
    this.gateway = gateway;
    this.session = session;
    this.endpoint = endpoint;
    this.ivrInUse = false;
    // Initialize the rest of the media group state.
    this.observers = new ArrayList<ActorRef>();
  }
  
  private void collect(final Object message, final ActorRef sender) {
    final ActorRef self = self();
    final Collect request = (Collect)message;
    final PlayCollect.Builder builder = PlayCollect.builder();
    for(final URI prompt : request.prompts()) {
      builder.addPrompt(prompt);
    }
    builder.setClearDigitBuffer(true);
    builder.setDigitPattern(request.pattern());
    builder.setFirstDigitTimer(request.timeout());
    builder.setInterDigitTimer(request.timeout());
    builder.setEndInputKey(request.endInputKey());
    builder.setMaxNumberOfDigits(request.numberOfDigits());
    stop();
    ivr.tell(builder.build(), self);
    ivrInUse = true;
  }
  
  private void play(final Object message, final ActorRef sender) {
    final ActorRef self = self();
    final Play request = (Play)message;
    final List<URI> uris = request.uris();
    final int iterations = request.iterations();
    final org.mobicents.servlet.restcomm.mgcp.Play play =
        new org.mobicents.servlet.restcomm.mgcp.Play(uris, iterations);
    stop();
    ivr.tell(play, self);
    ivrInUse = true;
  }
  
  @SuppressWarnings("unchecked")
  private void notification(final Object message) {
    final IvrEndpointResponse<String> response = (IvrEndpointResponse<String>)message;
    final ActorRef self = self();
    MediaGroupResponse<String> event = null;
    if(response.succeeded()) {
      event = new MediaGroupResponse<String>(response.get());
    } else {
      event = new MediaGroupResponse<String>(response.cause(), response.error());
    }
    for(final ActorRef observer : observers) {
      observer.tell(event, self);
    }
    ivrInUse = false;
  }
  
  private void observe(final Object message) {
	final ActorRef self = self();
	final Observe request = (Observe)message;
    final ActorRef observer = request.observer();
    if(observer != null) {
      observers.add(observer);
      observer.tell(new Observing(self), self);
    }
  }

  // FSM logic.
  @Override public void onReceive(final Object message) throws Exception {
    final Class<?> klass = message.getClass();
    final ActorRef sender = sender();
    final State state = fsm.state();
    if(Observe.class.equals(klass)) {
      observe(message);
    } else if(StopObserving.class.equals(klass)) {
      stopObserving(message);
    } else if(StartMediaGroup.class.equals(klass)) {
      fsm.transition(message, acquiringIvr);
    } else if(MediaGatewayResponse.class.equals(klass)) {
      if(acquiringIvr.equals(state)) {
        fsm.transition(message, acquiringLink);
      } else if(acquiringLink.equals(state)) {
        fsm.transition(message, initializingLink);
      }
    } else if(LinkStateChanged.class.equals(klass)) {
      final LinkStateChanged response = (LinkStateChanged)message;
      if(LinkStateChanged.State.CLOSED == response.state()) {
        if(initializingLink.equals(state)) {
          fsm.transition(message, openingLink);
        } else if(openingLink.equals(state) || deactivating.equals(state)) {
          fsm.transition(message, inactive);
        }
      } else if(LinkStateChanged.State.OPEN == response.state()) {
        if(openingLink.equals(state)) {
          fsm.transition(message, active);
        }
      }
    } else if(StopMediaGroup.class.equals(klass)) {
      if(acquiringLink.equals(state) || initializingLink.equals(state)) {
        fsm.transition(message, inactive);
      } else {
        fsm.transition(message, deactivating);
      }
    } else if(active.equals(state)) {
      if(Play.class.equals(klass)) {
        play(message, sender);
      } else if(Collect.class.equals(klass)) {
        collect(message, sender);
      } else if(Record.class.equals(klass)) {
        record(message, sender);
      } else if(Stop.class.equals(klass)) {
        stop();
      } else if(IvrEndpointResponse.class.equals(klass)) {
        notification(message);
      }
    }
  }
  
  private void record(final Object message, final ActorRef sender) {
	final ActorRef self = self();
    final Record request = (Record)message;
    final PlayRecord.Builder builder = PlayRecord.builder();
    for(final URI prompt : request.prompts()) {
      builder.addPrompt(prompt);
    }
    builder.setClearDigitBuffer(true);
    builder.setPreSpeechTimer(request.timeout());
    builder.setPostSpeechTimer(request.timeout());
    builder.setRecordingLength(request.length());
    builder.setEndInputKey(request.endInputKey());
    builder.setRecordingId(request.destination());
    ivr.tell(builder.build(), self);
    ivrInUse = true;
  }
  
  private void stop() {
    if(ivrInUse) {
      final ActorRef self = self();
      ivr.tell(new StopEndpoint(), self);
      ivrInUse = false;
    }
  }
  
  private void stopObserving(final Object message) {
	final StopObserving request = (StopObserving)message;
    final ActorRef observer = request.observer();
    if(observer != null) {
      observers.remove(observer);
    }
  }
  
  private abstract class AbstractAction implements Action {
    protected final ActorRef source;
    
    public AbstractAction(final ActorRef source) {
      super();
      this.source = source;
    }
  }
  
  private final class AcquiringIvr extends AbstractAction {
    public AcquiringIvr(final ActorRef source) {
      super(source);
    }

	@Override public void execute(final Object message) throws Exception {
      gateway.tell(new CreateIvrEndpoint(session), source);
	}
  }
  
  private final class AcquiringLink extends AbstractAction {
    public AcquiringLink(final ActorRef source) {
      super(source);
    }

	@SuppressWarnings("unchecked")
	@Override public void execute(final Object message) throws Exception {
	  final MediaGatewayResponse<ActorRef> response = (MediaGatewayResponse<ActorRef>)message;
	  ivr = response.get();
	  gateway.tell(new CreateLink(session), source);
	}
  }
  
  private final class InitializingLink extends AbstractAction {
    public InitializingLink(final ActorRef source) {
      super(source);
    }

	@SuppressWarnings("unchecked")
	@Override public void execute(final Object message) throws Exception {
	  final MediaGatewayResponse<ActorRef> response = (MediaGatewayResponse<ActorRef>)message;
	  link = response.get();
	  link.tell(new Observe(source), source);
	  link.tell(new InitializeLink(endpoint, ivr), source);
	}
  }

  private final class OpeningLink extends AbstractAction {
    public OpeningLink(final ActorRef source) {
      super(source);
    }

	@Override public void execute(final Object message) throws Exception {
	  gateway.tell(new OpenLink(ConnectionMode.SendRecv), source);
	}
  }
  
  private final class Active extends AbstractAction {
    public Active(final ActorRef source) {
      super(source);
    }

	@Override public void execute(final Object message) throws Exception {
	  // Notify the observers.
	  final MediaGroupStateChanged event = new MediaGroupStateChanged(MediaGroupStateChanged.State.ACTIVE);
	  for(final ActorRef observer : observers) {
	    observer.tell(event, source);
	  }
	}
  }
  
  private final class Inactive extends AbstractAction {
    public Inactive(final ActorRef source) {
      super(source);
    }

	@Override public void execute(final Object message) throws Exception {
	  if(link != null) {
	    gateway.tell(new DestroyLink(link), source);
	    link = null;
	  }
	  if(ivr != null) {
	    gateway.tell(new DestroyEndpoint(ivr), source);
	    ivr = null;
	  }
	  // Notify the observers.
	  final MediaGroupStateChanged event = new MediaGroupStateChanged(MediaGroupStateChanged.State.INACTIVE);
	  for(final ActorRef observer : observers) {
	    observer.tell(event, source);
	  }
	}
  }
  
  private final class Deactivating extends AbstractAction {
    public Deactivating(final ActorRef source) {
      super(source);
    }

	@Override public void execute(final Object message) throws Exception {
	  link.tell(new CloseLink(), source);
	}
  }
}
