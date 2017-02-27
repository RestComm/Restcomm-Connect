package org.restcomm.connect.mrb;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.UnknownHostException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.entities.CallDetailRecord;
import org.restcomm.connect.dao.entities.ConferenceDetailRecord;
import org.restcomm.connect.mgcp.MediaResourceBrokerResponse;
import org.restcomm.connect.mrb.api.GetMediaGateway;
import org.restcomm.connect.mrb.api.MediaGatewayForConference;
import org.restcomm.connect.mrb.util.MediaResourceBrokerTestUtil;
import org.restcomm.connect.telephony.api.ConferenceStateChanged;

import akka.actor.ActorRef;
import akka.testkit.JavaTestKit;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
public class MediaResourceBrokerTest extends MediaResourceBrokerTestUtil{
	private final static Logger logger = Logger.getLogger(MediaResourceBrokerTest.class.getName());

	private static final String EXISTING_CALL_SID="CA01a09068a1f348269b6670ef599a6e57";
	private static final String NON_EXISTING_CALL_SID="CA00000000000000000000000000000000";
	@Before
    public void before() throws UnknownHostException, ConfigurationException, MalformedURLException {
    	if(logger.isDebugEnabled())
    		logger.debug("before");
        configurationNode1 = createCfg(CONFIG_PATH_NODE_1);
        
        startDaoManager();

        mediaResourceBrokerNode1 = mediaResourceBroker(configurationNode1.subset("media-server-manager"), daoManager, getClass().getClassLoader());
    	if(logger.isDebugEnabled())
    		logger.debug("before completed");
    }

	@After
	public void after(){
    	if(logger.isDebugEnabled())
    		logger.debug("after");
        daoManager.shutdown();
        if(!mediaResourceBrokerNode1.isTerminated()){
        	system.stop(mediaResourceBrokerNode1);
        }
    	if(logger.isDebugEnabled())
    		logger.debug("after completed");
	}

    /**
     * testGetMediaGatewayForACall for an inbound call
     * 
     */
    @Test
	public void testGetMediaGatewayForAInboundCall() {
        new JavaTestKit(system) {
            {
                final ActorRef tester = getRef();
            	if(logger.isDebugEnabled())
            		logger.debug("test 1 for a call found in DB - we should get mediagateway");
            	mediaResourceBrokerNode1.tell(new GetMediaGateway(new Sid(EXISTING_CALL_SID)), tester);
            	MediaResourceBrokerResponse<ActorRef> mrbResponse = expectMsgClass(MediaResourceBrokerResponse.class);
                assertTrue(!mrbResponse.get().isTerminated());

            	if(logger.isDebugEnabled())
            		logger.debug("cdr should be updated with ms id");
                CallDetailRecord cdr = daoManager.getCallDetailRecordsDao().getCallDetailRecord(new Sid(EXISTING_CALL_SID));
                assertTrue(cdr.getMsId() != null);

                if(logger.isDebugEnabled())
            		logger.debug("test 2 for a call not found in DB - we should get mediagateway anyway.");
                mediaResourceBrokerNode1.tell(new GetMediaGateway(new Sid(NON_EXISTING_CALL_SID)), tester);
            	mrbResponse = expectMsgClass(MediaResourceBrokerResponse.class);
                assertTrue(!mrbResponse.get().isTerminated());
            }};
	}

    /**
     * testGetMediaGatewayForACall for an outbound call
     * 
     */
    @Test
	public void testGetMediaGatewayForAnOutboundCall() {
        new JavaTestKit(system) {
            {
                final ActorRef tester = getRef();
            	if(logger.isDebugEnabled())
            		logger.debug("testGetMediaGatewayForACall for an outbound call.");
            	Sid callSid = null;
            	mediaResourceBrokerNode1.tell(new GetMediaGateway(callSid), tester);
            	MediaResourceBrokerResponse<ActorRef> mrbResponse = expectMsgClass(MediaResourceBrokerResponse.class);
                assertTrue(!mrbResponse.get().isTerminated());
            }};
	}

    /**
     * test GetMediaGateway For A Conference:
     */
    @Test
	public void testGetMediaGatewayForAConference() {
        new JavaTestKit(system) {
            {
                final ActorRef tester = getRef();
            	if(logger.isDebugEnabled())
            		logger.debug("testGetMediaGatewayForAConference");
            	mediaResourceBrokerNode1.tell(new GetMediaGateway(new Sid(EXISTING_CALL_SID), CONFERENCE_FRIENDLY_NAME_1, null), tester);
            	MediaResourceBrokerResponse<MediaGatewayForConference> mrbResponse = expectMsgClass(MediaResourceBrokerResponse.class);
            	assertTrue(mrbResponse != null);
            	MediaGatewayForConference mgfc = mrbResponse.get();
            	assertTrue(mgfc != null);
            	if(logger.isDebugEnabled())
            		logger.debug(""+mgfc);
            	//make sure we get a working mediaGateway
            	assertTrue(mgfc.mediaGateway() != null && !mgfc.mediaGateway().isTerminated());
            	//make sure we get a generated conferenceSid and that we are not master since this is community mrb and have no notion of master/slave.
            	assertTrue(mgfc.conferenceSid() != null && !mgfc.isThisMaster());

            	ConferenceDetailRecord cdr = daoManager.getConferenceDetailRecordsDao().getConferenceDetailRecord(mgfc.conferenceSid());
            	//mrb must generate a conference cdr
            	assertTrue(cdr != null);
            	// check status
            	assertTrue(cdr.getStatus().equals(ConferenceStateChanged.State.RUNNING_INITIALIZING+""));
            }};
	}
}
