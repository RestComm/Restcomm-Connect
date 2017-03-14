package org.restcomm.connect.testsuite.http;

import static org.junit.Assert.fail;

import java.net.URL;

import org.apache.log4j.Logger;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

/**
 * LiveCallModificationMuteUnMuteTest
 * @author mariafarooq
 *
 */
@RunWith(Arquillian.class)
public class LiveCallModificationMuteUnMuteTest {

    private final static Logger logger = Logger.getLogger(CreateCallsTest.class.getName());
    private static final String version = Version.getVersion();

    private static final byte[] bytes = new byte[] { 118, 61, 48, 13, 10, 111, 61, 117, 115, 101, 114, 49, 32, 53, 51, 54, 53,
            53, 55, 54, 53, 32, 50, 51, 53, 51, 54, 56, 55, 54, 51, 55, 32, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46,
            48, 46, 49, 13, 10, 115, 61, 45, 13, 10, 99, 61, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49,
            13, 10, 116, 61, 48, 32, 48, 13, 10, 109, 61, 97, 117, 100, 105, 111, 32, 54, 48, 48, 48, 32, 82, 84, 80, 47, 65,
            86, 80, 32, 48, 13, 10, 97, 61, 114, 116, 112, 109, 97, 112, 58, 48, 32, 80, 67, 77, 85, 47, 56, 48, 48, 48, 13, 10 };
    private static final String body = new String(bytes);

    @ArquillianResource
    URL deploymentUrl;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090); // No-args constructor defaults to port 8080

    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    private static SipStackTool tool1;

    private SipStack mariaSipStack;
    private SipPhone mariaPhone;
    private String mariaContact = "sip:maria@127.0.0.1:5090";

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("LiveCallModification1");
    }

    @Before
    public void before() throws Exception {
        mariaSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5090", "127.0.0.1:5080");
        mariaPhone = mariaSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, mariaContact);
    }

    @After
    public void after() throws Exception {
        if (mariaPhone != null) {
            mariaPhone.dispose();
        }
        if (mariaSipStack != null) {
            mariaSipStack.dispose();
        }
        Thread.sleep(1000);
        wireMockRule.resetRequests();
        Thread.sleep(4000);
    }

    /**
     * muteInProgressCall
     * @throws Exception
     */
    @Test
    public void muteInProgressCall() throws Exception {
    	fail("Not Implemented");
    }

    /**
     * unMuteInProgressCall
     * @throws Exception
     */
    @Test
    public void unMuteInProgressCall() throws Exception {
    	fail("Not Implemented");
    }

    /**
     * unMuteUnMutedCall: we can mute only unmuted call
     * @throws Exception
     */
    @Test
    public void muteMutedCall() throws Exception {
    	fail("Not Implemented");
    }

    /**
     * unMuteUnMutedCall: we can unmute only muted call
     * @throws Exception
     */
    @Test
    public void unMuteUnMutedCall() throws Exception {
    	fail("Not Implemented");
    }
    
    /**
     * muteCompletedCall: we can mute/unmute only in progress call
     * @throws Exception
     */
    @Test
    public void muteCompletedCall() throws Exception {
    	fail("Not Implemented");
    }

    /**
     * unMuteCompletedCall: we can mute/unmute only in progress call
     * @throws Exception
     */
    @Test
    public void unMuteCompletedCall() throws Exception {
    	fail("Not Implemented");
    }
    
    @Deployment(name = "LiveCallModificationTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
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
        archive.addAsWebInfResource("restcomm.script_dialTest", "data/hsql/restcomm.script");
        archive.addAsWebResource("dial-number-entry.xml");
        archive.addAsWebResource("dial-client-entry.xml");
        archive.addAsWebResource("hello-play.xml");
        logger.info("Packaged Test App");
        return archive;
    }

}
