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
package org.mobicents.servlet.restcomm.telephony.ua;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;

import javax.sip.RequestEvent;
import javax.sip.address.SipURI;
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
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
//import org.mobicents.servlet.restcomm.telephony.Version;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@Ignore
@RunWith(Arquillian.class)
public final class UserAgentManagerTest {
    private static final String version = org.mobicents.servlet.restcomm.Version.getVersion();

    @ArquillianResource
    private Deployer deployer;

    private static SipStackTool tool;
    private SipStack receiver;
    private SipPhone phone;

    public UserAgentManagerTest() {
        super();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool = new SipStackTool("UserAgentTest");
    }

    @Before
    public void before() throws Exception {
        receiver = tool.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5070", "127.0.0.1:5080");
        phone = receiver.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, "sip:alice@127.0.0.1:5070");
    }

    @After
    public void after() throws Exception {
        if (phone != null) {
            phone.dispose();
        }
        if (receiver != null) {
            receiver.dispose();
        }
        deployer.undeploy("UserAgentTest");
    }
    @Test
    public void testVersion() {
        
    }

    @Test
    public void registerUserAgent() throws Exception {
        deployer.deploy("UserAgentTest");
        SipURI uri = receiver.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(phone.register(uri, "alice", "1234", "sip:127.0.0.1:5070", 3600, 3600));
        assertTrue(phone.unregister("sip:127.0.0.1:5070", 0));
    }

    @Test
    public void registerUserAgentWithTransport() throws Exception {
        deployer.deploy("UserAgentTest");
        SipURI uri = receiver.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(phone.register(uri, "alice", "1234", "sip:127.0.0.1:5070;transport=udp", 3600, 3600));
        assertTrue(phone.unregister("sip:127.0.0.1:5070;transport=udp", 0));
    }

    @Test
    public void registerUserAgentWithReRegister() throws Exception {
        deployer.deploy("UserAgentTest");
        SipURI uri = receiver.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(phone.register(uri, "alice", "1234", "sip:127.0.0.1:5070", 3600, 3600));
        assertTrue(phone.register(uri, "alice", "1234", "sip:127.0.0.1:5070", 3600, 3600));
        assertTrue(phone.unregister("sip:127.0.0.1:5070", 0));
    }

    @Test
    public void registerUserAgentWithOptionsPing() throws ParseException {
        deployer.deploy("UserAgentTest");
        // Register the phone so we can get OPTIONS pings from RestComm.
        SipURI uri = receiver.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        assertTrue(phone.register(uri, "alice", "1234", "sip:127.0.0.1:5070;transport=udp", 3600, 3600));
        // This is necessary for SipUnit to accept unsolicited requests.
        phone.setLoopback(true);
        // Due to some limitations in hsql to load new databases after the container
        // has been started we have to compensate for REGISTER messages coming from
        // the proxy tests so this test code is more complex than it should be but
        // it should still make sense.
        boolean timedOut = false;
        RequestEvent event = null;
        do {
            phone.listenRequestMessage();
            // Wait for an incoming request.
            event = phone.waitRequest(75 * 1000);
            if (event == null) {
                timedOut = true;
            } else {
                final String method = event.getRequest().getMethod();
                if ("REGISTER".equalsIgnoreCase(method)) {
                    event = null;
                    continue;
                } else {
                    // Validate the request.
                    assertTrue("OPTIONS".equalsIgnoreCase(method));
                    // Send the OK response.
                    final MessageFactory factory = receiver.getMessageFactory();
                    final Request request = event.getRequest();
                    Response ok = factory.createResponse(200, request);
                    SipTransaction transaction = phone.sendReply(event, ok);
                    // Validate the transaction.
                    assertNotNull(transaction);
                    // Exit
                    timedOut = true;
                }
            }
        } while (!timedOut);
        // Clean up (Unregister).
        assertTrue(phone.unregister("sip:127.0.0.1:5070;transport=udp", 0));
        assertNotNull(event);
    }

    @Deployment(name = "UserAgentTest", managed = false, testable = false)
    public static WebArchive createWebArchive() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("com.telestax.servlet:restcomm.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("restcomm.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script", "data/hsql/restcomm.script");
        return archive;
    }
}
