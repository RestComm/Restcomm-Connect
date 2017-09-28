package org.restcomm.connect.testsuite.telephony.proxy;

import gov.nist.javax.sip.header.Authorization;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sip.InvalidArgumentException;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.sip.RequestEvent;
import javax.sip.address.SipURI;
import javax.sip.header.ContactHeader;
import javax.sip.header.WWWAuthenticateHeader;
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
public final class ProxyManagerTest {
    private static final String version = Version.getVersion();

    @ArquillianResource
    private Deployer deployer;

    final String deploymentUrl = "http://127.0.0.1:8080/restcomm/";
    private static SipStackTool tool1;
    private static SipStackTool tool2;
    
    private SipStack augustSipStack;
    private SipPhone augustPhone;
    private String augustContact = "sip:august@127.0.0.1:5092";
    
    private SipStack imsSipStack;
    private SipPhone imsAugustPhone;
    private String imsContact = "sip:127.0.0.1";

    private boolean isAuthorizationHasRightUserName = false;
    private boolean isRegisterRequestReceived = false;
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
        
        imsSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5060", "127.0.0.1:5080");

        augustSipStack = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5092", "127.0.0.1:5080");
        augustPhone = augustSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, augustContact);
        imsAugustPhone = imsSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, augustContact);
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
        
        deployer.undeploy("ProxyManagerTest");
    }

    @Test
    public void testRegisterWithGateWay() throws ParseException, InterruptedException, SQLException {
        deployer.deploy("ProxyManagerTest");
        SipURI uri = augustSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
        final String userName = "august@127.0.0.1:5092";
        isAuthorizationHasRightUserName = false;
        isRegisterRequestReceived = false;
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
            	imsAugustPhone.listenRequestMessage();
                RequestEvent requestEvent = imsAugustPhone.waitRequest(10000);
                assertNotNull(requestEvent);
                if (requestEvent.getRequest() != null) {
                    isRegisterRequestReceived = true;
                }
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
                    assertNotNull(auth);
                    if (auth.getUsername().equals(userName)) {
                        isAuthorizationHasRightUserName = true;
                    }
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
        });

        RestcommCallsTool.getInstance().setGateWay(deploymentUrl, adminAccountSid, adminAuthToken, "friendlyName", 
            userName, "abcdef", "127.0.0.1", true, "3600");

        Thread.sleep(10000);
        assertTrue(isAuthorizationHasRightUserName);
        assertTrue(isRegisterRequestReceived);
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
