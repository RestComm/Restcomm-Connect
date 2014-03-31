/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
 * by the @authors tag.
 *
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
package org.mobicents.servlet.restcomm.ussd.telephony;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.servlet.sip.Address;
import javax.servlet.sip.ServletParseException;
import javax.servlet.sip.SipFactory;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.sip.SipSession;
import javax.servlet.sip.SipURI;

import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.dao.CallDetailRecordsDao;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.fsm.Action;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;
import org.mobicents.servlet.restcomm.telephony.Answer;
import org.mobicents.servlet.restcomm.telephony.CallInfo;
import org.mobicents.servlet.restcomm.telephony.CallResponse;
import org.mobicents.servlet.restcomm.telephony.CallStateChanged;
import org.mobicents.servlet.restcomm.telephony.CreateCall;
import org.mobicents.servlet.restcomm.telephony.GetCallInfo;
import org.mobicents.servlet.restcomm.telephony.GetCallObservers;
import org.mobicents.servlet.restcomm.ussd.commons.UssdRestcommResponse;

import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.event.Logging;
import akka.event.LoggingAdapter;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public class UssdCall extends UntypedActor  {

    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    private final String ussdContentType = "application/vnd.3gpp.ussd+xml";

    private UssdCallType ussdCallType;

    private final FiniteStateMachine fsm;
    // States for the FSM.
    private final State uninitialized;
    private final State ringing;
    private final State inProgress;
    private final State ready;
    private final State processingUssdMessage;
    private final State completed;

    // SIP runtime stuff.
    private final SipFactory factory;
    private String apiVersion;
    private Sid accountId;
    private String name;
    private SipURI from;
    private SipURI to;
    private String username;
    private String password;
    private CreateCall.Type type;
    private long timeout;
    private SipServletRequest invite;
    private SipServletResponse lastResponse;

    // Runtime stuff.
    private final Sid id;
    private CallStateChanged.State external;
    private String direction;
    private DateTime created;
    private final List<ActorRef> observers;

    private CallDetailRecordsDao recordsDao;

    public UssdCall(final SipFactory factory, final ActorRef gateway) {
        super();
        final ActorRef source = self();
        // Initialize the states for the FSM.
        uninitialized = new State("uninitialized", null, null);
        ringing = new State("ringing", new Ringing(source), null);
        inProgress = new State("in progress", new InProgress(source), null);
        ready = new State("answering", new Ready(source), null);
        processingUssdMessage = new State("processing UssdMessage", new ProcessingUssdMessage(source), null);
        completed = new State("Completed", new Completed(source), null);

        // Initialize the transitions for the FSM.
        final Set<Transition> transitions = new HashSet<Transition>();
        transitions.add(new Transition(uninitialized, ringing));
        transitions.add(new Transition(ringing, inProgress));
        transitions.add(new Transition(inProgress, processingUssdMessage));
        transitions.add(new Transition(processingUssdMessage, ready));
        transitions.add(new Transition(processingUssdMessage, inProgress));
        transitions.add(new Transition(processingUssdMessage, completed));
        transitions.add(new Transition(processingUssdMessage, processingUssdMessage));

        // Initialize the FSM.
        this.fsm = new FiniteStateMachine(uninitialized, transitions);
        // Initialize the SIP runtime stuff.
        this.factory = factory;
        // Initialize the runtime stuff.
        this.id = Sid.generate(Sid.Type.CALL);
        this.created = DateTime.now();
        this.observers = Collections.synchronizedList(new ArrayList<ActorRef>());
    }

    private void observe(final Object message) {
        final ActorRef self = self();
        final Observe request = (Observe) message;
        final ActorRef observer = request.observer();
        if (observer != null) {
            observers.add(observer);
            observer.tell(new Observing(self), self);
        }
    }

    private void stopObserving(final Object message) {
        final StopObserving request = (StopObserving) message;
        final ActorRef observer = request.observer();
        if (observer != null) {
            observers.remove(observer);
        }
    }

    private CallResponse<CallInfo> info() {
        final String from = this.from.getUser();
        final String to = this.to.getUser();
        final CallInfo info = new CallInfo(id, external, type, direction, created, null, name, from, to, lastResponse);
        return new CallResponse<CallInfo>(info);
    }

    private SipURI getInitialIpAddressPort(SipServletMessage message) throws ServletParseException, UnknownHostException {
        // Issue #268 - https://bitbucket.org/telestax/telscale-restcomm/issue/268
        // First get the Initial Remote Address (real address that the request came from)
        // Then check the following:
        //    1. If contact header address is private network address
        //    2. If there are no "Record-Route" headers (there is no proxy in the call)
        //    3. If contact header address != real ip address
        // Finally, if all of the above are true, create a SIP URI using the realIP address and the SIP port
        // and store it to the sip session to be used as request uri later
        String realIP = message.getInitialRemoteAddr();
        int realPort = message.getInitialRemotePort();
        final ListIterator<String> recordRouteHeaders = message.getHeaders("Record-Route");
        final Address contactAddr = factory.createAddress(message.getHeader("Contact"));

        InetAddress contactInetAddress = InetAddress.getByName(((SipURI) contactAddr.getURI()).getHost());
        InetAddress inetAddress = InetAddress.getByName(realIP);

        int remotePort = message.getRemotePort();
        int contactPort = ((SipURI)contactAddr.getURI()).getPort();
        String remoteAddress = message.getRemoteAddr();

        //Issue #332: https://telestax.atlassian.net/browse/RESTCOMM-332
        final String initialIpBeforeLB = message.getHeader("X-Sip-Balancer-InitialRemoteAddr");
        String initialPortBeforeLB = message.getHeader("X-Sip-Balancer-InitialRemotePort");

        SipURI uri = null;

        if (initialIpBeforeLB != null) {
            if(initialPortBeforeLB == null)
                initialPortBeforeLB = "5060";
            logger.info("We are behind load balancer, storing Initial Remote Address " + initialIpBeforeLB+":"+initialPortBeforeLB
                    + " to the session for later use");
            realIP = initialIpBeforeLB+":"+initialPortBeforeLB;
            uri = factory.createSipURI(null, realIP);
        } else if (contactInetAddress.isSiteLocalAddress() && !recordRouteHeaders.hasNext()
                && !contactInetAddress.toString().equalsIgnoreCase(inetAddress.toString())) {
            logger.info("Contact header address " + contactAddr.toString()
                    + " is a private network ip address, storing Initial Remote Address " + realIP+":"+realPort
                    + " to the session for later use");
            realIP = realIP + ":" + realPort;
            uri = factory.createSipURI(null, realIP);
        }
//        //Assuming that the contactPort (from the Contact header) is the port that is assigned to the sip client,
//        //If RemotePort (either from Packet or from the Via header rport) is not the same as the contactPort, then we
//        //should use the remotePort and remoteAddres for the URI to use later for client behind NAT
//        else if(remotePort != contactPort) {
//            logger.info("RemotePort: "+remotePort+" is different than the Contact Address port: "+contactPort+" so storing for later use the "
//                    + remoteAddress+":"+remotePort);
//            realIP = remoteAddress+":"+remotePort;
//            uri = factory.createSipURI(null, realIP);
//        }
        return uri;
    }

    @Override
    public void onReceive(final Object message) throws Exception {
        final UntypedActorContext context = getContext();
        final Class<?> klass = message.getClass();
        final ActorRef self = self();
        final ActorRef sender = sender();
        final State state = fsm.state();
        logger.info("UssdCall's Current State: \"" + state.toString());
        logger.info("UssdCall Processing Message: \"" + klass.getName());

        if (Observe.class.equals(klass)) {
            observe(message);
        } else if (StopObserving.class.equals(klass)) {
            stopObserving(message);
        } else if (GetCallObservers.class.equals(klass)) {
            sender.tell(new CallResponse<List<ActorRef>>(observers), self);
        } else if (GetCallInfo.class.equals(klass)) {
            sender.tell(info(), self);
        } else if (message instanceof SipServletRequest) {
            final SipServletRequest request = (SipServletRequest) message;
            final String method = request.getMethod();
            if ("INVITE".equalsIgnoreCase(method)) {
                if (uninitialized.equals(state)) {
                    fsm.transition(message, ringing);
                }
            }
        } else if (message instanceof SipServletResponse) {
            final SipServletResponse response = (SipServletResponse) message;
            lastResponse = response;
        } else if (UssdRestcommResponse.class.equals(klass)) {
            fsm.transition(message, processingUssdMessage);
        } else if (Answer.class.equals(klass)) {
            fsm.transition(message, inProgress);
        }
    }

    private abstract class AbstractAction implements Action {
        protected final ActorRef source;

        public AbstractAction(final ActorRef source) {
            super();
            this.source = source;
        }
    }

    private final class Ringing extends AbstractAction {
        public Ringing(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            if (message instanceof SipServletRequest) {
                invite = (SipServletRequest) message;
                from = (SipURI) invite.getFrom().getURI();
                to = (SipURI) invite.getTo().getURI();
                timeout = -1;
                direction = "inbound";
                // Send a ringing response.
                final SipServletResponse ringing = invite.createResponse(SipServletResponse.SC_RINGING);
                ringing.send();

                SipURI initialInetUri = getInitialIpAddressPort(invite);

                if(initialInetUri != null)
                    invite.getSession().setAttribute("realInetUri", initialInetUri);

            } else if (message instanceof SipServletResponse) {
                final UntypedActorContext context = getContext();
                context.setReceiveTimeout(Duration.Undefined());
            }
            // Notify the observers.
            external = CallStateChanged.State.RINGING;
            final CallStateChanged event = new CallStateChanged(external);
            for (final ActorRef observer : observers) {
                logger.info("Telling observers that state changed to RINGING");
                observer.tell(event, source);
            }
        }
    }

    private final class InProgress extends AbstractAction {
        public InProgress(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            final State state = fsm.state();
            final SipServletResponse okay = invite.createResponse(SipServletResponse.SC_OK);
            okay.send();

            invite.getApplicationSession().setExpires(0);

            // Notify the observers.
            external = CallStateChanged.State.IN_PROGRESS;
            final CallStateChanged event = new CallStateChanged(external);
            for (final ActorRef observer : observers) {
                observer.tell(event, source);
            }
        }
    }

    private final class Ready extends AbstractAction {
        public Ready(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {

        }
    }

    private final class ProcessingUssdMessage extends AbstractAction {
        public ProcessingUssdMessage(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {
            UssdRestcommResponse ussdRequest = (UssdRestcommResponse) message;
            SipSession session = invite.getSession();
            SipServletRequest request = null;

            if(ussdRequest.getIsFinalMessage()) {
             request = session.createRequest("BYE");
//             fsm.transition(request, completed);
            } else {
                request = session.createRequest("INFO");
            }
            request.setContent(ussdRequest.createUssdPayload().toString(), ussdContentType);

            SipURI realInetUri = (SipURI) session.getAttribute("realInetUri");
            if (realInetUri != null) {
                logger.info("Using the real ip address of the sip client " + realInetUri.toString()
                        + " as a request uri of the BYE request");
                request.setRequestURI(realInetUri);
            }

            request.send();
        }
    }

    private final class Completed extends AbstractAction {
        public Completed(final ActorRef source) {
            super(source);
        }

        @Override
        public void execute(final Object message) throws Exception {

        }
    }
}