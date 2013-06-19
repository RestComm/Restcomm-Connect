package org.mobicents.servlet.restcomm.sms;

import java.text.ParseException;

import javax.sip.RequestEvent;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
import org.cafesip.sipunit.SipTransaction;

import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@RunWith(Arquillian.class)
public final class SmsSessionTest {
  private static final String version = "1.6.0.GA";
  
  @ArquillianResource
  private Deployer deployer;
  
  private static SipStackTool tool;
  private SipStack receiver;
  private SipPhone phone;
  
  public SmsSessionTest() {
    super();
  }

  @BeforeClass public static void beforeClass() throws Exception {
    tool = new SipStackTool("SMSSessionTest");
  }
  
  @Before public void before() throws Exception {
    receiver = tool.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5070", "127.0.0.1:5080");
    phone = receiver.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, "sip:alice@127.0.0.1:5070");
  }
  
  @After public void after() throws Exception {
    if(phone != null) {
      phone.dispose();
    }
    if(receiver != null) {
      receiver.dispose();
    }
    deployer.undeploy("SMSSessionTest");
  }

  @Ignore @Test public void testReceiveSms() throws ParseException {
    deployer.deploy("SmsSessionTest");
    final MessageFactory factory = receiver.getMessageFactory();
    final Request sms = factory.createRequest("MESSAGE");
    
  }
  
  @Ignore @Test public void testSendSms() throws ParseException {
    deployer.deploy("SmsSessionTest");
    // This is necessary for SipUnit to accept unsolicited requests.
    phone.setLoopback(true);
    // Wait for an incoming request.
 	phone.listenRequestMessage();
    final RequestEvent event = phone.waitRequest(180 * 1000);
    assertTrue(event != null);
    final String method = event.getRequest().getMethod();
    // Validate the request.
    assertTrue("MESSAGE".equalsIgnoreCase(method));
    // Send the Accepted response.
    final MessageFactory factory = receiver.getMessageFactory();
    final Request request = event.getRequest();
    Response ok = factory.createResponse(202, request);
    SipTransaction transaction = phone.sendReply(event, ok);
    // Validate the transaction.
    assertNotNull(transaction);
  }
  
  @Deployment(name="SmsSessionTest", managed=false, testable=false)
  public static WebArchive createWebArchive() {
    WebArchive archive = ShrinkWrapMaven.resolver()
        .resolve("com.telestax.servlet:restcomm.application:war:" + version)
        .withoutTransitivity().asSingle(WebArchive.class);
    archive.delete("/WEB-INF/conf/restcomm.xml");
    archive.delete("/WEB-INF/data/hsql/restcomm.script");
    archive.addAsWebInfResource("restcomm.xml", "conf/restcomm.xml");
	archive.addAsWebInfResource("sms-restcomm.script", "data/hsql/restcomm.script");
    return archive;
  }
}
