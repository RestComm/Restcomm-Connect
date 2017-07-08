package org.restcomm.connect.mrb;

import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.UnknownHostException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.connect.mrb.util.MediaResourceBrokerTestUtil;

import akka.actor.ActorRef;
import akka.testkit.JavaTestKit;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
public class MediaResourceBrokerTestExceptionHandling extends MediaResourceBrokerTestUtil{
	private final static Logger logger = Logger.getLogger(MediaResourceBrokerTestExceptionHandling.class.getName());

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
     * testGetMediaGatewayExceptionHandling
     */
    @Test
	public void testGetMediaGatewayExceptionHandling() {
        new JavaTestKit(system) {
            {
                final ActorRef tester = getRef();
            	if(logger.isDebugEnabled())
            		logger.debug("test GetMediaGateway Exception Handling");
            	String mrbRequest = new String("Hello");
            	mediaResourceBrokerNode1.tell(mrbRequest, tester);
            	String mrbResponse = expectMsgClass(String.class);
                assertTrue(mrbResponse.equals(mrbRequest));
            }};
	}
}
