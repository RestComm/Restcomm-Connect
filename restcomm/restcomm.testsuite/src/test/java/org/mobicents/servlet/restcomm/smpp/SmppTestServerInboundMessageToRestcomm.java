package org.mobicents.servlet.restcomm.smpp;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletException;

import org.cafesip.sipunit.SipStack;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mobicents.servlet.restcomm.sms.SmsOutTest;
import org.apache.log4j.Logger;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 * @author gvagenas@gmail.com (George Vagenas)
 */
@RunWith(Arquillian.class)
public class SmppTestServerInboundMessageToRestcomm {

       private final static Logger logger = Logger.getLogger(SmppTestServerInboundMessageToRestcomm.class);
	   private static final String version = org.mobicents.servlet.restcomm.Version.getVersion();

	    @ArquillianResource
	    private Deployer deployer;
	
	    @BeforeClass
	    public static void beforeClass() throws Exception {

	    }
	    
	    @Before
	    public void before() throws Exception {
	        logger.info("************BEFORE*****************");
	       	
	        try {
	            TimeUnit.SECONDS.sleep(30);
	        } catch (InterruptedException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	    	
    }
	    @Test 
	    public void testSendMessageToRestcomm () throws IOException, ServletException{
	    	
	        logger.info("************SMPP TEST STARTING*****************");

	        try {
	            TimeUnit.SECONDS.sleep(30);
	        } catch (InterruptedException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }
	    	
            logger.info("********SENIDNG MESSAGE TO RESTCOMM...******* " );
	    	
	      SmppServerServletListener.SmppPrepareInboundMessage.sendMessageToRestcommFromSmppServer();
    	
	    	
	    }
	
	    
	    @After
	    public void after() throws Exception {
	        try {
	            TimeUnit.SECONDS.sleep(60);
	        } catch (InterruptedException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        }

	    }
	
    @Deployment(name = "SmppTestServerInboundMessageToRestcomm", managed = true, testable = false)
    public static WebArchive createWebArchive() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("com.telestax.servlet:restcomm.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        //archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("restcomm-smpp.xml", "conf/restcomm.xml");
        //archive.addAsWebInfResource("restcomm.script_SmsTest", "data/hsql/restcomm.script");
        //archive.addAsWebResource("entry.xml");
       // archive.addAsWebResource("sms.xml");
        //archive.addAsWebResource("sms_to_alice.xml");
        return archive;
    }
}
