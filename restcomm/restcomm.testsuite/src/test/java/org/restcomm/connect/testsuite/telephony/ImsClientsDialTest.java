package org.restcomm.connect.testsuite.telephony;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import gov.nist.javax.sip.header.ContentType;

import org.apache.log4j.Logger;
import org.cafesip.sipunit.*;
import org.jboss.arquillian.container.mss.extension.SipStackTool;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.*;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;
import org.restcomm.connect.testsuite.telephony.security.DigestServerAuthenticationMethod;

import javax.sip.Dialog;
import javax.sip.InvalidArgumentException;
import javax.sip.RequestEvent;
import javax.sip.ResponseEvent;
import javax.sip.SipException;
import javax.sip.address.Address;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.*;
import javax.sip.message.Request;
import javax.sip.message.Response;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.cafesip.sipunit.SipAssert.assertLastOperationSuccess;
import static org.junit.Assert.*;

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

    private String pstnNumber = "+151261006100";


    // Maria is a Restcomm Client **without** VoiceURL. This Restcomm Client can dial anything.
    private SipStack augustSipStack;
    private SipPhone augustPhone;
    private String augustContact = "sip:august@127.0.0.1:5092";
    private boolean isAugustRegistered = false;
    
    private SipStack juliusSipStack;
    private SipPhone juliusPhone;
    private String juliusContact = "sip:julius@127.0.0.1:5094";
    private boolean isJuliusRegistered = false;

    private SipStack imsSipStack;
    private SipPhone imsAugustPhone;
    private SipPhone imsJuliusPhone;
    private String imsContact = "sip:127.0.0.1";
    
    private SipPhone pstnPhone;
    private String pstnContact = "sip:"+pstnNumber+"@127.0.0.1:5060";


    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("ImsClientsDialTest1");
        tool2 = new SipStackTool("ImsClientsDialTest2");
        tool3 = new SipStackTool("ImsClientsDialTest3");
    }

    @Before
    public void before() throws Exception {
    	
    	imsSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5060", "127.0.0.1:5050");

        augustSipStack = tool2.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5092", "127.0.0.1:5050");
        augustPhone = augustSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5050, augustContact);
        imsAugustPhone = imsSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5050, augustContact);
        imsAugustPhone.setLoopback(true);
        
        juliusSipStack = tool3.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5094", "127.0.0.1:5050");
        juliusPhone = juliusSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5050, juliusContact);
        imsJuliusPhone = imsSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5050, juliusContact);
        imsJuliusPhone.setLoopback(true);
        
        pstnPhone = imsSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5050, pstnContact);
        
        if(isAugustRegistered){
        	unregisterAugust();
        }
        
        if(isJuliusRegistered){
        	unregisterJulius();
        }

    }

    @After
    public void after() throws Exception {
    	System.out.println("dupa after");
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
        if (imsJuliusPhone != null) {
        	imsJuliusPhone.dispose();
        }

        Thread.sleep(3000);
        wireMockRule.resetRequests();
        Thread.sleep(3000);
    }

    @Test
    public void testRegisterClients() throws ParseException, InterruptedException {
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

        Thread.sleep(1000);

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
        isAugustRegistered = false;
    }
    
    @Test
    public void testWebRTCClientOutgoingAdisconnect() throws ParseException, InterruptedException {

        logger.info("testWebRTCClientOutgoingAdisconnect");
        registerAugust();

        SipCall pstnCall = pstnPhone.createSipCall();
        final SipCall augustCall = augustPhone.createSipCall();
        initiateAugust(pstnCall,pstnContact,augustCall);
        
        /*assertTrue(pstnCall.waitForIncomingCall(5 * 1000));
        assertTrue(pstnCall.sendIncomingCallResponse(Response.RINGING, "RINGING-Pstn", 3600));
        
        Thread.sleep(500);

        SipRequest lastReceivedRequest = pstnCall.getLastReceivedRequest();
        String receivedBody = new String(lastReceivedRequest.getRawContent());
        assertTrue(pstnCall.sendIncomingCallResponse(Response.OK, "OK-Pstn", 3600, receivedBody, "application", "sdp", null,
                null));

        int responseAugust = augustCall.getLastReceivedResponse().getStatusCode();
            while(responseAugust != Response.OK){
                assertTrue(augustCall.waitOutgoingCallResponse(5 * 1000));
                responseAugust = augustCall.getLastReceivedResponse().getStatusCode();
            }
        assertTrue(augustCall.sendInviteOkAck());*/

        // temporary start
        assertTrue(pstnCall.waitForIncomingCall(5 * 1000));
        assertTrue(pstnCall.sendIncomingCallResponse(Response.RINGING, "RINGING-pstn", 3600));

        SipRequest lastReceivedRequest = pstnCall.getLastReceivedRequest();
        String receivedBody = new String(lastReceivedRequest.getRawContent());
        assertTrue(pstnCall.sendIncomingCallResponse(Response.OK, "OK-pstn", 3600, receivedBody, "application", "sdp", null,
                null));
        // temporary end
        


        Thread.sleep(3000);
        pstnCall.listenForDisconnect();
        assertTrue(augustCall.disconnect());

        assertTrue(pstnCall.waitForDisconnect(5 * 1000));
        assertTrue(pstnCall.respondToDisconnect());

        
        Thread.sleep(1000);
        
        unregisterAugust();
    }

    @Test
    public void testWebRTCClientOutgoingAHold() throws ParseException, InterruptedException, InvalidArgumentException {

        logger.info("testWebRTCClientOutgoingAHold");
        registerAugust();

        SipCall pstnCall = pstnPhone.createSipCall();
        final SipCall augustCall = augustPhone.createSipCall();
        initiateAugust(pstnCall,pstnContact,augustCall);
        
        /*assertTrue(pstnCall.waitForIncomingCall(5 * 1000));
        assertTrue(pstnCall.sendIncomingCallResponse(Response.RINGING, "RINGING-Pstn", 3600));
        
        Thread.sleep(500);
        
        SipRequest lastReceivedRequest = pstnCall.getLastReceivedRequest();
        String receivedBody = new String(lastReceivedRequest.getRawContent());
        assertTrue(pstnCall.sendIncomingCallResponse(Response.OK, "OK-Pstn", 3600, receivedBody, "application", "sdp", null,
                null));
        
        int responseAugust = augustCall.getLastReceivedResponse().getStatusCode();
        while(responseAugust != Response.OK){
            assertTrue(augustCall.waitOutgoingCallResponse(5 * 1000));
            responseAugust = augustCall.getLastReceivedResponse().getStatusCode();
        }
        assertTrue(augustCall.sendInviteOkAck());*/
        
        
       // temporary start
        assertTrue(pstnCall.waitForIncomingCall(5 * 1000));
        assertTrue(pstnCall.sendIncomingCallResponse(Response.RINGING, "RINGING-pstn", 3600));

        SipRequest lastReceivedRequest = pstnCall.getLastReceivedRequest();
        String receivedBody = new String(lastReceivedRequest.getRawContent());
        assertTrue(pstnCall.sendIncomingCallResponse(Response.OK, "OK-pstn", 3600, receivedBody, "application", "sdp", null,
                null));
        // temporary end
        
        Thread.sleep(1000);
        
        //HOLD - start
        SipTransaction augustReinviteTx = augustCall.sendReinvite(augustContact, augustContact, body + "a=sendonly", "application", "sdp");
        assertTrue(augustCall.waitReinviteResponse(augustReinviteTx, 5 * 1000));
        augustCall.sendReinviteOkAck(augustReinviteTx);
        //HOLD - end


        Thread.sleep(3000);
        pstnCall.listenForDisconnect();
        assertTrue(augustCall.disconnect());

        assertTrue(pstnCall.waitForDisconnect(5 * 1000));
        assertTrue(pstnCall.respondToDisconnect());
        Thread.sleep(1000);
        
        unregisterAugust();
    }
    
    @Test
    public void testWebRTCClientOutgoingBHold() throws ParseException, InterruptedException, InvalidArgumentException {

        logger.info("testWebRTCClientOutgoingBHold");
        registerAugust();

        SipCall pstnCall = pstnPhone.createSipCall();
        final SipCall augustCall = augustPhone.createSipCall();
        initiateAugust(pstnCall,pstnContact,augustCall);
        
        /*assertTrue(pstnCall.waitForIncomingCall(5 * 1000));
        assertTrue(pstnCall.sendIncomingCallResponse(Response.RINGING, "RINGING-Pstn", 3600));
        
        Thread.sleep(500);

        SipRequest lastReceivedRequest = pstnCall.getLastReceivedRequest();
        String receivedBody = new String(lastReceivedRequest.getRawContent());
        assertTrue(pstnCall.sendIncomingCallResponse(Response.OK, "OK-Pstn", 3600, receivedBody, "application", "sdp", null,
                null));

        //        For a reason the ACK will never reach Restcomm. This is only when working with the sipUnit
        //        assertTrue(georgeCall.waitForAck(5 * 1000));
        
        int responseAugust = Response.RINGING;
        while(responseAugust != Response.OK){
            assertTrue(augustCall.waitOutgoingCallResponse(5 * 1000));
            responseAugust = augustCall.getLastReceivedResponse().getStatusCode();
        }
        assertTrue(augustCall.sendInviteOkAck());*/
     
        // temporary start
        assertTrue(pstnCall.waitForIncomingCall(5 * 1000));
        assertTrue(pstnCall.sendIncomingCallResponse(Response.RINGING, "RINGING-pstn", 3600));

        SipRequest lastReceivedRequest = pstnCall.getLastReceivedRequest();
        String receivedBody = new String(lastReceivedRequest.getRawContent());
        assertTrue(pstnCall.sendIncomingCallResponse(Response.OK, "OK-pstn", 3600, receivedBody, "application", "sdp", null,
                null));
        // temporary end
        
        Thread.sleep(1000);
        
        //HOLD - start
        SipTransaction pstnReinviteTx = pstnCall.sendReinvite(pstnContact, pstnContact, body + "a=sendonly", "application", "sdp");
        assertTrue(pstnCall.waitReinviteResponse(pstnReinviteTx, 5 * 1000));
        pstnCall.sendReinviteOkAck(pstnReinviteTx);
        //HOLD - end


        Thread.sleep(3000);
        augustCall.listenForDisconnect();
        assertTrue(pstnCall.disconnect());

        assertTrue(augustCall.waitForDisconnect(5 * 1000));
        assertTrue(augustCall.respondToDisconnect());
        Thread.sleep(1000);
        
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

        /*int responsePstn = Response.RINGING;
        while(responsePstn != Response.OK){
            assertTrue(pstnCall.waitOutgoingCallResponse(5 * 1000));
            responsePstn = pstnCall.getLastReceivedResponse().getStatusCode();
        }
        assertTrue(pstnCall.sendInviteOkAck());*/
        
        Thread.sleep(3000);

        // hangup.
        augustCall.listenForDisconnect();
        pstnCall.disconnect();
        assertTrue(augustCall.waitForDisconnect(30 * 1000));
        assertTrue(augustCall.respondToDisconnect());
        Thread.sleep(1000);
        
        unregisterAugust();
    }
    
    @Test
    public void testWebRTCClientIncomingAHold() throws InterruptedException, ParseException {
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

        /*int responsePstn = Response.RINGING;
        while(responsePstn != Response.OK){
            assertTrue(pstnCall.waitOutgoingCallResponse(5 * 1000));
            responsePstn = pstnCall.getLastReceivedResponse().getStatusCode();
        }
        assertTrue(pstnCall.sendInviteOkAck());*/
        
        Thread.sleep(1000);
        
        //HOLD - start
        SipTransaction pstnReinviteTx = pstnCall.sendReinvite(pstnContact, pstnContact, body + "a=sendonly", "application", "sdp");
        assertTrue(pstnCall.waitReinviteResponse(pstnReinviteTx, 5 * 1000));
        pstnCall.sendReinviteOkAck(pstnReinviteTx);
        //HOLD - end

        // hangup.
        augustCall.disconnect();

        pstnCall.listenForDisconnect();
        assertTrue(pstnCall.waitForDisconnect(30 * 1000));
        assertTrue(pstnCall.respondToDisconnect());
        Thread.sleep(1000);
        
        unregisterAugust();
    }
    
    @Test
    public void testWebRTCClientIncomingBHold() throws InterruptedException, ParseException {
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

        /*int responsePstn = Response.RINGING;
        while(responsePstn != Response.OK){
            assertTrue(pstnCall.waitOutgoingCallResponse(5 * 1000));
            responsePstn = pstnCall.getLastReceivedResponse().getStatusCode();
        }
        assertTrue(pstnCall.sendInviteOkAck());*/
        
        Thread.sleep(1000);
        
        //HOLD - start
        SipTransaction augustReinviteTx = augustCall.sendReinvite(augustContact, augustContact, body + "a=sendonly", "application", "sdp");
        assertTrue(augustCall.waitReinviteResponse(augustReinviteTx, 5 * 1000));
        augustCall.sendReinviteOkAck(augustReinviteTx);        
        Thread.sleep(1000);
        //HOLD - end

        // hangup.
        pstnCall.disconnect();

        augustCall.listenForDisconnect();
        assertTrue(augustCall.waitForDisconnect(30 * 1000));
        assertTrue(augustCall.respondToDisconnect());
        Thread.sleep(1000);
        
        unregisterAugust();
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
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("restcomm-ims.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script_dialTest", "data/hsql/restcomm.script");
        archive.addAsWebResource("dial-conference-entry.xml");
        archive.addAsWebResource("dial-fork-entry.xml");
        archive.addAsWebResource("dial-uri-entry.xml");
        archive.addAsWebResource("dial-client-entry.xml");
        archive.addAsWebResource("dial-number-entry.xml");
        return archive;
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
    
    private void registerJulius() throws ParseException, InterruptedException{
    	SipURI uri = juliusSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");

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
                    contactHeader.setExpires(14400);
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

        assertTrue(juliusPhone.register(uri, "julius", "1234", juliusContact, 14400, 3600));
        isJuliusRegistered = true;
        Thread.sleep(1000);
        
        Credential c = new Credential("127.0.0.1", "julius", "1234");
        juliusPhone.addUpdateCredential(c);
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
        URI uri1 = augustSipStack.getAddressFactory().createURI("sip:127.0.0.1:5050");
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
        
        // temporary start
        assertTrue(augustCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, augustCall.getLastReceivedResponse().getStatusCode());
        assertTrue(augustCall.sendInviteOkAck());
        // temporary end
        
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
    
    private void initiatePstn(SipCall pstnCall, SipCall augustCall) throws ParseException, InterruptedException {
        // Prepare august phone to receive call
        
        augustCall.listenForIncomingCall();

        // Create outgoing call with pstn phone
        ArrayList<String> replaceHeaders = new ArrayList<String>();
        URI uri1 = augustSipStack.getAddressFactory().createURI("sip:august@127.0.0.1:5050");
        SipURI sipURI = (SipURI) uri1;
        sipURI.setLrParam();
        Address address = augustSipStack.getAddressFactory().createAddress(uri1);
        ToHeader toHeader = augustSipStack.getHeaderFactory().createToHeader(address, null);
        replaceHeaders.add(toHeader.toString());
        
        pstnCall.initiateOutgoingCall(pstnContact, "sip:127.0.0.1:5050", null, body, "application", "sdp", null, replaceHeaders);
        assertLastOperationSuccess(pstnCall);
        assertTrue(pstnCall.waitOutgoingCallResponse(5 * 1000));
        int responsePstn = pstnCall.getLastReceivedResponse().getStatusCode();
        assertTrue(responsePstn == Response.TRYING || responsePstn == Response.RINGING);

        if (responsePstn == Response.TRYING) {
            assertTrue(pstnCall.waitOutgoingCallResponse(5 * 1000));
            assertEquals(Response.RINGING, pstnCall.getLastReceivedResponse().getStatusCode());
        }    
        
        // temporary start
        assertTrue(pstnCall.waitOutgoingCallResponse(5 * 1000));
        assertEquals(Response.OK, pstnCall.getLastReceivedResponse().getStatusCode());
        assertTrue(pstnCall.sendInviteOkAck());
        // temporary end
    }
}
