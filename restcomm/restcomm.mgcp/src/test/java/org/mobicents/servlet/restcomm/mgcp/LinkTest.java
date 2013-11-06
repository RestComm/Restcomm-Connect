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
package org.mobicents.servlet.restcomm.mgcp;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;

import jain.protocol.ip.mgcp.JainMgcpEvent;
import jain.protocol.ip.mgcp.message.CreateConnection;
import jain.protocol.ip.mgcp.message.CreateConnectionResponse;
import jain.protocol.ip.mgcp.message.DeleteConnection;
import jain.protocol.ip.mgcp.message.DeleteConnectionResponse;
import jain.protocol.ip.mgcp.message.ModifyConnection;
import jain.protocol.ip.mgcp.message.ModifyConnectionResponse;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ConnectionMode;
import jain.protocol.ip.mgcp.message.parms.EndpointIdentifier;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.mobicents.servlet.restcomm.mgcp.CloseLink;
import org.mobicents.servlet.restcomm.mgcp.CreateBridgeEndpoint;
import org.mobicents.servlet.restcomm.mgcp.CreateIvrEndpoint;
import org.mobicents.servlet.restcomm.mgcp.CreateLink;
import org.mobicents.servlet.restcomm.mgcp.CreateMediaSession;
import org.mobicents.servlet.restcomm.mgcp.InitializeLink;
import org.mobicents.servlet.restcomm.mgcp.LinkStateChanged;
import org.mobicents.servlet.restcomm.mgcp.MediaGatewayResponse;
import org.mobicents.servlet.restcomm.mgcp.MediaSession;
import org.mobicents.servlet.restcomm.mgcp.OpenLink;
import org.mobicents.servlet.restcomm.mgcp.UpdateLink;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;
import org.mobicents.servlet.restcomm.fsm.Action;
import org.mobicents.servlet.restcomm.fsm.FiniteStateMachine;
import org.mobicents.servlet.restcomm.fsm.State;
import org.mobicents.servlet.restcomm.fsm.Transition;

/**
 * @author thomas.quintana@telestax.com (Thomas Quintana)
 */
public class LinkTest {
    private static ActorSystem system;

    public LinkTest() {
        super();
    }

    @BeforeClass
    public static void before() throws Exception {
        system = ActorSystem.create();
    }

