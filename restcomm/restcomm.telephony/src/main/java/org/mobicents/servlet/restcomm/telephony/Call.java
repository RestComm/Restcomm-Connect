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

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.ReceiveTimeout;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorContext;
import akka.actor.UntypedActorFactory;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.sdp.Connection;
import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpFactory;
import javax.sdp.SessionDescription;
import javax.servlet.sip.*;

import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.fsm.Action;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;
import org.mobicents.servlet.restcomm.mgcp.CloseConnection;
import org.mobicents.servlet.restcomm.mgcp.CloseLink;
import org.mobicents.servlet.restcomm.mgcp.ConnectionStateChanged;
import org.mobicents.servlet.restcomm.mgcp.CreateBridgeEndpoint;
import org.mobicents.servlet.restcomm.mgcp.CreateConnection;
import org.mobicents.servlet.restcomm.mgcp.CreateLink;
import org.mobicents.servlet.restcomm.mgcp.CreateMediaSession;
import org.mobicents.servlet.restcomm.mgcp.DestroyConnection;
import org.mobicents.servlet.restcomm.mgcp.DestroyEndpoint;
import org.mobicents.servlet.restcomm.mgcp.DestroyLink;
import org.mobicents.servlet.restcomm.mgcp.GetMediaGatewayInfo;
import org.mobicents.servlet.restcomm.mgcp.InitializeConnection;
import org.mobicents.servlet.restcomm.mgcp.InitializeLink;
import org.mobicents.servlet.restcomm.mgcp.LinkStateChanged;
import org.mobicents.servlet.restcomm.mgcp.MediaGatewayInfo;
import org.mobicents.servlet.restcomm.mgcp.MediaGatewayResponse;
import org.mobicents.servlet.restcomm.mgcp.MediaSession;
import org.mobicents.servlet.restcomm.mgcp.OpenConnection;
import org.mobicents.servlet.restcomm.mgcp.OpenLink;
import org.mobicents.servlet.restcomm.mgcp.UpdateConnection;
import org.mobicents.servlet.restcomm.mgcp.UpdateLink;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;
import org.mobicents.servlet.restcomm.util.IPUtils;

import scala.concurrent.duration.Duration;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author jean.deruelle@telestax.com
 */
