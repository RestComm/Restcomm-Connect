package org.restcomm.connect.telephony;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.connect.commons.configuration.RestcommConfiguration;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.commons.patterns.Observe;
import org.restcomm.connect.commons.patterns.Observing;
import org.restcomm.connect.dao.CallDetailRecordsDao;
import org.restcomm.connect.dao.ConferenceDetailRecordsDao;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.entities.CallDetailRecord;
import org.restcomm.connect.http.client.CallApiResponse;
import org.restcomm.connect.http.client.api.CallApiClient;
import org.restcomm.connect.mscontrol.api.MediaServerControllerFactory;
import org.restcomm.connect.mscontrol.api.messages.JoinComplete;
import org.restcomm.connect.mscontrol.api.messages.Leave;
import org.restcomm.connect.mscontrol.api.messages.Left;
import org.restcomm.connect.mscontrol.mms.MockFailingMmsControllerFactory;
import org.restcomm.connect.mscontrol.mms.MockMmsControllerFactory;
import org.restcomm.connect.telephony.api.AddParticipant;
import org.restcomm.connect.telephony.api.ConferenceCenterResponse;
import org.restcomm.connect.telephony.api.ConferenceInfo;
import org.restcomm.connect.telephony.api.ConferenceResponse;
import org.restcomm.connect.telephony.api.ConferenceStateChanged;
import org.restcomm.connect.telephony.api.CreateConference;
import org.restcomm.connect.telephony.api.StopConference;
import org.restcomm.connect.telephony.util.ConferenceTestUtil;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.testkit.JavaTestKit;
import scala.concurrent.duration.FiniteDuration;

public class ConferenceTest extends ConferenceTestUtil{
	private final static Logger logger = Logger.getLogger(ConferenceTest.class.getName());
    private static final Sid TEST_CALL_SID = new Sid("ID8deb35fc5121429fa96635aebe3976d2-CA6d61e3877f3c47828a26efc498a9e8f9");
    private static final String TEST_CALL_URI = "/2012-04-24/Accounts/ACae6e420f425248d6a26948c17a9e2acf/Calls/ID8deb35fc5121429fa96635aebe3976d2-CA6d61e3877f3c47828a26efc498a9e8f9";


	@Before
    public void before() throws UnknownHostException, ConfigurationException, MalformedURLException {
        configurationNode1 = createCfg(CONFIG_PATH_NODE_1);
        
        startDaoManager();
    }

