package org.restcomm.connect.testsuite.http;

import com.google.gson.JsonObject;
import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.archive.ShrinkWrapMaven;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.restcomm.connect.commons.Version;

import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Arquillian.class)
public class AccountsMigrationEndpointTest {
    final static Logger logger = Logger.getLogger(AccountsMigrationEndpointTest.class);
    private static final String version = Version.getVersion();

    @ArquillianResource
    URL deploymentUrl;

    private String adminUsername = "administrator@company.com";
    private String adminFriendlyName = "Default Administrator Account";
    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";
    private String adminPassword = "RestComm";

    private String childUsername = "child@company.com";
    private String childSid = "AC574d775522c96f9aacacc5ca60c8c74f";
    private String childAuthToken ="77f8c12cc7b8f8423e5c38b035249166";

    @Test
    public void testMigrateSingleAccount() {
        String accountSid = "AC574d775522c96f9aacacc5ca60c8c74f";
        String newOrganizationSid = "ORafbe225ad37541eba518a74248f0ac4d";
        JsonObject migratedAccount = RestcommAccountsTool.getInstance().migrateAccount(deploymentUrl.toString(), adminUsername,
                adminAuthToken, accountSid, newOrganizationSid);
        assertNotNull(migratedAccount);
        String org = migratedAccount.getAsJsonObject().get("organization").getAsString();
        assertEquals(newOrganizationSid, org);
    }

    @Deployment(name = "AccountsMigrationEndpointTest", managed = true, testable = false)
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
        archive.addAsWebInfResource("restcomm.script_accounts_migration_test", "data/hsql/restcomm.script");
        logger.info("Packaged Test App");
        return archive;
    }

}
