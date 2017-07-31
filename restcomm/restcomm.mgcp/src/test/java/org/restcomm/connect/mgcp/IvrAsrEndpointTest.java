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
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
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
import org.apache.commons.codec.binary.Hex;

import java.net.URI;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Dmitriy Nadolenko
 * @version 1.0
 * @since 1.0
 */
public class IvrAsrEndpointTest {

    private static final String ASR_RESULT_TEXT = "Super_text";
    private static final String ASR_RESULT_TEXT_HEX = Hex.encodeHexString(ASR_RESULT_TEXT.getBytes());
    private static final String HINTS = "hint 1, hint 2";

    private static ActorSystem system;

    public IvrAsrEndpointTest() {
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

    @Test
    @SuppressWarnings("unchecked")
    public void testSuccessfulAsrScenario() {
        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                // Create a new mock media gateway to simulate the real thing.
                final ActorRef gateway = system.actorOf(new Props(IvrAsrEndpointTest.MockAsrMediaGateway.class));
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

                AsrSignal asr = new AsrSignal("no_name_driver", "en-US", Collections.singletonList(URI.create("hello.wav")), "#", 10, 10, 10, HINTS, "dtmf_speech", 1);
                endpoint.tell(asr, observer);
                final IvrEndpointResponse ivrResponse = expectMsgClass(IvrEndpointResponse.class);
                assertTrue(ivrResponse.succeeded());
                assertTrue(ASR_RESULT_TEXT.equals(ivrResponse.get().getResult()));
                assertTrue(ivrResponse.get().isAsr());

                final IvrEndpointResponse ivrResponse2 = expectMsgClass(IvrEndpointResponse.class);
                assertTrue(ivrResponse2.succeeded());
                assertTrue(ASR_RESULT_TEXT.equals(ivrResponse2.get().getResult()));
                assertTrue(ivrResponse2.get().isAsr());


                final IvrEndpointResponse ivrResponse3 = expectMsgClass(IvrEndpointResponse.class);
                assertTrue(ivrResponse3.succeeded());
                assertTrue(ivrResponse3.get().getResult().isEmpty());
                assertTrue(ivrResponse2.get().isAsr());

                // Stop observing events from the IVR end point.
                endpoint.tell(new StopObserving(observer), observer);
            }
        };
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFailureScenario() {
        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                // Create a new mock media gateway to simulate the real thing.
                final ActorRef gateway = system.actorOf(new Props(IvrAsrEndpointTest.FailingMockAsrMediaGateway.class));
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

                AsrSignal asr = new AsrSignal("no_name_driver", "en-US", Collections.singletonList(URI.create("hello.wav")), "#", 10, 10, 10, HINTS, "dtmf_speech", 1);
                endpoint.tell(asr, observer);
                final IvrEndpointResponse ivrResponse = expectMsgClass(IvrEndpointResponse.class);
                assertFalse(ivrResponse.succeeded());
                String errorMessage = "jain.protocol.ip.mgcp.JainIPMgcpException: The IVR request failed with the following error code 300";
                assertTrue(ivrResponse.cause().toString().equals(errorMessage));
                assertTrue(ivrResponse.get() == null);
            }
        };
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEndSignal() {
        new JavaTestKit(system) {
            {
                final ActorRef observer = getRef();
                // Create a new mock media gateway to simulate the real thing.
                final ActorRef gateway = system.actorOf(new Props(MockAsrWithEndSignal.class));
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

                AsrSignal asr = new AsrSignal("no_name_driver", "en-US", Collections.singletonList(URI.create("hello.wav")), "#", 10, 10, 10, ASR_RESULT_TEXT, "dtmf_speech", 1);
                endpoint.tell(asr, observer);
                final IvrEndpointResponse ivrResponse = expectMsgClass(IvrEndpointResponse.class);
                assertTrue(ivrResponse.succeeded());
                assertTrue(ASR_RESULT_TEXT.equals(ivrResponse.get().getResult()));
                assertTrue(ivrResponse.get().isAsr());

                // EndSignal to IVR
                endpoint.tell(new StopEndpoint(AsrSignal.REQUEST_ASR), observer);

                final IvrEndpointResponse ivrResponse2 = expectMsgClass(IvrEndpointResponse.class);
                assertTrue(ivrResponse2.succeeded());

                // Stop observing events from the IVR end point.
                endpoint.tell(new StopObserving(observer), observer);
            }
        };
    }


    private static final class MockAsrMediaGateway extends AuAbstractMockMediaGateway {
        @SuppressWarnings("unused")
        public MockAsrMediaGateway() {
            super();
        }

        @Override
        protected void event(final Object message, final ActorRef sender) {
            final ActorRef self = self();
            final Class<?> klass = message.getClass();
            if (NotificationRequest.class.equals(klass)) {
                // Send a successful response for this request.
                final NotificationRequest request = (NotificationRequest) message;
                final JainMgcpResponseEvent response = new NotificationRequestResponse(this,
                        ReturnCode.Transaction_Executed_Normally);
                sender.tell(response, self);

                // Send the notification.
                Notify notify = createNotify(request, (int) transactionIdPool.get(), AUMgcpEvent.auoc.withParm("rc=101 asrr=" + ASR_RESULT_TEXT_HEX));
                sender.tell(notify, self);

                notify = createNotify(request, (int) transactionIdPool.get(), AUMgcpEvent.auoc.withParm("rc=101 asrr=" + ASR_RESULT_TEXT_HEX));
                sender.tell(notify, self);

                notify = createNotify(request, (int) transactionIdPool.get(), AUMgcpEvent.auoc.withParm("rc=100"));
                sender.tell(notify, self);
            }
        }
    }

    private static final class FailingMockAsrMediaGateway extends AuAbstractMockMediaGateway {
        @SuppressWarnings("unused")
        public FailingMockAsrMediaGateway() {
            super();
        }

        @Override
        protected void event(final Object message, final ActorRef sender) {
            final ActorRef self = self();
            final Class<?> klass = message.getClass();
            if (NotificationRequest.class.equals(klass)) {
                // Send a successful response for this request.
                final NotificationRequest request = (NotificationRequest) message;
                final JainMgcpResponseEvent response = new NotificationRequestResponse(this,
                        ReturnCode.Transaction_Executed_Normally);
                response.setTransactionHandle(request.getTransactionHandle());
                sender.tell(response, self);

                // Send the notification.
                final Notify notify = createNotify(request, (int) transactionIdPool.get(), AUMgcpEvent.auof.withParm("rc=300"));
                sender.tell(notify, self);
            }
        }
    }

    private static final class MockAsrWithEndSignal extends AuAbstractMockMediaGateway {

        @SuppressWarnings("unused")
        public MockAsrWithEndSignal() {
            super();
        }

        @Override
        protected void event(final Object message, final ActorRef sender) {
            final ActorRef self = self();
            final Class<?> klass = message.getClass();
            if (NotificationRequest.class.equals(klass)) {
                // Send a successful response for this request.
                final NotificationRequest request = (NotificationRequest) message;
                if ("AU/es(sg=asr)".equals(request.getSignalRequests()[0].toString())) {
                    //handle stop request
                    final JainMgcpResponseEvent response = new NotificationRequestResponse(this, ReturnCode.Transaction_Executed_Normally);
                    sender.tell(response, self);

                    Notify notify = createNotify(request, (int) transactionIdPool.get(), AUMgcpEvent.auoc.withParm("rc=100"));
                    sender.tell(notify, self);
                    return;
                }
                final JainMgcpResponseEvent response = new NotificationRequestResponse(this, ReturnCode.Transaction_Executed_Normally);
                sender.tell(response, self);
                // Send the notification.
                Notify notify = createNotify(request, (int) transactionIdPool.get(), AUMgcpEvent.auoc.withParm("rc=101 asrr=" + ASR_RESULT_TEXT_HEX));
                sender.tell(notify, self);
            }
        }
    }

    private static abstract class AuAbstractMockMediaGateway extends AbstractMockMediaGateway {

        protected Notify createNotify(final NotificationRequest request, int transactionId, final MgcpEvent event) {
            final EventName[] events = {new EventName(AUPackage.AU, event)};
            Notify notify = new Notify(this, request.getEndpointIdentifier(), request.getRequestIdentifier(), events);
            notify.setTransactionHandle(transactionId);
            return notify;
        }

    }
}
