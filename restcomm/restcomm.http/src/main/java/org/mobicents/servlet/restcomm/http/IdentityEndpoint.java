/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
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
package org.mobicents.servlet.restcomm.http;

import javax.annotation.PostConstruct;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.mobicents.servlet.restcomm.configuration.RestcommConfiguration;
import org.mobicents.servlet.restcomm.configuration.sets.MutableIdentityConfigurationSet;
import org.mobicents.servlet.restcomm.configuration.sets.IdentityConfigurationSet;
import org.mobicents.servlet.restcomm.dao.AccountsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.identity.RestcommIdentityApi;
import org.mobicents.servlet.restcomm.identity.migration.IdentityMigrationTool;

import com.google.gson.Gson;

/**
 * @author orestis.tsakiridis@telestax.com (Orestis Tsakiridis)
 */
@Path("/instance")
public class IdentityEndpoint extends AccountsCommonEndpoint {

    private MutableIdentityConfigurationSet iConfig;
    private IdentityConfigurationSet imConfig;
    private AccountsDao accountsDao;

    public IdentityEndpoint() {
        // TODO Auto-generated constructor stub
    }

    @PostConstruct
    private void init() {
        this.iConfig = RestcommConfiguration.getInstance().getMutableIdentity();
        this.imConfig = RestcommConfiguration.getInstance().getIdentity();
        DaoManager daoManager = (DaoManager) context.getAttribute(DaoManager.class.getName());
        this.accountsDao = daoManager.getAccountsDao();
    }

    /**
     * Registers this restcomm instance to the authorization server. username/password credentials are used for authentication.
     * The registering user is granted Administrator access to the instance and is linked to the (legacy) administrator account.
     * @param baseUrl
     * @param username
     * @param password
     * @param instanceSecret
     * @return
     */
    @POST
    @Path("/register")
    public Response registerInstance(@FormParam("restcommBaseUrl") String baseUrl, @FormParam("username") String username, @FormParam("password") String password, @FormParam("instanceSecret") String instanceSecret )  {
        // make sure registration/migration through UI is enabled
        if ( ! imConfig.getMethod().equals(IdentityConfigurationSet.MigrationMethod.ui))
            return Response.status(Status.BAD_REQUEST).build();
        // if it is already registered do nothing
        if ( ! "init".equals(iConfig.getMode()) )
            return Response.status(Status.CONFLICT).entity("Instance already registered").build();
        // do the actual migration/registration
        RestcommIdentityApi api = new RestcommIdentityApi(imConfig.getAuthServerBaseUrl(),username,password, imConfig.getRealm(),null);
        IdentityMigrationTool migrationTool = new IdentityMigrationTool(accountsDao, api, true,null, iConfig,new String[] {baseUrl});
        migrationTool.migrate();
        // build response
        MutableIdentityConfigurationSet newConfig = RestcommConfiguration.getInstance().getMutableIdentity();
        IdentityInstanceEntity instanceEntity = new IdentityInstanceEntity();
        instanceEntity.setInstanceName(newConfig.getInstanceId());
        Gson gson = new Gson();
        return Response.ok().entity(gson.toJson(instanceEntity)).build();
    }

    // generate a random secret for the instance/restcomm-rest client if none specified in the request
    /*protected String generateInstanceSecret() {
        return UUID.randomUUID().toString();
    }*/

    public class IdentityInstanceEntity {
        private String instanceName;

        public void setInstanceName(String instanceName) {
            this.instanceName = instanceName;
        }
    }

}
