/*
 *  TeleStax, Open Source Cloud Communications
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.ExtensionsConfigurationDao;
import org.restcomm.connect.dao.entities.RestCommResponse;
import org.restcomm.connect.extension.api.ExtensionConfiguration;
import org.restcomm.connect.http.converter.ExtensionConfigurationConverter;
import org.restcomm.connect.http.converter.RestCommResponseConverter;
import org.restcomm.connect.http.exceptions.InsufficientPermission;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.ok;
import static javax.ws.rs.core.Response.status;

/**
 * Created by gvagenas on 12/10/2016.
 */
public class ExtensionsConfigurationEndpoint extends SecuredEndpoint {
    protected Configuration allConfiguration;
    protected Configuration configuration;
    protected Gson gson;
    protected XStream xstream;
    protected ExtensionsConfigurationDao extensionsConfigurationDao;

    public ExtensionsConfigurationEndpoint() { super(); }

    @PostConstruct
    void init() {
        allConfiguration = (Configuration) context.getAttribute(Configuration.class.getName());
        configuration = allConfiguration.subset("runtime-settings");
        super.init(configuration);
        extensionsConfigurationDao = ((DaoManager) context.getAttribute(DaoManager.class.getName())).getExtensionsConfigurationDao();
        final ExtensionConfigurationConverter converter = new ExtensionConfigurationConverter(configuration);
        final GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(ExtensionConfiguration.class, converter);
        builder.setPrettyPrinting();
        gson = builder.create();
        xstream = new XStream();
        xstream.alias("RestcommResponse", RestCommResponse.class);
        xstream.registerConverter(converter);
        xstream.registerConverter(new ExtensionConfigurationConverter(configuration));
        xstream.registerConverter(new RestCommResponseConverter(configuration));
        // Make sure there is an authenticated account present when this endpoint is used
        checkAuthenticatedAccount();
    }

    /**
     * Will be used to get configuration for extension
     * @param extensionId
     * @param info
     * @param responseType
     * @return
     */
    protected Response getConfiguration(final String extensionId, final UriInfo info, final MediaType responseType) {
        // The request might contain extension name and configuration property name in order to retreive specific configuration
        // Check for configuration property at info or otherwise get all configuration for the given extension
        //Parameter "extensionId" could be the extension Sid or extension name.
        //Parameter "info" can contain "specific"
        if (!isSuperAdmin()) {
            throw new InsufficientPermission();
        }

        ExtensionConfiguration extensionConfiguration = null;

        if (Sid.pattern.matcher(extensionId).matches()) {
            try {
                extensionConfiguration = extensionsConfigurationDao.getConfigurationBySid(new Sid(extensionId));
            } catch (Exception e) {
                return status(NOT_FOUND).build();
            }
        } else {
            try {
                extensionConfiguration = extensionsConfigurationDao.getConfigurationByName(extensionId);
            } catch (Exception e) {
                return status(NOT_FOUND).build();
            }
        }

        if (extensionConfiguration == null) {
            return status(NOT_FOUND).build();
        } else {
            if (APPLICATION_XML_TYPE == responseType) {
                final RestCommResponse response = new RestCommResponse(extensionConfiguration);
                return ok(xstream.toXML(response), APPLICATION_XML).build();
            } else if (APPLICATION_JSON_TYPE == responseType) {
                return ok(gson.toJson(extensionConfiguration), APPLICATION_JSON).build();
            } else {
                return null;
            }
        }
    }

    protected Response updateConfiguration(String extension, MultivaluedMap<String, String> data, MediaType applicationJsonType) {
        return null;
    }
}
