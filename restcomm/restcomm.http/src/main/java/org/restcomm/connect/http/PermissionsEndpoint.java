/*  TeleStax, Open Source Cloud Communications
 *  Copyright 2011-2016, Telestax Inc and individual contributors
 *  by the @authors tag.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package org.restcomm.connect.http;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.PermissionsDao;

import org.restcomm.connect.dao.entities.Permission;
import org.restcomm.connect.dao.entities.PermissionList;
import org.restcomm.connect.dao.entities.RestCommResponse;

import org.restcomm.connect.http.converter.PermissionsConverter;
import org.restcomm.connect.http.converter.RestCommResponseConverter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;

public class PermissionsEndpoint extends SecuredEndpoint {
    protected Configuration allConfiguration;
    protected Configuration configuration;
    protected Gson gson;
    protected XStream xstream;
    protected PermissionsDao permissionsDao;

    public PermissionsEndpoint() {
        // TODO Auto-generated constructor stub

    }

    @PostConstruct
    void init() {
        allConfiguration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = allConfiguration.subset("runtime-settings");
        super.init(configuration);

        permissionsDao = ((DaoManager) context.getAttribute(DaoManager.class.getName())).getPermissionsDao();
        final PermissionsConverter converter = new PermissionsConverter(configuration);
        //final PermissionsListConverter listConverter = new PermissionsListConverter(configuration);
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(Permission.class, converter);
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(converter);
        xstream.registerConverter(new PermissionsConverter(configuration));
        xstream.registerConverter(new RestCommResponseConverter(configuration));
        // Make sure there is an authenticated account present when this endpoint is used
        checkAuthenticatedAccount();
    }

    protected Response getPermissionsList(MediaType responseType) {
        final List<Permission> permissions = new ArrayList<Permission>();
        permissions.addAll(permissionsDao.getPermissions());
        if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(new PermissionList(permissions));
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(permissions), APPLICATION_JSON).build();
        } else {
            return null;
        }
    }

    protected Response addPermission(MultivaluedMap<String, String> data, MediaType responseType) {
        Sid permissionSid = Sid.generate(Sid.Type.PERMISSION);
        String permissionName = data.getFirst("Name");
        Permission permission = new Permission(permissionSid, permissionName);
        permissionsDao.addPermission(permission);

        if (permission == null) {
            return status(NOT_FOUND).build();
        } else {
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(permission);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(permission), APPLICATION_JSON).build();
            } else {
                return null;
            }
        }
    }

    //protected Response getPermissionByName(Sid sid, MediaType responseType){
    protected Response getPermission(Sid sid, MediaType responseType){

        Permission permission = null;
        permission = permissionsDao.getPermission(sid);

        if (permission == null) {
            return status(NOT_FOUND).build();
        } else {
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(permission);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(permission), APPLICATION_JSON).build();
            } else {
                return null;
            }
        }
    }
    //protected Response deletePermissionByName(Sid permissionSid, MediaType responseType) {}
    protected Response deletePermission(Sid permissionSid, MediaType responseType) {
        Permission permission = permissionsDao.getPermission(permissionSid);
        if (permission != null) {
            //catch PersistenceException
            permissionsDao.deletePermission(permissionSid);

            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(permission);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(permission), APPLICATION_JSON).build();
            } else {
                return null;
            }
        } else {
            return status(Response.Status.NOT_FOUND).build();
        }
    }

    protected Response updatePermission(Sid permissionSid, MultivaluedMap<String, String> data, MediaType responseType) {
        Permission permission = permissionsDao.getPermission(permissionSid);
        String name = data.getFirst("Name");
        permission.setName(name);
        if (permission != null) {
            permissionsDao.updatePermission(permissionSid, permission);
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(permission);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(permission), APPLICATION_JSON).build();
            } else {
                return null;
            }
        } else {
            return status(Response.Status.NOT_FOUND).build();
        }
    }
}
