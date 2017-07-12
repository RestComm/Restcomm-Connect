/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package org.restcomm.connect.mgcp;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.JavaTestKit;
import jain.protocol.ip.mgcp.JainMgcpEvent;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.DeleteConnection;
import jain.protocol.ip.mgcp.message.DeleteConnectionResponse;
import jain.protocol.ip.mgcp.message.NotificationRequest;
import jain.protocol.ip.mgcp.message.NotificationRequestResponse;
import jain.protocol.ip.mgcp.message.Notify;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.protocols.mgcp.jain.pkg.AUMgcpEvent;
import org.mobicents.protocols.mgcp.jain.pkg.AUPackage;
import org.restcomm.connect.commons.patterns.Observe;
import org.restcomm.connect.commons.patterns.Observing;
import org.restcomm.connect.commons.patterns.StopObserving;

import static org.junit.Assert.assertTrue;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
public class EndpointTest {
    private static ActorSystem system;

    public EndpointTest() {
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


    /**
     * https://github.com/RestComm/Restcomm-Connect/issues/2310
     */
    @Test
    public void testDeleteEnpointSuccessScenario() {
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
                // Create an IVR end point.
                gateway.tell(new CreateIvrEndpoint(session, "mobicents/ivr/1"), observer);
                final MediaGatewayResponse<ActorRef> endpointResponse = expectMsgClass(MediaGatewayResponse.class);
                assertTrue(endpointResponse.succeeded());
                final ActorRef endpoint = endpointResponse.get();
                // Start observing events from the IVR end point.
                endpoint.tell(new Observe(observer), observer);
                final Observing observingResponse = expectMsgClass(Observing.class);
                assertTrue(observingResponse.succeeded());

                // Destroy Endpoint
                endpoint.tell(new DestroyEndpoint(), observer);
                
                final EndpointStateChanged endpointStateChanged = expectMsgClass(EndpointStateChanged.class);
                assertTrue(endpointStateChanged.getState().equals(EndpointState.DESTROYED));
                // Stop observing events from the IVR end point.
                endpoint.tell(new StopObserving(observer), observer);
            }
        };
    }


    /**
     * https://github.com/RestComm/Restcomm-Connect/issues/2310
     */
    @Test
    public void testDeleteEnpointFailureScenario() {
        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                // Create a new mock media gateway to simulate the real thing.
                final ActorRef gateway = system.actorOf(new Props(FailingMockMediaGateway.class));
                // Create a media session. This is just an identifier that groups
                // a set of end points, connections, and lists in to one call.
                gateway.tell(new CreateMediaSession(), observer);
                final MediaGatewayResponse<MediaSession> mediaSessionResponse = expectMsgClass(MediaGatewayResponse.class);
                assertTrue(mediaSessionResponse.succeeded());
                final MediaSession session = mediaSessionResponse.get();
                // Create an IVR end point.
                gateway.tell(new CreateIvrEndpoint(session, "mobicents/ivr/1"), observer);
                final MediaGatewayResponse<ActorRef> endpointResponse = expectMsgClass(MediaGatewayResponse.class);
                assertTrue(endpointResponse.succeeded());
                final ActorRef endpoint = endpointResponse.get();
                // Start observing events from the IVR end point.
                endpoint.tell(new Observe(observer), observer);
                final Observing observingResponse = expectMsgClass(Observing.class);
                assertTrue(observingResponse.succeeded());

                // Destroy Endpoint
                endpoint.tell(new DestroyEndpoint(), observer);
                
                final EndpointStateChanged endpointStateChanged = expectMsgClass(EndpointStateChanged.class);
                assertTrue(endpointStateChanged.getState().equals(EndpointState.FAILED));
                // Stop observing events from the IVR end point.
                endpoint.tell(new StopObserving(observer), observer);
            }
        };
    }

    private static final class MockMediaGateway extends AbstractMockMediaGateway {
        @SuppressWarnings("unused")
        public MockMediaGateway() {
            super();
        }

        @Override
        protected void event(final Object message, final ActorRef sender) {
            final ActorRef self = self();
            if (message instanceof JainMgcpEvent) {
                System.out.println(message.toString());
            }
            final Class<?> klass = message.getClass();
            if (NotificationRequest.class.equals(klass)) {
                // Send a successful response for this request.
                final NotificationRequest request = (NotificationRequest) message;
                final JainMgcpResponseEvent response = new NotificationRequestResponse(this,
                        ReturnCode.Transaction_Executed_Normally);
                sender.tell(response, self);
                System.out.println(response.toString());
                // Send the notification.
                final MgcpEvent event = AUMgcpEvent.auoc.withParm("rc=100 dc=1");
                final EventName[] events = { new EventName(AUPackage.AU, event) };
                final Notify notify = new Notify(this, request.getEndpointIdentifier(), request.getRequestIdentifier(), events);
                notify.setTransactionHandle((int) transactionIdPool.get());
                sender.tell(notify, self);
                System.out.println(notify.toString());
            }else if(DeleteConnection.class.equals(klass)){
            	final JainMgcpResponseEvent response = new DeleteConnectionResponse(this,
                        ReturnCode.Transaction_Executed_Normally);
                sender.tell(response, self);
            }else{
            	System.out.println("inside else");
            }
        }
    }

    private static final class FailingMockMediaGateway extends AbstractMockMediaGateway {
        @SuppressWarnings("unused")
        public FailingMockMediaGateway() {
            super();
        }

        @Override
        protected void event(final Object message, final ActorRef sender) {
            final ActorRef self = self();
            if (message instanceof JainMgcpEvent) {
                System.out.println("event received: "+message.toString());
            }
            final Class<?> klass = message.getClass();
            if (NotificationRequest.class.equals(klass)) {
                // Send a successful response for this request.
                final NotificationRequest request = (NotificationRequest) message;
                final JainMgcpResponseEvent response = new NotificationRequestResponse(this,
                        ReturnCode.Transaction_Executed_Normally);
                response.setTransactionHandle(request.getTransactionHandle());
                sender.tell(response, self);
                System.out.println(response.toString());
                // Send the notification.
                final MgcpEvent event = AUMgcpEvent.auoc.withParm("rc=300");
                final EventName[] events = { new EventName(AUPackage.AU, event) };
                final Notify notify = new Notify(this, request.getEndpointIdentifier(), request.getRequestIdentifier(), events);
                notify.setTransactionHandle((int) transactionIdPool.get());
                sender.tell(notify, self);
                System.out.println(notify.toString());
            } else if(DeleteConnection.class.equals(klass)){
            	final JainMgcpResponseEvent response = new DeleteConnectionResponse(this,
                        ReturnCode.Endpoint_Unknown);
                sender.tell(response, self);
            }else{
            	System.out.println("inside else");
            }
        }
    }
}