    @AfterClass
    public static void after() throws Exception {
        system.shutdown();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSuccessfulScenario() {
        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                // Create a new mock media gateway to simulate the real thing.
                final ActorRef gateway = system.actorOf(new Props(MockMediaGateway.class));
                // Create a media session. This is just an identifier that groups
                // a set of end points, connections, and links in to one call.
                gateway.tell(new CreateMediaSession(), observer);
                final MediaGatewayResponse<MediaSession> mediaSessionResponse = expectMsgClass(MediaGatewayResponse.class);
                assertTrue(mediaSessionResponse.succeeded());
                final MediaSession session = mediaSessionResponse.get();
                // Create a bridge end point.
                gateway.tell(new CreateBridgeEndpoint(session), observer);
                MediaGatewayResponse<ActorRef> endpointResponse = expectMsgClass(MediaGatewayResponse.class);
                assertTrue(endpointResponse.succeeded());
                final ActorRef bridge = endpointResponse.get();
                // Create an ivr end point.
                gateway.tell(new CreateIvrEndpoint(session), observer);
                endpointResponse = expectMsgClass(MediaGatewayResponse.class);
                assertTrue(endpointResponse.succeeded());
                final ActorRef ivr = endpointResponse.get();
                // Create a link.
                gateway.tell(new CreateLink(session), observer);
                endpointResponse = expectMsgClass(MediaGatewayResponse.class);
                assertTrue(endpointResponse.succeeded());
                final ActorRef link = endpointResponse.get();
                // Start observing events from the link.
                link.tell(new Observe(observer), observer);
                final Observing observingResponse = expectMsgClass(Observing.class);
                assertTrue(observingResponse.succeeded());
                // Initialize the link.
                final InitializeLink initialize = new InitializeLink(bridge, ivr);
                link.tell(initialize, observer);
                LinkStateChanged event = expectMsgClass(LinkStateChanged.class);
                assertTrue(LinkStateChanged.State.CLOSED == event.state());
                // Open the link.
                link.tell(new OpenLink(ConnectionMode.SendRecv), observer);
                event = expectMsgClass(LinkStateChanged.class);
                assertTrue(LinkStateChanged.State.OPEN == event.state());
                // Close the link.
                link.tell(new CloseLink(), observer);
                event = expectMsgClass(LinkStateChanged.class);
                assertTrue(LinkStateChanged.State.CLOSED == event.state());
                // Stop observing events from the link.
                link.tell(new StopObserving(observer), observer);
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSuccessfulScenarioWithModify() {
        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                // Create a new mock media gateway to simulate the real thing.
                final ActorRef gateway = system.actorOf(new Props(MockMediaGateway.class));
                // Create a media session. This is just an identifier that groups
                // a set of end points, connections, and links in to one call.
                gateway.tell(new CreateMediaSession(), observer);
                final MediaGatewayResponse<MediaSession> mediaSessionResponse = expectMsgClass(MediaGatewayResponse.class);
                assertTrue(mediaSessionResponse.succeeded());
                final MediaSession session = mediaSessionResponse.get();
                // Create a bridge end point.
                gateway.tell(new CreateBridgeEndpoint(session), observer);
                MediaGatewayResponse<ActorRef> endpointResponse = expectMsgClass(MediaGatewayResponse.class);
                assertTrue(endpointResponse.succeeded());
                final ActorRef bridge = endpointResponse.get();
                // Create an ivr end point.
                gateway.tell(new CreateIvrEndpoint(session), observer);
                endpointResponse = expectMsgClass(MediaGatewayResponse.class);
                assertTrue(endpointResponse.succeeded());
                final ActorRef ivr = endpointResponse.get();
                // Create a link.
                gateway.tell(new CreateLink(session), observer);
                endpointResponse = expectMsgClass(MediaGatewayResponse.class);
                assertTrue(endpointResponse.succeeded());
                final ActorRef link = endpointResponse.get();
                // Start observing events from the link.
                link.tell(new Observe(observer), observer);
                final Observing observingResponse = expectMsgClass(Observing.class);
                assertTrue(observingResponse.succeeded());
                // Initialize the link.
                final InitializeLink initialize = new InitializeLink(bridge, ivr);
                link.tell(initialize, observer);
                LinkStateChanged event = expectMsgClass(LinkStateChanged.class);
                assertTrue(LinkStateChanged.State.CLOSED == event.state());
                // Open the link.
                link.tell(new OpenLink(ConnectionMode.SendRecv), observer);
                event = expectMsgClass(LinkStateChanged.class);
                assertTrue(LinkStateChanged.State.OPEN == event.state());
                // Modify the primary connection in the link.
                UpdateLink update = new UpdateLink(ConnectionMode.RecvOnly, UpdateLink.Type.PRIMARY);
                link.tell(update, observer);
                event = expectMsgClass(LinkStateChanged.class);
                assertTrue(LinkStateChanged.State.OPEN == event.state());
                // Modify the secondary connection in the link.
                update = new UpdateLink(ConnectionMode.SendOnly, UpdateLink.Type.SECONDARY);
                link.tell(update, observer);
                event = expectMsgClass(LinkStateChanged.class);
                assertTrue(LinkStateChanged.State.OPEN == event.state());
                // Close the link.
                link.tell(new CloseLink(), observer);
                event = expectMsgClass(LinkStateChanged.class);
                assertTrue(LinkStateChanged.State.CLOSED == event.state());
                // Stop observing events from the link.
                link.tell(new StopObserving(observer), observer);
            }
        };
    }

    private static final class MockMediaGateway extends AbstractMockMediaGateway {
        private final State primaryClosed;
        private final State secondaryClosed;
        private final State open;

        private final FiniteStateMachine fsm;

        // Some necessary state.
        private ActorRef sender;

        @SuppressWarnings("unused")
        public MockMediaGateway() {
            super();
            final ActorRef source = self();
            primaryClosed = new State("primaryClosed", new Closed(), null);
            secondaryClosed = new State("secondaryClosed", new Closed(), null);
            open = new State("open", new Open(), null);
            final Set<Transition> transitions = new HashSet<Transition>();
            transitions.add(new Transition(secondaryClosed, open));
            transitions.add(new Transition(open, open));
            transitions.add(new Transition(open, primaryClosed));
            transitions.add(new Transition(primaryClosed, secondaryClosed));
            fsm = new FiniteStateMachine(secondaryClosed, transitions);
        }

