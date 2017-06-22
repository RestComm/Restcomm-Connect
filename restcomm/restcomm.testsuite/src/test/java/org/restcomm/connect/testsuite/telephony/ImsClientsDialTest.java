package org.restcomm.connect.testsuite.telephony;

import static org.cafesip.sipunit.SipAssert.assertLastOperationSuccess;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.net.URL;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sip.Dialog;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.SipException;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.ProxyAuthenticateHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.UserAgentHeader;
import javax.sip.header.ViaHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import org.apache.log4j.Logger;
import org.cafesip.sipunit.Credential;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipRequest;
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
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;
import org.restcomm.connect.testsuite.http.RestcommCallsTool;
import org.restcomm.connect.testsuite.telephony.security.DigestServerAuthenticationMethod;
import org.restcomm.connect.testsuite.tools.MonitoringServiceTool;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.JsonObject;

/**
 * Test for clients with or without VoiceURL (Bitbucket issue 115). Clients without VoiceURL can dial anything.
 *
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */
@RunWith(Arquillian.class)
public class ImsClientsDialTest {

    private static final String version = Version.getVersion();

    private static Logger logger = Logger.getLogger(ImsClientsDialTest.class);

    private static final byte[] bytes = new byte[] { 118, 61, 48, 13, 10, 111, 61, 117, 115, 101, 114, 49, 32, 53, 51, 54, 53,
        53, 55, 54, 53, 32, 50, 51, 53, 51, 54, 56, 55, 54, 51, 55, 32, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46,
        48, 46, 49, 13, 10, 115, 61, 45, 13, 10, 99, 61, 73, 78, 32, 73, 80, 52, 32, 49, 50, 55, 46, 48, 46, 48, 46, 49,
        13, 10, 116, 61, 48, 32, 48, 13, 10, 109, 61, 97, 117, 100, 105, 111, 32, 54, 48, 48, 48, 32, 82, 84, 80, 47, 65,
        86, 80, 32, 48, 13, 10, 97, 61, 114, 116, 112, 109, 97, 112, 58, 48, 32, 80, 67, 77, 85, 47, 56, 48, 48, 48, 13, 10 };
    private static final String body = new String(bytes);

