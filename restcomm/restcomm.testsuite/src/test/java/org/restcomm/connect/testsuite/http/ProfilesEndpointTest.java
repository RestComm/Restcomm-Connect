package org.restcomm.connect.testsuite.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;
import org.restcomm.connect.commons.annotations.FeatureExpTests;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResourceLinkHeaders;
import com.sun.jersey.core.header.LinkHeader;

/**
 * @author maria
 */

@RunWith(Arquillian.class)
public class ProfilesEndpointTest extends EndpointTest {
    private final static Logger logger = Logger.getLogger(ProfilesEndpointTest.class.getName());

    private static final String version = Version.getVersion();
    
    private static final String profileDocument="{ \"featureEnablement\": { \"DIDPurchase\": { \"allowedCountries\": [\"US\", \"CA\"] }, \"destinations\": { \"allowedPrefixes\": [\"+1\"] }, \"outboundPSTN\": { }, \"inboundPSTN\": { }, \"outboundSMS\": { }, \"inboundSMS\": { } }, \"sessionThrottling\": { \"PSTNCallsPerTime\": { \"events\" : 300, \"time\" : 30, \"timeUnit\" : \"days\" } } }";
    private static final String updatedProfileDocument="{ \"featureEnablement\": { \"DIDPurchase\": { \"allowedCountries\": [\"PK\", \"CA\"] }, \"destinations\": { \"allowedPrefixes\": [\"+1\"] }, \"outboundPSTN\": { }, \"inboundPSTN\": { }, \"outboundSMS\": { }, \"inboundSMS\": { } }, \"sessionThrottling\": { \"PSTNCallsPerTime\": { \"events\" : 300, \"time\" : 30, \"timeUnit\" : \"days\" } } }";
    private static final String invalidProfileDocument="{ \"featureEnablement\": { \"DIDPurchase\": { \"allowedCountries\": [\"PKeuietue\", \"CA\"] }, \"destinations\": { \"allowedPrefixes\": [\"+1\"] }, \"outboundPSTN\": { }, \"inboundPSTN\": { }, \"outboundSMS\": { }, \"inboundSMS\": { } }, \"sessionThrottling\": { \"PSTNCallsPerTime\": { \"events\" : 300, \"time\" : 30, \"timeUnit\" : \"days\" } } }";

    @ArquillianResource
    private Deployer deployer;
    @ArquillianResource
    URL deploymentUrl;

    //super admin account
    private String superAdminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    //admin account
    private String adminAccountSid = "AC574d775522c96f9aacacc5ca60c8c74g";
    //developer account
    private String devAccountSid = "AC574d775522c96f9aacacc5ca60c8c74f";
    private String authToken = "77f8c12cc7b8f8423e5c38b035249166";

    private static final String DEFAULT_PROFILE_SID = "PRae6e420f425248d6a26948c17a9e2acf";
    private static final String UNKNOWN_PROFILE_SID = "PRafbe225ad37541eba518a74248f0ac4d";
    private static final String GARBAGE_PROFILE_SID = "afbe225ad37541eba518a74248f0ac4d";
    private static final String ORGANIZATION_SID = "ORafbe225ad37541eba518a74248f0ac4c";

    @Before
    public void before() {
    }

    /**
     * this test will try to Read single profile
     */
    @Test
    public void getProfile(){
    	ClientResponse clientResponse = RestcommProfilesTool.getInstance().getProfileResponse(deploymentUrl.toString(), superAdminAccountSid, authToken, DEFAULT_PROFILE_SID);
    	logger.info("profile: "+clientResponse);
    	assertEquals(200, clientResponse.getStatus());
    	// TODO Read and verify further response
    }

    /**
     * getProfileList
     */
    @Test
    public void getProfileList(){
    	ClientResponse clientResponse = RestcommProfilesTool.getInstance().getProfileListClientResponse(deploymentUrl.toString(), superAdminAccountSid, authToken);
    	logger.info("profile list: "+clientResponse.getEntity(String.class));
    	assertNotNull(clientResponse);
    	assertEquals(200, clientResponse.getStatus());
    }

