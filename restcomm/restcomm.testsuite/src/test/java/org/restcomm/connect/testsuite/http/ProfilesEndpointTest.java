package org.restcomm.connect.testsuite.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;
import org.restcomm.connect.commons.annotations.FeatureExpTests;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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

    @ArquillianResource
    private Deployer deployer;
    @ArquillianResource
    URL deploymentUrl;

    //super admin account
    private String superAdminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String superAdminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    //admin account
    private String adminAccountSid = "AC574d775522c96f9aacacc5ca60c8c74g";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    //developer account
    private String devAccountSid = "AC574d775522c96f9aacacc5ca60c8c74f";
    private String devAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    private final String profileSid = "PRafbe225ad37541eba518a74248f0ac4c";
    private final String organizationSid = "ORafbe225ad37541eba518a74248f0ac4c";

    JsonObject profileDocument;
    JsonObject updatedProfileDocument;

    @BeforeClass
    public static void beforeClass() {
    }

    /**
     * SuperAdmin is allowed to read any profile
     * this test will try to Read single profile
     */
    @Test
    public void getProfile(){
    	JsonObject profileJsonObject = RestcommProfilesTool.getInstance().getProfile(deploymentUrl.toString(), superAdminAccountSid, superAdminAuthToken, profileSid);
    	assertNotNull(profileJsonObject);
    	logger.info("profile: "+profileJsonObject);
    	// TODO Read and verify further response
    }

    /**
     * getProfileList
     */
    @Test
    public void getProfileList(){
    	JsonArray jsonArray = null;
    	jsonArray = RestcommProfilesTool.getInstance().getProfileListJsonResponse(deploymentUrl.toString(), superAdminAccountSid, superAdminAuthToken);
    	logger.info("profile list: "+jsonArray);
    	assertNotNull(jsonArray);
    	assertEquals(0,jsonArray.size());
    	// TODO Add default list in DB script, Read and verify further response
    }

    /**
     * Administrators can not read profile
     */
    @Test
    @Category(FeatureExpTests.class)
    public void getProfileFromAdministratorAccount(){
    	ClientResponse clientResponse = RestcommProfilesTool.getInstance().getProfileResponse(deploymentUrl.toString(), adminAccountSid, adminAuthToken, profileSid);
    	assertNotNull(clientResponse);
    	logger.info("profile: "+clientResponse);
    	assertEquals(403, clientResponse.getStatus());

    }
    /**
     * Developers can not read profile
     */
    @Test
    @Category(FeatureExpTests.class)
    public void getProfileFromDeveloperAccount(){
    	ClientResponse clientResponse = RestcommProfilesTool.getInstance().getProfileResponse(deploymentUrl.toString(), devAccountSid, devAuthToken, profileSid);
    	assertNotNull(clientResponse);
    	logger.info("profile: "+clientResponse);
    	assertEquals(403, clientResponse.getStatus());

    }

    /**
     * createProfileTest
     * only super admin can create a new profile
     */
    @Test
    public void createProfileTest(){
    	/*
		 * create a profile 
		 */
    	ClientResponse clientResponse = RestcommProfilesTool.getInstance().createProfileResponse(deploymentUrl.toString(), superAdminAccountSid, superAdminAuthToken, profileDocument);
    	logger.info("clientResponse: "+clientResponse);
    	assertEquals(200, clientResponse.getStatus());

    	// TODO Read and verify further response
    }

    /**
     * updateProfileTest
     */
    @Test
    public void updateProfileTest(){
    	/*
		 * update a profile 
		 */
    	ClientResponse clientResponse = RestcommProfilesTool.getInstance().updateProfileResponse(deploymentUrl.toString(), superAdminAccountSid, superAdminAuthToken, profileSid, updatedProfileDocument);
    	logger.info("clientResponse: "+clientResponse);
    	assertEquals(200, clientResponse.getStatus());

    	// TODO Read and verify further response
    }

    /**
     * deleteProfileTest
     */
    @Test
    public void deleteProfileTest(){
    	/*
		 * delete a profile 
		 */
    	ClientResponse clientResponse = RestcommProfilesTool.getInstance().deleteProfileResponse(deploymentUrl.toString(), superAdminAccountSid, superAdminAuthToken, profileSid);
    	logger.info("clientResponse: "+clientResponse);
    	assertEquals(200, clientResponse.getStatus());

    	// TODO Read and verify further response
    }

    @Test 
    @Category(FeatureExpTests.class)
    public void createProfilePermissionTest(){
    	//admin tries to create profile
    	ClientResponse  clientResponse = RestcommProfilesTool.getInstance().createProfileResponse(deploymentUrl.toString(), adminAccountSid, adminAuthToken, profileDocument);
    	logger.info("clientResponse: "+clientResponse);
    	assertTrue(clientResponse.getStatus() == 403);
    	//developer tries to create profile
    	clientResponse = RestcommProfilesTool.getInstance().createProfileResponse(deploymentUrl.toString(), devAccountSid, devAuthToken, profileDocument);
    	logger.info("clientResponse: "+clientResponse);
    	assertTrue(clientResponse.getStatus() == 403);
    }

    /**
     * link/unlink a give Profile To an Account
     * @throws IOException 
     * @throws ClientProtocolException 
     */
    @Test
    public void linkUnLinkProfileToAccount() throws ClientProtocolException, IOException{
		/*
		 * link a profile to an account 
		 */
    	HttpResponse response = RestcommProfilesTool.getInstance().linkProfile(deploymentUrl.toString(), superAdminAccountSid, superAdminAuthToken, profileSid, superAdminAccountSid, RestcommProfilesTool.AssociatedResourceType.ACCOUNT);
    	logger.info("HttpResponse: "+response);
    	assertEquals(200, response.getStatusLine().getStatusCode());
    	
    	/* 
    	 * Get associated profile
    	 * from Accounts endpoint:
    	 * to verify association establishment.
    	 */
    	ClientResponse accountEndopintResponse = RestcommAccountsTool.getInstance().getAccountResponse(deploymentUrl.toString(), superAdminAccountSid, superAdminAuthToken, superAdminAccountSid);
    	WebResourceLinkHeaders linkHeaders = accountEndopintResponse.getLinks();
    	logger.info("accountEndopintResponse WebResourceLinkHeaders: "+linkHeaders);
    	assertNotNull(linkHeaders);
    	LinkHeader linkHeader = linkHeaders.getLink("related");
    	logger.info("accountEndopintResponse WebResourceLinkHeaders linkHeader: "+linkHeader);
    	assertNotNull(linkHeader);
    	assertTrue(linkHeader.getUri().toString().contains(profileSid));

    	/*
		 * unlink a profile from an account 
		 */
    	response = RestcommProfilesTool.getInstance().unLinkProfile(deploymentUrl.toString(), superAdminAccountSid, superAdminAuthToken, profileSid, superAdminAccountSid, RestcommProfilesTool.AssociatedResourceType.ACCOUNT);
    	logger.info("HttpResponse: "+response);
    	assertEquals(200, response.getStatusLine().getStatusCode());
    	
    	/* 
    	 * Get associated profile
    	 * from Accounts endpoint: 
    	 * to verify association removal
    	 */
    	accountEndopintResponse = RestcommAccountsTool.getInstance().getAccountResponse(deploymentUrl.toString(), superAdminAccountSid, superAdminAuthToken, superAdminAccountSid);
    	linkHeaders = accountEndopintResponse.getLinks();
    	logger.info("accountEndopintResponse WebResourceLinkHeaders: "+linkHeaders);
    	linkHeader = linkHeaders.getLink("related");
    	logger.info("accountEndopintResponse WebResourceLinkHeaders linkHeader: "+linkHeader);
    	assertNull(linkHeader);
    }

    /**
     * link/unlink a give Profile To an Organization
     * @throws IOException 
     * @throws ClientProtocolException 
     */
    @Test
    public void linkUnLinkProfileToOrganization() throws ClientProtocolException, IOException{
    	/*
		 * link a profile to an organizations 
		 */
    	HttpResponse response = RestcommProfilesTool.getInstance().linkProfile(deploymentUrl.toString(), superAdminAccountSid, superAdminAuthToken, profileSid, organizationSid, RestcommProfilesTool.AssociatedResourceType.ORGANIZATION);
    	logger.info("HttpResponse: "+response);
    	assertEquals(200, response.getStatusLine().getStatusCode());
    	
    	/* 
    	 * Get associated profile
    	 * from Organizations endpoint:
    	 * to verify association establishment.
    	 */
    	ClientResponse orgEndopintResponse = RestcommOrganizationsTool.getInstance().getOrganizationResponse(deploymentUrl.toString(), superAdminAccountSid, superAdminAuthToken, organizationSid);
    	WebResourceLinkHeaders linkHeaders = orgEndopintResponse.getLinks();
    	logger.info("accountEndopintResponse WebResourceLinkHeaders: "+linkHeaders);
    	assertNotNull(linkHeaders);
    	LinkHeader linkHeader = linkHeaders.getLink("related");
    	logger.info("accountEndopintResponse WebResourceLinkHeaders linkHeader: "+linkHeader);
    	assertNotNull(linkHeader);
    	assertTrue(linkHeader.getUri().toString().contains(profileSid));

    	/*
		 * unlink a profile from an organization 
		 */
    	response = RestcommProfilesTool.getInstance().unLinkProfile(deploymentUrl.toString(), superAdminAccountSid, superAdminAuthToken, profileSid, organizationSid, RestcommProfilesTool.AssociatedResourceType.ORGANIZATION);
    	logger.info("HttpResponse: "+response);
    	assertEquals(200, response.getStatusLine().getStatusCode());
    	
    	/* 
    	 * Get associated profile
    	 * from Organizations endpoint:
    	 * to verify association removal.
    	 */
    	orgEndopintResponse = RestcommOrganizationsTool.getInstance().getOrganizationResponse(deploymentUrl.toString(), superAdminAccountSid, superAdminAuthToken, organizationSid);
    	linkHeaders = orgEndopintResponse.getLinks();
    	logger.info("orgEndopintResponse WebResourceLinkHeaders: "+linkHeaders);
    	linkHeader = linkHeaders.getLink("related");
    	logger.info("orgEndopintResponse WebResourceLinkHeaders linkHeader: "+linkHeader);
    	assertNull(linkHeader);
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
