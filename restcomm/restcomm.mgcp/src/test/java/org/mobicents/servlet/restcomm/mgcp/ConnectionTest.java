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
import jain.protocol.ip.mgcp.message.parms.ConnectionDescriptor;
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

import org.mobicents.servlet.restcomm.mgcp.CloseConnection;
import org.mobicents.servlet.restcomm.mgcp.ConnectionStateChanged;
import org.mobicents.servlet.restcomm.mgcp.CreateMediaSession;
import org.mobicents.servlet.restcomm.mgcp.CreatePacketRelayEndpoint;
import org.mobicents.servlet.restcomm.mgcp.InitializeConnection;
import org.mobicents.servlet.restcomm.mgcp.MediaGatewayResponse;
import org.mobicents.servlet.restcomm.mgcp.MediaSession;
import org.mobicents.servlet.restcomm.mgcp.OpenConnection;
import org.mobicents.servlet.restcomm.mgcp.UpdateConnection;
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
public final class ConnectionTest {
    private static final String sdp = "v=0\n" + "o=- 1362546170756 1 IN IP4 192.168.1.100\n" + "s=Mobicents Media Server\n"
            + "c=IN IP4 192.168.1.100\n" + "t=0 0\n" + "m=audio 63044 RTP/AVP 97 8 0 101\n" + "a=rtpmap:97 l16/8000\n"
            + "a=rtpmap:8 pcma/8000\n" + "a=rtpmap:0 pcmu/8000\n" + "a=rtpmap:101 telephone-event/8000\n" + "a=fmtp:101 0-15\n";

    private static ActorSystem system;