    /**
     * this test will try to Read single profile with an unknown profile Sid
     */
    @Test
    @Category(FeatureExpTests.class)
    public void getProfileUnknownSid(){
    	ClientResponse clientResponse = RestcommProfilesTool.getInstance().getProfileResponse(deploymentUrl.toString(), superAdminAccountSid, authToken, UNKNOWN_PROFILE_SID);
    	assertNotNull(clientResponse);
    	assertEquals(404, clientResponse.getStatus());
    }

    /**
     * this test will try to Read single profile with an garbage profile Sid
     */
    @Test
    @Category(FeatureExpTests.class)
    public void getProfileInvalidSid(){
    	ClientResponse clientResponse = RestcommProfilesTool.getInstance().getProfileResponse(deploymentUrl.toString(), superAdminAccountSid, authToken, GARBAGE_PROFILE_SID);
    	assertNotNull(clientResponse);
    	assertEquals(400, clientResponse.getStatus());
    }

    /**
     * Administrators and Developers can not read profile
     */
    @Test
    @Category(FeatureExpTests.class)
    public void getProfilePermissionTest(){
    	ClientResponse clientResponse = RestcommProfilesTool.getInstance().getProfileResponse(deploymentUrl.toString(), adminAccountSid, authToken, DEFAULT_PROFILE_SID);
    	assertNotNull(clientResponse);
    	assertEquals(403, clientResponse.getStatus());
    	

    	clientResponse = RestcommProfilesTool.getInstance().getProfileResponse(deploymentUrl.toString(), devAccountSid, authToken, DEFAULT_PROFILE_SID);
    	assertNotNull(clientResponse);
    	assertEquals(403, clientResponse.getStatus());

    }

    /**
     * Create, Read And Update Profile Test
     */
    @Test
    public void createReadAndUpdateProfileTest(){
    	/*
		 * create a profile 
		 */
    	ClientResponse clientResponse = RestcommProfilesTool.getInstance().createProfileResponse(deploymentUrl.toString(), superAdminAccountSid, authToken, profileDocument);
    	logger.info("clientResponse: "+clientResponse);
    	assertEquals(201, clientResponse.getStatus());
    	assertEquals(profileDocument, clientResponse.getEntity(String.class));
    	
    	//extract profileSid of newly created profile Sid.
    	URI location = clientResponse.getLocation();
    	assertNotNull(location);
    	String profileLocation  = location.toString();
    	String[] profileUriElements = profileLocation.split("/");
    	assertNotNull(profileUriElements);
    	String newlyCreatedProfileSid = profileUriElements[profileUriElements.length-1];
    	logger.info("newlyCreatedProfileSid: "+newlyCreatedProfileSid);
    	
    	/*
		 * read newly created profile 
		 */
    	clientResponse = RestcommProfilesTool.getInstance().getProfileResponse(deploymentUrl.toString(), superAdminAccountSid, authToken, newlyCreatedProfileSid);
    	logger.info("profile: "+clientResponse);
    	assertEquals(200, clientResponse.getStatus());
    	assertEquals(profileDocument, clientResponse.getEntity(String.class));
    	
    	/*
		 * update the profile 
		 */
    	clientResponse = RestcommProfilesTool.getInstance().updateProfileResponse(deploymentUrl.toString(), superAdminAccountSid, authToken, newlyCreatedProfileSid, updatedProfileDocument);
    	logger.info("clientResponse for update: "+clientResponse);
    	assertEquals(200, clientResponse.getStatus());
    	assertEquals(updatedProfileDocument, clientResponse.getEntity(String.class));
    }

    /**
     * createProfileTestInvalidSchema
     */
    @Test
    @Category(FeatureExpTests.class)
    public void createProfileTestInvalidSchema(){
    	/*
		 * create a profile with invalid schema
		 */
    	ClientResponse clientResponse = RestcommProfilesTool.getInstance().createProfileResponse(deploymentUrl.toString(), superAdminAccountSid, authToken, invalidProfileDocument);
    	logger.info("clientResponse: "+clientResponse);
    	assertEquals(400, clientResponse.getStatus());
    }

