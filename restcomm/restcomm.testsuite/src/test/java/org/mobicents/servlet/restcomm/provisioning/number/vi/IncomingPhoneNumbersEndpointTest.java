package org.mobicents.servlet.restcomm.provisioning.number.vi;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;

/**
 * @author <a href="mailto:jean.deruelle@telestax.com">Jean Deruelle</a>
 */

@RunWith(Arquillian.class)
public class IncomingPhoneNumbersEndpointTest {
    private final static Logger logger = Logger.getLogger(IncomingPhoneNumbersEndpointTest.class.getName());

    private static final String version = org.mobicents.servlet.restcomm.Version.getVersion();

    @ArquillianResource
    private Deployer deployer;
    @ArquillianResource
    URL deploymentUrl;
    static boolean accountUpdated = false;

    private String adminUsername = "administrator@company.com";
    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";
    private String baseURL = "2012-04-24/Accounts/" + adminAccountSid + "/";
    
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(8090); // No-args constructor defaults to port 8080
    
    /*
     * https://www.twilio.com/docs/api/rest/incoming-phone-numbers#list-post-example-1
     * Purchases a new phone number for your account. If a phone number is found for your request, 
     * Twilio will add it to your account and bill you for the first month's cost of the phone number. 
     */
    @Test
    public void testPurchasePhoneNumberSuccess() {
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("queryDID"))
                .withRequestBody(containing("4156902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.queryDIDSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("assignDID"))
                .withRequestBody(containing("4156902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.purchaseNumberSuccessResponse)));
        // Get Account using admin email address and user email address
    	Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        String provisioningURL = deploymentUrl + baseURL + "IncomingPhoneNumbers.json";
        WebResource webResource = jerseyClient.resource(provisioningURL);

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("PhoneNumber", "+14156902867");
        formData.add("VoiceUrl", "http://demo.telestax.com/docs/voice.xml");
        formData.add("FriendlyName", "My Company Line");
        formData.add("VoiceMethod", "GET");
        ClientResponse clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept("application/json").post(ClientResponse.class, formData);
        assertTrue(clientResponse.getStatus() == 200);
        String response = clientResponse.getEntity(String.class);
        System.out.println(response);
        assertTrue(!response.trim().equalsIgnoreCase("[]"));
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
        
        System.out.println(jsonResponse.toString());
        
        assertTrue(IncomingPhoneNumbersEndpointTestUtils.match(jsonResponse.toString(),IncomingPhoneNumbersEndpointTestUtils.jSonResultPurchaseNumber));
    }
    
    /*
     * https://www.twilio.com/docs/api/rest/incoming-phone-numbers#list-post-example-1
     * Purchases a new phone number for your account.
     * If Twilio cannot find a phone number to match your request, you will receive an HTTP 400 with Twilio error code 21452.
     */
    @Test
    public void testPurchasePhoneNumberNoPhoneNumberFound() {
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("queryDID"))
                .withRequestBody(containing("4156902868"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.queryDIDSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("assignDID"))
                .withRequestBody(containing("4156902868"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.purchaseNumberSuccessResponse)));
        // Get Account using admin email address and user email address
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        String provisioningURL = deploymentUrl + baseURL + "IncomingPhoneNumbers.json";
        WebResource webResource = jerseyClient.resource(provisioningURL);

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("PhoneNumber", "+14156902867");
        formData.add("VoiceUrl", "http://demo.telestax.com/docs/voice.xml");
        formData.add("FriendlyName", "My Company Line");
        formData.add("VoiceMethod", "GET");
        ClientResponse clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept("application/json").post(ClientResponse.class, formData);
        assertTrue(clientResponse.getStatus() == 400);
        String response = clientResponse.getEntity(String.class);
        System.out.println(response);
        assertTrue(!response.trim().equalsIgnoreCase("[]"));
        JsonParser parser = new JsonParser();
        String jsonResponse = parser.parse(response).getAsString();
        assertTrue(jsonResponse.toString().equalsIgnoreCase("21452"));
    }
    
    /*
     * https://www.twilio.com/docs/api/rest/incoming-phone-numbers#list-post-example-1
     * Purchases a new phone number for your account. If a phone number is found for your request, 
     * Twilio will add it to your account and bill you for the first month's cost of the phone number. 
     */
    @Test
    public void testPurchaseLocalPhoneNumberSuccess() {
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("queryDID"))
                .withRequestBody(containing("4166902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.queryDIDSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("assignDID"))
                .withRequestBody(containing("4166902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.purchaseNumberSuccessResponse)));
        // Get Account using admin email address and user email address
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        String provisioningURL = deploymentUrl + baseURL + "IncomingPhoneNumbers/Local.json";
        WebResource webResource = jerseyClient.resource(provisioningURL);

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("PhoneNumber", "+14166902867");
        formData.add("VoiceUrl", "http://demo.telestax.com/docs/voice.xml");
        formData.add("FriendlyName", "My Company Line");
        formData.add("VoiceMethod", "GET");
        ClientResponse clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept("application/json").post(ClientResponse.class, formData);
        assertTrue(clientResponse.getStatus() == 200);
        String response = clientResponse.getEntity(String.class);
        System.out.println(response);
        assertTrue(!response.trim().equalsIgnoreCase("[]"));
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
        
        System.out.println(jsonResponse.toString());
        
        assertTrue(IncomingPhoneNumbersEndpointTestUtils.match(jsonResponse.toString(),IncomingPhoneNumbersEndpointTestUtils.jSonResultLocalPurchaseNumber));
    }
    
    /*
     * https://www.twilio.com/docs/api/rest/incoming-phone-numbers#list-post-example-1
     * Purchases a new phone number for your account.
     * If Twilio cannot find a phone number to match your request, you will receive an HTTP 400 with Twilio error code 21452.
     */
    @Test
    public void testPurchaseLocalPhoneNumberNoPhoneNumberFound() {
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("queryDID"))
                .withRequestBody(containing("4156902868"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.queryDIDSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("assignDID"))
                .withRequestBody(containing("4156902868"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.purchaseNumberSuccessResponse)));
        // Get Account using admin email address and user email address
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        String provisioningURL = deploymentUrl + baseURL + "IncomingPhoneNumbers/Local.json";
        WebResource webResource = jerseyClient.resource(provisioningURL);

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("PhoneNumber", "+14156902867");
        formData.add("VoiceUrl", "http://demo.telestax.com/docs/voice.xml");
        formData.add("FriendlyName", "My Company Line");
        formData.add("VoiceMethod", "GET");
        ClientResponse clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept("application/json").post(ClientResponse.class, formData);
        assertTrue(clientResponse.getStatus() == 400);
        String response = clientResponse.getEntity(String.class);
        System.out.println(response);
        assertTrue(!response.trim().equalsIgnoreCase("[]"));
        JsonParser parser = new JsonParser();
        String jsonResponse = parser.parse(response).getAsString();
        assertTrue(jsonResponse.toString().equalsIgnoreCase("21452"));
    }
    
    /*
     * https://www.twilio.com/docs/api/rest/incoming-phone-numbers#list-post-example-1
     * Purchases a new phone number for your account. If a phone number is found for your request, 
     * Twilio will add it to your account and bill you for the first month's cost of the phone number. 
     */
    @Test
    public void testPurchaseTollFreePhoneNumberSuccess() {
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("queryDID"))
                .withRequestBody(containing("4176902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.queryDIDSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("assignDID"))
                .withRequestBody(containing("4176902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.purchaseNumberSuccessResponse)));
        // Get Account using admin email address and user email address
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        String provisioningURL = deploymentUrl + baseURL + "IncomingPhoneNumbers/TollFree.json";
        WebResource webResource = jerseyClient.resource(provisioningURL);

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("PhoneNumber", "+14176902867");
        formData.add("VoiceUrl", "http://demo.telestax.com/docs/voice.xml");
        formData.add("FriendlyName", "My Company Line");
        formData.add("VoiceMethod", "GET");
        ClientResponse clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept("application/json").post(ClientResponse.class, formData);
        assertTrue(clientResponse.getStatus() == 200);
        String response = clientResponse.getEntity(String.class);
        System.out.println(response);
        assertTrue(!response.trim().equalsIgnoreCase("[]"));
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
        
        System.out.println(jsonResponse.toString());
        
        assertTrue(IncomingPhoneNumbersEndpointTestUtils.match(jsonResponse.toString(),IncomingPhoneNumbersEndpointTestUtils.jSonResultTollFreePurchaseNumber));
    }
    
    /*
     * https://www.twilio.com/docs/api/rest/incoming-phone-numbers#list-post-example-1
     * Purchases a new phone number for your account.
     * If Twilio cannot find a phone number to match your request, you will receive an HTTP 400 with Twilio error code 21452.
     */
    @Test
    public void testPurchaseTollFreePhoneNumberNoPhoneNumberFound() {
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("queryDID"))
                .withRequestBody(containing("4156902868"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.queryDIDSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("assignDID"))
                .withRequestBody(containing("4156902868"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.purchaseNumberSuccessResponse)));
        // Get Account using admin email address and user email address
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        String provisioningURL = deploymentUrl + baseURL + "IncomingPhoneNumbers/TollFree.json";
        WebResource webResource = jerseyClient.resource(provisioningURL);

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("PhoneNumber", "+14156902867");
        formData.add("VoiceUrl", "http://demo.telestax.com/docs/voice.xml");
        formData.add("FriendlyName", "My Company Line");
        formData.add("VoiceMethod", "GET");
        ClientResponse clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept("application/json").post(ClientResponse.class, formData);
        assertTrue(clientResponse.getStatus() == 400);
        String response = clientResponse.getEntity(String.class);
        System.out.println(response);
        assertTrue(!response.trim().equalsIgnoreCase("[]"));
        JsonParser parser = new JsonParser();
        String jsonResponse = parser.parse(response).getAsString();
        assertTrue(jsonResponse.toString().equalsIgnoreCase("21452"));
    }
    
    /*
     * https://www.twilio.com/docs/api/rest/incoming-phone-numbers#list-post-example-1
     * Purchases a new phone number for your account. If a phone number is found for your request, 
     * Twilio will add it to your account and bill you for the first month's cost of the phone number. 
     */
    @Test
    public void testPurchaseMobilePhoneNumberSuccess() {
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("queryDID"))
                .withRequestBody(containing("4186902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.queryDIDSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("assignDID"))
                .withRequestBody(containing("4186902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.purchaseNumberSuccessResponse)));
        // Get Account using admin email address and user email address
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        String provisioningURL = deploymentUrl + baseURL + "IncomingPhoneNumbers/Mobile.json";
        WebResource webResource = jerseyClient.resource(provisioningURL);

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("PhoneNumber", "+14186902867");
        formData.add("VoiceUrl", "http://demo.telestax.com/docs/voice.xml");
        formData.add("FriendlyName", "My Company Line");
        formData.add("VoiceMethod", "GET");
        ClientResponse clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept("application/json").post(ClientResponse.class, formData);
        assertTrue(clientResponse.getStatus() == 200);
        String response = clientResponse.getEntity(String.class);
        System.out.println(response);
        assertTrue(!response.trim().equalsIgnoreCase("[]"));
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
        
        System.out.println(jsonResponse.toString());
        
        assertTrue(IncomingPhoneNumbersEndpointTestUtils.match(jsonResponse.toString(),IncomingPhoneNumbersEndpointTestUtils.jSonResultMobilePurchaseNumber));
    }
    
    /*
     * https://www.twilio.com/docs/api/rest/incoming-phone-numbers#list-post-example-1
     * Purchases a new phone number for your account.
     * If Twilio cannot find a phone number to match your request, you will receive an HTTP 400 with Twilio error code 21452.
     */
    @Test
    public void testPurchaseMobilePhoneNumberNoPhoneNumberFound() {
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("queryDID"))
                .withRequestBody(containing("4156902868"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.queryDIDSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("assignDID"))
                .withRequestBody(containing("4156902868"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.purchaseNumberSuccessResponse)));
        // Get Account using admin email address and user email address
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        String provisioningURL = deploymentUrl + baseURL + "IncomingPhoneNumbers/Mobile.json";
        WebResource webResource = jerseyClient.resource(provisioningURL);

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("PhoneNumber", "+14156902867");
        formData.add("VoiceUrl", "http://demo.telestax.com/docs/voice.xml");
        formData.add("FriendlyName", "My Company Line");
        formData.add("VoiceMethod", "GET");
        ClientResponse clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept("application/json").post(ClientResponse.class, formData);
        assertTrue(clientResponse.getStatus() == 400);
        String response = clientResponse.getEntity(String.class);
        System.out.println(response);
        assertTrue(!response.trim().equalsIgnoreCase("[]"));
        JsonParser parser = new JsonParser();
        String jsonResponse = parser.parse(response).getAsString();
        assertTrue(jsonResponse.toString().equalsIgnoreCase("21452"));
    }
    
    /*
     * https://www.twilio.com/docs/api/rest/incoming-phone-numbers#instance-delete
     * Release this phone number from your account. Twilio will no longer answer calls to this number, and you will stop being billed the monthly phone number fee. The phone number will eventually be recycled and potentially given to another customer, so use with care. If you make a mistake, contact us. We may be able to give you the number back.
     * 
     * If successful, returns an HTTP 204 response with no body.
     */
    @Test
    public void testDeletePhoneNumberSuccess() {
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("queryDID"))
                .withRequestBody(containing("4196902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.queryDIDSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("assignDID"))
                .withRequestBody(containing("4196902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.purchaseNumberSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("releaseDID"))
                .withRequestBody(containing("4196902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.deleteNumberSuccessResponse)));
        // Get Account using admin email address and user email address
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        String provisioningURL = deploymentUrl + baseURL + "IncomingPhoneNumbers.json";
        WebResource webResource = jerseyClient.resource(provisioningURL);

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("PhoneNumber", "+14196902867");
        formData.add("VoiceUrl", "http://demo.telestax.com/docs/voice.xml");
        formData.add("FriendlyName", "My Company Line");
        formData.add("VoiceMethod", "GET");
        ClientResponse clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept("application/json").post(ClientResponse.class, formData);
        assertTrue(clientResponse.getStatus() == 200);
        String response = clientResponse.getEntity(String.class);
        System.out.println(response);
        assertTrue(!response.trim().equalsIgnoreCase("[]"));
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
        System.out.println(jsonResponse.toString());
        assertTrue(IncomingPhoneNumbersEndpointTestUtils.match(jsonResponse.toString(),IncomingPhoneNumbersEndpointTestUtils.jSonResultDeletePurchaseNumber));
        
        String phoneNumberSid = jsonResponse.get("sid").getAsString();
        provisioningURL = deploymentUrl + baseURL + "IncomingPhoneNumbers/" + phoneNumberSid + ".json";
        webResource = jerseyClient.resource(provisioningURL);
        clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept("application/json").delete(ClientResponse.class);
        assertTrue(clientResponse.getStatus() == 204);
    }
    
    /*
     * https://www.twilio.com/docs/api/rest/incoming-phone-numbers#instance-post-example-1
     * Set the VoiceUrl and SmsUrl on a phone number
     */
    @Test
    public void testUpdatePhoneNumberSuccess() {
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("queryDID"))
                .withRequestBody(containing("4206902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.queryDIDSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("assignDID"))
                .withRequestBody(containing("4206902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.purchaseNumberSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("releaseDID"))
                .withRequestBody(containing("4206902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.deleteNumberSuccessResponse)));
        // Get Account using admin email address and user email address
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        String provisioningURL = deploymentUrl + baseURL + "IncomingPhoneNumbers.json";
        WebResource webResource = jerseyClient.resource(provisioningURL);

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("PhoneNumber", "+14206902867");
        formData.add("VoiceUrl", "http://demo.telestax.com/docs/voice.xml");
        formData.add("FriendlyName", "My Company Line");
        formData.add("VoiceMethod", "GET");
        ClientResponse clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept("application/json").post(ClientResponse.class, formData);
        assertTrue(clientResponse.getStatus() == 200);
        String response = clientResponse.getEntity(String.class);
        System.out.println(response);
        assertTrue(!response.trim().equalsIgnoreCase("[]"));
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
        System.out.println(jsonResponse.toString());
        assertTrue(IncomingPhoneNumbersEndpointTestUtils.match(jsonResponse.toString(),IncomingPhoneNumbersEndpointTestUtils.jSonResultUpdatePurchaseNumber));
        
        String phoneNumberSid = jsonResponse.get("sid").getAsString();
        provisioningURL = deploymentUrl + baseURL + "IncomingPhoneNumbers/" + phoneNumberSid + ".json";
        webResource = jerseyClient.resource(provisioningURL);
        formData = new MultivaluedMapImpl();
        formData.add("VoiceUrl", "http://demo.telestax.com/docs/voice2.xml");
        formData.add("SmsUrl", "http://demo.telestax.com/docs/sms2.xml");
        formData.add("VoiceMethod", "POST");
        formData.add("SMSMethod", "GET");
        clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept("application/json").post(ClientResponse.class, formData);
        assertTrue(clientResponse.getStatus() == 200);
        response = clientResponse.getEntity(String.class);
        System.out.println(response);
        assertTrue(!response.trim().equalsIgnoreCase("[]"));
        parser = new JsonParser();
        jsonResponse = parser.parse(response).getAsJsonObject();
        System.out.println(jsonResponse.toString());
        assertTrue(IncomingPhoneNumbersEndpointTestUtils.match(jsonResponse.toString(),IncomingPhoneNumbersEndpointTestUtils.jSonResultUpdateSuccessPurchaseNumber));
    }
    
    /*
     * https://www.twilio.com/docs/api/rest/incoming-phone-numbers#list-get-example-1
     */
    @Test
    public void testAccountAssociatedPhoneNumbers() {
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("queryDID"))
                .withRequestBody(containing("4216902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.queryDIDSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("assignDID"))
                .withRequestBody(containing("4216902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.purchaseNumberSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("releaseDID"))
                .withRequestBody(containing("4216902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.deleteNumberSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("queryDID"))
                .withRequestBody(containing("5216902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.queryDIDSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("assignDID"))
                .withRequestBody(containing("5216902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.purchaseNumberSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("releaseDID"))
                .withRequestBody(containing("5216902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.deleteNumberSuccessResponse)));
        // Get Account using admin email address and user email address
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        String provisioningURL = deploymentUrl + baseURL + "IncomingPhoneNumbers.json";
        WebResource webResource = jerseyClient.resource(provisioningURL);
        
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("PhoneNumber", "+14216902867");
        formData.add("VoiceUrl", "http://demo.telestax.com/docs/voice.xml");
        formData.add("FriendlyName", "My Company Line");
        formData.add("VoiceMethod", "GET");
        ClientResponse clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept("application/json").post(ClientResponse.class, formData);
        assertTrue(clientResponse.getStatus() == 200);
        String response = clientResponse.getEntity(String.class);
        System.out.println(response);
        assertTrue(!response.trim().equalsIgnoreCase("[]"));
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
        System.out.println(jsonResponse.toString());
        assertTrue(IncomingPhoneNumbersEndpointTestUtils.match(jsonResponse.toString(),IncomingPhoneNumbersEndpointTestUtils.jSonResultAccountAssociatedPurchaseNumber));
        String phoneNumberSid = jsonResponse.get("sid").getAsString();
        
        formData = new MultivaluedMapImpl();
        formData.add("PhoneNumber", "+15216902867");
        formData.add("VoiceUrl", "http://demo.telestax.com/docs/voice.xml");
        formData.add("FriendlyName", "My 2nd Company Line");
        formData.add("VoiceMethod", "GET");
        clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept("application/json").post(ClientResponse.class, formData);
        assertTrue(clientResponse.getStatus() == 200);
        response = clientResponse.getEntity(String.class);
        System.out.println(response);
        assertTrue(!response.trim().equalsIgnoreCase("[]"));
        parser = new JsonParser();
        jsonResponse = parser.parse(response).getAsJsonObject();
        String secondPhoneNumberSid = jsonResponse.get("sid").getAsString();
        
        provisioningURL = deploymentUrl + baseURL + "IncomingPhoneNumbers.json";
        webResource = jerseyClient.resource(provisioningURL);
//        formData = new MultivaluedMapImpl();
//        formData.add("VoiceUrl", "http://demo.telestax.com/docs/voice2.xml");
        clientResponse = webResource.
//                queryParams(formData).
                accept("application/json").get(ClientResponse.class);
        assertTrue(clientResponse.getStatus() == 200);
        response = clientResponse.getEntity(String.class);
        System.out.println(response);
        assertTrue(!response.trim().equalsIgnoreCase("[]"));
        parser = new JsonParser();
        JsonArray jsonArray = parser.parse(response).getAsJsonArray();
        
        System.out.println(jsonArray + " \n " + jsonArray.size());
        
        assertTrue(jsonArray.size() >= 21);
        System.out.println("testAccountAssociatedPhoneNumbers:" + (jsonArray.get(jsonArray.size()-1).getAsJsonObject().toString()));
        assertTrue(IncomingPhoneNumbersEndpointTestUtils.match(jsonArray.get(jsonArray.size()-1).getAsJsonObject().toString(),IncomingPhoneNumbersEndpointTestUtils.jSonResultAccountAssociatedPurchaseNumberResult));
        
        provisioningURL = deploymentUrl + baseURL + "IncomingPhoneNumbers/" + phoneNumberSid + ".json";
        webResource = jerseyClient.resource(provisioningURL);
        clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept("application/json").delete(ClientResponse.class);
        assertTrue(clientResponse.getStatus() == 204);
        
        provisioningURL = deploymentUrl + baseURL + "IncomingPhoneNumbers/" + secondPhoneNumberSid + ".json";
        webResource = jerseyClient.resource(provisioningURL);
        clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept("application/json").delete(ClientResponse.class);
        assertTrue(clientResponse.getStatus() == 204);
    }
    
    /*
     * https://www.twilio.com/docs/api/rest/incoming-phone-numbers#list-get-example-2
     */
    @Test
    public void testAccountAssociatedPhoneNumbersFilter() {
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("queryDID"))
                .withRequestBody(containing("4216902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.queryDIDSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("assignDID"))
                .withRequestBody(containing("4216902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.purchaseNumberSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("releaseDID"))
                .withRequestBody(containing("4216902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.deleteNumberSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("queryDID"))
                .withRequestBody(containing("5216902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.queryDIDSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("assignDID"))
                .withRequestBody(containing("5216902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.purchaseNumberSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("releaseDID"))
                .withRequestBody(containing("5216902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.deleteNumberSuccessResponse)));
        // Get Account using admin email address and user email address
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        String provisioningURL = deploymentUrl + baseURL + "IncomingPhoneNumbers.json";
        WebResource webResource = jerseyClient.resource(provisioningURL);

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("PhoneNumber", "+14216902867");
        formData.add("VoiceUrl", "http://demo.telestax.com/docs/voice.xml");
        formData.add("FriendlyName", "My Company Line");
        formData.add("VoiceMethod", "GET");
        ClientResponse clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept("application/json").post(ClientResponse.class, formData);
        assertTrue(clientResponse.getStatus() == 200);
        String response = clientResponse.getEntity(String.class);
        System.out.println(response);
        assertTrue(!response.trim().equalsIgnoreCase("[]"));
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
        System.out.println(jsonResponse.toString());
        assertTrue(IncomingPhoneNumbersEndpointTestUtils.match(jsonResponse.toString(),IncomingPhoneNumbersEndpointTestUtils.jSonResultAccountAssociatedPurchaseNumber));
        String phoneNumberSid = jsonResponse.get("sid").getAsString();
        
        formData = new MultivaluedMapImpl();
        formData.add("PhoneNumber", "+15216902867");
        formData.add("VoiceUrl", "http://demo.telestax.com/docs/voice.xml");
        formData.add("FriendlyName", "My 2nd Company Line");
        formData.add("VoiceMethod", "GET");
        clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept("application/json").post(ClientResponse.class, formData);
        assertTrue(clientResponse.getStatus() == 200);
        response = clientResponse.getEntity(String.class);
        System.out.println(response);
        assertTrue(!response.trim().equalsIgnoreCase("[]"));
        parser = new JsonParser();
        jsonResponse = parser.parse(response).getAsJsonObject();
        String secondPhoneNumberSid = jsonResponse.get("sid").getAsString();
        
        provisioningURL = deploymentUrl + baseURL + "IncomingPhoneNumbers.json";
        webResource = jerseyClient.resource(provisioningURL);
        formData = new MultivaluedMapImpl();
        formData.add("PhoneNumber", "+15216902867");
        clientResponse = webResource.queryParams(formData).accept("application/json").get(ClientResponse.class);
        assertTrue(clientResponse.getStatus() == 200);
        response = clientResponse.getEntity(String.class);
        System.out.println(response);
        assertTrue(!response.trim().equalsIgnoreCase("[]"));
        parser = new JsonParser();
        JsonArray jsonArray = parser.parse(response).getAsJsonArray();
        
        System.out.println(jsonArray + " \n " + jsonArray.size());
        
        assertTrue(jsonArray.size() == 1);
        System.out.println((jsonArray.get(0).getAsJsonObject().toString()));
        assertTrue(IncomingPhoneNumbersEndpointTestUtils.match(jsonArray.get(0).getAsJsonObject().toString(),IncomingPhoneNumbersEndpointTestUtils.jSonResultAccountAssociatedPurchaseNumberResult));
        
        provisioningURL = deploymentUrl + baseURL + "IncomingPhoneNumbers/" + phoneNumberSid + ".json";
        webResource = jerseyClient.resource(provisioningURL);
        clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept("application/json").delete(ClientResponse.class);
        assertTrue(clientResponse.getStatus() == 204);
        
        provisioningURL = deploymentUrl + baseURL + "IncomingPhoneNumbers/" + secondPhoneNumberSid + ".json";
        webResource = jerseyClient.resource(provisioningURL);
        clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept("application/json").delete(ClientResponse.class);
        assertTrue(clientResponse.getStatus() == 204);
    }
    
    /*
     * https://www.twilio.com/docs/api/rest/incoming-phone-numbers#list-get-example-3
     * Return the set of all phone numbers containing the digits 867.
     */
    @Test
    public void testAccountAssociatedPhoneNumbersSecondFilter() {
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("queryDID"))
                .withRequestBody(containing("4216902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.queryDIDSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("assignDID"))
                .withRequestBody(containing("4216902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.purchaseNumberSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("releaseDID"))
                .withRequestBody(containing("4216902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.deleteNumberSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("queryDID"))
                .withRequestBody(containing("5216902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.queryDIDSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("assignDID"))
                .withRequestBody(containing("5216902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.purchaseNumberSuccessResponse)));
        
        stubFor(post(urlEqualTo("/test"))
                .withRequestBody(containing("releaseDID"))
                .withRequestBody(containing("5216902867"))
                .willReturn(aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "text/xml")
                    .withBody(IncomingPhoneNumbersEndpointTestUtils.deleteNumberSuccessResponse)));
        // Get Account using admin email address and user email address
        Client jerseyClient = Client.create();
        jerseyClient.addFilter(new HTTPBasicAuthFilter(adminUsername, adminAuthToken));

        String provisioningURL = deploymentUrl + baseURL + "IncomingPhoneNumbers.json";
        WebResource webResource = jerseyClient.resource(provisioningURL);

        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("PhoneNumber", "+14216902867");
        formData.add("VoiceUrl", "http://demo.telestax.com/docs/voice.xml");
        formData.add("FriendlyName", "My Company Line");
        formData.add("VoiceMethod", "GET");
        ClientResponse clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept("application/json").post(ClientResponse.class, formData);
        assertTrue(clientResponse.getStatus() == 200);
        String response = clientResponse.getEntity(String.class);
        System.out.println(response);
        assertTrue(!response.trim().equalsIgnoreCase("[]"));
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(response).getAsJsonObject();
        System.out.println(jsonResponse.toString());
        assertTrue(IncomingPhoneNumbersEndpointTestUtils.match(jsonResponse.toString(),IncomingPhoneNumbersEndpointTestUtils.jSonResultAccountAssociatedPurchaseNumber));
        String phoneNumberSid = jsonResponse.get("sid").getAsString();
        
        formData = new MultivaluedMapImpl();
        formData.add("PhoneNumber", "+15216902867");
        formData.add("VoiceUrl", "http://demo.telestax.com/docs/voice.xml");
        formData.add("FriendlyName", "My 2nd Company Line");
        formData.add("VoiceMethod", "GET");
        clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept("application/json").post(ClientResponse.class, formData);
        assertTrue(clientResponse.getStatus() == 200);
        response = clientResponse.getEntity(String.class);
        System.out.println(response);
        assertTrue(!response.trim().equalsIgnoreCase("[]"));
        parser = new JsonParser();
        jsonResponse = parser.parse(response).getAsJsonObject();
        String secondPhoneNumberSid = jsonResponse.get("sid").getAsString();
        
        provisioningURL = deploymentUrl + baseURL + "IncomingPhoneNumbers.json";
        webResource = jerseyClient.resource(provisioningURL);
        formData = new MultivaluedMapImpl();
        formData.add("PhoneNumber", "867");
        clientResponse = webResource.queryParams(formData).accept("application/json").get(ClientResponse.class);
        assertTrue(clientResponse.getStatus() == 200);
        response = clientResponse.getEntity(String.class);
        System.out.println(response);
        assertTrue(!response.trim().equalsIgnoreCase("[]"));
        parser = new JsonParser();
        JsonArray jsonArray = parser.parse(response).getAsJsonArray();
        
        System.out.println(jsonArray + " \n " + jsonArray.size());
        
        assertTrue(jsonArray.size() >= 2);
        System.out.println((jsonArray.get(jsonArray.size() - 1).getAsJsonObject().toString()));
        assertTrue(IncomingPhoneNumbersEndpointTestUtils.match(jsonArray.get(jsonArray.size() - 1).getAsJsonObject().toString(),IncomingPhoneNumbersEndpointTestUtils.jSonResultAccountAssociatedPurchaseNumberResult));
        
        provisioningURL = deploymentUrl + baseURL + "IncomingPhoneNumbers/" + phoneNumberSid + ".json";
        webResource = jerseyClient.resource(provisioningURL);
        clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept("application/json").delete(ClientResponse.class);
        assertTrue(clientResponse.getStatus() == 204);
        
        provisioningURL = deploymentUrl + baseURL + "IncomingPhoneNumbers/" + secondPhoneNumberSid + ".json";
        webResource = jerseyClient.resource(provisioningURL);
        clientResponse = webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).accept("application/json").delete(ClientResponse.class);
        assertTrue(clientResponse.getStatus() == 204);
    }
    
    @Deployment(name = "AvailablePhoneNumbersEndpointTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
        logger.info("version");
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("com.telestax.servlet:restcomm.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("restcomm_AvailablePhoneNumbers_Test.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script_dialTest", "data/hsql/restcomm.script");
        logger.info("Packaged Test App");
        return archive;
    }
}