        @Override
        protected void event(final Object message, final ActorRef sender) {
            this.sender = sender;
            final State state = fsm.state();
            try {
                // Print the requests.
                if (message instanceof JainMgcpEvent) {
                    System.out.println(message.toString());
                }
                // FSM logic.
                final Class<?> klass = message.getClass();
                if (CreateConnection.class.equals(klass)) {
                    fsm.transition(message, open);
                } else if (ModifyConnection.class.equals(klass)) {
                    fsm.transition(message, open);
                } else if (DeleteConnection.class.equals(klass)) {
                    if (open.equals(state)) {
                        fsm.transition(message, primaryClosed);
                    } else if (primaryClosed.equals(state)) {
                        fsm.transition(message, secondaryClosed);
                    }
                }
            } catch (final Exception exception) {
                exception.printStackTrace();
            }
        }

        private final class Closed implements Action {
            public Closed() {
                super();
            }

            @Override
            public void execute(final Object message) throws Exception {
                final ActorRef self = self();
                final DeleteConnection request = (DeleteConnection) message;
                DeleteConnectionResponse response = null;
                if (request.getConnectionIdentifier().toString().equals("183")
                        && request.getEndpointIdentifier().toString().equals("mobicents/bridge/1@192.168.1.1:2427")) {
                    response = new DeleteConnectionResponse(this, ReturnCode.Transaction_Executed_Normally);
                } else if (request.getConnectionIdentifier().toString().equals("184")
                        && request.getEndpointIdentifier().toString().equals("mobicents/ivr/1@192.168.1.1:2427")) {
                    response = new DeleteConnectionResponse(this, ReturnCode.Transaction_Executed_Normally);
                } else {
                    response = new DeleteConnectionResponse(this, ReturnCode.Transient_Error);
                }
                response.setTransactionHandle(request.getTransactionHandle());
                sender.tell(response, self);
                System.out.println(response.toString());
            }
        }

        private final class Open implements Action {
            public Open() {
                super();
            }

            @Override
            public void execute(final Object message) throws Exception {
                final ActorRef self = self();
                final Class<?> klass = message.getClass();
                // Handle transitions from primaryClosed and half open.
                if (CreateConnection.class.equals(klass)) {
                    final CreateConnection request = (CreateConnection) message;
                    CreateConnectionResponse response = null;
                    // This is the new connection identifier and must be used to identify this connection.
                    final ConnectionIdentifier primaryConnId = new ConnectionIdentifier("183");
                    final ConnectionIdentifier secondaryConnId = new ConnectionIdentifier("184");
                    // This is the specific end point identifier and must be used to identify the end point
                    // being used by this connection for future communications.
                    final EndpointIdentifier primaryEndpointId = new EndpointIdentifier("mobicents/bridge/1", domain);
                    final EndpointIdentifier secondaryEndpointId = new EndpointIdentifier("mobicents/ivr/1", domain);
                    response = new CreateConnectionResponse(this, ReturnCode.Transaction_Executed_Normally, primaryConnId);
                    response.setSecondConnectionIdentifier(secondaryConnId);
                    response.setSpecificEndpointIdentifier(primaryEndpointId);
                    response.setSecondEndpointIdentifier(secondaryEndpointId);
                    response.setTransactionHandle(request.getTransactionHandle());
                    sender.tell(response, self);
                    System.out.println(response.toString());
                } else if (ModifyConnection.class.equals(klass)) {
                    final ModifyConnection request = (ModifyConnection) message;
                    ModifyConnectionResponse response = null;
                    if (request.getConnectionIdentifier().toString().equals("183")
                            && request.getEndpointIdentifier().toString().equals("mobicents/bridge/1@192.168.1.1:2427")) {
                        response = new ModifyConnectionResponse(this, ReturnCode.Transaction_Executed_Normally);
                    } else if (request.getConnectionIdentifier().toString().equals("184")
                            && request.getEndpointIdentifier().toString().equals("mobicents/ivr/1@192.168.1.1:2427")) {
                        response = new ModifyConnectionResponse(this, ReturnCode.Transaction_Executed_Normally);
                    } else {
                        response = new ModifyConnectionResponse(this, ReturnCode.Transient_Error);
                    }
                    response.setTransactionHandle(request.getTransactionHandle());
                    sender.tell(response, self);
                    System.out.println(response.toString());
                }
            }
        }
    }
}
