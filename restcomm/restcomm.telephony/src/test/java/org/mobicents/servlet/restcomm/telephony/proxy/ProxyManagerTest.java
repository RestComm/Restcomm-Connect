package org.mobicents.servlet.restcomm.telephony.proxy;

import static org.junit.Assert.*;

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
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public final class ProxyManagerTest {
  private static final String version = "1.6.0.GA";
  
  @ArquillianResource
  private Deployer deployer;
  
  private static SipStackTool tool;
  private SipStack receiver;
  private SipPhone phone;

  public ProxyManagerTest() {
    super();
  }
  
  @BeforeClass public static void beforeClass() throws Exception {
    tool = new SipStackTool("ProxyManagerTest");
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
    deployer.undeploy("ProxyManagerTest");
  }
  
  @Test public void testRestCommRegistration() throws Exception {
	deployer.deploy("ProxyManagerTest");
    // This is necessary for SipUnit to accept unsolicited requests.
	phone.setLoopback(true);
	phone.listenRequestMessage();
    // Wait for the SIP REGISTER request.
    final RequestEvent register = phone.waitRequest(75 * 1000);
    // Validate the request.
    final String method = register.getRequest().getMethod();
    assertTrue("REGISTER".equalsIgnoreCase(method));
    // Send the OK response.
    final MessageFactory factory = receiver.getMessageFactory();
    final Request request = register.getRequest();
    Response ok = factory.createResponse(200, request);
    SipTransaction transaction = phone.sendReply(register, ok);
    // Validate the transaction.
    assertNotNull(transaction);
  }
  
  @Deployment(name="ProxyManagerTest", managed=false, testable=false)
  public static WebArchive createWebArchive() {
    WebArchive archive = ShrinkWrapMaven.resolver()
        .resolve("com.telestax.servlet:restcomm.application:war:" + version)
        .withoutTransitivity().asSingle(WebArchive.class);
    archive.delete("/WEB-INF/conf/restcomm.xml");
    archive.delete("/WEB-INF/data/hsql/restcomm.script");
    archive.addAsWebInfResource("restcomm.xml", "conf/restcomm.xml");
	archive.addAsWebInfResource("telephony-restcomm.script", "data/hsql/restcomm.script");
    return archive;
  }
}
