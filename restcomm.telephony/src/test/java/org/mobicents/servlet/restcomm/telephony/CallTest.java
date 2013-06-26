/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.restcomm.telephony;

import java.util.List;

import javax.sip.message.Response;

import static org.cafesip.sipunit.SipAssert.*;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;

import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;

import org.junit.After;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@RunWith(Arquillian.class)
public final class CallTest {
  private static final String version = "1.6.0.GA";
  private static final byte[] bytes = new byte[] { 118, 61, 48, 13, 10, 111, 61, 117, 115, 101, 114,
      49, 32, 53, 51, 54, 53, 53, 55, 54, 53, 32, 50, 51, 53, 51, 54, 56, 55, 54, 51, 55, 32,
      73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49, 13, 10, 115, 61, 45, 13,
      10, 99, 61, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49, 13, 10, 116,
      61, 48, 32, 48, 13, 10, 109, 61, 97, 117, 100, 105, 111, 32, 54, 48, 48, 48, 32, 82, 84,
      80, 47, 65, 86, 80, 32, 48, 13, 10, 97, 61, 114, 116, 112, 109, 97, 112, 58, 48, 32, 80,
      67, 77, 85, 47, 56, 48, 48, 48, 13, 10 };
  private static final String body = new String(bytes);
  
  @ArquillianResource
  private Deployer deployer;
  
  private static SipStackTool tool;
  private SipStack receiver;
  private SipPhone phone;
  
  public CallTest() {
    super();
  }
  
  @BeforeClass public static void beforeClass() throws Exception {
    tool = new SipStackTool("CallTest");
  }
  
  @Before public void before() throws Exception {
    receiver = tool.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5070", "127.0.0.1:5080");
    phone = receiver.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, "sip:+17778889999@127.0.0.1:5070");
  }
  
  @After public void after() throws Exception {
    if(phone != null) {
      phone.dispose();
    }
    if(receiver != null) {
      receiver.dispose();
    }
    deployer.undeploy("CallTest");
  }

  @Test public void testInboundRedirectAndSms() {
    deployer.deploy("CallTest");
    final SipCall call = phone.createSipCall();
    call.initiateOutgoingCall("sip:+17778889999@127.0.0.1:5070", "sip:+12223334444@127.0.0.1:5080", null, body, "application", "sdp", null, null);
    assertLastOperationSuccess(call);
    assertTrue(call.waitOutgoingCallResponse(1000));
    final int response = call.getLastReceivedResponse().getStatusCode();
    assertTrue(response == Response.TRYING || response == Response.RINGING);
    if(response == Response.TRYING) {
      assertTrue(call.waitOutgoingCallResponse(1000));
      assertEquals(Response.RINGING, call.getLastReceivedResponse().getStatusCode());
    }
    assertTrue(call.waitOutgoingCallResponse(1000));
    assertEquals(Response.OK, call.getLastReceivedResponse().getStatusCode());
    call.sendInviteOkAck();
    // Wait for an sms.
    final SipCall receiver = phone.createSipCall();
    phone.setLoopback(true);
    phone.listenRequestMessage();
    assertTrue(receiver.waitForMessage(10 * 1000));
    receiver.sendMessageResponse(202, "Accepted", -1);
 	final List<String> messages = receiver.getAllReceivedMessagesContent();
 	assertTrue(messages.size() > 0);
 	assertTrue(messages.get(0).equals("Hello World!"));
 	// HangUp the call.
    assertTrue(!(call.getLastReceivedResponse().getStatusCode() >= 400));
    assertTrue(call.waitForDisconnect(5000));
    try {
      Thread.sleep(10 * 1000);
    } catch(final InterruptedException exception) {
      exception.printStackTrace();
    }
  }
  
  @Test public void testPauseRejectBusy() {
    deployer.deploy("CallTest");
    final SipCall call = phone.createSipCall();
    call.initiateOutgoingCall("sip:+17778889999@127.0.0.1:5070", "sip:+12223334445@127.0.0.1:5080", null, body, "application", "sdp", null, null);
    assertLastOperationSuccess(call);
    assertTrue(call.waitOutgoingCallResponse(1000));
    final int response = call.getLastReceivedResponse().getStatusCode();
    assertTrue(response == Response.TRYING || response == Response.RINGING);
    if(response == Response.TRYING) {
      assertTrue(call.waitOutgoingCallResponse(1000));
      assertEquals(Response.RINGING, call.getLastReceivedResponse().getStatusCode());
    }
    assertTrue(call.waitOutgoingCallResponse(1000));
    assertEquals(Response.BUSY_HERE, call.getLastReceivedResponse().getStatusCode());
  }

  @Deployment(name="CallTest", managed=false, testable=false)
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
        .resolve("com.telestax.servlet:restcomm.asr:jar:" + version)
        .withoutTransitivity().asSingle(JavaArchive.class);
    archive.addAsLibrary(dependency);
    dependency = ShrinkWrapMaven.resolver()
        .resolve("com.telestax.servlet:restcomm.fax:jar:" + version)
        .withoutTransitivity().asSingle(JavaArchive.class);
    archive.addAsLibrary(dependency);
    dependency = ShrinkWrapMaven.resolver()
        .resolve("com.telestax.servlet:restcomm.tts:jar:" + version)
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
        .resolve("com.telestax.servlet:restcomm.telephony.api:jar:" + version)
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
    dependency = ShrinkWrapMaven.resolver()
        .resolve("joda-time:joda-time:jar:2.0")
        .withoutTransitivity().asSingle(JavaArchive.class);
    archive.addAsLibrary(dependency);
    dependency = ShrinkWrapMaven.resolver()
        .resolve("com.iSpeech:iSpeech:jar:1.0.1")
        .withoutTransitivity().asSingle(JavaArchive.class);
    archive.addAsLibrary(dependency);
    archive.delete("/WEB-INF/sip.xml");
    archive.delete("/WEB-INF/conf/restcomm.xml");
    archive.delete("/WEB-INF/data/hsql/restcomm.script");
    archive.addAsWebInfResource("sip.xml");
    archive.addAsWebInfResource("restcomm.xml", "conf/restcomm.xml");
	archive.addAsWebInfResource("restcomm.script", "data/hsql/restcomm.script");
	archive.addAsWebResource("redirect-sms-entry.xml");
	archive.addAsWebResource("redirect-sms-sms.xml");
	archive.addAsWebResource("pause-reject-busy-entry.xml");
    return archive;
  }
}