    public ConnectionTest() {
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
    public void testSuccessfulScenarioWithOutInitialSdp() {
        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                // Create a new mock media gateway to simulate the real thing.
                final ActorRef gateway = system.actorOf(new Props(MockMediaGateway.class));
                // Create a media session. This is just an identifier that groups
                // a set of end points, connections, and lists in to one call.
                gateway.tell(new CreateMediaSession(), observer);
                final MediaGatewayResponse<MediaSession> mediaSessionResponse = expectMsgClass(MediaGatewayResponse.class);
                assertTrue(mediaSessionResponse.succeeded());
                final MediaSession session = mediaSessionResponse.get();
                // Create an end point.
                gateway.tell(new CreatePacketRelayEndpoint(session), observer);
                final MediaGatewayResponse<ActorRef> endpointResponse = expectMsgClass(MediaGatewayResponse.class);
                assertTrue(endpointResponse.succeeded());
                final ActorRef endpoint = endpointResponse.get();
                // Create a connection.
                // The CreateConnection message collides with CreateConnection from JAIN MGCP so the full
                // class path is required to avoid confusing the JVM.
                gateway.tell(new org.mobicents.servlet.restcomm.mgcp.CreateConnection(session), observer);
                final MediaGatewayResponse<ActorRef> connectionResponse = expectMsgClass(MediaGatewayResponse.class);
                assertTrue(connectionResponse.succeeded());
                final ActorRef connection = connectionResponse.get();
                // Start observing events from the connection.
                connection.tell(new Observe(observer), observer);
                final Observing observingResponse = expectMsgClass(Observing.class);
                assertTrue(observingResponse.succeeded());
                // Initialize the connection.
                connection.tell(new InitializeConnection(endpoint), observer);
                ConnectionStateChanged event = expectMsgClass(ConnectionStateChanged.class);
                assertTrue(ConnectionStateChanged.State.CLOSED == event.state());
                // Open the connection half way by not passing an sdp.
                final OpenConnection open = new OpenConnection(ConnectionMode.SendRecv);
                connection.tell(open, observer);
                event = expectMsgClass(ConnectionStateChanged.class);
                assertTrue(ConnectionStateChanged.State.HALF_OPEN == event.state());
                assertTrue(event.descriptor() != null);
                // Open the connection by updating the sdp.
                final UpdateConnection update = new UpdateConnection(new ConnectionDescriptor(sdp));
                connection.tell(update, observer);
                event = expectMsgClass(ConnectionStateChanged.class);
                assertTrue(ConnectionStateChanged.State.OPEN == event.state());
                assertTrue(event.descriptor() != null);
                // Close the connection.
                connection.tell(new CloseConnection(), observer);
                event = expectMsgClass(ConnectionStateChanged.class);
                assertTrue(ConnectionStateChanged.State.CLOSED == event.state());
                // Stop observing events from the connection.
                connection.tell(new StopObserving(observer), observer);
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSuccessfulScenarioWithInitialSdp() {
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
                // Create an end point.
                gateway.tell(new CreatePacketRelayEndpoint(session), observer);
                final MediaGatewayResponse<ActorRef> endpointResponse = expectMsgClass(MediaGatewayResponse.class);
                assertTrue(endpointResponse.succeeded());
                final ActorRef endpoint = endpointResponse.get();
                // Create a connection.
                // The CreateConnection message collides with CreateConnection from JAIN MGCP so the full
                // class path is required to avoid confusing the JVM.
                gateway.tell(new org.mobicents.servlet.restcomm.mgcp.CreateConnection(session), observer);
                final MediaGatewayResponse<ActorRef> connectionResponse = expectMsgClass(MediaGatewayResponse.class);
                assertTrue(connectionResponse.succeeded());
                final ActorRef connection = connectionResponse.get();
                // Start observing events from the connection.
                connection.tell(new Observe(observer), observer);
                final Observing observingResponse = expectMsgClass(Observing.class);
                assertTrue(observingResponse.succeeded());
                // Initialize the connection.
                connection.tell(new InitializeConnection(endpoint), observer);
                ConnectionStateChanged event = expectMsgClass(ConnectionStateChanged.class);
                assertTrue(ConnectionStateChanged.State.CLOSED == event.state());
                // Open the connection with an sdp.
                final ConnectionDescriptor descriptor = new ConnectionDescriptor(sdp);
                final OpenConnection open = new OpenConnection(descriptor, ConnectionMode.SendRecv);
                connection.tell(open, observer);
                event = expectMsgClass(ConnectionStateChanged.class);
                assertTrue(ConnectionStateChanged.State.OPEN == event.state());
                assertTrue(event.descriptor() != null);
                // Close the connection.
                connection.tell(new CloseConnection(), observer);
                event = expectMsgClass(ConnectionStateChanged.class);
                assertTrue(ConnectionStateChanged.State.CLOSED == event.state());
                // Stop observing events from the connection.
                connection.tell(new StopObserving(observer), observer);
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSuccessfulScenarioWithInitialSdpWithModify() {
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
                // Create an end point.
                gateway.tell(new CreatePacketRelayEndpoint(session), observer);
                final MediaGatewayResponse<ActorRef> endpointResponse = expectMsgClass(MediaGatewayResponse.class);
                assertTrue(endpointResponse.succeeded());
                final ActorRef endpoint = endpointResponse.get();
                // Create a connection.
                // The CreateConnection message collides with CreateConnection from JAIN MGCP so the full
                // class path is required to avoid confusing the JVM.
                gateway.tell(new org.mobicents.servlet.restcomm.mgcp.CreateConnection(session), observer);
                final MediaGatewayResponse<ActorRef> connectionResponse = expectMsgClass(MediaGatewayResponse.class);
                assertTrue(connectionResponse.succeeded());
                final ActorRef connection = connectionResponse.get();
                // Start observing events from the connection.
                connection.tell(new Observe(observer), observer);
                final Observing observingResponse = expectMsgClass(Observing.class);
                assertTrue(observingResponse.succeeded());
                // Initialize the connection.
                connection.tell(new InitializeConnection(endpoint), observer);
                ConnectionStateChanged event = expectMsgClass(ConnectionStateChanged.class);
                assertTrue(ConnectionStateChanged.State.CLOSED == event.state());
                // Open the connection with an sdp.
                final ConnectionDescriptor descriptor = new ConnectionDescriptor(sdp);
                final OpenConnection open = new OpenConnection(descriptor, ConnectionMode.SendRecv);
                connection.tell(open, observer);
                event = expectMsgClass(ConnectionStateChanged.class);
                assertTrue(ConnectionStateChanged.State.OPEN == event.state());
                assertTrue(event.descriptor() != null);
                // Modify the connection.
                final UpdateConnection update = new UpdateConnection(ConnectionMode.RecvOnly);
                connection.tell(update, observer);
                event = expectMsgClass(ConnectionStateChanged.class);
                assertTrue(ConnectionStateChanged.State.OPEN == event.state());
                assertTrue(event.descriptor() != null);
                // Close the connection.
                connection.tell(new CloseConnection(), observer);
                event = expectMsgClass(ConnectionStateChanged.class);
                assertTrue(ConnectionStateChanged.State.CLOSED == event.state());
                // Stop observing events from the connection.
                connection.tell(new StopObserving(observer), observer);
            }
        };
    }

    private static final class MockMediaGateway extends AbstractMockMediaGateway {
        private final State closed;
        private final State halfOpen;
        private final State open;

        private final FiniteStateMachine fsm;

        // Some necessary state.
        private ActorRef sender;

        @SuppressWarnings("unused")
        public MockMediaGateway() {
            super();
            final ActorRef source = self();
            closed = new State("closed", new Closed(), null);
            halfOpen = new State("half open", new Open(), null);
            open = new State("open", new Open(), null);
            final Set<Transition> transitions = new HashSet<Transition>();
            transitions.add(new Transition(closed, halfOpen));
            transitions.add(new Transition(closed, open));
            transitions.add(new Transition(halfOpen, open));
            transitions.add(new Transition(open, open));
            transitions.add(new Transition(open, closed));
            fsm = new FiniteStateMachine(closed, transitions);
        }

        @Override
        protected void event(final Object message, final ActorRef sender) {
            this.sender = sender;
            try {
                // Print the requests.
                if (message instanceof JainMgcpEvent) {
                    System.out.println(message.toString());
                }
                // FSM logic.
                final Class<?> klass = message.getClass();
                if (CreateConnection.class.equals(klass)) {
                    final CreateConnection request = (CreateConnection) message;
                    if (request.getRemoteConnectionDescriptor() == null) {
                        fsm.transition(message, halfOpen);
                    } else {
                        fsm.transition(message, open);
                    }
                } else if (ModifyConnection.class.equals(klass)) {
                    fsm.transition(message, open);
                } else if (DeleteConnection.class.equals(klass)) {
                    fsm.transition(message, closed);
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
                if (!request.getConnectionIdentifier().toString().equals("183")) {
                    response = new DeleteConnectionResponse(this, ReturnCode.Incorrect_Connection_ID);
                } else if (!request.getEndpointIdentifier().toString().equals("mobicents/relay/1@192.168.1.1:2427")) {
                    response = new DeleteConnectionResponse(this, ReturnCode.Endpoint_Unknown);
                } else {
                    response = new DeleteConnectionResponse(this, ReturnCode.Transaction_Executed_Normally);
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
                final Class<?> klass = message.getClass();
                final ActorRef self = self();
                final ConnectionDescriptor descriptor = new ConnectionDescriptor(sdp);
                // Handle transitions from closed and half open.
                if (CreateConnection.class.equals(klass)) {
                    final CreateConnection request = (CreateConnection) message;
                    CreateConnectionResponse response = null;
                    // This is the new connection identifier and must be used to identify this connection.
                    final ConnectionIdentifier connId = new ConnectionIdentifier("183");
                    // This is the specific end point identifier and must be used to identify the end point
                    // being used by this connection for future communications.
                    final EndpointIdentifier endpointId = new EndpointIdentifier("mobicents/relay/1", domain);
                    response = new CreateConnectionResponse(this, ReturnCode.Transaction_Executed_Normally, connId);
                    response.setSpecificEndpointIdentifier(endpointId);
                    response.setLocalConnectionDescriptor(descriptor);
                    response.setTransactionHandle(request.getTransactionHandle());
                    sender.tell(response, self);
                    System.out.println(response.toString());
                } else if (ModifyConnection.class.equals(klass)) {
                    final ModifyConnection request = (ModifyConnection) message;
                    ModifyConnectionResponse response = null;
                    if (!request.getConnectionIdentifier().toString().equals("183")) {
                        response = new ModifyConnectionResponse(this, ReturnCode.Incorrect_Connection_ID);
                    } else if (!request.getEndpointIdentifier().toString().equals("mobicents/relay/1@192.168.1.1:2427")) {
                        response = new ModifyConnectionResponse(this, ReturnCode.Endpoint_Unknown);
                    } else {
                        response = new ModifyConnectionResponse(this, ReturnCode.Transaction_Executed_Normally);
                        response.setLocalConnectionDescriptor(descriptor);
                    }
                    response.setTransactionHandle(request.getTransactionHandle());
                    sender.tell(response, self);
                    System.out.println(response.toString());
                }
            }
        }
    }
}
