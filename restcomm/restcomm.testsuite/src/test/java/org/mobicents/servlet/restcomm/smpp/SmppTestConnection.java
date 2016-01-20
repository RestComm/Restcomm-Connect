package org.mobicents.servlet.restcomm.smpp;

import static org.junit.Assert.*;

import java.net.URL;
import java.text.ParseException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.cloudhopper.smpp.SmppServerSession;



@RunWith(Arquillian.class)
public class SmppTestConnection {

    private final static Logger logger = Logger.getLogger(SmppTestConnection.class);
    private static final String version = org.mobicents.servlet.restcomm.Version.getVersion();
  
    /*
    static Runnable runSmppServer = new Runnable(){
        public void run(){
           System.out.println("Runnable running");
           new SmppServer();
        }
      };

      static Thread smppThread = new Thread(runSmppServer);
*/
    
    @ArquillianResource
    private Deployer deployer;
    @ArquillianResource
    URL deploymentUrl;




    @BeforeClass
    public static void beforeClass() throws Exception {

        //new SmppServer();
       //smppThread.start();
        
        
        
    }

    @Before
    public void before() throws Exception {
        logger.info("************BEFORE ******************");
        
       try {
            TimeUnit.SECONDS.sleep(60);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @After
    public void after() throws Exception {
        logger.info("************AFTER*****************");
       	
        try {
            TimeUnit.SECONDS.sleep(0);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    @Test
    public void testSmppIsConnected() throws ParseException {

      final SmppServerSession smppServerSession = SmppServerServletListener.DefaultSmppServerHandler.getSmppServerSession();

       logger.info("************SMPP TEST STARTING*****************");
   	
        try {
            TimeUnit.SECONDS.sleep(60);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        if(smppServerSession.isBound() || smppServerSession.isBinding()){

            logger.info("******SMPP SESSION IS CONNECTED****");
         }else {
        	 logger.info("******SMPP SESSION FAILED****");
         }
  
    	Assert.assertTrue(smppServerSession.isBound());

      	

    }

    @Deployment(name = "SmppTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");


        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("com.telestax.servlet:restcomm.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        restcommArchive.addClass(SmsRcmlServlet.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/web.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("restcomm-smpp.xml", "conf/restcomm.xml");
        logger.info("Packaged Test App");

        return archive;
    }
}

