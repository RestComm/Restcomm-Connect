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

import com.sun.jersey.api.NotFoundException;
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

import junit.framework.Assert;

/**
 * @author <a href="mailto:abdulazizali@acm.org">abdulazizali77</a>
 */

@RunWith(Arquillian.class)
public class PermissionsTest {
    private static Logger logger = Logger.getLogger(PermissionsTest.class);

    private static final String version = Version.getVersion();
    private static final String revision = Version.getRevision();

    @ArquillianResource
    URL deploymentUrl;

    private String adminAccountSid = "ACae6e420f425248d6a26948c17a9e2acf";
    private String adminAuthToken = "77f8c12cc7b8f8423e5c38b035249166";

    private String dbPermissionId1 = "PE00000000000000000000000000000001";
    private String dbPermissionName1 = "RestComm:*:MOD1";
    private String dbPermissionId2 = "PE00000000000000000000000000000002";
    private String dbPermissionName2 = "RestComm:*:MOD2";
    private String dbPermissionId3 = "PE00000000000000000000000000000003";
    private String dbPermissionName3 = "RestComm:*:MOD3";

    @Test
    public void testGetPermissionsList() {
        JsonElement permissions;
        permissions = RestcommPermissionsTool.getInstance().getPermissionsList(deploymentUrl.toString(), adminAccountSid, adminAuthToken, false);

        assert(permissions.isJsonArray());
        JsonArray jsonArray = permissions.getAsJsonArray();
        assert(jsonArray.size()==3);
        assert(((JsonObject)jsonArray.get(0)).get("sid").getAsString().equals(dbPermissionId1));
        assert(((JsonObject)jsonArray.get(0)).get("name").getAsString().equals(dbPermissionName1));

        assert(((JsonObject)jsonArray.get(1)).get("sid").getAsString().equals(dbPermissionId2));
        assert(((JsonObject)jsonArray.get(1)).get("name").getAsString().equals(dbPermissionName2));

        assert(((JsonObject)jsonArray.get(2)).get("sid").getAsString().equals(dbPermissionId3));
        assert(((JsonObject)jsonArray.get(2)).get("name").getAsString().equals(dbPermissionName3));
    }

    @Test
    public void testGetPermission() throws Exception{
        String permissionSid1 = dbPermissionId1;
        JsonElement permission = null;
        permission = RestcommPermissionsTool.getInstance().getPermission(deploymentUrl.toString(), adminAccountSid, adminAuthToken, permissionSid1);

        assert(permission.isJsonObject());
        assert(permission.getAsJsonObject().get("sid").getAsString().equals(dbPermissionId1));
        assert(permission.getAsJsonObject().get("name").getAsString().equals(dbPermissionName1));
    }

    @Test
    public void testAddPermission() throws Exception {
        MultivaluedMap<String, String> permissionParams = new MultivaluedMapImpl();
        String permissionSid1 = null;
        String permissionName1 = "RestComm:*:MOD99";

        permissionParams.add("Name", permissionName1);
        JsonObject permission = RestcommPermissionsTool.getInstance().addPermission(deploymentUrl.toString(), adminAccountSid, adminAuthToken, permissionParams);
        assert(permission.isJsonObject());
        assert(permission.get("name").getAsString().equals(permissionName1));

        permissionSid1 = permission.get("sid").getAsString();
        JsonObject permission2 = RestcommPermissionsTool.getInstance().getPermission(deploymentUrl.toString(), adminAccountSid, adminAuthToken, permissionSid1);

        assert(permission2.isJsonObject());
        assert(permission2.get("sid").getAsString().equals(permissionSid1));
        assert(permission2.get("name").getAsString().equals(permissionName1));
    }

    @Test
    public void testUpdatePermission() throws Exception {
        //FIXME: simplify, create abstract validation function
        String newName1 = "RestComm:*:NEW1";
        String newName2 = "RestComm:*:NEW2";

        MultivaluedMap<String, String> configurationParams = null;
        JsonObject permission1;
        JsonObject permission2;

        //update 1
        configurationParams = new MultivaluedMapImpl();
        configurationParams.add("Name", newName1);
        permission2 = RestcommPermissionsTool.getInstance().getPermission(deploymentUrl.toString(), adminAccountSid,
                adminAuthToken, dbPermissionId1);
        assertTrue(permission2.get("sid").getAsString().equals(dbPermissionId1));
        assertTrue(permission2.get("name").getAsString().equals(dbPermissionName1));

        permission1 = RestcommPermissionsTool.getInstance().updatePermission(deploymentUrl.toString(), adminAccountSid,
                adminAuthToken, dbPermissionId1, configurationParams);
        assertTrue(permission1.get("sid").getAsString().equals(dbPermissionId1));
        assertTrue(permission1.get("name").getAsString().equals(newName1));

        permission2 = RestcommPermissionsTool.getInstance().getPermission(deploymentUrl.toString(), adminAccountSid,
                adminAuthToken, dbPermissionId1);
        assertTrue(permission2.get("sid").getAsString().equals(dbPermissionId1));
        assertTrue(permission2.get("name").getAsString().equals(newName1));

        //update2
        configurationParams = new MultivaluedMapImpl();
        configurationParams.add("Name", newName2);
        permission2 = RestcommPermissionsTool.getInstance().getPermission(deploymentUrl.toString(), adminAccountSid,
                adminAuthToken, dbPermissionId2);
        assertTrue(permission2.get("sid").getAsString().equals(dbPermissionId2));
        assertTrue(permission2.get("name").getAsString().equals(dbPermissionName2));

        permission1 = RestcommPermissionsTool.getInstance().updatePermission(deploymentUrl.toString(), adminAccountSid,
                adminAuthToken, dbPermissionId2, configurationParams, false);
        assertTrue(permission1.get("sid").getAsString().equals(dbPermissionId2));
        assertTrue(permission1.get("name").getAsString().equals(newName2));

        permission2 = RestcommPermissionsTool.getInstance().getPermission(deploymentUrl.toString(), adminAccountSid,
                adminAuthToken, dbPermissionId2);
        assertTrue(permission2.get("sid").getAsString().equals(dbPermissionId2));
        assertTrue(permission2.get("name").getAsString().equals(newName2));
    }

    @Test(expected = NotFoundException.class)
    public void testDeletePermission() throws Exception {
        JsonObject permission1;
        JsonElement permission2;

        //get
        permission1 = RestcommPermissionsTool.getInstance().getPermission(deploymentUrl.toString(), adminAccountSid, adminAuthToken, dbPermissionId1);
        assertNotNull(permission1);
        assertTrue(permission1.isJsonObject());
        assertTrue(permission1.get("sid").getAsString().equals(dbPermissionId1));
        assertTrue(permission1.get("name").getAsString().equals(dbPermissionName1));

        //delete
        permission2 = RestcommPermissionsTool.getInstance().removePermission(deploymentUrl.toString(), adminAccountSid, adminAuthToken, dbPermissionId1);
        assertNotNull(permission2);
        assertTrue(permission2.isJsonObject());
        assertTrue((permission2.getAsJsonObject()).get("sid").getAsString().equals(dbPermissionId1));
        assertTrue((permission2.getAsJsonObject()).get("name").getAsString().equals(dbPermissionName1));

        //FIXME: need to change EPs to catch NotFoundException??
        RestcommPermissionsTool.getInstance().getPermission(deploymentUrl.toString(), adminAccountSid, adminAuthToken, dbPermissionId1, false);

    }

    @Test
    public void testDeletePermissionByName() {

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