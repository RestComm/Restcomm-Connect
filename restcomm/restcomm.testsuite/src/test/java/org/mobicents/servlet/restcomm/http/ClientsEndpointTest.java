package org.mobicents.servlet.restcomm.http;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.sip.address.SipURI;

import org.apache.http.client.ClientProtocolException;
import org.cafesip.sipunit.SipCall;
import org.cafesip.sipunit.SipPhone;
import org.cafesip.sipunit.SipStack;
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
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * 
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 */

@RunWith(Arquillian.class)
public class ClientsEndpointTest {

    private static final String version = org.mobicents.servlet.restcomm.Version.getVersion();

    @ArquillianResource
    private Deployer deployer;
    @ArquillianResource
    URL deploymentUrl;

    private static SipStackTool tool1;
    private SipStack bobSipStack;
    private SipPhone bobPhone;
    private String bobContact = "sip:bob@127.0.0.1:5090";
    private String adminUsername = "administrator@company.com";
    private String presenceUsername = "presence@company.com";
    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String presenceAccountSid = "ACae6e420f425248d6a26948c17a9e2acd";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";
    private String steveClientSid = "CLae6e420f425248d6a26948c17a9e2acd";

    @BeforeClass
    public static void beforeClass() throws Exception {
        tool1 = new SipStackTool("ClientsEndpointTest");
    }

    @Before
    public void before() throws Exception {
        bobSipStack = tool1.initializeSipStack(SipStack.PROTOCOL_UDP, "127.0.0.1", "5090", "127.0.0.1:5080");
        bobPhone = bobSipStack.createSipPhone("127.0.0.1", SipStack.PROTOCOL_UDP, 5080, bobContact);
    }

    @After
    public void after() throws Exception {
        if (bobPhone != null) {
            bobPhone.dispose();
        }
        if (bobSipStack != null) {
            bobSipStack.dispose();
        }
    }

    // Issue 109: https://bitbucket.org/telestax/telscale-restcomm/issue/109
    @Test
    public void createClientTest() throws ClientProtocolException, IOException, ParseException, InterruptedException {

        SipURI reqUri = bobSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");

        String clientSID = CreateClientsTool.getInstance().createClient(deploymentUrl.toString(), "bob", "1234",
                "http://127.0.0.1:8080/restcomm/demos/welcome.xml");
        assertNotNull(clientSID);

        Thread.sleep(3000);

        String clientSID2 = CreateClientsTool.getInstance().createClient(deploymentUrl.toString(), "bob", "1234",
                "http://127.0.0.1:8080/restcomm/demos/welcome.xml");
        assertNotNull(clientSID2);

        Thread.sleep(3000);

        assertTrue(clientSID.equalsIgnoreCase(clientSID2));
        assertTrue(bobPhone.register(reqUri, "bob", "1234", bobContact, 1800, 1800));
        bobContact = "sip:mobile@127.0.0.1:5090";
        assertTrue(bobPhone.register(reqUri, "bob", "1234", bobContact, 1800, 1800));
        assertTrue(bobPhone.unregister(bobContact, 0));
    }

    @Test
    public void createClientTestNoVoiceUrl() throws ClientProtocolException, IOException, ParseException, InterruptedException {

        SipURI reqUri = bobSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");

        String clientSID = CreateClientsTool.getInstance().createClient(deploymentUrl.toString(), "bob", "1234", null);
        assertNotNull(clientSID);

        Thread.sleep(3000);

        String clientSID2 = CreateClientsTool.getInstance().createClient(deploymentUrl.toString(), "bob", "1234", null);
        assertNotNull(clientSID2);

        Thread.sleep(3000);

        assertTrue(clientSID.equalsIgnoreCase(clientSID2));
        assertTrue(bobPhone.register(reqUri, "bob", "1234", bobContact, 1800, 1800));
        bobContact = "sip:mobile@127.0.0.1:5090";
        assertTrue(bobPhone.register(reqUri, "bob", "1234", bobContact, 1800, 1800));
        assertTrue(bobPhone.unregister(bobContact, 0));
    }
    
    @Test
    public void presenceInfoAtClientsList() throws ClientProtocolException, IOException, ParseException, InterruptedException{
        JsonArray clients = RestcommClientsTool.getInstance().getClients(deploymentUrl.toString(), presenceUsername,
                presenceAccountSid, adminAuthToken);
        JsonObject client1 = clients.get(0).getAsJsonObject();
        String latestAppearance = client1.get("latest_appearance").getAsString();
        assertTrue(latestAppearance.equalsIgnoreCase("offline"));
    }
    
    @Test
    public void presenceInfoAtSingleClient() throws ClientProtocolException, IOException, ParseException, InterruptedException{
        JsonObject client = RestcommClientsTool.getInstance().getClient(deploymentUrl.toString(), presenceUsername,
                presenceAccountSid, adminAuthToken, steveClientSid, false);
        String latestAppearance = client.get("latest_appearance").getAsString();
        assertTrue(latestAppearance.equalsIgnoreCase("offline"));
    }
    
    @Test
    public void presenceInfoSpecificPath() throws ClientProtocolException, IOException, ParseException, InterruptedException{
        JsonObject client = RestcommClientsTool.getInstance().getClient(deploymentUrl.toString(), presenceUsername,
                presenceAccountSid, adminAuthToken, steveClientSid, true);
        String latestAppearance = client.get("latest_appearance").getAsString();
        assertTrue(latestAppearance.equalsIgnoreCase("offline"));
    }
    
    @Test
    public void presenceInfoUpdate() throws ClientProtocolException, IOException, ParseException, InterruptedException{
    	// Creating client
    	SipURI reqUri = bobSipStack.getAddressFactory().createSipURI(null, "127.0.0.1:5080");
    	String clientSID = CreateClientsTool.getInstance().createClient(deploymentUrl.toString(), "bob", "1234", null);
    	
    	// Sending register message
    	bobContact = "sip:bob@127.0.0.1:5090";
    	assertTrue(bobPhone.register(reqUri, "bob", "1234", bobContact, 1800, 1800));
    	
    	// Verifying new presence info at clients list
    	JsonArray clients = RestcommClientsTool.getInstance().getClients(deploymentUrl.toString(), adminUsername, adminAccountSid, adminAuthToken);
    	JsonObject client = clients.get(0).getAsJsonObject();
        String latestAppearanceString = client.get("latest_appearance").getAsString();
    	Date latestAppearance = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).parse(latestAppearanceString);
    	assertNotNull(latestAppearance);
    	
    	//Verifying new presence info at single client
    	client = RestcommClientsTool.getInstance().getClient(deploymentUrl.toString(), adminUsername, adminAccountSid, adminAuthToken, clientSID, false);
        latestAppearanceString = client.get("latest_appearance").getAsString();
    	latestAppearance = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).parse(latestAppearanceString);
    	assertNotNull(latestAppearance);
    	
    	//Verifying new presence info at specific path
    	client = RestcommClientsTool.getInstance().getClient(deploymentUrl.toString(), adminUsername, adminAccountSid, adminAuthToken, clientSID, true);
        latestAppearanceString = client.get("latest_appearance").getAsString();
    	latestAppearance = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).parse(latestAppearanceString);
    	assertNotNull(latestAppearance);
    }
    
    @Deployment(name = "ClientsEndpointTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
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
        archive.addAsWebInfResource("restcomm.script_clientsTest", "data/hsql/restcomm.script");
        return archive;
    }
}
