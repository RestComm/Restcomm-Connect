/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.dao.OrgIdentityDao;
import org.mobicents.servlet.restcomm.dao.OrganizationsDao;
import org.mobicents.servlet.restcomm.entities.OrgIdentity;
import org.mobicents.servlet.restcomm.entities.Organization;
import org.mobicents.servlet.restcomm.entities.RestCommResponse;
import org.mobicents.servlet.restcomm.entities.Sid;
import org.mobicents.servlet.restcomm.http.converter.OrgIdentityConverter;
import org.mobicents.servlet.restcomm.http.converter.RestCommResponseConverter;
import org.mobicents.servlet.restcomm.http.exceptions.AuthorizationException;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

/**
 * TODO support XML responses ? This endpoint is supposed to be used internally from AdminUI.
 *
 * @author Orestis Tsakiridis
 */
public class OrgIdentityEndpoint extends SecuredEndpoint {
    //MainConfigurationSet mainConfig;
    OrgIdentityDao orgIdentityDao;
    OrganizationsDao organizationDao;
    protected Gson gson;
    protected XStream xstream;

    // retrieve deps from context, singletons etc.
    public OrgIdentityEndpoint() {
        this.configuration = ((Configuration) context.getAttribute(Configuration.class.getName())).subset("runtime-settings");
        final DaoManager daos = (DaoManager) context.getAttribute(DaoManager.class.getName());
        //mainConfig = RestcommConfiguration.getInstance().getMain();

        this.orgIdentityDao = daos.getOrgIdentityDao();
        this.organizationDao = daos.getOrganizationsDao();
    }

    // used for manually bootstrapping the endpoint when tested
    public OrgIdentityEndpoint(ServletContext context, HttpServletRequest request, Configuration rootConfiguration, DaoManager daoManager) {
        super(context,request);
        this.configuration = rootConfiguration.subset("runtime-settings");
        //mainConfig = restcommConfiguration.getMain();

        this.orgIdentityDao = daoManager.getOrgIdentityDao();
        this.organizationDao = daoManager.getOrganizationsDao();
    }

    @PostConstruct
    void init() {
        super.init(configuration); // this is needed by AbstractEndpoint
        // converters
        final OrgIdentityConverter converter = new OrgIdentityConverter(configuration);
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(OrgIdentity.class, converter);
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(converter);
        xstream.registerConverter(new RestCommResponseConverter(configuration));
    }

    /**
     * Create a new OrgIdentity entity. It also applies the logic for picking the right name for it.
     *
     *
     * @param name
     * @param redirectUrl
     * @return
     */
    protected Response createOrgIdentity(String name, String organizationDomain, String redirectUrl) {
        // TODO apply access control rules
        OrgIdentity oi = new OrgIdentity();

        if ( StringUtils.isEmpty(name) ) {
            String errorResponse = "{\"error\":\"no name specified\"}";
            return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).type(APPLICATION_JSON_TYPE).build();
        } else {
            // TODO validate name here
            oi.setName(name);
        }

        // determing the organization that will be secured
        Organization securedOrganization;
        if (StringUtils.isEmpty(organizationDomain)) {
            // No organization info provided. Use the one from the url.
            securedOrganization = getOrganization();
        } else {
            securedOrganization = organizationDao.getOrganization(organizationDomain);
        }
        if (securedOrganization == null)
            return Response.status(Response.Status.BAD_REQUEST).type(APPLICATION_JSON_TYPE).build();
        oi.setOrganizationSid(securedOrganization.getSid());

        // check if it's already there
        if ( orgIdentityDao.getOrgIdentityByName(name) != null)
            return Response.status(Response.Status.CONFLICT).build();

        // store it
        orgIdentityDao.addOrgIdentity(oi);

        return Response.ok(gson.toJson(oi), APPLICATION_JSON).build();
    }

    protected Response updateOrgIdentity(String sid, String name) {
        // TODO apply access control rules
        if (StringUtils.isEmpty(sid) || StringUtils.isEmpty(name))
            return Response.status(Response.Status.BAD_REQUEST).build();

        Sid orgIdentitySid = new Sid(sid);
        OrgIdentity orgIdentity = orgIdentityDao.getOrgIdentity(orgIdentitySid);
        if (orgIdentity != null) {
            orgIdentity.setName(name);
            orgIdentityDao.updateOrgIdentity(orgIdentity);
            return Response.ok(gson.toJson(orgIdentity), APPLICATION_JSON).build();
        } else
            return Response.status(Response.Status.NOT_FOUND).build();
    }


    /**
     * Returns the OrgIdentity for 'current' organization if any.
     * @return
     */
    protected Response getCurrentOrgIdentity() {
        // TODO use a proper converter here
        if (getOrgIdentity() == null)
            return Response.status(Response.Status.NOT_FOUND).build();
        else {
            return Response.ok(gson.toJson(getOrgIdentity()), APPLICATION_JSON).build();
        }
    }

    protected Response removeOrgIdentity(String sid) {
        if ( ! hasAccountRole(getAdministratorRole()) )
            throw new AuthorizationException();
        Sid orgIdentitySid;
        try {
            orgIdentitySid = new Sid(sid);
        } catch (Exception e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        OrgIdentity instance = orgIdentityDao.getOrgIdentity(orgIdentitySid);
        if (instance != null) {
            orgIdentityDao.removeOrgIdentity(orgIdentitySid);
            return Response.ok().build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }




}