public final class Call extends UntypedActor {
	// Define possible directions.
	private static final String INBOUND = "inbound";
	private static final String OUTBOUND_API = "outbound-api";
	private static final String OUTBOUND_DIAL = "outbound-dial";
	// Logging
	private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);
	// States for the FSM.
	private final State uninitialized;
	private final State queued;
	private final State ringing;
	private final State busy;
	private final State notFound;
	private final State canceled;
	private final State noAnswer;
	private final State inProgress;
	private final State completed;
	private final State failed;
	// Intermediate states.
	private final State canceling;
	private final State acquiringMediaGatewayInfo;
	private final State acquiringMediaSession;
	private final State acquiringBridge;
	private final State acquiringRemoteConnection;
	private final State initializingRemoteConnection;
	private final State openingRemoteConnection;
	private final State updatingRemoteConnection;
	private final State dialing;
	private final State failing;
	private final State failingBusy;
	private final State failingNoAnswer;
	private final State muting;
	private final State unmuting;
	private final State acquiringInternalLink;
	private final State initializingInternalLink;
	private final State openingInternalLink;
	private final State updatingInternalLink;
	private final State closingInternalLink;
	private final State closingRemoteConnection;
	// FSM.
	private final FiniteStateMachine fsm;
	// SIP runtime stuff.
	private final SipFactory factory;
    private String apiVersion;
    private Sid accountId;
	private String name;
	private SipURI from;
	private SipURI to;
    // custom headers for SIP Out https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out
    private Map<String,String> headers;
    private String username;
    private String password;
	private long timeout;
	private SipServletRequest invite;
	// MGCP runtime stuff.
	private final ActorRef gateway;
	private MediaGatewayInfo gatewayInfo;
	private MediaSession session;
	private ActorRef bridge;
	private ActorRef remoteConn;
	private ActorRef internalLink;
	private ActorRef internalLinkEndpoint;
	private ConnectionMode internalLinkMode;
	// Runtime stuff.
	private final Sid id;
	private CallStateChanged.State external;
	private String direction;
	private String forwardedFrom;
	private DateTime created;
	private final List<ActorRef> observers;
	
	private ActorRef group;

    public Call(final SipFactory factory, final ActorRef gateway) {
		super();
		final ActorRef source = self();
		// Initialize the states for the FSM.
		uninitialized = new State("uninitialized", null, null);
		queued = new State("queued", new Queued(source), null);
		ringing = new State("ringing", new Ringing(source), null);
		busy = new State("busy", new Busy(source), null);
		notFound = new State("not found", new NotFound(source), null);
		canceled = new State("canceled", new Canceled(source), null);
		noAnswer = new State("no answer", new NoAnswer(source), null);
		inProgress = new State("in progress", new InProgress(source), null);
		completed = new State("completed", new Completed(source), null);
		failed = new State("failed", new Failed(source), null);
		// Initialize the intermediate states for the FSM.
		canceling = new State("canceling", new Canceling(source), null);
		acquiringMediaGatewayInfo = new State("acquiring media gateway info",
				new AcquiringMediaGatewayInfo(source), null);
		acquiringMediaSession = new State("acquiring media session",
				new AcquiringMediaSession(source), null);
		acquiringBridge = new State("acquiring bridge", new AcquiringBridge(source), null);
		acquiringRemoteConnection = new State("acquiring remote connection",
				new AcquiringRemoteConnection(source), null);
		initializingRemoteConnection = new State("initializing remote connection",
				new InitializingRemoteConnection(source), null);
		openingRemoteConnection = new State("opening remote connection",
				new OpeningRemoteConnection(source), null);
		updatingRemoteConnection = new State("updating remote connection",
				new UpdatingRemoteConnection(source), null);
		dialing = new State("dialing", new Dialing(source), null);
		failing = new State("failing", new Failing(source), null);
		failingBusy = new State("failing busy", new FailingBusy(source), null);
		failingNoAnswer = new State("failing no answer", new FailingNoAnswer(source), null);
		acquiringInternalLink = new State("acquiring internal link",
				new AcquiringInternalLink(source), null);
		initializingInternalLink = new State("initializing internal link",
				new InitializingInternalLink(source), null);
		openingInternalLink = new State("opening internal link",
				new OpeningInternalLink(source), null);
		updatingInternalLink = new State("updating internal link",
				new UpdatingInternalLink(source), null);
		closingInternalLink = new State("closing internal link",
				new EnteringClosingInternalLink(source), new ExitingClosingInternalLink(source));
		muting = new State("muting", new Muting(source), null);
		unmuting = new State("unmuting", new Unmuting(source), null);
		closingRemoteConnection = new State("closing remote connection",
				new ClosingRemoteConnection(source), null);
		// Initialize the transitions for the FSM.
		final Set<Transition> transitions = new HashSet<Transition>();
		transitions.add(new Transition(uninitialized, queued));
		transitions.add(new Transition(uninitialized, ringing));
		transitions.add(new Transition(queued, canceled));
		transitions.add(new Transition(queued, acquiringMediaGatewayInfo));
		transitions.add(new Transition(queued, closingRemoteConnection));
		transitions.add(new Transition(acquiringMediaGatewayInfo, canceled));
		transitions.add(new Transition(acquiringMediaGatewayInfo, acquiringMediaSession));
		transitions.add(new Transition(acquiringMediaSession, canceled));
		transitions.add(new Transition(acquiringMediaSession, acquiringBridge));
		transitions.add(new Transition(acquiringBridge, canceled));
		transitions.add(new Transition(acquiringBridge, acquiringRemoteConnection));
		transitions.add(new Transition(acquiringRemoteConnection, canceled));
		transitions.add(new Transition(acquiringRemoteConnection, initializingRemoteConnection));
		transitions.add(new Transition(initializingRemoteConnection, canceled));
		transitions.add(new Transition(initializingRemoteConnection, openingRemoteConnection));
		transitions.add(new Transition(openingRemoteConnection, canceling));
		transitions.add(new Transition(openingRemoteConnection, dialing));
		transitions.add(new Transition(openingRemoteConnection, failed));
		transitions.add(new Transition(openingRemoteConnection, inProgress));
		transitions.add(new Transition(dialing, busy));
		transitions.add(new Transition(dialing, canceling));
		transitions.add(new Transition(dialing, failingNoAnswer));
		transitions.add(new Transition(dialing, ringing));
		transitions.add(new Transition(dialing, failed));
		transitions.add(new Transition(dialing, updatingRemoteConnection));
		transitions.add(new Transition(ringing, canceled));
		transitions.add(new Transition(ringing, busy));
		transitions.add(new Transition(ringing, notFound));
		transitions.add(new Transition(ringing, canceling));
		transitions.add(new Transition(ringing, noAnswer));
		transitions.add(new Transition(ringing, updatingRemoteConnection));
		transitions.add(new Transition(ringing, acquiringMediaGatewayInfo));
		transitions.add(new Transition(ringing, failingBusy));
		transitions.add(new Transition(ringing, closingRemoteConnection));
		transitions.add(new Transition(failingNoAnswer, noAnswer));
		transitions.add(new Transition(failingBusy, busy));
		transitions.add(new Transition(canceling, canceled));
		transitions.add(new Transition(updatingRemoteConnection, inProgress));
		transitions.add(new Transition(updatingRemoteConnection, closingRemoteConnection));
		transitions.add(new Transition(inProgress, muting));
		transitions.add(new Transition(inProgress, unmuting));
		transitions.add(new Transition(inProgress, acquiringInternalLink));
		transitions.add(new Transition(inProgress, closingInternalLink));
		transitions.add(new Transition(inProgress, closingRemoteConnection));
		transitions.add(new Transition(inProgress, acquiringMediaGatewayInfo));
		transitions.add(new Transition(acquiringInternalLink, closingRemoteConnection));
		transitions.add(new Transition(acquiringInternalLink, initializingInternalLink));
		transitions.add(new Transition(initializingInternalLink, closingRemoteConnection));
		transitions.add(new Transition(initializingInternalLink, openingInternalLink));
		transitions.add(new Transition(openingInternalLink, closingInternalLink));
		transitions.add(new Transition(openingInternalLink, closingRemoteConnection));
		transitions.add(new Transition(openingInternalLink, updatingInternalLink));
		transitions.add(new Transition(updatingInternalLink, closingInternalLink));
		transitions.add(new Transition(updatingInternalLink, closingRemoteConnection));
		transitions.add(new Transition(updatingInternalLink, inProgress));
		transitions.add(new Transition(closingInternalLink, inProgress));
		transitions.add(new Transition(closingInternalLink, completed));
		transitions.add(new Transition(muting, inProgress));
		transitions.add(new Transition(muting, closingRemoteConnection));
		transitions.add(new Transition(unmuting, inProgress));
		transitions.add(new Transition(unmuting, closingRemoteConnection));
		transitions.add(new Transition(closingRemoteConnection, closingInternalLink));
		transitions.add(new Transition(closingRemoteConnection, completed));
		// Initialize the FSM.
		this.fsm = new FiniteStateMachine(uninitialized, transitions);
		// Initialize the SIP runtime stuff.
		this.factory = factory;
		// Initialize the MGCP runtime stuff.
		this.gateway = gateway;
		// Initialize the runtime stuff.
		this.id = Sid.generate(Sid.Type.CALL);
		this.created = DateTime.now();
		this.observers = new ArrayList<ActorRef>();
	}

	private ActorRef getMediaGroup(final Object message) {
		return getContext().actorOf(new Props(new UntypedActorFactory() {
			private static final long serialVersionUID = 1L;
			@Override public UntypedActor create() throws Exception {
				return new MediaGroup(gateway, session, bridge);
			}
		}));
	}

	private void forwarding(final Object message) {

	}

	private CallResponse<CallInfo> info() {
		final String from = this.from.getUser();
		final String to = this.to.getUser();
		final CallInfo info =  new CallInfo(id, external, direction, created,
				forwardedFrom, name, from, to);
		return new CallResponse<CallInfo>(info);
	}

	private void invite(final Object message) {
		final AddParticipant request = (AddParticipant)message;
		final Join join = new Join(bridge, ConnectionMode.SendRecv);
		final ActorRef call = request.call();
		final ActorRef self = self();
		call.tell(join, self);
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

	@Override public void onReceive(final Object message) throws Exception {
		final UntypedActorContext context = getContext();
		final Class<?> klass = message.getClass();
		final ActorRef self = self();
		final ActorRef sender = sender();
		final State state = fsm.state();
		logger.info("********** Call's Current State: \"" + state.toString());
		logger.info("********** Call Processing Message: \"" + klass.getName());

		if(Observe.class.equals(klass)) {
			observe(message);
    } else if(StopObserving.class.equals(klass)) {
			stopObserving(message);
    } else if(GetCallObservers.class.equals(klass)) {
    	sender.tell(new CallResponse<List<ActorRef>>(observers), self);
    } else if(GetCallInfo.class.equals(klass)) {
			sender.tell(info(), self);
    } else if(InitializeOutbound.class.equals(klass)) {
			fsm.transition(message, queued);
    } else if(Answer.class.equals(klass) ||
				Dial.class.equals(klass)) {
			fsm.transition(message, acquiringMediaGatewayInfo);
    } else if(Reject.class.equals(klass)) {
			fsm.transition(message, busy);
    } else if(MediaGatewayResponse.class.equals(klass)) {
			if(acquiringMediaGatewayInfo.equals(state)) {
				fsm.transition(message, acquiringMediaSession);
			} else if(acquiringMediaSession.equals(state)) {
				fsm.transition(message, acquiringBridge);
			} else if(acquiringBridge.equals(state)) {
				fsm.transition(message, acquiringRemoteConnection);
			} else if(acquiringRemoteConnection.equals(state)) {
				fsm.transition(message, initializingRemoteConnection);
			} else if(acquiringInternalLink.equals(state)) {
				fsm.transition(message, initializingInternalLink);
			}
    } else if(ConnectionStateChanged.class.equals(klass)) {
			final ConnectionStateChanged event = (ConnectionStateChanged)message;

			if(ConnectionStateChanged.State.CLOSED == event.state()) {
				if(initializingRemoteConnection.equals(state)) {
					fsm.transition(message, openingRemoteConnection);
				} else if(openingRemoteConnection.equals(state)) {
					fsm.transition(message, failed);
				} else if(failing.equals(state)) {
					fsm.transition(message, failed);
				} else if(failingBusy.equals(state)) {
					fsm.transition(message, busy);
				} else if(failingNoAnswer.equals(state)) {
					fsm.transition(message, noAnswer);
				} else if(muting.equals(state) || unmuting.equals(state)) {
					fsm.transition(message, closingRemoteConnection);
				} else if(closingRemoteConnection.equals(state)) {		
					if(internalLink != null) {
						fsm.transition(message, closingInternalLink);
					} else {
						fsm.transition(message, completed);
					}
				}
      } else if(ConnectionStateChanged.State.HALF_OPEN == event.state()) {
				fsm.transition(message, dialing);
      } else if(ConnectionStateChanged.State.OPEN == event.state()) {
				fsm.transition(message, inProgress);
			}
    } else if(Cancel.class.equals(klass)) {
			if(openingRemoteConnection.equals(state) || dialing.equals(state) ||
					ringing.equals(state)) {
				fsm.transition(message, canceling);
			}
    } else if(LinkStateChanged.class.equals(klass)) {
			final LinkStateChanged event = (LinkStateChanged)message;
			if(LinkStateChanged.State.CLOSED == event.state()) {
				if(initializingInternalLink.equals(state)) {
					fsm.transition(message, openingInternalLink);
				} else if(openingInternalLink.equals(state)) {
					fsm.transition(message, closingRemoteConnection);
				} else if(closingInternalLink.equals(state)) {
					if(remoteConn != null) {
						fsm.transition(message, inProgress);
					} else {
						fsm.transition(message, completed);
					}
				}
			} else if(LinkStateChanged.State.OPEN == event.state()) {
				if(openingInternalLink.equals(state)) {
					fsm.transition(message, updatingInternalLink);
				} else if(updatingInternalLink.equals(state)) {
					fsm.transition(message, inProgress);
				}
			}
    } else if(message instanceof ReceiveTimeout) {
			fsm.transition(message, failingNoAnswer);
    } else if(message instanceof SipServletRequest) {
			final SipServletRequest request = (SipServletRequest)message;
			final String method = request.getMethod();
			if("INVITE".equalsIgnoreCase(method)) {
				if(uninitialized.equals(state)) {
					fsm.transition(message, ringing);
				}
			} else if("CANCEL".equalsIgnoreCase(method)) {
				if(openingRemoteConnection.equals(state)) {
					fsm.transition(message, canceling);
				} else {
					fsm.transition(message, canceled);
				}
			} else if("BYE".equalsIgnoreCase(method)) {
				fsm.transition(message, closingRemoteConnection);
			}
    } else if(message instanceof SipServletResponse) {
			final SipServletResponse response = (SipServletResponse)message;
			final int code = response.getStatus();
			switch(code) {
			case SipServletResponse.SC_CALL_BEING_FORWARDED: {
				forwarding(message);
				break;
			}
			case SipServletResponse.SC_RINGING:
			case SipServletResponse.SC_SESSION_PROGRESS: {
				fsm.transition(message, ringing);
				break;
			}
			case SipServletResponse.SC_BUSY_HERE:
			case SipServletResponse.SC_BUSY_EVERYWHERE: {
				fsm.transition(message, failingBusy);
				break;
			}
            case SipServletResponse.SC_UNAUTHORIZED:
            case SipServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED: {
                // Handles Auth for https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out
                if(username == null || password == null) {
                    fsm.transition(message, failing);
                } else {
                    AuthInfo authInfo = factory.createAuthInfo();
                    String authHeader = response.getHeader("Proxy-Authenticate");
                    if(authHeader == null) {
                        authHeader = response.getHeader("WWW-Authenticate");
                    }
                    String tempRealm = authHeader.substring(authHeader.indexOf("realm=\"") + "realm=\"".length());
                    String realm = tempRealm.substring(0, tempRealm.indexOf("\""));
                    authInfo.addAuthInfo(response.getStatus(), realm, username, password);
                    SipServletRequest challengeRequest = response.getSession().createRequest(
                            response.getRequest().getMethod());
                    challengeRequest.addAuthHeader(response, authInfo);
                    invite = challengeRequest;
                    challengeRequest.send();
                }
                break;
            }
			case SipServletResponse.SC_OK: {
          if(dialing.equals(state) || (ringing.equals(state) &&
              !direction.equals("inbound"))) {
					fsm.transition(message, updatingRemoteConnection);
				} 
				break;
			}
			default: {
				if(code >= 400 && code != 487) {
					fsm.transition(message, failing);
				}
			}
			}
    } else if(inProgress.equals(state)) {
			if(CreateMediaGroup.class.equals(klass)) {
				if(group == null)
					group = getMediaGroup(message);
				sender.tell(new CallResponse<ActorRef>(group), self);
			} else if(DestroyMediaGroup.class.equals(klass)) {
				final DestroyMediaGroup request = (DestroyMediaGroup)message;
				context.stop(request.group());
			} else if(AddParticipant.class.equals(klass)) {
				invite(message);
			} else if(RemoveParticipant.class.equals(klass)) {
				remove(message);
			} else if(Join.class.equals(klass)) {
				fsm.transition(message, acquiringInternalLink);
			} else if(Leave.class.equals(klass)) {
				fsm.transition(message, closingInternalLink);
			} else if(Mute.class.equals(klass)) {
				fsm.transition(message, muting);
			} else if(Unmute.class.equals(klass)) {
				fsm.transition(message, unmuting);
			} else if(Hangup.class.equals(klass)) {
				fsm.transition(message, closingRemoteConnection);
			} 
		} else if (Hangup.class.equals(klass)) {
			if (queued.equals(state) || ringing.equals(state)){
				fsm.transition(message, closingRemoteConnection);
			}
		} else if(ringing.equals(state)) {
		    if(org.mobicents.servlet.restcomm.telephony.NotFound.class.equals(klass)) {
                fsm.transition(message, notFound);
            }
		}
	}

	@SuppressWarnings("unchecked")
	private String patch(final byte[] data, final String externalIp)
			throws UnknownHostException, SdpException {
		final String text = new String(data);
		final SessionDescription sdp = SdpFactory.getInstance().createSessionDescription(text);
		// Handle the connection at the session level.
		fix(sdp.getConnection(), externalIp);
		// Handle the connections at the media description level.
		final Vector<MediaDescription> descriptions = sdp.getMediaDescriptions(false);
		for(final MediaDescription description : descriptions) {
			fix(description.getConnection(), externalIp);
		}
		return sdp.toString();
	}

	private void fix(final Connection connection, final String externalIp)
			throws UnknownHostException, SdpException {
		if(connection != null) {
			if(Connection.IN.equals(connection.getNetworkType())) {
				if(Connection.IP4.equals(connection.getAddressType())) {
					final InetAddress address = InetAddress.getByName(connection.getAddress());
					final String ip = address.getHostAddress();
					if(!IPUtils.isRoutableAddress(ip)) {
						connection.setAddress(externalIp);
					}
				}
			}
		}
	}

	private void remove(final Object message) {
		final RemoveParticipant request = (RemoveParticipant)message;
		final ActorRef call = request.call();
		final ActorRef self = self();
		final Leave leave = new Leave();
		call.tell(leave, self);
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


	private final class Queued extends AbstractAction {
		public Queued(final ActorRef source) {
			super(source);
		}

		@Override public void execute(Object message) throws Exception {
			final InitializeOutbound request = (InitializeOutbound)message;
			name = request.name();
			from = request.from();
            to = request.to();
            apiVersion = request.apiVersion();
            accountId = request.accountId();
            username = request.username();
            password = request.password();
            String toHeaderString = to.toString();
            if(toHeaderString.indexOf('?') != -1) {
                // custom headers parsing for SIP Out https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out
                headers = new HashMap<String, String>();
                // we keep only the to URI without the headers
                to = (SipURI) factory.createURI(toHeaderString.substring(0,toHeaderString.lastIndexOf('?')));
                String headersString = toHeaderString.substring(toHeaderString.lastIndexOf('?')+1);
                StringTokenizer tokenizer = new StringTokenizer(headersString, "&");
                while(tokenizer.hasMoreTokens()) {
                    String headerNameValue = tokenizer.nextToken();
                    String headerName= headerNameValue.substring(0, headerNameValue.lastIndexOf('='));
                    String headerValue = headerNameValue.substring(headerNameValue.lastIndexOf('=')+1);
                    headers.put(headerName, headerValue);
                }
            }
			timeout = request.timeout();
			if(request.isFromApi()) {
				direction = OUTBOUND_API;
			} else {
				direction = OUTBOUND_DIAL;
			}
			// Notify the observers.
			external = CallStateChanged.State.QUEUED;
			final CallStateChanged event = new CallStateChanged(external);
			for(final ActorRef observer : observers) {
				observer.tell(event, source);
			}
		}
	}


	private final class AcquiringMediaGatewayInfo extends AbstractAction {
		public AcquiringMediaGatewayInfo(final ActorRef source) {
			super(source);
		}

		@Override public void execute(final Object message) throws Exception {
			final ActorRef self = self();
			gateway.tell(new GetMediaGatewayInfo(), self);
		}
	}

	private final class AcquiringMediaSession extends AbstractAction {
		public AcquiringMediaSession(final ActorRef source) {
			super(source);
		}

		@SuppressWarnings("unchecked")
		@Override public void execute(final Object message) throws Exception {
			final MediaGatewayResponse<MediaGatewayInfo> response =
					(MediaGatewayResponse<MediaGatewayInfo>)message;
			gatewayInfo = response.get();
			gateway.tell(new CreateMediaSession(), source);
		}
	}

	public final class AcquiringBridge extends AbstractAction {
		public AcquiringBridge(final ActorRef source) {
			super(source);
		}

		@SuppressWarnings("unchecked")
		@Override public void execute(final Object message) throws Exception {
			final MediaGatewayResponse<MediaSession> response = (MediaGatewayResponse<MediaSession>)message;
			session = response.get();
			gateway.tell(new CreateBridgeEndpoint(session), source);
		}
	}

	private final class AcquiringRemoteConnection extends AbstractAction {
		public AcquiringRemoteConnection(final ActorRef source) {
			super(source);
		}

		@SuppressWarnings("unchecked")
		@Override public void execute(final Object message) throws Exception {
			final MediaGatewayResponse<ActorRef> response = (MediaGatewayResponse<ActorRef>)message;
			bridge = response.get();
			gateway.tell(new CreateConnection(session), source);
		}
	}

	private final class InitializingRemoteConnection extends AbstractAction {
		public InitializingRemoteConnection(ActorRef source) {
			super(source);
		}

		@SuppressWarnings("unchecked")
		@Override public void execute(final Object message) throws Exception {
			final MediaGatewayResponse<ActorRef> response = (MediaGatewayResponse<ActorRef>)message;
			remoteConn = response.get();
			remoteConn.tell(new Observe(source), source);
			remoteConn.tell(new InitializeConnection(bridge), source);
		}
	}

	private final class OpeningRemoteConnection extends AbstractAction {
		public OpeningRemoteConnection(final ActorRef source) {
			super(source);
		}

		@Override public void execute(final Object message) throws Exception {
			OpenConnection open = null;
			if(OUTBOUND_DIAL.equals(direction) || OUTBOUND_API.equals(direction)) {
				open = new OpenConnection(ConnectionMode.SendRecv);
			} else {
				final String externalIp = invite.getInitialRemoteAddr();
				final byte[] sdp = invite.getRawContent();
				final String offer = patch(sdp, externalIp);
				final ConnectionDescriptor descriptor = new ConnectionDescriptor(offer);
				open = new OpenConnection(descriptor, ConnectionMode.SendRecv);
			}
			remoteConn.tell(open, source);
		}
	}

	private final class Dialing extends AbstractAction {
		public Dialing(final ActorRef source) {
			super(source);
		}

		@Override public void execute(Object message) throws Exception {
			final ConnectionStateChanged response = (ConnectionStateChanged)message;
			final ActorRef self = self();
			// Create a SIP invite to initiate a new session.
			final StringBuilder buffer = new StringBuilder();
			buffer.append(to.getHost());
			if(to.getPort() > -1) {
				buffer.append(":").append(to.getPort());
			}
			final SipURI uri = factory.createSipURI(null, buffer.toString());
			final SipApplicationSession application = factory.createApplicationSession();
			application.setAttribute(Call.class.getName(), self);
			invite = factory.createRequest(application, "INVITE", from, to);
			invite.pushRoute(uri);

            if(headers != null) {
                // adding custom headers for SIP Out https://bitbucket.org/telestax/telscale-restcomm/issue/132/implement-twilio-sip-out
                Set<Map.Entry<String, String>> entrySet = headers.entrySet();
                for(Map.Entry<String, String> entry : entrySet) {
                    invite.addHeader("X-" + entry.getKey(), entry.getValue());
                }
            }
            invite.addHeader("X-RestComm-ApiVersion" , apiVersion);
            invite.addHeader("X-RestComm-AccountSid" , accountId.toString());
            invite.addHeader("X-RestComm-CallSid", id.toString());
			final SipSession session = invite.getSession();
			session.setHandler("CallManager");
			String offer = null;
			if(gatewayInfo.useNat()) {
				final String externalIp = gatewayInfo.externalIP().getHostAddress();
				final byte[] sdp = response.descriptor().toString().getBytes();
				offer = patch(sdp, externalIp);
			} else {
				offer = response.descriptor().toString();
			}
			invite.setContent(offer, "application/sdp");
			// Send the invite.
			invite.send();
			// Set the timeout period.
			final UntypedActorContext context = getContext();
			context.setReceiveTimeout(Duration.create(timeout, TimeUnit.SECONDS));
		}
	}

	private final class Ringing extends AbstractAction {
		public Ringing(final ActorRef source) {
			super(source);
		}

		@Override public void execute(final Object message) throws Exception {
			if(message instanceof SipServletRequest) {
				invite = (SipServletRequest)message;
				from = (SipURI)invite.getFrom().getURI();
				to = (SipURI)invite.getTo().getURI();
				timeout = -1;
				direction = INBOUND;
				// Send a ringing response.
				final SipServletResponse ringing = invite.createResponse(SipServletResponse.SC_RINGING);
				ringing.send();
			} else if(message instanceof SipServletResponse) {
				final UntypedActorContext context = getContext();
				context.setReceiveTimeout(Duration.Undefined());
			}
			// Notify the observers.
			external = CallStateChanged.State.RINGING;
			final CallStateChanged event = new CallStateChanged(external);
			for(final ActorRef observer : observers) {
				observer.tell(event, source);
			}
		}
	}

	private final class Canceling extends AbstractAction {
		public Canceling(final ActorRef source) {
			super(source);
		}

		@Override public void execute(final Object message) throws Exception {
			final State state = fsm.state();
			if(dialing.equals(state) || (ringing.equals(state) &&
					OUTBOUND_DIAL.equals(direction) || OUTBOUND_API.equals(direction))) {
				final UntypedActorContext context = getContext();
				context.setReceiveTimeout(Duration.Undefined());
				final SipServletRequest cancel = invite.createCancel();
				cancel.send();
			}
			remoteConn.tell(new CloseConnection(), source);
		}
	}

	private final class Canceled extends AbstractAction {
		public Canceled(ActorRef source) {
			super(source);
		}

		@Override public void execute(final Object message) throws Exception {
			if(remoteConn != null) {
				gateway.tell(new DestroyConnection(remoteConn), source);
				remoteConn = null;
			}
			// Explicitly invalidate the application session.
			invite.getSession().invalidate();
			invite.getApplicationSession().invalidate();
			// Notify the observers.
			external = CallStateChanged.State.CANCELED;
			final CallStateChanged event = new CallStateChanged(external);
			for(final ActorRef observer : observers) {
				observer.tell(event, source);
			}
		}
	}

	private class Failing extends ClosingRemoteConnection {
		public Failing(final ActorRef source) {
			super(source);
		}

		@Override public void execute(final Object message) throws Exception {
			if(message instanceof ReceiveTimeout) {
				final UntypedActorContext context = getContext();
				context.setReceiveTimeout(Duration.Undefined());
			}
			super.execute(message);
		}
	}

	private final class FailingBusy extends Failing {
		public FailingBusy(final ActorRef source) {
			super(source);
		}
	}

	private final class FailingNoAnswer extends Failing {
		public FailingNoAnswer(final ActorRef source) {
			super(source);
		}
	}

	private final class Busy extends AbstractAction {
		public Busy(final ActorRef source) {
			super(source);
		}

		@Override public void execute(final Object message) throws Exception {
			final Class<?> klass = message.getClass();
			final State state = fsm.state();
			if(Reject.class.equals(klass) && ringing.equals(state) && INBOUND.equals(direction)) {
				final SipServletResponse busy = invite.createResponse(SipServletResponse.SC_BUSY_HERE);
				busy.send();
			}
			if(remoteConn != null) {
				gateway.tell(new DestroyConnection(remoteConn), source);
				remoteConn = null;
			}
			// Explicitly invalidate the application session.
			invite.getSession().invalidate();
			invite.getApplicationSession().invalidate();
			// Notify the observers.
			external = CallStateChanged.State.BUSY;
			final CallStateChanged event = new CallStateChanged(external);
			for(final ActorRef observer : observers) {
				observer.tell(event, source);
			}
		}
	}
	
	private final class NotFound extends AbstractAction {
        public NotFound(final ActorRef source) {
            super(source);
        }

        @Override public void execute(final Object message) throws Exception {
            final Class<?> klass = message.getClass();
            if(org.mobicents.servlet.restcomm.telephony.NotFound.class.equals(klass) && INBOUND.equals(direction)) {
                final SipServletResponse notFound = invite.createResponse(SipServletResponse.SC_NOT_FOUND);
                notFound.send();
            }            
            // Notify the observers.
            external = CallStateChanged.State.NOT_FOUND;
            final CallStateChanged event = new CallStateChanged(external);
            for(final ActorRef observer : observers) {
                observer.tell(event, source);
            }
        }
    }

	private final class NoAnswer extends AbstractAction {
		public NoAnswer(final ActorRef source) {
			super(source);
		}

		@Override public void execute(final Object message) throws Exception {
			if(remoteConn != null) {
				gateway.tell(new DestroyConnection(remoteConn), source);
				remoteConn = null;
			}
			// Explicitly invalidate the application session.
			invite.getSession().invalidate();
			invite.getApplicationSession().invalidate();
			// Notify the observers.
			external = CallStateChanged.State.NO_ANSWER;
			final CallStateChanged event = new CallStateChanged(external);
			for(final ActorRef observer : observers) {
				observer.tell(event, source);
			}
		}
	}

	private final class Failed extends AbstractAction {
		public Failed(final ActorRef source) {
			super(source);
		}

		@Override public void execute(final Object message) throws Exception {
			if(remoteConn != null) {
				gateway.tell(new DestroyConnection(remoteConn), source);
				remoteConn = null;
			}
			// Explicitly invalidate the application session.
			invite.getSession().invalidate();
			invite.getApplicationSession().invalidate();
			// Notify the observers.
			external = CallStateChanged.State.FAILED;
			final CallStateChanged event = new CallStateChanged(external);
			for(final ActorRef observer : observers) {
				observer.tell(event, source);
			}
		}
	}

	private final class UpdatingRemoteConnection extends AbstractAction {
		public UpdatingRemoteConnection(final ActorRef source) {
			super(source);
		}

		@Override public void execute(final Object message) throws Exception {
			final State state = fsm.state();
			if(dialing.equals(state)) {
				final UntypedActorContext context = getContext();
				context.setReceiveTimeout(Duration.Undefined());
			}
			final SipServletResponse response = (SipServletResponse)message;
			//Issue 99: http://www.google.com/url?q=https://bitbucket.org/telestax/telscale-restcomm/issue/99/dial-uri-fails&usd=2&usg=ALhdy29vtLfDNXNpjTxYYp08YRatKfV9Aw
			if(response.getStatus()==SipServletResponse.SC_OK && 
					(OUTBOUND_DIAL.equals(direction) || OUTBOUND_API.equals(direction))){
				SipServletRequest ack = response.createAck();
				ack.send();
				logger.info("Just sent out ACK : "+ack.toString());
			}
				
			final String externalIp = invite.getInitialRemoteAddr();
			final byte[] sdp = response.getRawContent();
			final String answer = patch(sdp, externalIp);
			final ConnectionDescriptor descriptor = new ConnectionDescriptor(answer);
			final UpdateConnection update = new UpdateConnection(descriptor);
			remoteConn.tell(update, source);
		}
	}

	private final class InProgress extends AbstractAction {
		public InProgress(final ActorRef source) {
			super(source);
		}

		@Override public void execute(final Object message) throws Exception {
			final State state = fsm.state();
			if(openingRemoteConnection.equals(state)) {
				final ConnectionStateChanged response = (ConnectionStateChanged)message;
				final SipServletResponse okay = invite.createResponse(SipServletResponse.SC_OK);
				final byte[] sdp = response.descriptor().toString().getBytes();
				String answer = null;
				if(gatewayInfo.useNat()) {
					final String externalIp = gatewayInfo.externalIP().getHostAddress();
					answer = patch(sdp, externalIp);
				} else {
					answer = response.descriptor().toString();
				}
				okay.setContent(answer, "application/sdp");
				okay.send();
			}
			if(openingRemoteConnection.equals(state) || updatingRemoteConnection.equals(state)) {
				// Make sure the SIP session doesn't end pre-maturely.
				invite.getApplicationSession().setExpires(0);
			}
			// Notify the observers.
			external = CallStateChanged.State.IN_PROGRESS;
			final CallStateChanged event = new CallStateChanged(external);
			for(final ActorRef observer : observers) {
				observer.tell(event, source);
			}
		}
	}

	private final class AcquiringInternalLink extends AbstractAction {
		public AcquiringInternalLink(final ActorRef source) {
			super(source);
		}

		@Override public void execute(final Object message) throws Exception {
			final Class<?> klass = message.getClass();
			if(Join.class.equals(klass)) {
				final Join request = (Join)message;
				internalLinkEndpoint = request.endpoint();
				internalLinkMode = request.mode();
			}
			gateway.tell(new CreateLink(session), source);
		}
	}

	private final class InitializingInternalLink extends AbstractAction {
		public InitializingInternalLink(final ActorRef source) {
			super(source);
		}

		@SuppressWarnings("unchecked")
		@Override public void execute(Object message) throws Exception {
			final MediaGatewayResponse<ActorRef> response = (MediaGatewayResponse<ActorRef>)message;
			internalLink = response.get();
			internalLink.tell(new Observe(source), source);
			internalLink.tell(new InitializeLink(bridge, internalLinkEndpoint), source);
		}
	}

	private final class OpeningInternalLink extends AbstractAction {
		public OpeningInternalLink(final ActorRef source) {
			super(source);
		}

		@Override public void execute(Object message) throws Exception {
			internalLink.tell(new OpenLink(internalLinkMode), source);
		}
	}

	private final class UpdatingInternalLink extends AbstractAction {
		public UpdatingInternalLink(final ActorRef source) {
			super(source);
		}

		@Override public void execute(final Object message) throws Exception {
			final UpdateLink update = new UpdateLink(ConnectionMode.SendRecv, UpdateLink.Type.PRIMARY);
			internalLink.tell(update, source);
		}
	}

	private final class Muting extends AbstractAction {
		public Muting(final ActorRef source) {
			super(source);
		}

		@Override public void execute(final Object message) throws Exception {
			final UpdateConnection update = new UpdateConnection(ConnectionMode.RecvOnly);
			remoteConn.tell(update, source);

		}
	}

	private final class Unmuting extends AbstractAction {
		public Unmuting(final ActorRef source) {
			super(source);
		}

		@Override public void execute(Object message) throws Exception {
			final UpdateConnection update = new UpdateConnection(ConnectionMode.SendRecv);
			remoteConn.tell(update, source);
		}
	}

	private final class EnteringClosingInternalLink extends AbstractAction {
		public EnteringClosingInternalLink(final ActorRef source) {
			super(source);
		}

		@Override public void execute(final Object message) throws Exception {
			internalLink.tell(new CloseLink(), source);
		}
	}

	private final class ExitingClosingInternalLink extends AbstractAction {
		public ExitingClosingInternalLink(final ActorRef source) {
			super(source);
		}

		@Override public void execute(final Object message) throws Exception {
			gateway.tell(new DestroyLink(internalLink), source);
			internalLink = null;
			internalLinkEndpoint = null;
			internalLinkMode = null;
		}
	}

	private class ClosingRemoteConnection extends AbstractAction {
		public ClosingRemoteConnection(final ActorRef source) {
			super(source);
		}

		@Override public void execute(Object message) throws Exception {
			final Class<?> klass = message.getClass();
			if(Hangup.class.equals(klass)) {
				final SipSession session = invite.getSession();
				final SipServletRequest bye = session.createRequest("BYE");
				bye.send();
			} else if(message instanceof SipServletRequest) {
				final SipServletRequest bye = (SipServletRequest)message;
				final SipServletResponse okay = bye.createResponse(SipServletResponse.SC_OK);
				okay.send();

				//Issue 99: http://www.google.com/url?q=https://bitbucket.org/telestax/telscale-restcomm/issue/99/dial-uri-fails&usd=2&usg=ALhdy29vtLfDNXNpjTxYYp08YRatKfV9Aw
				// Notify the observers.
				external = CallStateChanged.State.COMPLETED;
				final CallStateChanged event = new CallStateChanged(external);
				for(final ActorRef observer : observers) {
					observer.tell(event, source);
				}
				
			} else if(message instanceof SipServletResponse){
				final SipServletResponse resp = (SipServletResponse)message;
				if(resp.equals(SipServletResponse.SC_BUSY_HERE) || 
						resp.equals(SipServletResponse.SC_BUSY_EVERYWHERE)){
					// Notify the observers.
					external = CallStateChanged.State.BUSY;
					final CallStateChanged event = new CallStateChanged(external);
					for(final ActorRef observer : observers) {
						observer.tell(event, source);
					}
				}
			}
			if(remoteConn != null) {
				remoteConn.tell(new CloseConnection(), source);
			}
		}
	}

	private final class Completed extends AbstractAction {
		public Completed(final ActorRef source) {
			super(source);
		}

		@Override public void execute(final Object message) throws Exception {
			if(remoteConn != null) {
				gateway.tell(new DestroyConnection(remoteConn), source);
				remoteConn = null;
			}
			if(internalLink != null) {
				gateway.tell(new DestroyLink(internalLink), source);
				internalLink = null;
			}
			if(bridge != null) {
				gateway.tell(new DestroyEndpoint(bridge), source);
				bridge = null;
			}
			// Explicitly invalidate the application session.
			invite.getSession().invalidate();
			invite.getApplicationSession().invalidate();
			// Notify the observers.
			external = CallStateChanged.State.COMPLETED;
			final CallStateChanged event = new CallStateChanged(external);
			for(final ActorRef observer : observers) {
				observer.tell(event, source);
			}
		}
	}
}
