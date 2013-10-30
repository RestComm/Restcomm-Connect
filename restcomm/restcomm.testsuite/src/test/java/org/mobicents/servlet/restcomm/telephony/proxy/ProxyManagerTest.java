package org.mobicents.servlet.restcomm.telephony.proxy;

import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
import org.cafesip.sipunit.SipTransaction;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.*;
import org.junit.runner.RunWith;
//import org.mobicents.servlet.restcomm.telephony.Version;

import javax.sip.RequestEvent;
import javax.sip.message.MessageFactory;
import javax.sip.message.Request;
import javax.sip.message.Response;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
@Ignore
public final class ProxyManagerTest {
	private static final String version = "6.1.2-TelScale-SNAPSHOT";// Version.getInstance().getRestCommVersion();
  
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
    final WebArchive archive = ShrinkWrapMaven.resolver()
        .resolve("com.telestax.servlet:restcomm.application:war:" + version)
        .withoutTransitivity().asSingle(WebArchive.class);
    JavaArchive dependency = ShrinkWrapMaven.resolver()
        .resolve("com.telestax.servlet:restcomm.commons:jar:" + version)
        .withoutTransitivity().asSingle(JavaArchive.class);
    archive.addAsLibrary(dependency);
    dependency = ShrinkWrapMaven.resolver()
        .resolve("com.telestax.servlet:restcomm.dao:jar:" + version)
        .withoutTransitivity().asSingle(JavaArchive.class);
    archive.addAsLibrary(dependency);
    dependency = ShrinkWrapMaven.resolver()
        .resolve("com.telestax.servlet:restcomm.mgcp:jar:" + version)
        .withoutTransitivity().asSingle(JavaArchive.class);
    archive.addAsLibrary(dependency);
    dependency = ShrinkWrapMaven.resolver()
        .resolve("com.telestax.servlet:restcomm.http:jar:" + version)
        .withoutTransitivity().asSingle(JavaArchive.class);
    archive.addAsLibrary(dependency);
    dependency = ShrinkWrapMaven.resolver()
        .resolve("com.telestax.servlet:restcomm.interpreter:jar:" + version)
        .withoutTransitivity().asSingle(JavaArchive.class);
    archive.addAsLibrary(dependency);
    dependency = ShrinkWrapMaven.resolver()
        .resolve("com.telestax.servlet:restcomm.sms.api:jar:" + version)
        .withoutTransitivity().asSingle(JavaArchive.class);
    archive.addAsLibrary(dependency);
    dependency = ShrinkWrapMaven.resolver()
        .resolve("com.telestax.servlet:restcomm.sms:jar:" + version)
        .withoutTransitivity().asSingle(JavaArchive.class);
    archive.addAsLibrary(dependency);
    dependency = ShrinkWrapMaven.resolver()
        .resolve("com.telestax.servlet:restcomm.telephony:jar:" + version)
        .withoutTransitivity().asSingle(JavaArchive.class);
    archive.addAsLibrary(dependency);
    dependency = ShrinkWrapMaven.resolver()
        .resolve("commons-configuration:commons-configuration:jar:1.7")
        .withoutTransitivity().asSingle(JavaArchive.class);
    archive.addAsLibrary(dependency);
    dependency = ShrinkWrapMaven.resolver()
        .resolve("jain:jain-mgcp-ri:jar:1.0")
        .withoutTransitivity().asSingle(JavaArchive.class);
    archive.addAsLibrary(dependency);
    archive.delete("/WEB-INF/sip.xml");
    archive.delete("/WEB-INF/conf/restcomm.xml");
    archive.delete("/WEB-INF/data/hsql/restcomm.script");
    archive.addAsWebInfResource("sip.xml");
    archive.addAsWebInfResource("restcomm.xml", "conf/restcomm.xml");
	archive.addAsWebInfResource("restcomm.script", "data/hsql/restcomm.script");
    return archive;
  }
}
