package org.restcomm.connect.telephony;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.UnknownHostException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.commons.patterns.Observe;
import org.restcomm.connect.commons.patterns.Observing;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.mscontrol.api.MediaServerControllerFactory;
import org.restcomm.connect.mscontrol.api.messages.JoinComplete;
import org.restcomm.connect.mscontrol.api.messages.Left;
import org.restcomm.connect.mscontrol.mms.MockFailingMmsControllerFactory;
import org.restcomm.connect.mscontrol.mms.MockMmsControllerFactory;
import org.restcomm.connect.telephony.api.AddParticipant;
import org.restcomm.connect.telephony.api.ConferenceCenterResponse;
import org.restcomm.connect.telephony.api.ConferenceInfo;
import org.restcomm.connect.telephony.api.ConferenceResponse;
import org.restcomm.connect.telephony.api.ConferenceStateChanged;
import org.restcomm.connect.telephony.api.CreateConference;
import org.restcomm.connect.telephony.util.ConferenceTestUtil;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.actor.UntypedActorFactory;
import akka.testkit.JavaTestKit;

public class ConferenceTest extends ConferenceTestUtil{
	private final static Logger logger = Logger.getLogger(ConferenceTest.class.getName());

	@Before
    public void before() throws UnknownHostException, ConfigurationException, MalformedURLException {
    	if(logger.isDebugEnabled())
    		logger.debug("before");
        configurationNode1 = createCfg(CONFIG_PATH_NODE_1);
        
        startDaoManager();
        
    	if(logger.isDebugEnabled())
    		logger.debug("before completed");
    }

	@After
	public void after(){
    	if(logger.isDebugEnabled())
    		logger.debug("after");
        daoManager.shutdown();
    	if(logger.isDebugEnabled())
    		logger.debug("after completed");
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
                logger.info("conferenceCenterResponse 1: "+conferenceCenterResponse);
                ActorRef conferene = conferenceCenterResponse.get();
                
                // start observing conference
                conferene.tell(new Observe(tester), tester);
                Observing observingResponse = expectMsgClass(Observing.class);
                assertTrue(observingResponse.succeeded());

                // addparticipant in conference
                conferene.tell(new AddParticipant(tester), tester);
                //receieve sent to observers
                ConferenceResponse<ConferenceInfo> conferenceInfo = expectMsgClass(ConferenceResponse.class);
                //receieve sent to call (since we are pretending to call&VoiceInterpreter)
                conferenceInfo = expectMsgClass(ConferenceResponse.class);
                JoinComplete joinComplete = expectMsgClass(JoinComplete.class);

                // stop conference
                conferene.tell(new Left(), tester);
                ConferenceResponse conferenceResponse = expectMsgClass(ConferenceResponse.class);
                logger.info("conferenceResponse 2: "+conferenceResponse);
                
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

                conferenceInfo = expectMsgClass(ConferenceResponse.class);
                conferenceInfo = expectMsgClass(ConferenceResponse.class);
                joinComplete = expectMsgClass(JoinComplete.class);

                if(logger.isDebugEnabled())
            		logger.debug("check existing MediaGateway, which was already in db");
            	
            }};
	}
	
    @Test
	public void testJoinCompletedConference() {
        new JavaTestKit(system) {
            {
                final ActorRef tester = getRef();
                // Create MockFailingMmsControllerFactory
                MediaServerControllerFactory factory = new MockMmsControllerFactory(system, null);
                // Create ConferenceCenter
                final ActorRef conferenceCenter = conferenceCenter(factory, daoManager);

                // get a fresh conference from conferenecneter
                final CreateConference create = new CreateConference(CONFERENCE_FRIENDLY_NAME_1, new Sid(CALL_SID));
                conferenceCenter.tell(create, tester);
                ConferenceCenterResponse conferenceCenterResponse = expectMsgClass(ConferenceCenterResponse.class);
                logger.info("conferenceCenterResponse 1: "+conferenceCenterResponse);
                ActorRef conferene = conferenceCenterResponse.get();
                
                // start observing conference
                conferene.tell(new Observe(tester), tester);
                Observing observingResponse = expectMsgClass(Observing.class);
                assertTrue(observingResponse.succeeded());

                // addparticipant in conference
                conferene.tell(new AddParticipant(tester), tester);
                //receieve sent to observers
                ConferenceResponse<ConferenceInfo> conferenceInfo = expectMsgClass(ConferenceResponse.class);
                //receieve sent to call (since we are pretending to call&VoiceInterpreter)
                conferenceInfo = expectMsgClass(ConferenceResponse.class);
                JoinComplete joinComplete = expectMsgClass(JoinComplete.class);

                // stop conference
                conferene.tell(new Left(), tester);
                ConferenceResponse conferenceResponse = expectMsgClass(ConferenceResponse.class);
                expectMsgClass(ConferenceStateChanged.class);
                logger.info("conferenceResponse 2: "+conferenceResponse);
                
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

                conferenceInfo = expectMsgClass(ConferenceResponse.class);
                conferenceInfo = expectMsgClass(ConferenceResponse.class);
                joinComplete = expectMsgClass(JoinComplete.class);

                if(logger.isDebugEnabled())
            		logger.debug("check existing MediaGateway, which was already in db");
            	
            }};
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

}