    /**
     * updateProfileUnknownSidTest
     */
    @Test
    @Category(FeatureExpTests.class)
    public void updateProfileUnknownSidTest(){
    	/*
		 * update a profile 
		 */
    	ClientResponse clientResponse = RestcommProfilesTool.getInstance().updateProfileResponse(deploymentUrl.toString(), superAdminAccountSid, authToken, UNKNOWN_PROFILE_SID, updatedProfileDocument);
    	logger.info("clientResponse: "+clientResponse);
    	assertEquals(404, clientResponse.getStatus());
    }

    /**
     * updateProfilePermissionTest
     */
    @Test
    @Category(FeatureExpTests.class)
    public void updateProfilePermissionTest(){
    	/*
		 * update a profile from admin account
		 */
    	ClientResponse clientResponse = RestcommProfilesTool.getInstance().updateProfileResponse(deploymentUrl.toString(), adminAccountSid, authToken, DEFAULT_PROFILE_SID, updatedProfileDocument);
    	logger.info("clientResponse: "+clientResponse);
    	assertEquals(403, clientResponse.getStatus());
    	/*
		 * update a profile from dev account
		 */
    	clientResponse = RestcommProfilesTool.getInstance().updateProfileResponse(deploymentUrl.toString(), devAccountSid, authToken, DEFAULT_PROFILE_SID, updatedProfileDocument);
    	logger.info("clientResponse: "+clientResponse);
    	assertEquals(403, clientResponse.getStatus());
    }

    /**
     * deleteProfileTest
     */
    @Test
    public void deleteProfileTest(){
    	/*
		 * delete a profile 
		 */
    	ClientResponse clientResponse = RestcommProfilesTool.getInstance().deleteProfileResponse(deploymentUrl.toString(), superAdminAccountSid, authToken, DEFAULT_PROFILE_SID);
    	logger.info("clientResponse: "+clientResponse);
    	assertEquals(200, clientResponse.getStatus());

    	// TODO Read and verify further response
    }

    /**
     * deleteProfilePermissionTest
     */
    @Test
    @Category(FeatureExpTests.class)
    public void deleteProfilePermissionTest(){
    	/*
		 * delete a profile from admin account
		 */
    	ClientResponse clientResponse = RestcommProfilesTool.getInstance().deleteProfileResponse(deploymentUrl.toString(), adminAccountSid, authToken, DEFAULT_PROFILE_SID);
    	logger.info("clientResponse: "+clientResponse);
    	assertEquals(403, clientResponse.getStatus());
    	/*
		 * delete a profile from Dev account
		 */
    	clientResponse = RestcommProfilesTool.getInstance().deleteProfileResponse(deploymentUrl.toString(), devAccountSid, authToken, DEFAULT_PROFILE_SID);
    	logger.info("clientResponse: "+clientResponse);
    	assertEquals(403, clientResponse.getStatus());
    }

    /**
     * deleteProfileUnknownSidTest
     */
    @Test
    @Category(FeatureExpTests.class)
    public void deleteProfileUnknownSidTest(){
    	/*
		 * delete a profile with unknown sid 
		 */
    	ClientResponse clientResponse = RestcommProfilesTool.getInstance().deleteProfileResponse(deploymentUrl.toString(), superAdminAccountSid, authToken, UNKNOWN_PROFILE_SID);
    	logger.info("clientResponse: "+clientResponse);
    	assertEquals(404, clientResponse.getStatus());
    }

    @Test 
    @Category(FeatureExpTests.class)
    public void createProfilePermissionTest(){
    	//admin tries to create profile
    	ClientResponse  clientResponse = RestcommProfilesTool.getInstance().createProfileResponse(deploymentUrl.toString(), adminAccountSid, authToken, profileDocument);
    	logger.info("clientResponse: "+clientResponse);
    	assertEquals(403, clientResponse.getStatus());
    	//developer tries to create profile
    	clientResponse = RestcommProfilesTool.getInstance().createProfileResponse(deploymentUrl.toString(), devAccountSid, authToken, profileDocument);
    	logger.info("clientResponse: "+clientResponse);
    	assertEquals(403, clientResponse.getStatus());
    }

