/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.restcomm.connect.testsuite.http;

import static org.junit.Assert.*;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.testsuite.http.RestcommUsageRecordsTool;

import java.net.URL;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

/**
 * @author <a href="mailto:abdulazizali@acm.org">abdulazizali77</a>
 */

@RunWith(Arquillian.class)
public class AccountPermissionsTest {
    private static Logger logger = Logger.getLogger(AccountPermissionsTest.class);

    private static final String version = Version.getVersion();
    private static final String revision = Version.getRevision();

    @ArquillianResource
    URL deploymentUrl;

    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAccountSid1 = "AC00000000000000000000000000000001";
    private String adminAccountSid2 = "AC00000000000000000000000000000002";
    private String adminAccountSid3 = "AC00000000000000000000000000000003";
    private String adminAccountSid4 = "AC00000000000000000000000000000004";

    private String adminUsername1 = "accperm1@company.com";
    private String adminUsername2 = "accperm2@company.com";
    private String adminUsername3 = "accperm3@company.com";
    private String adminUsername4 = "accperm4@company.com";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    private Sid dbPermissionSid1 = new Sid("PE00000000000000000000000000000001");
    private Sid dbPermissionSid2 = new Sid("PE00000000000000000000000000000002");
    private Sid dbPermissionSid3 = new Sid("PE00000000000000000000000000000003");

    private String dbPermissionName1 = "RestComm:*:MOD1";
    private String dbPermissionName2 = "RestComm:*:MOD2";
    private String dbPermissionName3 = "RestComm:*:MOD3";
    private String dbPermissionName4 = "RestComm:*:MOD4";
    private String dbPermissionName5 = "RestComm:*:MOD5";
    private String dbPermissionName6 = "RestComm:*:MOD6";

    @Test
    public void testGetAccountPermissionsList() {
        //GET Accounts.json/{sid}/Permissions
        //GET Accounts.json/{sid}
        JsonElement je = RestcommAccountsTool.getInstance().getAccountPermissions(deploymentUrl.toString(), adminAccountSid1, adminAuthToken);
        je = RestcommAccountsTool.getInstance().getAccountPermissions(deploymentUrl.toString(), adminAccountSid1, adminAuthToken);

        assert(je.isJsonArray());
        JsonArray ja = je.getAsJsonArray();
        assert((ja.get(0)).getAsJsonObject().get("sid").getAsString().equals(dbPermissionSid1.toString()));
        assert((ja.get(1)).getAsJsonObject().get("sid").getAsString().equals(dbPermissionSid2.toString()));
        assert((ja.get(2)).getAsJsonObject().get("sid").getAsString().equals(dbPermissionSid3.toString()));

        assert((ja.get(0)).getAsJsonObject().get("name").getAsString().equals(dbPermissionName1));
        assert((ja.get(1)).getAsJsonObject().get("name").getAsString().equals(dbPermissionName2));
        assert((ja.get(2)).getAsJsonObject().get("name").getAsString().equals(dbPermissionName3));

        assert((ja.get(0)).getAsJsonObject().get("value").getAsString().equals("true"));
        assert((ja.get(1)).getAsJsonObject().get("value").getAsString().equals("false"));
        assert((ja.get(2)).getAsJsonObject().get("value").getAsString().equals("true"));
    }

    @Test
    public void testAddAccountPermissions() {
        //Either PermissionId or PermissionName allowed, not both
        //If PermissionValue missing, then assumed true

        //POST Accounts.json/{sid} -d "PermissionId={permissionSid}"
        //POST Accounts.json/{sid} -d "PermissionId={permissionSid}" -d "PermissionValue=true"
        //POST Accounts.json/{sid} -d "PermissionId={permissionSid}" -d "PermissionValue=false"
        //POST Accounts.json/{sid} -d "PermissionName=Domain1:Action1:Module1" -d "PermissionValue=true"
        //POST Accounts.json/{sid} -d "PermissionName=Domain1:Action1:Module1" -d "PermissionValue=false"
        //POST Accounts.json/{sid} -d "PermissionName=Domain1:Action1:Module1"

        JsonElement je = RestcommAccountsTool.getInstance().getAccountPermissions(deploymentUrl.toString(), adminAccountSid2, adminAuthToken);
        assert(je.isJsonArray());
        assert(je.getAsJsonArray().size()==0);

        //add permissions
        JsonObject createAccountResponse = RestcommAccountsTool.getInstance().addAccountPermission(deploymentUrl.toString(),
                adminAccountSid2, adminAuthToken, dbPermissionSid1.toString(), "false");

        createAccountResponse = RestcommAccountsTool.getInstance().addAccountPermission(deploymentUrl.toString(),
                adminAccountSid2, adminAuthToken, dbPermissionSid2.toString(), "true");

        createAccountResponse = RestcommAccountsTool.getInstance().addAccountPermission(deploymentUrl.toString(),
                adminAccountSid2, adminAuthToken, dbPermissionSid3.toString(), "false");

        //validate
        je = RestcommAccountsTool.getInstance().getAccountPermissions(deploymentUrl.toString(), adminAccountSid2, adminAuthToken);
        assert(je.isJsonArray());
        JsonArray ja = je.getAsJsonArray();
        assert(ja.size()==3);
        assert((ja.get(0)).getAsJsonObject().get("sid").getAsString().equals(dbPermissionSid1.toString()));
        assert((ja.get(1)).getAsJsonObject().get("sid").getAsString().equals(dbPermissionSid2.toString()));
        assert((ja.get(2)).getAsJsonObject().get("sid").getAsString().equals(dbPermissionSid3.toString()));

        assert((ja.get(0)).getAsJsonObject().get("name").getAsString().equals(dbPermissionName1));
        assert((ja.get(1)).getAsJsonObject().get("name").getAsString().equals(dbPermissionName2));
        assert((ja.get(2)).getAsJsonObject().get("name").getAsString().equals(dbPermissionName3));
        
        assert((ja.get(0)).getAsJsonObject().get("value").getAsString().equals("false"));
        assert((ja.get(1)).getAsJsonObject().get("value").getAsString().equals("true"));
        assert((ja.get(2)).getAsJsonObject().get("value").getAsString().equals("false"));
    }