    @ArquillianResource
    private Deployer deployer;
    @ArquillianResource
    URL deploymentUrl;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090); // No-args constructor defaults to port 8080

    private static SipStackTool tool1;
    private static SipStackTool tool2;
    private static SipStackTool tool3;
    private static SipStackTool tool4;

    private String pstnNumber = "+151261006100";


    // Maria is a Restcomm Client **without** VoiceURL. This Restcomm Client can dial anything.
    private SipStack augustSipStack;
    private SipPhone augustPhone;
    private String augustContact = "sip:august@127.0.0.1:5092";
    private boolean isAugustRegistered = false;

    private SipStack bobSipStack;
    private SipPhone bobPhone;
    private String bobContact = "sip:bob@bob.com:5095";
    private boolean isBobRegistered = false;

    private SipStack juliusSipStack;
    private SipPhone juliusPhone;
    private String juliusContact = "sip:julius@127.0.0.1:5094";
    private boolean isJuliusRegistered = false;

    private SipStack imsSipStack;
    private SipPhone imsAugustPhone;
    private SipPhone imsAugustPhone2;
    private SipPhone imsJuliusPhone;
    private SipPhone imsBobPhone;

    private String imsContact = "sip:127.0.0.1";

    private SipPhone pstnPhone;
    private String pstnContact = "sip:"+pstnNumber+"@127.0.0.1:5060";

    private String adminAccountSid = "AC27f2dd02ab51ba5d5a9ff7fc5537a09a";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("ImsClientsDialTest1");
        tool2 = new SipStackTool("ImsClientsDialTest2");
        tool3 = new SipStackTool("ImsClientsDialTest3");
        tool4 = new SipStackTool("ImsClientsDialTest4");

        Class.forName("org.hsqldb.jdbc.JDBCDriver");
    }

    @Before
    public void before() throws Exception {

        imsSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5060", "127.0.0.1:5080");

        augustSipStack = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5092", "127.0.0.1:5080");
        augustPhone = augustSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, augustContact);
        imsAugustPhone = imsSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, augustContact);
        imsAugustPhone.setLoopback(true);

        imsAugustPhone2 = imsSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, "sip:august@ims.com");
        imsAugustPhone2.setLoopback(true);

        juliusSipStack = tool3.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5094", "127.0.0.1:5080");
        juliusPhone = juliusSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, juliusContact);
        imsJuliusPhone = imsSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, juliusContact);
        imsJuliusPhone.setLoopback(true);

        bobSipStack = tool4.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5095", "127.0.0.1:5080");
        bobPhone = bobSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, bobContact);
        imsBobPhone = imsSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, bobContact);
        imsBobPhone.setLoopback(true);

        pstnPhone = imsSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, pstnContact);

        if(isAugustRegistered){
            unregisterAugust();
        }

        if(isBobRegistered){
            unregisterBob();
        }

        if(isJuliusRegistered){
            unregisterJulius();
        }

    }

    @After
    public void after() throws Exception {
        if (augustPhone != null) {
            augustPhone.dispose();
        }

        if (augustSipStack != null) {
            augustSipStack.dispose();
        }

        if (bobPhone != null) {
            bobPhone.dispose();
        }
        if (bobSipStack != null) {
            bobSipStack.dispose();
        }

        if (imsBobPhone != null) {
            imsBobPhone.dispose();
        }

        if (imsSipStack != null) {
            imsSipStack.dispose();
        }
        if (imsAugustPhone != null) {
            imsAugustPhone.dispose();
        }
        if (imsAugustPhone2 != null) {
            imsAugustPhone2.dispose();
        }
        if (imsJuliusPhone != null) {
            imsJuliusPhone.dispose();
        }

        Thread.sleep(3000);
        wireMockRule.resetRequests();
        Thread.sleep(3000);
    }

    @Test
    public void testRegisterClients() throws ParseException, InterruptedException, SQLException {
        logger.info("testRegisterClients");
        SipURI uri = augustSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                imsAugustPhone.listenRequestMessage();
                RequestEvent requestEvent = imsAugustPhone.waitRequest(10000);
                assertNotNull(requestEvent);
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

        assertTrue(augustPhone.register(uri, "august", "1234", augustContact, 3600, 3600));
        isAugustRegistered = true;
        augustPhone.createSipCall().listenForIncomingCall();

        Thread.sleep(500);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                imsAugustPhone.listenRequestMessage();
                RequestEvent requestEvent = imsAugustPhone.waitRequest(10000);
                assertNotNull(requestEvent);
                try {
                    Response response = imsSipStack.getMessageFactory().createResponse(200, requestEvent.getRequest());
                    ContactHeader contactHeader = augustSipStack.getHeaderFactory().createContactHeader();
                    contactHeader.setExpires(0);
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
        assertTrue(augustPhone.unregister(augustContact, 0));
        Thread.sleep(500);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
        isAugustRegistered = false;
    }

    @Test
    public void testRegisterClientForbidden() throws ParseException, InterruptedException, SQLException {
        logger.info("testRegisterClients");
        SipURI uri = augustSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                imsAugustPhone.listenRequestMessage();
                RequestEvent requestEvent = imsAugustPhone.waitRequest(10000);
                assertNotNull(requestEvent);
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
                    response = imsSipStack.getMessageFactory().createResponse(Response.FORBIDDEN, requestEvent.getRequest());
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

        assertFalse(augustPhone.register(uri, "august", "1234", augustContact, 3600, 3600));

        Thread.sleep(500);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);

    }

    @Test
    public void testReRegisterClientForbidden() throws ParseException, InterruptedException, SQLException {
        logger.info("testRegisterClients");
        SipURI uri = augustSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                imsAugustPhone.listenRequestMessage();
                RequestEvent requestEvent = imsAugustPhone.waitRequest(10000);
                assertNotNull(requestEvent);
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

        assertTrue(augustPhone.register(uri, "august", "1234", augustContact, 3600, 3600));
        isAugustRegistered = true;
        augustPhone.createSipCall().listenForIncomingCall();

        Thread.sleep(500);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                imsAugustPhone.listenRequestMessage();
                RequestEvent requestEvent = imsAugustPhone.waitRequest(10000);
                assertNotNull(requestEvent);
                try {
                    Response response = imsSipStack.getMessageFactory().createResponse(Response.FORBIDDEN, requestEvent.getRequest());
                    ContactHeader contactHeader = augustSipStack.getHeaderFactory().createContactHeader();
                    contactHeader.setExpires(0);
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
        assertFalse(augustPhone.register(uri, "august", "1234", augustContact, 3600, 3600));
        Thread.sleep(500);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
        isAugustRegistered = false;
    }



    @Test
    public void testReRegisterClientForbidden2() throws ParseException, InterruptedException, SQLException, InvalidArgumentException {
        try{
        logger.info("testReRegisterImsClientForbidden");
        SipURI uri = augustSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                imsAugustPhone2.listenRequestMessage();
                RequestEvent requestEvent = imsAugustPhone2.waitRequest(10000);
                assertNotNull(requestEvent);
                try {
                    Response response = imsSipStack.getMessageFactory().createResponse(200, requestEvent.getRequest());
                    ContactHeader contactHeader = augustSipStack.getHeaderFactory().createContactHeader();
                    contactHeader.setExpires(600);
                    contactHeader.setAddress(augustSipStack.getAddressFactory().createAddress(imsContact));
                    response.addHeader(contactHeader);
                    imsAugustPhone2.sendReply(requestEvent, response);
                } catch (ParseException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }catch (InvalidArgumentException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }catch (Exception e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }

            }
        });
        ToHeader to = augustSipStack.getHeaderFactory().createToHeader(
                augustSipStack.getAddressFactory().createAddress(
                        augustSipStack.getAddressFactory().createSipURI(null, "august@ims.com")),
                "to_tag");
        FromHeader from = augustSipStack.getHeaderFactory().createFromHeader(
                augustSipStack.getAddressFactory().createAddress(
                        augustSipStack.getAddressFactory().createSipURI(null, "august@ims.com")),
                "from_tag");
        CallIdHeader callId = (CallIdHeader)augustSipStack.getHeaderFactory().createHeader("Call-ID", "12345");
        CSeqHeader cseq = augustSipStack.getHeaderFactory().createCSeqHeader((long)1, "REGISTER");
        ViaHeader via = augustSipStack.getHeaderFactory().createViaHeader("127.0.0.1", 5092, "wss", "branch_12345");
        List<Header> vias = new ArrayList<Header>();
        vias.add(via);
        MaxForwardsHeader maxForwards = (MaxForwardsHeader)augustSipStack.getHeaderFactory().createHeader("Max-Forwards", "70");
        Header expires = augustSipStack.getHeaderFactory().createHeader("Expires", "600");
        ContactHeader contact = augustSipStack.getHeaderFactory().createContactHeader(augustSipStack.getAddressFactory().createAddress(
                augustSipStack.getAddressFactory().createSipURI(null, "august@127.0.0.1:5092")));

        Request register = augustSipStack.getMessageFactory().createRequest(uri, "REGISTER", callId, cseq, from, to, vias, maxForwards);
        register.addHeader(expires);
        register.addHeader(contact);

        assertTrue(augustPhone.sendUnidirectionalRequest(register, true));

        Thread.sleep(500);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==1);

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                imsAugustPhone2.listenRequestMessage();
                RequestEvent requestEvent = imsAugustPhone2.waitRequest(10000);
                assertNotNull(requestEvent);
                try {
                    Response response = imsSipStack.getMessageFactory().createResponse(Response.FORBIDDEN, requestEvent.getRequest());
                    ContactHeader contactHeader = augustSipStack.getHeaderFactory().createContactHeader();
                    contactHeader.setExpires(0);
                    contactHeader.setAddress(augustSipStack.getAddressFactory().createAddress(imsContact));
                    response.addHeader(contactHeader);
                    imsAugustPhone2.sendReply(requestEvent, response);
                } catch (ParseException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }catch (InvalidArgumentException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }

            }
        });
        CallIdHeader callId2 = (CallIdHeader)augustSipStack.getHeaderFactory().createHeader("Call-ID", "67890");
        Request cloneRegister = augustSipStack.getMessageFactory().createRequest(uri, "REGISTER", callId2, cseq, from, to, vias, maxForwards);
        cloneRegister.addHeader(expires);
        cloneRegister.addHeader(contact);

        assertTrue(augustPhone.sendUnidirectionalRequest(cloneRegister, true));
        Thread.sleep(2000);
        assertTrue(MonitoringServiceTool.getInstance().getRegisteredUsers(deploymentUrl.toString(),adminAccountSid, adminAuthToken)==0);
        isAugustRegistered = false;
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testWebRTCClientOutgoingAdisconnect() throws ParseException, InterruptedException, SQLException {

        logger.info("testWebRTCClientOutgoingAdisconnect");
        registerAugust();

        SipCall pstnCall = pstnPhone.createSipCall();
        final SipCall augustCall = augustPhone.createSipCall();
        initiateAugust(pstnCall,pstnContact,augustCall);

        assertTrue(pstnCall.waitForIncomingCall(5 * 1000));
        assertTrue(pstnCall.sendIncomingCallResponse(Response.RINGING, "RINGING-pstn", 3600));

        SipRequest lastReceivedRequest = pstnCall.getLastReceivedRequest();
        String receivedBody = new String(lastReceivedRequest.getRawContent());
        assertTrue(pstnCall.sendIncomingCallResponse(Response.OK, "OK-pstn", 3600, receivedBody, "application", "sdp", null,
                null));

        Thread.sleep(1000);

        int liveCalls = MonitoringServiceTool.getInstance().getStatistics(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        int liveCallsArraySize = MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        assertTrue( liveCalls == 2);
        assertTrue(liveCallsArraySize  == 2);

        Map<String, String> filters = new HashMap<String, String>();
        JsonObject filteredCallsByStatusObject = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(),
                adminAccountSid, adminAuthToken, filters);
        assertEquals(2, filteredCallsByStatusObject.get("calls").getAsJsonArray().size());

        pstnCall.listenForDisconnect();
        assertTrue(augustCall.disconnect());

        assertTrue(pstnCall.waitForDisconnect(5 * 1000));
        assertTrue(pstnCall.respondToDisconnect());

        Thread.sleep(1000);

        filteredCallsByStatusObject = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(),
                adminAccountSid, adminAuthToken, filters);
        assertEquals(0, filteredCallsByStatusObject.get("calls").getAsJsonArray().size());

        unregisterAugust();
    }

    @Test
    public void testWebRTCClientOutgoingOtherDomain() throws ParseException, InterruptedException, SQLException {

        logger.info("testWebRTCClientOutgoingAdisconnect");
        registerBob();

        SipCall pstnCall = pstnPhone.createSipCall();
        final SipCall bobCall = bobPhone.createSipCall();
        initiateBob(pstnCall,pstnContact,bobCall);

        assertTrue(pstnCall.waitForIncomingCall(5 * 1000));
        assertTrue(pstnCall.sendIncomingCallResponse(Response.RINGING, "RINGING-pstn", 3600));

        SipRequest lastReceivedRequest = pstnCall.getLastReceivedRequest();
        String receivedBody = new String(lastReceivedRequest.getRawContent());
        assertTrue(pstnCall.sendIncomingCallResponse(Response.OK, "OK-pstn", 3600, receivedBody, "application", "sdp", null,
                null));

        assertTrue(lastReceivedRequest.getRequestEvent().getRequest().getHeader("From").toString().contains("bob@bob.com"));

        Thread.sleep(1000);
        int liveCalls = MonitoringServiceTool.getInstance().getStatistics(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        int liveCallsArraySize = MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        assertTrue( liveCalls == 2);
        assertTrue(liveCallsArraySize  == 2);

        Map<String, String> filters = new HashMap<String, String>();
        JsonObject filteredCallsByStatusObject = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(),
                adminAccountSid, adminAuthToken, filters);
        assertEquals(2, filteredCallsByStatusObject.get("calls").getAsJsonArray().size());

        pstnCall.listenForDisconnect();
        assertTrue(bobCall.disconnect());

        assertTrue(pstnCall.waitForDisconnect(5 * 1000));
        assertTrue(pstnCall.respondToDisconnect());

        Thread.sleep(1000);

        filteredCallsByStatusObject = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(),
                adminAccountSid, adminAuthToken, filters);
        assertEquals(0, filteredCallsByStatusObject.get("calls").getAsJsonArray().size());

        unregisterBob();
    }

    @Test
    public void testWebRTCClientOutgoingAHold() throws SipException, ParseException, InterruptedException, InvalidArgumentException {

        logger.info("testWebRTCClientOutgoingAHold");
        registerAugust();

        SipCall pstnCall = pstnPhone.createSipCall();
        final SipCall augustCall = augustPhone.createSipCall();
        initiateAugust(pstnCall,pstnContact,augustCall);

        assertTrue(pstnCall.waitForIncomingCall(5 * 1000));
        assertTrue(pstnCall.sendIncomingCallResponse(Response.RINGING, "RINGING-pstn", 3600));

        SipRequest lastReceivedRequest = pstnCall.getLastReceivedRequest();
        String receivedBody = new String(lastReceivedRequest.getRawContent());
        assertTrue(pstnCall.sendIncomingCallResponse(Response.OK, "OK-pstn", 3600, receivedBody, "application", "sdp", null,
                null));

        Thread.sleep(1000);

        int liveCalls = MonitoringServiceTool.getInstance().getStatistics(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        int liveCallsArraySize = MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        assertTrue( liveCalls == 2);
        assertTrue(liveCallsArraySize  == 2);

        Map<String, String> filters = new HashMap<String, String>();
        JsonObject filteredCallsByStatusObject = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(),
                adminAccountSid, adminAuthToken, filters);
        assertEquals(2, filteredCallsByStatusObject.get("calls").getAsJsonArray().size());

        //HOLD - start
        SipTransaction augustReinviteTx = augustCall.sendReinvite(augustContact, augustContact, body + "a=sendonly", "application", "sdp");
        assertTrue(augustCall.waitReinviteResponse(augustReinviteTx, 5 * 1000));
        augustCall.sendReinviteOkAck(augustReinviteTx);

        assertTrue(pstnCall.waitForMessage(5 * 1000));
        lastReceivedRequest = pstnCall.getLastReceivedRequest();
        receivedBody = new String(lastReceivedRequest.getRawContent());
        assertTrue(receivedBody.equals("action=onHold"));
        SipTransaction pstnMessageTx = pstnCall.getLastTransaction();
        Request pstnMessage = pstnMessageTx.getServerTransaction().getRequest();
        Response pstnMessageAccepted = imsSipStack.getMessageFactory().createResponse(Response.ACCEPTED, pstnMessage);
        pstnMessageTx.getServerTransaction().sendResponse(pstnMessageAccepted);

        augustReinviteTx = augustCall.sendReinvite(augustContact, augustContact, body + "a=sendrecv", "application", "sdp");
        assertTrue(augustCall.waitReinviteResponse(augustReinviteTx, 5 * 1000));
        augustCall.sendReinviteOkAck(augustReinviteTx);

        assertTrue(pstnCall.waitForMessage(5 * 1000));
        lastReceivedRequest = pstnCall.getLastReceivedRequest();
        receivedBody = new String(lastReceivedRequest.getRawContent());
        assertTrue(receivedBody.equals("action=offHold"));
        pstnMessageTx = pstnCall.getLastTransaction();
        pstnMessage = pstnMessageTx.getServerTransaction().getRequest();
        pstnMessageAccepted = imsSipStack.getMessageFactory().createResponse(Response.ACCEPTED, pstnMessage);
        pstnMessageTx.getServerTransaction().sendResponse(pstnMessageAccepted);
        //HOLD - end

        Thread.sleep(1000);

        pstnCall.listenForDisconnect();
        assertTrue(augustCall.disconnect());

        assertTrue(pstnCall.waitForDisconnect(5 * 1000));
        assertTrue(pstnCall.respondToDisconnect());
        Thread.sleep(1000);

        filteredCallsByStatusObject = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(),
                adminAccountSid, adminAuthToken, filters);
        assertEquals(0, filteredCallsByStatusObject.get("calls").getAsJsonArray().size());

        unregisterAugust();
    }

    @Test
    public void testWebRTCClientOutgoingBHold() throws SipException, ParseException, InterruptedException, InvalidArgumentException {

        logger.info("testWebRTCClientOutgoingBHold");
        registerAugust();

        SipCall pstnCall = pstnPhone.createSipCall();
        final SipCall augustCall = augustPhone.createSipCall();
        initiateAugust(pstnCall,pstnContact,augustCall);

        assertTrue(pstnCall.waitForIncomingCall(5 * 1000));
        assertTrue(pstnCall.sendIncomingCallResponse(Response.RINGING, "RINGING-pstn", 3600));

        SipRequest lastReceivedRequest = pstnCall.getLastReceivedRequest();
        String receivedBody = new String(lastReceivedRequest.getRawContent());
        assertTrue(pstnCall.sendIncomingCallResponse(Response.OK, "OK-pstn", 3600, receivedBody, "application", "sdp", null,
                null));

        Thread.sleep(1000);

        int liveCalls = MonitoringServiceTool.getInstance().getStatistics(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        int liveCallsArraySize = MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        assertTrue( liveCalls == 2);
        assertTrue(liveCallsArraySize  == 2);

        Map<String, String> filters = new HashMap<String, String>();
        JsonObject filteredCallsByStatusObject = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(),
                adminAccountSid, adminAuthToken, filters);
        assertEquals(2, filteredCallsByStatusObject.get("calls").getAsJsonArray().size());

        Thread.sleep(1000);

        //HOLD - start
        augustCall.listenForMessage();

        SipTransaction pstnReinviteTx = pstnCall.sendReinvite(pstnContact, pstnContact, body + "a=sendonly", "application", "sdp");
        assertTrue(pstnCall.waitReinviteResponse(pstnReinviteTx, 5 * 1000));
        pstnCall.sendReinviteOkAck(pstnReinviteTx);

        assertTrue(augustCall.waitForMessage(5 * 1000));
        lastReceivedRequest = augustCall.getLastReceivedRequest();
        receivedBody = new String(lastReceivedRequest.getRawContent());
        assertTrue(receivedBody.equals("action=onHold"));
        SipTransaction augustMessageTx = augustCall.getLastTransaction();
        Request augustMessage = augustMessageTx.getServerTransaction().getRequest();
        Response augustMessageAccepted = augustSipStack.getMessageFactory().createResponse(Response.ACCEPTED, augustMessage);
        augustMessageTx.getServerTransaction().sendResponse(augustMessageAccepted);

        pstnReinviteTx = pstnCall.sendReinvite(pstnContact, pstnContact, body + "a=sendrecv", "application", "sdp");
        assertTrue(pstnCall.waitReinviteResponse(pstnReinviteTx, 5 * 1000));
        pstnCall.sendReinviteOkAck(pstnReinviteTx);

        assertTrue(augustCall.waitForMessage(5 * 1000));
        lastReceivedRequest = augustCall.getLastReceivedRequest();
        receivedBody = new String(lastReceivedRequest.getRawContent());
        assertTrue(receivedBody.equals("action=offHold"));
        augustMessageTx = augustCall.getLastTransaction();
        augustMessage = augustMessageTx.getServerTransaction().getRequest();
        augustMessageAccepted = augustSipStack.getMessageFactory().createResponse(Response.ACCEPTED, augustMessage);
        augustMessageTx.getServerTransaction().sendResponse(augustMessageAccepted);
        //HOLD - end


        Thread.sleep(1000);
        augustCall.listenForDisconnect();
        assertTrue(pstnCall.disconnect());

        assertTrue(augustCall.waitForDisconnect(5 * 1000));
        assertTrue(augustCall.respondToDisconnect());
        Thread.sleep(1000);

        filteredCallsByStatusObject = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(),
                adminAccountSid, adminAuthToken, filters);
        assertEquals(0, filteredCallsByStatusObject.get("calls").getAsJsonArray().size());

        unregisterAugust();
    }

    @Test
    public void testWebRTCClientIncomingADisconnect() throws InterruptedException, ParseException {
        logger.info("testWebRTCClientIncomingADisconnect");

        registerAugust();


        SipCall augustCall = augustPhone.createSipCall();
        SipCall pstnCall = pstnPhone.createSipCall();
        initiatePstn(pstnCall, augustCall);


        assertTrue(augustCall.waitForIncomingCall(30 * 1000));
        assertTrue(augustCall.sendIncomingCallResponse(Response.RINGING, "Ringing-August", 3600));
        String receivedBody = new String(augustCall.getLastReceivedRequest().getRawContent());
        assertTrue(augustCall.sendIncomingCallResponse(Response.OK, "OK-August", 3600, receivedBody, "application", "sdp", null,
                null));
        assertTrue(augustCall.waitForAck(50 * 1000));

        Thread.sleep(1000);

        int liveCalls = MonitoringServiceTool.getInstance().getStatistics(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        int liveCallsArraySize = MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        assertTrue( liveCalls == 2);
        assertTrue(liveCallsArraySize  == 2);

        Map<String, String> filters = new HashMap<String, String>();
        JsonObject filteredCallsByStatusObject = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(),
                adminAccountSid, adminAuthToken, filters);
        assertEquals(2, filteredCallsByStatusObject.get("calls").getAsJsonArray().size());

        Thread.sleep(1000);

        // hangup.
        augustCall.listenForDisconnect();
        pstnCall.disconnect();
        assertTrue(augustCall.waitForDisconnect(30 * 1000));
        assertTrue(augustCall.respondToDisconnect());
        Thread.sleep(1000);

        filteredCallsByStatusObject = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(),
                adminAccountSid, adminAuthToken, filters);
        assertEquals(0, filteredCallsByStatusObject.get("calls").getAsJsonArray().size());

        unregisterAugust();
    }

    @Test
    public void testWebRTCClientIncomingBusy() throws InterruptedException, ParseException {
        logger.info("testWebRTCClientIncomingBusy");

        registerAugust();


        SipCall augustCall = augustPhone.createSipCall();
        SipCall pstnCall = pstnPhone.createSipCall();
        initiatePstn(pstnCall, augustCall);


        assertTrue(augustCall.waitForIncomingCall(30 * 1000));
        assertTrue(augustCall.sendIncomingCallResponse(Response.BUSY_HERE, "Busy-August", 3600));
        assertTrue(augustCall.waitForAck(50 * 1000));

        pstnCall.listenForDisconnect();
        assertTrue(pstnCall.waitForDisconnect(30 * 1000));
        assertTrue(pstnCall.respondToDisconnect());

        Thread.sleep(1000);

        Map<String, String> filters = new HashMap<String, String>();
        JsonObject filteredCallsByStatusObject = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(),
                adminAccountSid, adminAuthToken, filters);
        assertEquals(0, filteredCallsByStatusObject.get("calls").getAsJsonArray().size());

        unregisterAugust();
    }

    @Test
    public void testWebRTCClientIncomingAHold() throws SipException, InterruptedException, ParseException, InvalidArgumentException {
        logger.info("testWebRTCClientIncomingAHold");

        registerAugust();


        SipCall augustCall = augustPhone.createSipCall();
        SipCall pstnCall = pstnPhone.createSipCall();
        initiatePstn(pstnCall, augustCall);


        assertTrue(augustCall.waitForIncomingCall(30 * 1000));
        assertTrue(augustCall.sendIncomingCallResponse(Response.RINGING, "Ringing-August", 3600));
        String receivedBody = new String(augustCall.getLastReceivedRequest().getRawContent());
        assertTrue(augustCall.sendIncomingCallResponse(Response.OK, "OK-August", 3600, receivedBody, "application", "sdp", null,
                null));
        assertTrue(augustCall.waitForAck(50 * 1000));

        Thread.sleep(1000);

        int liveCalls = MonitoringServiceTool.getInstance().getStatistics(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        int liveCallsArraySize = MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        assertTrue( liveCalls == 2);
        assertTrue(liveCallsArraySize  == 2);

        Map<String, String> filters = new HashMap<String, String>();
        JsonObject filteredCallsByStatusObject = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(),
                adminAccountSid, adminAuthToken, filters);
        assertEquals(2, filteredCallsByStatusObject.get("calls").getAsJsonArray().size());

        Thread.sleep(1000);

        //HOLD - start
        SipTransaction pstnReinviteTx = pstnCall.sendReinvite(pstnContact, pstnContact, body + "a=sendonly", "application", "sdp");
        assertTrue(pstnCall.waitReinviteResponse(pstnReinviteTx, 5 * 1000));
        pstnCall.sendReinviteOkAck(pstnReinviteTx);

        assertTrue(augustCall.waitForMessage(5 * 1000));
        SipRequest lastReceivedRequest = augustCall.getLastReceivedRequest();
        receivedBody = new String(lastReceivedRequest.getRawContent());
        assertTrue(receivedBody.equals("action=onHold"));
        SipTransaction augustMessageTx = augustCall.getLastTransaction();
        Request augustMessage = augustMessageTx.getServerTransaction().getRequest();
        Response augustMessageAccepted = augustSipStack.getMessageFactory().createResponse(Response.ACCEPTED, augustMessage);
        augustMessageTx.getServerTransaction().sendResponse(augustMessageAccepted);

        pstnReinviteTx = pstnCall.sendReinvite(pstnContact, pstnContact, body + "a=sendrecv", "application", "sdp");
        assertTrue(pstnCall.waitReinviteResponse(pstnReinviteTx, 5 * 1000));
        pstnCall.sendReinviteOkAck(pstnReinviteTx);

        assertTrue(augustCall.waitForMessage(5 * 1000));
        lastReceivedRequest = augustCall.getLastReceivedRequest();
        receivedBody = new String(lastReceivedRequest.getRawContent());
        assertTrue(receivedBody.equals("action=offHold"));
        augustMessageTx = augustCall.getLastTransaction();
        augustMessage = augustMessageTx.getServerTransaction().getRequest();
        augustMessageAccepted = augustSipStack.getMessageFactory().createResponse(Response.ACCEPTED, augustMessage);
        augustMessageTx.getServerTransaction().sendResponse(augustMessageAccepted);
        //HOLD - end

        // hangup.
        augustCall.disconnect();

        pstnCall.listenForDisconnect();
        assertTrue(pstnCall.waitForDisconnect(30 * 1000));
        assertTrue(pstnCall.respondToDisconnect());
        Thread.sleep(1000);

        filteredCallsByStatusObject = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(),
                adminAccountSid, adminAuthToken, filters);
        assertEquals(0, filteredCallsByStatusObject.get("calls").getAsJsonArray().size());

        unregisterAugust();
    }

    @Test
    public void testWebRTCClientIncomingBHold() throws SipException, InvalidArgumentException, InterruptedException, ParseException {
        logger.info("testWebRTCClientIncomingBHold");

        registerAugust();


        SipCall augustCall = augustPhone.createSipCall();
        SipCall pstnCall = pstnPhone.createSipCall();
        initiatePstn(pstnCall, augustCall);


        assertTrue(augustCall.waitForIncomingCall(30 * 1000));
        assertTrue(augustCall.sendIncomingCallResponse(Response.RINGING, "Ringing-August", 3600));
        String receivedBody = new String(augustCall.getLastReceivedRequest().getRawContent());
        assertTrue(augustCall.sendIncomingCallResponse(Response.OK, "OK-August", 3600, receivedBody, "application", "sdp", null,
                null));
        assertTrue(augustCall.waitForAck(50 * 1000));

        Thread.sleep(1000);

        int liveCalls = MonitoringServiceTool.getInstance().getStatistics(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        int liveCallsArraySize = MonitoringServiceTool.getInstance().getLiveCallsArraySize(deploymentUrl.toString(), adminAccountSid, adminAuthToken);
        assertTrue( liveCalls == 2);
        assertTrue(liveCallsArraySize  == 2);

        Map<String, String> filters = new HashMap<String, String>();
        JsonObject filteredCallsByStatusObject = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(),
                adminAccountSid, adminAuthToken, filters);
        assertEquals(2, filteredCallsByStatusObject.get("calls").getAsJsonArray().size());

        Thread.sleep(1000);

        //HOLD - start
        pstnCall.listenForMessage();

        SipTransaction augustReinviteTx = augustCall.sendReinvite(augustContact, augustContact, body + "a=sendonly", "application", "sdp");
        assertTrue(augustCall.waitReinviteResponse(augustReinviteTx, 5 * 1000));
        augustCall.sendReinviteOkAck(augustReinviteTx);

        assertTrue(pstnCall.waitForMessage(5 * 1000));
        SipRequest lastReceivedRequest = pstnCall.getLastReceivedRequest();
        receivedBody = new String(lastReceivedRequest.getRawContent());
        assertTrue(receivedBody.equals("action=onHold"));
        SipTransaction pstnMessageTx = pstnCall.getLastTransaction();
        Request pstnMessage = pstnMessageTx.getServerTransaction().getRequest();
        Response pstnMessageAccepted = imsSipStack.getMessageFactory().createResponse(Response.ACCEPTED, pstnMessage);
        pstnMessageTx.getServerTransaction().sendResponse(pstnMessageAccepted);

        augustReinviteTx = augustCall.sendReinvite(augustContact, augustContact, body + "a=sendrecv", "application", "sdp");
        assertTrue(augustCall.waitReinviteResponse(augustReinviteTx, 5 * 1000));
        augustCall.sendReinviteOkAck(augustReinviteTx);

        assertTrue(pstnCall.waitForMessage(5 * 1000));
        lastReceivedRequest = pstnCall.getLastReceivedRequest();
        receivedBody = new String(lastReceivedRequest.getRawContent());
        assertTrue(receivedBody.equals("action=offHold"));
        pstnMessageTx = pstnCall.getLastTransaction();
        pstnMessage = pstnMessageTx.getServerTransaction().getRequest();
        pstnMessageAccepted = imsSipStack.getMessageFactory().createResponse(Response.ACCEPTED, pstnMessage);
        pstnMessageTx.getServerTransaction().sendResponse(pstnMessageAccepted);
        //HOLD - end

        // hangup.
        pstnCall.disconnect();

        augustCall.listenForDisconnect();
        assertTrue(augustCall.waitForDisconnect(30 * 1000));
        assertTrue(augustCall.respondToDisconnect());
        Thread.sleep(1000);

        filteredCallsByStatusObject = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(),
                adminAccountSid, adminAuthToken, filters);
        assertEquals(0, filteredCallsByStatusObject.get("calls").getAsJsonArray().size());

        unregisterAugust();
    }

    @Test
    public void testWebRTCClientIncomingRequestTimeout() throws InterruptedException, ParseException {
        logger.info("testWebRTCClientIncomingRequestTimeout");

        registerAugust();


        SipCall augustCall = augustPhone.createSipCall();
        SipCall pstnCall = pstnPhone.createSipCall();
        initiatePstn(pstnCall, augustCall);

        assertTrue(augustCall.waitForIncomingCall(5 * 1000));
        assertTrue(augustCall.sendIncomingCallResponse(Response.RINGING, "RINGING-pstn", 3600));
        SipTransaction augustInviteTx = augustCall.getLastTransaction();
        assertTrue(augustCall.listenForCancel());
        assertTrue(pstnCall.listenForDisconnect());

        Thread.sleep(30000);


        SipTransaction augustCancelTransaction = augustCall.waitForCancel(5 * 1000);
        assertTrue(augustCancelTransaction != null);
        augustCall.respondToCancel(augustCancelTransaction, 200, "OK-pstn", 3600);
        logger.info("finish waiting");

        Request augustInvite = augustInviteTx.getServerTransaction().getRequest();
        Response augustResponseTerminated = imsSipStack.getMessageFactory().createResponse(Response.REQUEST_TERMINATED, augustInvite);
        try{
            augustInviteTx.getServerTransaction().sendResponse(augustResponseTerminated);
        }
        catch(Exception e){
            e.printStackTrace();
        }

           assertTrue(pstnCall.waitForDisconnect(5 * 1000));
           assertTrue(pstnCall.respondToDisconnect());
           logger.info("august disconnected");

       Map<String, String> filters = new HashMap<String, String>();
       JsonObject filteredCallsByStatusObject = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(),
               adminAccountSid, adminAuthToken, filters);
       assertEquals(0, filteredCallsByStatusObject.get("calls").getAsJsonArray().size());

        unregisterAugust();
    }

    @Test //Non regression test for issue https://github.com/RestComm/Restcomm-Connect/issues/1042 - Support WebRTC clients to dial out through MediaServer
    public void testWebRTCClientOutgoingBusy() throws ParseException, InterruptedException {

        logger.info("testWebRTCClientOutgoingBusy");
        registerAugust();


        SipCall pstnCall = pstnPhone.createSipCall();
        final SipCall augustCall = augustPhone.createSipCall();
        initiateAugust(pstnCall,pstnContact,augustCall);


        assertTrue(pstnCall.waitForIncomingCall(5 * 1000));
        assertTrue(pstnCall.sendIncomingCallResponse(Response.BUSY_HERE, "Busy-Pstn", 3600));

        augustCall.listenForDisconnect();
        assertTrue(augustCall.waitForDisconnect(30 * 1000));
        assertTrue(augustCall.respondToDisconnect());
        Thread.sleep(1000);

        Map<String, String> filters = new HashMap<String, String>();
        JsonObject filteredCallsByStatusObject = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(),
                adminAccountSid, adminAuthToken, filters);
        assertEquals(0, filteredCallsByStatusObject.get("calls").getAsJsonArray().size());

        unregisterAugust();
    }

    @Test //Non regression test for issue https://github.com/RestComm/Restcomm-Connect/issues/1042 - Support WebRTC clients to dial out through MediaServer
    public void testUnregisteredWebRTCClientOutgoing() throws ParseException, InterruptedException {
        logger.info("testUnregisteredWebRTCClientOutgoing");

        //Change UserAgent header to "sipunit" so CallManager
        ArrayList<String> replaceHeaders = new ArrayList<String>();
        List<String> userAgentList = new ArrayList<String>();
        userAgentList.add("wss-sipunit");
        UserAgentHeader userAgentHeader = augustSipStack.getHeaderFactory().createUserAgentHeader(userAgentList);
        replaceHeaders.add(userAgentHeader.toString());

        // August initiates a call to pstn
        final SipCall augustCall = augustPhone.createSipCall();
        URI uri1 = augustSipStack.getAddressFactory().createURI("sip:127.0.0.1:5080");
        SipURI sipURI = (SipURI) uri1;
        sipURI.setLrParam();
        Address address = augustSipStack.getAddressFactory().createAddress(uri1);

        RouteHeader routeHeader = augustSipStack.getHeaderFactory().createRouteHeader(address);
        replaceHeaders.add(routeHeader.toString());
        Header user = augustSipStack.getHeaderFactory().createHeader("X-RestComm-Ims-User", "myUser");
        Header pass = augustSipStack.getHeaderFactory().createHeader("X-RestComm-Ims-Password", "myPass");
        replaceHeaders.add(user.toString());
        replaceHeaders.add(pass.toString());
        augustCall.initiateOutgoingCall(augustContact, "sip:"+pstnNumber+"@127.0.0.1:5060", null, body, "application", "sdp", null, replaceHeaders);
        assertLastOperationSuccess(augustCall);

        assertTrue(augustCall.waitOutgoingCallResponse(5 * 1000));
        int responseAugust = augustCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseAugust == Response.NOT_FOUND);

        Map<String, String> filters = new HashMap<String, String>();
        JsonObject filteredCallsByStatusObject = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(),
                adminAccountSid, adminAuthToken, filters);
        assertEquals(0, filteredCallsByStatusObject.get("calls").getAsJsonArray().size());

    }

    @Test
    public void testUnregisteredWebRTCClientIncoming() throws InterruptedException, ParseException {
        logger.info("testUnregisteredWebRTCClientIncoming");

        // Prepare august phone to receive call
        SipCall augustCall = augustPhone.createSipCall();
        augustCall.listenForIncomingCall();

        // Create outgoing call with pstn phone
        final SipCall pstnCall = pstnPhone.createSipCall();
        pstnCall.initiateOutgoingCall(pstnContact, augustContact, null, body, "application", "sdp", null, null);
        assertLastOperationSuccess(pstnCall);
        assertTrue(pstnCall.waitOutgoingCallResponse(5 * 1000));
        final int response = pstnCall.getLastReceivedResponse().getStatusCode();
        assertTrue(response == Response.NOT_FOUND);

        Map<String, String> filters = new HashMap<String, String>();
        JsonObject filteredCallsByStatusObject = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(),
                adminAccountSid, adminAuthToken, filters);
        assertEquals(0, filteredCallsByStatusObject.get("calls").getAsJsonArray().size());
    }

    @Test //Non regression test for issue https://github.com/RestComm/Restcomm-Connect/issues/1042 - Support WebRTC clients to dial out through MediaServer
    public void testWebRTCClientOutgoingRequestTimeout() throws ParseException, InterruptedException {

        logger.info("testWebRTCClientOutgoingRequestTimeout");
        registerAugust();



        SipCall pstnCall = pstnPhone.createSipCall();
        final SipCall augustCall = augustPhone.createSipCall();
        initiateAugust(pstnCall,pstnContact,augustCall);

        assertTrue(pstnCall.waitForIncomingCall(5 * 1000));
        assertTrue(pstnCall.sendIncomingCallResponse(Response.RINGING, "RINGING-pstn", 3600));
        SipTransaction pstnInviteTx = pstnCall.getLastTransaction();
        assertTrue(pstnCall.listenForCancel());
        assertTrue(augustCall.listenForDisconnect());

        Thread.sleep(30000);

        SipTransaction pstnCancelTransaction = pstnCall.waitForCancel(5 * 1000);
        assertTrue(pstnCancelTransaction != null);
        pstnCall.respondToCancel(pstnCancelTransaction, 200, "OK-pstn", 3600);
        logger.info("finish waiting");

        Request pstnInvite = pstnInviteTx.getServerTransaction().getRequest();
        Response pstnResponseTerminated = imsSipStack.getMessageFactory().createResponse(Response.REQUEST_TERMINATED, pstnInvite);
        try{
            pstnInviteTx.getServerTransaction().sendResponse(pstnResponseTerminated);
        }
        catch(Exception e){
            e.printStackTrace();
        }

           assertTrue(augustCall.waitForDisconnect(5 * 1000));
           assertTrue(augustCall.respondToDisconnect());
           logger.info("august disconnected");

       Map<String, String> filters = new HashMap<String, String>();
       JsonObject filteredCallsByStatusObject = RestcommCallsTool.getInstance().getCallsUsingFilter(deploymentUrl.toString(),
               adminAccountSid, adminAuthToken, filters);
       assertEquals(0, filteredCallsByStatusObject.get("calls").getAsJsonArray().size());

        unregisterAugust();
    }

    private void unregisterAugust() throws InterruptedException{
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                imsAugustPhone.listenRequestMessage();
                RequestEvent requestEvent = imsAugustPhone.waitRequest(10000);
                assertNotNull(requestEvent);
                try {
                    Response response = imsSipStack.getMessageFactory().createResponse(200, requestEvent.getRequest());
                    ContactHeader contactHeader = augustSipStack.getHeaderFactory().createContactHeader();
                    contactHeader.setExpires(0);
                    contactHeader.setAddress(augustSipStack.getAddressFactory().createAddress(imsContact));
                    response.addHeader(contactHeader);
                    imsAugustPhone.sendReply(requestEvent, response);
                } catch (ParseException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                } catch (InvalidArgumentException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }

            }
        });

        assertTrue(augustPhone.unregister(augustContact, 3600));
        isAugustRegistered = false;
        Thread.sleep(1000);
    }

    private void unregisterBob() throws InterruptedException{
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                imsBobPhone.listenRequestMessage();
                RequestEvent requestEvent = imsBobPhone.waitRequest(10000);
                assertNotNull(requestEvent);
                try {
                    Response response = imsSipStack.getMessageFactory().createResponse(200, requestEvent.getRequest());
                    ContactHeader contactHeader = bobSipStack.getHeaderFactory().createContactHeader();
                    contactHeader.setExpires(0);
                    contactHeader.setAddress(bobSipStack.getAddressFactory().createAddress(imsContact));
                    response.addHeader(contactHeader);
                    imsBobPhone.sendReply(requestEvent, response);
                } catch (ParseException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                } catch (InvalidArgumentException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }

            }
        });

        assertTrue(bobPhone.unregister(bobContact, 3600));
        isBobRegistered = false;
        Thread.sleep(1000);
    }

    private void unregisterJulius() throws InterruptedException{
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                imsJuliusPhone.listenRequestMessage();
                RequestEvent requestEvent = imsJuliusPhone.waitRequest(10000);
                assertNotNull(requestEvent);
                try {
                    Response response = imsSipStack.getMessageFactory().createResponse(200, requestEvent.getRequest());
                    ContactHeader contactHeader = juliusSipStack.getHeaderFactory().createContactHeader();
                    contactHeader.setExpires(0);
                    contactHeader.setAddress(juliusSipStack.getAddressFactory().createAddress(imsContact));
                    response.addHeader(contactHeader);
                    imsJuliusPhone.sendReply(requestEvent, response);
                } catch (ParseException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                } catch (InvalidArgumentException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }

            }
        });

        assertTrue(juliusPhone.unregister(juliusContact, 3600));
        isJuliusRegistered = false;
        Thread.sleep(1000);
    }

    private void registerAugust() throws ParseException, InterruptedException{
        SipURI uri = augustSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                imsAugustPhone.listenRequestMessage();
                RequestEvent requestEvent = imsAugustPhone.waitRequest(10000);
                assertNotNull(requestEvent);
                try {
                    Response response = imsSipStack.getMessageFactory().createResponse(200, requestEvent.getRequest());
                    ContactHeader contactHeader = augustSipStack.getHeaderFactory().createContactHeader();
                    contactHeader.setExpires(14400);
                    contactHeader.setAddress(augustSipStack.getAddressFactory().createAddress(imsContact));
                    response.addHeader(contactHeader);
                    imsAugustPhone.sendReply(requestEvent, response);
                } catch (ParseException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                } catch (InvalidArgumentException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }

            }
        });

        assertTrue(augustPhone.register(uri, "august", "1234", augustContact, 14400, 3600));
        isAugustRegistered = true;
        Thread.sleep(1000);

        Credential c = new Credential("127.0.0.1", "august", "1234");
        augustPhone.addUpdateCredential(c);
    }

    private void registerBob() throws ParseException, InterruptedException{
        SipURI uri = bobSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                imsBobPhone.listenRequestMessage();
                RequestEvent requestEvent = imsBobPhone.waitRequest(10000);
                assertNotNull(requestEvent);
                try {
                    Response response = imsSipStack.getMessageFactory().createResponse(200, requestEvent.getRequest());
                    ContactHeader contactHeader = bobSipStack.getHeaderFactory().createContactHeader();
                    contactHeader.setExpires(14400);
                    contactHeader.setAddress(bobSipStack.getAddressFactory().createAddress(imsContact));
                    response.addHeader(contactHeader);
                    imsBobPhone.sendReply(requestEvent, response);
                } catch (ParseException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                } catch (InvalidArgumentException e) {
                    e.printStackTrace();
                    fail(e.getMessage());
                }

            }
        });

        assertTrue(bobPhone.register(uri, "bob@bob.com", "1234", bobContact, 14400, 3600));
        isBobRegistered = true;
        Thread.sleep(1000);

        Credential c = new Credential("127.0.0.1", "bob@bob.com", "1234");
        bobPhone.addUpdateCredential(c);
    }

    private void initiateAugust(SipCall toCall, String toUri, SipCall augustCall) throws ParseException, InterruptedException {
        toCall.listenForIncomingCall();


        Thread.sleep(1000);

        //Change UserAgent header to "sipunit" so CallManager
        ArrayList<String> replaceHeaders = new ArrayList<String>();
        List<String> userAgentList = new ArrayList<String>();
        userAgentList.add("wss-sipunit");
        UserAgentHeader userAgentHeader = augustSipStack.getHeaderFactory().createUserAgentHeader(userAgentList);
        replaceHeaders.add(userAgentHeader.toString());

        // August initiates a call to pstn
        URI uri1 = augustSipStack.getAddressFactory().createURI("sip:127.0.0.1:5080");
        SipURI sipURI = (SipURI) uri1;
        sipURI.setLrParam();
        Address address = augustSipStack.getAddressFactory().createAddress(uri1);

        RouteHeader routeHeader = augustSipStack.getHeaderFactory().createRouteHeader(address);
        replaceHeaders.add(routeHeader.toString());
        Header user = augustSipStack.getHeaderFactory().createHeader("X-RestComm-Ims-User", "myUser");
        Header pass = augustSipStack.getHeaderFactory().createHeader("X-RestComm-Ims-Password", "myPass");
        replaceHeaders.add(user.toString());
        replaceHeaders.add(pass.toString());
        augustCall.initiateOutgoingCall(augustContact, toUri, null, body, "application", "sdp", null, replaceHeaders);
        assertLastOperationSuccess(augustCall);

        assertTrue(augustCall.waitOutgoingCallResponse(5 * 1000));
        int responseAugust = augustCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseAugust == Response.TRYING || responseAugust == Response.RINGING);

        Dialog augustDialog = null;

        if (responseAugust == Response.TRYING) {
            assertTrue(augustCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, augustCall.getLastReceivedResponse().getStatusCode());
            augustDialog = augustCall.getDialog();
            assertNotNull(augustDialog);
        }

        assertTrue(augustCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, augustCall.getLastReceivedResponse().getStatusCode());
        assertTrue(augustCall.sendInviteOkAck());

        assertTrue(toCall.waitForIncomingCall(5 * 1000));

        DigestServerAuthenticationMethod dsam = new DigestServerAuthenticationMethod();
        dsam.initialize(); // it should read values from file, now all static

        ProxyAuthenticateHeader proxyAuthenticate = augustSipStack.getHeaderFactory().createProxyAuthenticateHeader(
                dsam.getScheme());
        proxyAuthenticate.setParameter("realm", dsam.getRealm(null));
        proxyAuthenticate.setParameter("nonce", dsam.generateNonce());
        // proxyAuthenticateImpl.setParameter("domain",authenticationMethod.getDomain());
        proxyAuthenticate.setParameter("opaque", "");

        proxyAuthenticate.setParameter("algorithm", dsam.getAlgorithm());
        ArrayList<Header> headers = new ArrayList<Header>();
        headers.add(proxyAuthenticate);
        assertTrue(toCall.sendIncomingCallResponse(Response.PROXY_AUTHENTICATION_REQUIRED, "Non authorized", 3600, headers,
                null, null));

    }

    private void initiateBob(SipCall toCall, String toUri, SipCall bobCall) throws ParseException, InterruptedException {
        toCall.listenForIncomingCall();


        Thread.sleep(1000);

        //Change UserAgent header to "sipunit" so CallManager
        ArrayList<String> replaceHeaders = new ArrayList<String>();
        List<String> userAgentList = new ArrayList<String>();
        userAgentList.add("wss-sipunit");
        UserAgentHeader userAgentHeader = bobSipStack.getHeaderFactory().createUserAgentHeader(userAgentList);
        replaceHeaders.add(userAgentHeader.toString());

        // Bob initiates a call to pstn
        URI uri1 = bobSipStack.getAddressFactory().createURI("sip:127.0.0.1:5080");
        SipURI sipURI = (SipURI) uri1;
        sipURI.setLrParam();
        Address address = bobSipStack.getAddressFactory().createAddress(uri1);

        RouteHeader routeHeader = bobSipStack.getHeaderFactory().createRouteHeader(address);
        replaceHeaders.add(routeHeader.toString());
        Header user = bobSipStack.getHeaderFactory().createHeader("X-RestComm-Ims-User", "myUser");
        Header pass = bobSipStack.getHeaderFactory().createHeader("X-RestComm-Ims-Password", "myPass");
        replaceHeaders.add(user.toString());
        replaceHeaders.add(pass.toString());
        bobCall.initiateOutgoingCall(bobContact, toUri, null, body, "application", "sdp", null, replaceHeaders);
        assertLastOperationSuccess(bobCall);

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        int responseBob = bobCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responseBob == Response.TRYING || responseBob == Response.RINGING);

        Dialog bobDialog = null;

        if (responseBob == Response.TRYING) {
            assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, bobCall.getLastReceivedResponse().getStatusCode());
            bobDialog = bobCall.getDialog();
            assertNotNull(bobDialog);
        }

        assertTrue(bobCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, bobCall.getLastReceivedResponse().getStatusCode());
        assertTrue(bobCall.sendInviteOkAck());

        assertTrue(toCall.waitForIncomingCall(5 * 1000));

        DigestServerAuthenticationMethod dsam = new DigestServerAuthenticationMethod();
        dsam.initialize(); // it should read values from file, now all static

        ProxyAuthenticateHeader proxyAuthenticate = bobSipStack.getHeaderFactory().createProxyAuthenticateHeader(
                dsam.getScheme());
        proxyAuthenticate.setParameter("realm", dsam.getRealm(null));
        proxyAuthenticate.setParameter("nonce", dsam.generateNonce());
        // proxyAuthenticateImpl.setParameter("domain",authenticationMethod.getDomain());
        proxyAuthenticate.setParameter("opaque", "");

        proxyAuthenticate.setParameter("algorithm", dsam.getAlgorithm());
        ArrayList<Header> headers = new ArrayList<Header>();
        headers.add(proxyAuthenticate);
        assertTrue(toCall.sendIncomingCallResponse(Response.PROXY_AUTHENTICATION_REQUIRED, "Non authorized", 3600, headers,
                null, null));

    }

    private void initiatePstn(SipCall pstnCall, SipCall augustCall) throws ParseException, InterruptedException {
        // Prepare august phone to receive call

        augustCall.listenForIncomingCall();

        // Create outgoing call with pstn phone
        ArrayList<String> replaceHeaders = new ArrayList<String>();
        URI uri1 = augustSipStack.getAddressFactory().createURI("sip:august@127.0.0.1:5080");
        SipURI sipURI = (SipURI) uri1;
        sipURI.setLrParam();
        Address address = augustSipStack.getAddressFactory().createAddress(uri1);
        ToHeader toHeader = augustSipStack.getHeaderFactory().createToHeader(address, null);
        replaceHeaders.add(toHeader.toString());

        pstnCall.initiateOutgoingCall(pstnContact, "sip:127.0.0.1:5080", null, body, "application", "sdp", null, replaceHeaders);
        assertLastOperationSuccess(pstnCall);
        assertTrue(pstnCall.waitOutgoingCallResponse(5 * 1000));
        int responsePstn = pstnCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responsePstn == Response.TRYING || responsePstn == Response.RINGING);

        if (responsePstn == Response.TRYING) {
            assertTrue(pstnCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, pstnCall.getLastReceivedResponse().getStatusCode());
        }

        assertTrue(pstnCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, pstnCall.getLastReceivedResponse().getStatusCode());
        assertTrue(pstnCall.sendInviteOkAck());
    }

    @Deployment(name = "ImsClientsDialTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("org.restcomm:restcomm-connect.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.addAsWebInfResource("sip-ims.xml", "/sip.xml");
        archive.addAsWebInfResource("restcomm-ims.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script_imsDialTest", "data/hsql/restcomm.script");
        archive.addAsWebResource("dial-conference-entry.xml");
        archive.addAsWebResource("dial-fork-entry.xml");
        archive.addAsWebResource("dial-uri-entry.xml");
        archive.addAsWebResource("dial-client-entry.xml");
        archive.addAsWebResource("dial-number-entry.xml");
        return archive;
    }
}
