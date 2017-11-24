package org.restcomm.connect.testsuite.telephony.proxy;

import gov.nist.javax.sip.header.Authorization;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import javax.sip.InvalidArgumentException;

import javax.sip.RequestEvent;
import javax.sip.address.SipURI;
import javax.sip.header.ContactHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Response;

import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;
import org.restcomm.connect.commons.annotations.ParallelClassTests;
import org.restcomm.connect.commons.annotations.WithInMinsTests;
import org.restcomm.connect.testsuite.NetworkPortAssigner;
import org.restcomm.connect.testsuite.WebArchiveUtil;
import org.restcomm.connect.testsuite.http.RestcommCallsTool;
//import org.restcomm.connect.telephony.Version;

@RunWith(Arquillian.class)
@Category(value={WithInMinsTests.class, ParallelClassTests.class})
public final class ProxyManagerTest {
    private static final String version = Version.getVersion();

    @ArquillianResource
    private Deployer deployer;

    private static int restcommPort = 5080;
    private static int restcommHTTPPort = 8080;
    private static String restcommContact = "127.0.0.1:" + restcommPort;
    private static int mediaPort = NetworkPortAssigner.retrieveNextPortByFile();

    private static SipStackTool tool1;
    private static SipStackTool tool2;

    private SipStack augustSipStack;
    private SipPhone augustPhone;
    private static int augustPort = NetworkPortAssigner.retrieveNextPort();
    private String augustContact = "sip:august@127.0.0.1:" + augustPort;

    private SipStack imsSipStack;
    private SipPhone imsAugustPhone;
    private static int imsPort = NetworkPortAssigner.retrieveNextPort();
    private String imsContact = "sip:127.0.0.1:" + imsPort;
    public ProxyManagerTest() {
        super();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("ImsClientsDialTest1");
        tool2 = new SipStackTool("ImsClientsDialTest2");
    }

    @Before
    public void before() throws Exception {

        imsSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", String.valueOf(imsPort), restcommContact);

        augustSipStack = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", String.valueOf(augustPort), restcommContact);
        augustPhone = augustSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, restcommPort, augustContact);
        imsAugustPhone = imsSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, restcommPort, augustContact);
        imsAugustPhone.setLoopback(true);
    }

    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    @After
    public void after() throws Exception {
        if (augustPhone != null) {
            augustPhone.dispose();
        }
        if (augustSipStack != null) {
            augustSipStack.dispose();
        }

        if (imsSipStack != null) {
            imsSipStack.dispose();
        }
        if (imsAugustPhone != null) {
            imsAugustPhone.dispose();
        }

        if (imsSipStack != null) {
            imsSipStack.dispose();
        }
        if (imsAugustPhone != null) {
            imsAugustPhone.dispose();
        }

        deployer.undeploy("ProxyManagerTest");
    }

    @Test
    public void testRegisterWithGateWayWhenUserNameContainHost() throws ParseException, InterruptedException, SQLException {
        deployer.deploy("ProxyManagerTest");
        SipURI uri = augustSipStack.getAddressFactory().createSipURI(null, restcommContact);
        final String userName = "august@127.0.0.1:" + augustPort;

        String deploymentUrl = "http://127.0.0.1:"+ restcommHTTPPort +"/restcomm/";
        RestcommCallsTool.getInstance().setGateWay(deploymentUrl, adminAccountSid, adminAuthToken, "friendlyName",
                userName, "abcdef", "127.0.0.1:" + imsPort, true, "3600");

        imsAugustPhone.listenRequestMessage();
        RequestEvent requestEvent = imsAugustPhone.waitRequest(10000);
        assertNotNull(requestEvent);
        assertTrue(requestEvent.getRequest() != null);
        try {
            Response response = imsSipStack.getMessageFactory().createResponse(401, requestEvent.getRequest());
            WWWAuthenticateHeader wwwAuthenticateHeader = imsSipStack.getHeaderFactory().createWWWAuthenticateHeader("Digest realm=\"ims.tp.pl\",\n" +
                    "   nonce=\"b7c9036dbf357f7683f054aea940e9703dc8f84c1108\",\n" +
                    "   opaque=\"ALU:QbkRBthOEgEQAkgVEwwHRAIBHgkdHwQCQ1lFRkZWDhMyIXBqLCs0Zj06ZTwhdHpgZmI_\",\n" +
                    "   algorithm=MD5,\n" +
                    "   qop=\"auth\"");
            response.setHeader(wwwAuthenticateHeader);
            ContactHeader contactHeader = augustSipStack.getHeaderFactory().createContactHeader();
            contactHeader.setAddress(augustSipStack.getAddressFactory().createAddress(imsContact));
            response.addHeader(contactHeader);
            imsAugustPhone.sendReply(requestEvent, response);
            requestEvent = imsAugustPhone.waitRequest(10000);
            response = imsSipStack.getMessageFactory().createResponse(200, requestEvent.getRequest());
            Authorization auth = (Authorization)requestEvent.getRequest().getHeader(Authorization.NAME);
            ContactHeader contHeader = (ContactHeader) requestEvent.getRequest().getHeader(ContactHeader.NAME);
            assertNotNull(auth);
            assertTrue(auth.getUsername().equals(userName));
            assertTrue(contHeader.toString().contains("august@127.0.0.1:" + restcommPort));
            contactHeader = augustSipStack.getHeaderFactory().createContactHeader();
            contactHeader.setExpires(600);
            contactHeader.setAddress(augustSipStack.getAddressFactory().createAddress(imsContact));
            response.addHeader(contactHeader);
            imsAugustPhone.sendReply(requestEvent, response);
        } catch (ParseException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }catch (InvalidArgumentException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public static void reconfigurePorts() {
        if (System.getProperty("arquillian_sip_port") != null) {
            restcommPort = Integer.valueOf(System.getProperty("arquillian_sip_port"));
            restcommContact = "127.0.0.1:" + restcommPort;
        }
        if (System.getProperty("arquillian_http_port") != null) {
            restcommHTTPPort = Integer.valueOf(System.getProperty("arquillian_http_port"));
        }
    }

    @Deployment(name = "ProxyManagerTest", managed = false, testable = false)
    public static WebArchive createWebArchive() {

        reconfigurePorts();

        Map<String,String> replacements = new HashMap();
        //replace mediaport 2727
        replacements.put("2727", String.valueOf(mediaPort));
        replacements.put("8080", String.valueOf(restcommHTTPPort));
        replacements.put("5080", String.valueOf(restcommPort));
        replacements.put("5060", String.valueOf(imsPort));
        replacements.put("5092", String.valueOf(augustPort));
        List<String> resources = new ArrayList(Arrays.asList("dial-client-entry_wActionUrl.xml"));
        return WebArchiveUtil.createWebArchiveNoGw("restcomm.xml",
                "restcomm.script",resources, replacements);
    }
}