    @Test
    public void testUpdateAccountPermissions() {
        //If exists, will update, if does not exist, will add
        //POST Accounts.json/{sid} -d "PermissionId={dbPermissionSid}"
        //POST Accounts.json/{sid} -d "PermissionId={dbPermissionSid}" -d "PermissionValue=true"
        //POST Accounts.json/{sid} -d "PermissionId={dbPermissionSid}" -d "PermissionValue=false"
        //POST Accounts.json/{sid} -d "PermissionName=Domain1:Action1:Module1" -d "PermissionValue=true"
        //POST Accounts.json/{sid} -d "PermissionName=Domain1:Action1:Module1" -d "PermissionValue=false"
        //POST Accounts.json/{sid} -d "PermissionName=Domain1:Action1:Module1"

        //POST Accounts.json/{sid}/Permission/{dbPermissionSid} -d "PermissionValue=false"
        //POST Accounts.json/{sid}/Permission/{permissionName} -d "PermissionValue=false" ???

        JsonElement je = RestcommAccountsTool.getInstance().getAccountPermissions(deploymentUrl.toString(), adminAccountSid3, adminAuthToken);
        assert(je.isJsonArray());
        JsonArray jsonArray = je.getAsJsonArray();
        assert(jsonArray.size()==3);
        assert((jsonArray.get(0)).getAsJsonObject().get("value").getAsString().equals("true"));
        assert((jsonArray.get(1)).getAsJsonObject().get("value").getAsString().equals("false"));
        assert((jsonArray.get(2)).getAsJsonObject().get("value").getAsString().equals("true"));

        //update preexisting rows
        JsonObject createAccountResponse = RestcommAccountsTool.getInstance().addAccountPermission(deploymentUrl.toString(),
                adminAccountSid3, adminAuthToken, dbPermissionSid1.toString(), "false");

        createAccountResponse = RestcommAccountsTool.getInstance().addAccountPermission(deploymentUrl.toString(),
                adminAccountSid3, adminAuthToken, dbPermissionSid2.toString(), "true");

        createAccountResponse = RestcommAccountsTool.getInstance().addAccountPermission(deploymentUrl.toString(),
                adminAccountSid3, adminAuthToken, dbPermissionSid3.toString(), "false");

        //validate
        je = RestcommAccountsTool.getInstance().getAccountPermissions(deploymentUrl.toString(), adminAccountSid3, adminAuthToken);
        assert(je.isJsonArray());
        jsonArray = je.getAsJsonArray();
        assert(jsonArray.size()==3);
        assert((jsonArray.get(0)).getAsJsonObject().get("value").getAsString().equals("false"));
        assert((jsonArray.get(1)).getAsJsonObject().get("value").getAsString().equals("true"));
        assert((jsonArray.get(2)).getAsJsonObject().get("value").getAsString().equals("false"));
    }

    @Test
    public void testDeleteAccountPermissions() {
        //DELETE Accounts.json/{sid}/Permission/{dbPermissionSid}"
        //DELETE Accounts.json/{sid}/Permission/{permissionName}" ???
        JsonElement je = RestcommAccountsTool.getInstance().getAccountPermissions(deploymentUrl.toString(), adminAccountSid4, adminAuthToken);
        assert(je.isJsonArray());
        JsonArray jsonArray = je.getAsJsonArray();
        assert(jsonArray.size()==3);
        assert((jsonArray.get(0)).getAsJsonObject().get("value").getAsString().equals("true"));
        assert((jsonArray.get(1)).getAsJsonObject().get("value").getAsString().equals("false"));
        assert((jsonArray.get(2)).getAsJsonObject().get("value").getAsString().equals("true"));

        JsonObject createAccountResponse = RestcommAccountsTool.getInstance().deleteAccountPermission(deploymentUrl.toString(),
                adminAccountSid4, adminAuthToken, dbPermissionSid2.toString());

        je = RestcommAccountsTool.getInstance().getAccountPermissions(deploymentUrl.toString(), adminAccountSid4, adminAuthToken);
        assert(je.isJsonArray());
        jsonArray = je.getAsJsonArray();
        assert(jsonArray.size()==2);
    }

    @Deployment(name = "PermissionsTest", managed = true, testable = false)
    public static WebArchive createWebArchiveNoGw() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "restcomm.war");
        final WebArchive restcommArchive = ShrinkWrapMaven.resolver()
                .resolve("org.restcomm:restcomm-connect.application:war:" + version).withoutTransitivity()
                .asSingle(WebArchive.class);
        archive = archive.merge(restcommArchive);
        archive.delete("/WEB-INF/sip.xml");
        archive.delete("/WEB-INF/conf/restcomm.xml");
        archive.delete("/WEB-INF/data/hsql/restcomm.script");
        // archive.delete("/WEB-INF/data/hsql/restcomm.properties");
        archive.addAsWebInfResource("sip.xml");
        archive.addAsWebInfResource("restcomm.xml", "conf/restcomm.xml");
        archive.addAsWebInfResource("restcomm.script_permissions_test", "data/hsql/restcomm.script");
        // archive.addAsWebInfResource("restcomm.properties", "data/hsql/restcomm.properties");
        return archive;
    }
}