    /**
     * link/unlink a give Profile To an Account
     * @throws IOException 
     * @throws ClientProtocolException 
     * @throws URISyntaxException 
     */
    @Test
    public void linkUnLinkProfileToAccount() throws ClientProtocolException, IOException, URISyntaxException{
		/*
		 * link a profile to an account 
		 */
    	HttpResponse response = RestcommProfilesTool.getInstance().linkProfile(deploymentUrl.toString(), superAdminAccountSid, authToken, DEFAULT_PROFILE_SID, superAdminAccountSid, RestcommProfilesTool.AssociatedResourceType.ACCOUNT);
    	logger.info("HttpResponse: "+response);
    	assertEquals(200, response.getStatusLine().getStatusCode());
    	
    	/* 
    	 * Get associated profile
    	 * from Accounts endpoint:
    	 * to verify association establishment.
    	 */
    	ClientResponse accountEndopintResponse = RestcommAccountsTool.getInstance().getAccountResponse(deploymentUrl.toString(), superAdminAccountSid, authToken, superAdminAccountSid);
    	WebResourceLinkHeaders linkHeaders = accountEndopintResponse.getLinks();
    	logger.info("accountEndopintResponse WebResourceLinkHeaders: "+linkHeaders);
    	assertNotNull(linkHeaders);
    	LinkHeader linkHeader = linkHeaders.getLink(RestcommProfilesTool.PROFILE_REL_TYPE);
    	logger.info("accountEndopintResponse WebResourceLinkHeaders linkHeader: "+linkHeader);
    	assertNotNull(linkHeader);
    	assertTrue(linkHeader.getUri().toString().contains(DEFAULT_PROFILE_SID));

    	/*
		 * unlink a profile from an account 
		 */
    	response = RestcommProfilesTool.getInstance().unLinkProfile(deploymentUrl.toString(), superAdminAccountSid, authToken, DEFAULT_PROFILE_SID, superAdminAccountSid, RestcommProfilesTool.AssociatedResourceType.ACCOUNT);
    	logger.info("HttpResponse: "+response);
    	assertEquals(200, response.getStatusLine().getStatusCode());
    	
    	/* 
    	 * Get associated profile
    	 * from Accounts endpoint: 
    	 * to verify association removal
    	 */
    	accountEndopintResponse = RestcommAccountsTool.getInstance().getAccountResponse(deploymentUrl.toString(), superAdminAccountSid, authToken, superAdminAccountSid);
    	linkHeaders = accountEndopintResponse.getLinks();
    	logger.info("accountEndopintResponse WebResourceLinkHeaders: "+linkHeaders);
    	linkHeader = linkHeaders.getLink(RestcommProfilesTool.PROFILE_REL_TYPE);
    	logger.info("accountEndopintResponse WebResourceLinkHeaders linkHeader: "+linkHeader);
    	assertNull(linkHeader);
    }

