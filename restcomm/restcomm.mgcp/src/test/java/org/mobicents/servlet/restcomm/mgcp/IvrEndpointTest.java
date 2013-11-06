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
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.NotificationRequest;
import jain.protocol.ip.mgcp.message.NotificationRequestResponse;
import jain.protocol.ip.mgcp.message.Notify;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.protocols.mgcp.jain.pkg.AUMgcpEvent;
import org.mobicents.protocols.mgcp.jain.pkg.AUPackage;
import org.mobicents.servlet.restcomm.mgcp.CreateIvrEndpoint;
import org.mobicents.servlet.restcomm.mgcp.CreateMediaSession;
import org.mobicents.servlet.restcomm.mgcp.IvrEndpointResponse;
import org.mobicents.servlet.restcomm.mgcp.MediaGatewayResponse;
import org.mobicents.servlet.restcomm.mgcp.MediaSession;
import org.mobicents.servlet.restcomm.mgcp.Play;
import org.mobicents.servlet.restcomm.mgcp.PlayCollect;
import org.mobicents.servlet.restcomm.patterns.Observe;
import org.mobicents.servlet.restcomm.patterns.Observing;
import org.mobicents.servlet.restcomm.patterns.StopObserving;

/**
 * @author thomas.quintana@telestax.com (Thomas Quintana)
 */
public class IvrEndpointTest {
    private static ActorSystem system;

    public IvrEndpointTest() {
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
                // a set of end points, connections, and lists in to one call.
                gateway.tell(new CreateMediaSession(), observer);
                final MediaGatewayResponse<MediaSession> mediaSessionResponse = expectMsgClass(MediaGatewayResponse.class);
                assertTrue(mediaSessionResponse.succeeded());
                final MediaSession session = mediaSessionResponse.get();
                // Create an IVR end point.
                gateway.tell(new CreateIvrEndpoint(session), observer);
                final MediaGatewayResponse<ActorRef> endpointResponse = expectMsgClass(MediaGatewayResponse.class);
                assertTrue(endpointResponse.succeeded());
                final ActorRef endpoint = endpointResponse.get();
                // Start observing events from the IVR end point.
                endpoint.tell(new Observe(observer), observer);
                final Observing observingResponse = expectMsgClass(Observing.class);
                assertTrue(observingResponse.succeeded());
                // Play some audio.
                final List<URI> announcements = new ArrayList<URI>();
                announcements.add(URI.create("hello.wav"));
                final Play play = new Play(announcements, 1);
                endpoint.tell(play, observer);
                final IvrEndpointResponse<String> ivrResponse = expectMsgClass(IvrEndpointResponse.class);
                assertTrue(ivrResponse.succeeded());
                // Stop observing events from the IVR end point.
                endpoint.tell(new StopObserving(observer), observer);
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSuccessfulScenarioWithDigits() {
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
                gateway.tell(new CreateIvrEndpoint(session), observer);
                final MediaGatewayResponse<ActorRef> endpointResponse = expectMsgClass(MediaGatewayResponse.class);
                assertTrue(endpointResponse.succeeded());
                final ActorRef endpoint = endpointResponse.get();
                // Start observing events from the IVR end point.
                endpoint.tell(new Observe(observer), observer);
                final Observing observingResponse = expectMsgClass(Observing.class);
                assertTrue(observingResponse.succeeded());
                // Play some audio and collect digits.
                final PlayCollect.Builder builder = PlayCollect.builder();
                builder.addPrompt(URI.create("hello.wav"));
                final PlayCollect playCollect = builder.build();
                endpoint.tell(playCollect, observer);
                final IvrEndpointResponse<String> ivrResponse = expectMsgClass(IvrEndpointResponse.class);
                assertTrue(ivrResponse.succeeded());
                assertTrue("1".equals(ivrResponse.get()));
                // Stop observing events from the IVR end point.
                endpoint.tell(new StopObserving(observer), observer);
            }
        };
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testFailureScenario() {
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
                gateway.tell(new CreateIvrEndpoint(session), observer);
                final MediaGatewayResponse<ActorRef> endpointResponse = expectMsgClass(MediaGatewayResponse.class);
                assertTrue(endpointResponse.succeeded());
                final ActorRef endpoint = endpointResponse.get();
                // Start observing events from the IVR end point.
                endpoint.tell(new Observe(observer), observer);
                final Observing observingResponse = expectMsgClass(Observing.class);
                assertTrue(observingResponse.succeeded());
                // Play some audio.
                final List<URI> announcements = new ArrayList<URI>();
                announcements.add(URI.create("hello.wav"));
                final Play play = new Play(announcements, 1);
                endpoint.tell(play, observer);
                final IvrEndpointResponse<String> ivrResponse = expectMsgClass(IvrEndpointResponse.class);
                assertFalse(ivrResponse.succeeded());
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
                System.out.println(message.toString());
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
            }
        }
    }
}
