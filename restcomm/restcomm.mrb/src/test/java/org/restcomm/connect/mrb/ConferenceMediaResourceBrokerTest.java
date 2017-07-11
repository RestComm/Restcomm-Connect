package org.restcomm.connect.mrb;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.UUID;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.commons.patterns.Observe;
import org.restcomm.connect.commons.patterns.Observing;
import org.restcomm.connect.dao.entities.ConferenceDetailRecord;
import org.restcomm.connect.mgcp.CreateConferenceEndpoint;
import org.restcomm.connect.mgcp.MediaGatewayResponse;
import org.restcomm.connect.mgcp.MediaResourceBrokerResponse;
import org.restcomm.connect.mgcp.MediaSession;
import org.restcomm.connect.mrb.api.ConferenceMediaResourceControllerStateChanged;
import org.restcomm.connect.mrb.api.GetConferenceMediaResourceController;
import org.restcomm.connect.mrb.api.GetMediaGateway;
import org.restcomm.connect.mrb.api.MediaGatewayForConference;
import org.restcomm.connect.mrb.api.StartConferenceMediaResourceController;
import org.restcomm.connect.mrb.util.MediaResourceBrokerTestUtil;
import org.restcomm.connect.telephony.api.ConferenceStateChanged;

import akka.actor.ActorRef;
import akka.testkit.JavaTestKit;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
public class ConferenceMediaResourceBrokerTest extends MediaResourceBrokerTestUtil {
    private final static Logger logger = Logger.getLogger(ConferenceMediaResourceBrokerTest.class.getName());

    private static final String EXISTING_CALL_SID = "CA01a09068a1f348269b6670ef599a6e57";

    private static final String RANDOM_CONFERENCE_NAME = UUID.randomUUID().toString().replace("-", "");

    @Before
    public void before() throws UnknownHostException, ConfigurationException, MalformedURLException {
        if (logger.isDebugEnabled())
            logger.debug("before");
        configurationNode1 = createCfg(CONFIG_PATH_NODE_1);

        startDaoManager();

        mediaResourceBrokerNode1 = mediaResourceBroker(configurationNode1.subset("media-server-manager"), daoManager,
                getClass().getClassLoader());
        if (logger.isDebugEnabled())
            logger.debug("before completed");
    }

    @After
    public void after() {
        if (logger.isDebugEnabled())
            logger.debug("after");
        daoManager.shutdown();
        if (!mediaResourceBrokerNode1.isTerminated()) {
            system.stop(mediaResourceBrokerNode1);
        }
        if (logger.isDebugEnabled())
            logger.debug("after completed");
    }

    /**
     * testConferenceMediaResourceController
     */
    @Test
    public void testConferenceMediaResourceController() {
        new JavaTestKit(system) {
            {
                final ActorRef tester = getRef();
                if (logger.isDebugEnabled())
                    logger.debug("testGetMediaGatewayForAConference");
                mediaResourceBrokerNode1.tell(
                        new GetMediaGateway(new Sid(EXISTING_CALL_SID), ACCOUNT_SID_1 + ":" + RANDOM_CONFERENCE_NAME, null),
                        tester);
                MediaResourceBrokerResponse<MediaGatewayForConference> mrbResponse = expectMsgClass(
                        MediaResourceBrokerResponse.class);
                assertTrue(mrbResponse != null);
                MediaGatewayForConference mgfc = mrbResponse.get();
                if (logger.isDebugEnabled())
                    logger.debug("" + mgfc);

                ActorRef mediaGateway = mgfc.mediaGateway();
                assertTrue(mgfc != null && mediaGateway != null && !mediaGateway.isTerminated() && mgfc.conferenceSid() != null
                        && !mgfc.isThisMaster());

                ConferenceDetailRecord cdr = daoManager.getConferenceDetailRecordsDao()
                        .getConferenceDetailRecord(mgfc.conferenceSid());
                // mrb must generate a proper conference cdr
                assertTrue(cdr != null && cdr.getStatus().equals(ConferenceStateChanged.State.RUNNING_INITIALIZING + "")
                        && cdr.isMasterPresent() && cdr.getFriendlyName().equals(RANDOM_CONFERENCE_NAME));

                // verify that we ger cmrc actor and its not terminated
                mediaResourceBrokerNode1.tell(new GetConferenceMediaResourceController(RANDOM_CONFERENCE_NAME), tester);
                MediaResourceBrokerResponse<ActorRef> mrbResponseForCMRC = expectMsgClass(MediaResourceBrokerResponse.class);
                assertTrue(mrbResponseForCMRC != null);
                ActorRef conferenceMediaResourceController = mrbResponseForCMRC.get();
                assertTrue(conferenceMediaResourceController != null && !conferenceMediaResourceController.isTerminated());

                // create media session
                mediaGateway.tell(new org.restcomm.connect.mgcp.CreateMediaSession(), tester);
                MediaSession mediaSession = (MediaSession) expectMsgClass(MediaGatewayResponse.class).get();
                assertTrue(mediaSession != null);

                // get conference endpoint.
                mediaGateway.tell(new CreateConferenceEndpoint(mediaSession), tester);
                MediaGatewayResponse<ActorRef> mgResponse = expectMsgClass(MediaGatewayResponse.class);
                ActorRef conferenceEndpoint = mgResponse.get();
                assertTrue(conferenceEndpoint != null && !conferenceEndpoint.isTerminated());

                // Start ConferenceMediaResourceController & check conferenceMediaResourceController state, should be ACTIVE
                conferenceMediaResourceController.tell(new Observe(tester), tester);
                expectMsgClass(Observing.class);
                conferenceMediaResourceController
                        .tell(new StartConferenceMediaResourceController(conferenceEndpoint, cdr.getSid()), tester);
                ConferenceMediaResourceControllerStateChanged conferenceMediaResourceControllerStateChanged = expectMsgClass(
                        ConferenceMediaResourceControllerStateChanged.class);
                assertTrue(conferenceMediaResourceControllerStateChanged.state()
                        .equals(ConferenceMediaResourceControllerStateChanged.MediaServerControllerState.ACTIVE));

                cdr = daoManager.getConferenceDetailRecordsDao().getConferenceDetailRecord(mgfc.conferenceSid());
                // check status
                assertTrue(cdr.getStatus().equals(ConferenceStateChanged.State.RUNNING_MODERATOR_ABSENT + ""));

            }
        };
    }
}