    /**
     * link/unlink a give Profile To an Organization
     * @throws IOException 
     * @throws ClientProtocolException 
     * @throws URISyntaxException 
     */
    @Test
    public void linkUnLinkProfileToOrganization() throws ClientProtocolException, IOException, URISyntaxException{
    	/*
		 * link a profile to an organizations 
		 */
    	HttpResponse response = RestcommProfilesTool.getInstance().linkProfile(deploymentUrl.toString(), superAdminAccountSid, authToken, DEFAULT_PROFILE_SID, ORGANIZATION_SID, RestcommProfilesTool.AssociatedResourceType.ORGANIZATION);
    	logger.info("HttpResponse: "+response);
    	assertEquals(200, response.getStatusLine().getStatusCode());
    	
    	/* 
    	 * Get associated profile
    	 * from Organizations endpoint:
    	 * to verify association establishment.
    	 */
    	ClientResponse orgEndopintResponse = RestcommOrganizationsTool.getInstance().getOrganizationResponse(deploymentUrl.toString(), superAdminAccountSid, authToken, ORGANIZATION_SID);
    	WebResourceLinkHeaders linkHeaders = orgEndopintResponse.getLinks();
    	logger.info("accountEndopintResponse WebResourceLinkHeaders: "+linkHeaders);
    	assertNotNull(linkHeaders);
    	LinkHeader linkHeader = linkHeaders.getLink(RestcommProfilesTool.PROFILE_REL_TYPE);
    	logger.info("accountEndopintResponse WebResourceLinkHeaders linkHeader: "+linkHeader);
    	assertNotNull(linkHeader);
    	assertTrue(linkHeader.getUri().toString().contains(DEFAULT_PROFILE_SID));

    	/*
		 * unlink a profile from an organization 
		 */
    	response = RestcommProfilesTool.getInstance().unLinkProfile(deploymentUrl.toString(), superAdminAccountSid, authToken, DEFAULT_PROFILE_SID, ORGANIZATION_SID, RestcommProfilesTool.AssociatedResourceType.ORGANIZATION);
    	logger.info("HttpResponse: "+response);
    	assertEquals(200, response.getStatusLine().getStatusCode());
    	
    	/* 
    	 * Get associated profile
    	 * from Organizations endpoint:
    	 * to verify association removal.
    	 */
    	orgEndopintResponse = RestcommOrganizationsTool.getInstance().getOrganizationResponse(deploymentUrl.toString(), superAdminAccountSid, authToken, ORGANIZATION_SID);
    	linkHeaders = orgEndopintResponse.getLinks();
    	logger.info("orgEndopintResponse WebResourceLinkHeaders: "+linkHeaders);
    	linkHeader = linkHeaders.getLink(RestcommProfilesTool.PROFILE_REL_TYPE);
    	logger.info("orgEndopintResponse WebResourceLinkHeaders linkHeader: "+linkHeader);
    	assertNull(linkHeader);
    }
    

    /**
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    @Category(FeatureExpTests.class)
    public void linkUnLinkProfilePermissionTest() throws ClientProtocolException, IOException, URISyntaxException{
    	/*
		 * link a profile by admin account
		 */
    	HttpResponse response = RestcommProfilesTool.getInstance().linkProfile(deploymentUrl.toString(), adminAccountSid, authToken, DEFAULT_PROFILE_SID, ORGANIZATION_SID, RestcommProfilesTool.AssociatedResourceType.ORGANIZATION);
    	assertEquals(403, response.getStatusLine().getStatusCode());
    	/*
		 * unlink a profile by dev account
		 */
    	response = RestcommProfilesTool.getInstance().linkProfile(deploymentUrl.toString(), devAccountSid, authToken, DEFAULT_PROFILE_SID, ORGANIZATION_SID, RestcommProfilesTool.AssociatedResourceType.ORGANIZATION);
    	assertEquals(403, response.getStatusLine().getStatusCode());
    }
    

    /**
     * @throws ClientProtocolException
     * @throws IOException
     * @throws URISyntaxException
     */
    @Test
    @Category(FeatureExpTests.class)
    public void linkUnLinkProfileUnknownProfileSidTest() throws ClientProtocolException, IOException, URISyntaxException{
    	/*
		 * link a profile with unknown profile sid
		 */
    	HttpResponse response = RestcommProfilesTool.getInstance().linkProfile(deploymentUrl.toString(), superAdminAccountSid, authToken, UNKNOWN_PROFILE_SID, ORGANIZATION_SID, RestcommProfilesTool.AssociatedResourceType.ORGANIZATION);
    	assertEquals(404, response.getStatusLine().getStatusCode());
    	/*
		 * unlink a profile with unknown profile sid
		 */
    	response = RestcommProfilesTool.getInstance().linkProfile(deploymentUrl.toString(), superAdminAccountSid, authToken, UNKNOWN_PROFILE_SID, ORGANIZATION_SID, RestcommProfilesTool.AssociatedResourceType.ORGANIZATION);
    	assertEquals(404, response.getStatusLine().getStatusCode());
    }
    
    @Deployment(name = "ProfilesEndpointTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        logger.info("Packaging Test App");
        logger.info("version");
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
        archive.addAsWebInfResource("restcomm.script_accounts_test", "data/hsql/restcomm.script");
        logger.info("Packaged Test App");
        return archive;
    }

}
