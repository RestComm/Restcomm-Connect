package org.restcomm.connect.testsuite.telephony.proxy;

import java.text.ParseException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.After;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;
import org.restcomm.connect.testsuite.http.RestcommCallsTool;
//import org.restcomm.connect.telephony.Version;

@RunWith(Arquillian.class)
@Ignore
public final class ProxyManagerTest {
    private static final String version = Version.getVersion();

    @ArquillianResource
    private Deployer deployer;

    final String deploymentUrl = "http://127.0.0.1:8080/restcomm/";
    private static SipStackTool tool;
    private SipStack receiver;
    private SipPhone phone;

    public ProxyManagerTest() {
        super();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool = new SipStackTool("ProxyManagerTest");
    }

    @Before
    public void before() throws Exception {
        receiver = tool.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5070", "127.0.0.1:5080");
        phone = receiver.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, "sip:alice@127.0.0.1:5070");
    }

    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    @After
    public void after() throws Exception {
        if (phone != null) {
            phone.dispose();
        }
        if (receiver != null) {
            receiver.dispose();
        }
        deployer.undeploy("ProxyManagerTest");
    }

    @Test
    public void testRestCommRegistration() throws Exception {
        deployer.deploy("ProxyManagerTest");
        // This is necessary for SipUnit to accept unsolicited requests.
        // Set Gateway Info
        // This test is need to do manually.
        RestcommCallsTool.getInstance().setGateWay(deploymentUrl, adminAccountSid, adminAuthToken, "friendlyName", 
            "abcdef@xyz.com", "abcdef", "127.0.0.1:5070", true, "3600");
        
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
//                phone.setLoopback(true);
                phone.listenRequestMessage();
                final RequestEvent register = phone.waitRequest(75 * 1000);
                assertNotNull(null);
                final String method = register.getRequest().getMethod();
                assertTrue("REGISTER".equalsIgnoreCase(method));
                // Send the OK response.
                final MessageFactory factory = receiver.getMessageFactory();
                final Request request = register.getRequest();
                 
                try {
                   Response ok = factory.createResponse(200, request);
                   SipTransaction transaction = phone.sendReply(register, ok);
                   assertNotNull(transaction);
                } catch (ParseException ex) {
                    fail(ex.getMessage());
                }
                
            }
        });
        
        Thread.sleep(10000);
    }

    @Deployment(name = "ProxyManagerTest", managed = false, testable = false)
    public static WebArchive createWebArchive() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("org.restcomm:restcomm-connect.application:war:" + version).withoutTransitivity()
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