	@After
	public void after(){
        try {
			daoManager.shutdown();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
    @Test
	public void testJoinStoppingConference() {
        new JavaTestKit(system) {
            {
                final ActorRef tester = getRef();
                // Create MockFailingMmsControllerFactory
                MediaServerControllerFactory factory = new MockFailingMmsControllerFactory(system, null);
                // Create ConferenceCenter
                final ActorRef conferenceCenter = conferenceCenter(factory, daoManager);

                // get a fresh conference from conferenecneter
                final CreateConference create = new CreateConference(CONFERENCE_FRIENDLY_NAME_1, new Sid(CALL_SID));
                conferenceCenter.tell(create, tester);
                ConferenceCenterResponse conferenceCenterResponse = expectMsgClass(ConferenceCenterResponse.class);
                ActorRef conferene = conferenceCenterResponse.get();
                
                // start observing conference
                conferene.tell(new Observe(tester), tester);
                Observing observingResponse = expectMsgClass(Observing.class);
                assertTrue(observingResponse.succeeded());

                // addparticipant in conference
                conferene.tell(new AddParticipant(tester), tester);
                //receieve sent to observers
                expectMsgClass(ConferenceResponse.class);
                //receieve sent to call (since we are pretending to call&VoiceInterpreter)
                expectMsgClass(ConferenceResponse.class);
                expectMsgClass(JoinComplete.class);

                // stop conference
                conferene.tell(new Left(), tester);
                expectMsgClass(ConferenceResponse.class);
                
                // get same conference again from conferenecneter
                conferenceCenter.tell(create, tester);
                conferenceCenterResponse = expectMsgClass(ConferenceCenterResponse.class);
                ActorRef conferene2 = conferenceCenterResponse.get();

                if(!conferene2.path().equals(conferene.path())){
                	assertTrue(!conferene2.path().equals(conferene.path()));
                    // start observing conference
                    conferene2.tell(new Observe(tester), tester);
                    observingResponse = expectMsgClass(Observing.class);
                    assertTrue(observingResponse.succeeded());

                    // addparticipant in conference
                    conferene2.tell(new AddParticipant(tester), tester);

                    expectMsgClass(ConferenceResponse.class);
                    expectMsgClass(ConferenceResponse.class);
                    expectMsgClass(JoinComplete.class);
                }else{
                	logger.info("testing VI impl");
                    // start observing conference
                    conferene2.tell(new Observe(tester), tester);
                    observingResponse = expectMsgClass(Observing.class);
                    assertTrue(observingResponse.succeeded());

                    // addparticipant in conference
                    conferene2.tell(new AddParticipant(tester), tester);

                    ConferenceStateChanged csc = expectMsgClass(ConferenceStateChanged.class);
                    assertTrue(csc.state().equals(ConferenceStateChanged.State.STOPPING));
                    
                    // get same conference again from conferenecneter
                    conferenceCenter.tell(create, tester);
                    conferenceCenterResponse = expectMsgClass(ConferenceCenterResponse.class);
                    conferene2 = conferenceCenterResponse.get();
                    
                    assertTrue(!conferene2.path().equals(conferene.path()));
                }
                
            	
            }};
	}
	
    @Test
	public void testJoinCompletedConference() throws URISyntaxException {
        new JavaTestKit(system) {
            {
            	daoManager = mock(DaoManager.class);
            	CallDetailRecordsDao callDetailRecordsDao = mock(CallDetailRecordsDao.class);
            	ConferenceDetailRecordsDao conferenceDetailRecordsDao = mock(ConferenceDetailRecordsDao.class);
            	when(callDetailRecordsDao.getTotalRunningCallDetailRecordsByConferenceSid(any(Sid.class))).thenReturn(0);
            	when(daoManager.getCallDetailRecordsDao()).thenReturn(callDetailRecordsDao);
            	when(daoManager.getConferenceDetailRecordsDao()).thenReturn(conferenceDetailRecordsDao);
            	
            	
                final ActorRef tester = getRef();
                // Create MockFailingMmsControllerFactory
                MediaServerControllerFactory factory = new MockMmsControllerFactory(system, null);
                // Create ConferenceCenter
                final ActorRef conferenceCenter = conferenceCenter(factory, daoManager);

                // get a fresh conference from conferenecneter
                final CreateConference create = new CreateConference(CONFERENCE_FRIENDLY_NAME_1, new Sid(CALL_SID));
                conferenceCenter.tell(create, tester);
                ConferenceCenterResponse conferenceCenterResponse = expectMsgClass(ConferenceCenterResponse.class);
                ActorRef conferene = conferenceCenterResponse.get();
                
                // start observing conference
                conferene.tell(new Observe(tester), tester);
                Observing observingResponse = expectMsgClass(Observing.class);
                assertTrue(observingResponse.succeeded());

                // addparticipant in conference
                conferene.tell(new AddParticipant(tester), tester);
                //receieve sent to observers
                expectMsgClass(ConferenceResponse.class);
                //receieve sent to call (since we are pretending to call&VoiceInterpreter)
                expectMsgClass(ConferenceResponse.class);
                expectMsgClass(JoinComplete.class);

                // stop conference
                conferene.tell(new Left(), tester);
                expectMsgClass(ConferenceResponse.class);
                expectMsgClass(ConferenceStateChanged.class);
                
                // get same conference again from conferenecneter
                conferenceCenter.tell(create, tester);
                conferenceCenterResponse = expectMsgClass(ConferenceCenterResponse.class);
                logger.info("conferenceCenterResponse 2: "+conferenceCenterResponse);
                ActorRef conferene2 = conferenceCenterResponse.get();

                assertTrue(!conferene2.path().equals(conferene.path()));
                // start observing conference
                conferene2.tell(new Observe(tester), tester);
                observingResponse = expectMsgClass(Observing.class);
                assertTrue(observingResponse.succeeded());

                // addparticipant in conference
                conferene2.tell(new AddParticipant(tester), tester);

                expectMsgClass(ConferenceResponse.class);
                expectMsgClass(ConferenceResponse.class);
                expectMsgClass(JoinComplete.class);
            	
            }};
	}
	
    @Test
	public void testConferenceTimeout() throws InterruptedException, URISyntaxException {
        new JavaTestKit(system) {
            {
            	FiniteDuration finiteDuration =FiniteDuration.apply(15, TimeUnit.SECONDS);
            	daoManager = mock(DaoManager.class);
            	ConferenceDetailRecordsDao conferenceDetailRecordsDao = mock(ConferenceDetailRecordsDao.class);
            	CallDetailRecordsDao callDetailRecordsDao = mock(CallDetailRecordsDao.class);
            	when(callDetailRecordsDao.getRunningCallDetailRecordsByConferenceSid(any(Sid.class))).thenReturn(mockedRemoteParticipants());
            	when(daoManager.getConferenceDetailRecordsDao()).thenReturn(conferenceDetailRecordsDao);
            	when(daoManager.getCallDetailRecordsDao()).thenReturn(callDetailRecordsDao);

            	// set conference timeout to 10 seconds
            	RestcommConfiguration.getInstance().getMain().setConferenceTimeout(10);

            	final ActorRef tester = getRef();
                // Create MockMmsControllerFactory
                MediaServerControllerFactory factory = new MockMmsControllerFactory(system, null);
                // Create ConferenceCenter
                final ActorRef conferenceCenter = conferenceCenter(factory, daoManager);

                // get a fresh conference from conferenecneter
                final CreateConference create = new CreateConference(CONFERENCE_FRIENDLY_NAME_1, new Sid(CALL_SID));
                conferenceCenter.tell(create, tester);
                ConferenceCenterResponse conferenceCenterResponse = expectMsgClass(ConferenceCenterResponse.class);
                ActorRef conferene = conferenceCenterResponse.get();

                // start observing conference
                conferene.tell(new Observe(tester), tester);
                Observing observingResponse = expectMsgClass(Observing.class);
                assertTrue(observingResponse.succeeded());

                // addparticipant in conference
                conferene.tell(new AddParticipant(tester), tester);
                //receieve sent to observers
                ConferenceResponse addParticipantConferenceResponse = expectMsgClass(ConferenceResponse.class);
                ConferenceInfo conferenceInfo = (ConferenceInfo)addParticipantConferenceResponse.get();
                logger.info("conferenceInfo: "+conferenceInfo);
                //receieve sent to call (since we are pretending to call&VoiceInterpreter)
                expectMsgClass(finiteDuration, ConferenceResponse.class);
                expectMsgClass(finiteDuration, JoinComplete.class);

                expectMsgClass(finiteDuration, Leave.class);
                
                ActorRef testActor = system.actorOf(new Props(new UntypedActorFactory() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public UntypedActor create() throws Exception {
                        return new CallApiClient(TEST_CALL_SID, daoManager);
                    }
                }));
                assertFalse(testActor.isTerminated());
                //send a CallApiResponse to conference on behalf of CallApiClient to see if it stops it
                conferene.tell(new CallApiResponse(new Exception("testing")), testActor);
                //wait a bit
                Thread.sleep(500);
                assertTrue(testActor.isTerminated());
                
                //tell conference that call left on behalf of the call actor
                conferene.tell(new Left(tester), tester);
                expectMsgClass(finiteDuration, ConferenceResponse.class);
                ConferenceStateChanged conferenceStateChanged = expectMsgClass(finiteDuration, ConferenceStateChanged.class);
                assertEquals(ConferenceStateChanged.State.COMPLETED, conferenceStateChanged.state());
            }};
	}

    private List<CallDetailRecord> mockedRemoteParticipants() throws URISyntaxException{
    	List<CallDetailRecord> mockedRemoteParticipants = new ArrayList<CallDetailRecord>();
    	CallDetailRecord.Builder builder = CallDetailRecord.builder();
    	builder.setSid(TEST_CALL_SID);
    	builder.setUri(new URI(TEST_CALL_URI));
    	builder.setConferenceSid(TEST_CNF_SID);
    	builder.setInstanceId(Sid.generate(Sid.Type.INSTANCE)+"");
    	mockedRemoteParticipants.add(builder.build());
    	return mockedRemoteParticipants;
    }

    private ActorRef conferenceCenter(final MediaServerControllerFactory factory, final DaoManager daoManager) {

        final Props props = new Props(new UntypedActorFactory() {
            private static final long serialVersionUID = 1L;
            @Override
            public UntypedActor create() throws Exception {
                return new ConferenceCenter(factory, daoManager);
            }
        });
        return system.actorOf(props);
    }

    @Test
	public void testStopConferenceWithNoLocalParticipants() throws URISyntaxException {
        new JavaTestKit(system) {
            {
            	daoManager = mock(DaoManager.class);
            	CallDetailRecordsDao callDetailRecordsDao = mock(CallDetailRecordsDao.class);
            	ConferenceDetailRecordsDao conferenceDetailRecordsDao = mock(ConferenceDetailRecordsDao.class);
            	when(callDetailRecordsDao.getTotalRunningCallDetailRecordsByConferenceSid(any(Sid.class))).thenReturn(0);
            	when(daoManager.getCallDetailRecordsDao()).thenReturn(callDetailRecordsDao);
            	when(daoManager.getConferenceDetailRecordsDao()).thenReturn(conferenceDetailRecordsDao);
            	
            	
                final ActorRef tester = getRef();
                // Create MockFailingMmsControllerFactory
                MediaServerControllerFactory factory = new MockMmsControllerFactory(system, null);
                // Create ConferenceCenter
                final ActorRef conferenceCenter = conferenceCenter(factory, daoManager);

                // get a fresh conference from conferenecneter
                final CreateConference create = new CreateConference(CONFERENCE_FRIENDLY_NAME_1, new Sid(CALL_SID));
                conferenceCenter.tell(create, tester);
                ConferenceCenterResponse conferenceCenterResponse = expectMsgClass(ConferenceCenterResponse.class);
                ActorRef conferene = conferenceCenterResponse.get();
                
                // start observing conference
                conferene.tell(new Observe(tester), tester);
                Observing observingResponse = expectMsgClass(Observing.class);
                assertTrue(observingResponse.succeeded());

                // stop conference while no local participants is there
                conferene.tell(new StopConference(), tester);
                ConferenceStateChanged conferenceStateChanged = expectMsgClass(ConferenceStateChanged.class);
                assertEquals(ConferenceStateChanged.State.COMPLETED, conferenceStateChanged.state());
            	
            }};
	}

}
