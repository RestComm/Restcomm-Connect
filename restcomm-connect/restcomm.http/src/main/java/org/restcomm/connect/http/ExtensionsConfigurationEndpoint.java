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
import org.joda.time.DateTime;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.ExtensionsConfigurationDao;
import org.restcomm.connect.dao.entities.RestCommResponse;
import org.restcomm.connect.extension.api.ConfigurationException;
import org.restcomm.connect.extension.api.ExtensionConfiguration;
import org.restcomm.connect.http.converter.ExtensionConfigurationConverter;
import org.restcomm.connect.http.converter.RestCommResponseConverter;
import org.restcomm.connect.http.exceptions.InsufficientPermission;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;
import static javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_ACCEPTABLE;
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
     * @param responseType
     * @return
     */
    protected Response getConfiguration(final String extensionId, final Sid accountSid, final MediaType responseType) {
        //Parameter "extensionId" could be the extension Sid or extension name.
        if (!isSuperAdmin()) {
            throw new InsufficientPermission();
        }

        ExtensionConfiguration extensionConfiguration = null;
        ExtensionConfiguration extensionAccountConfiguration = null;
        Sid extensionSid = null;
        String extensionName = null;

        if(Sid.pattern.matcher(extensionId).matches()){
            extensionSid = new Sid(extensionId);
        } else {
            extensionName = extensionId;
        }

        if (Sid.pattern.matcher(extensionId).matches()) {
            try {
                extensionConfiguration = extensionsConfigurationDao.getConfigurationBySid(extensionSid);
            } catch (Exception e) {
                return status(NOT_FOUND).build();
            }
        } else {
            try {
                extensionConfiguration = extensionsConfigurationDao.getConfigurationByName(extensionName);
            } catch (Exception e) {
                return status(NOT_FOUND).build();
            }
        }

        if (accountSid!=null) {
            if(extensionSid == null ){
                extensionSid = extensionConfiguration.getSid();
            }
            try {
                extensionAccountConfiguration = extensionsConfigurationDao.getAccountExtensionConfiguration(accountSid.toString(), extensionSid.toString());
                extensionConfiguration.setConfigurationData(extensionAccountConfiguration.getConfigurationData(), extensionAccountConfiguration.getConfigurationType());
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

    private void validate(final MultivaluedMap<String, String> data) throws NullPointerException {
        if (!data.containsKey("ExtensionName")) {
            throw new NullPointerException("Extension name can not be null.");
        } else if (!data.containsKey("ConfigurationData")) {
            throw new NullPointerException("ConfigurationData can not be null.");
        }
    }

    private ExtensionConfiguration createFrom(final MultivaluedMap<String, String> data, final MediaType responseType) {
        validate(data);
        Sid sid = Sid.generate(Sid.Type.EXTENSION_CONFIGURATION);
        String extension = data.getFirst("ExtensionName");
        boolean enabled = Boolean.parseBoolean(data.getFirst("Enabled"));
        Object configurationData = data.getFirst("ConfigurationData");
        ExtensionConfiguration.configurationType configurationType = null;
        if (responseType.equals(APPLICATION_JSON_TYPE)) {
            configurationType = ExtensionConfiguration.configurationType.JSON;
        } else if (responseType.equals(APPLICATION_XML_TYPE)) {
            configurationType = ExtensionConfiguration.configurationType.XML;
        }
        DateTime dateCreated = DateTime.now();
        DateTime dateUpdated = DateTime.now();
        ExtensionConfiguration extensionConfiguration = new ExtensionConfiguration(sid, extension, enabled, configurationData, configurationType, dateCreated, dateUpdated);

        return extensionConfiguration;
    }

    protected Response postConfiguration(final MultivaluedMap<String, String> data, final MediaType responseType) {
        if (!isSuperAdmin()) {
            throw new InsufficientPermission();
        }

        Sid accountSid = null;

        String accountSidQuery = data.getFirst("AccountSid");
        if(accountSidQuery != null && !accountSidQuery.isEmpty()){
            accountSid = new Sid(accountSidQuery);
        }
        //if extension doesnt exist, add new extension
        String extensionName = data.getFirst("ExtensionName");
        ExtensionConfiguration extensionConfiguration = extensionsConfigurationDao.getConfigurationByName(extensionName);

        if(extensionConfiguration==null){
            try {
                extensionConfiguration = createFrom(data, responseType);
            } catch (final NullPointerException exception) {
                return status(BAD_REQUEST).entity(exception.getMessage()).build();
            }
            try {
                extensionsConfigurationDao.addConfiguration(extensionConfiguration);
            } catch (ConfigurationException exception) {
                return status(NOT_ACCEPTABLE).entity(exception.getMessage()).build();
            }
        }
        if (accountSid!=null) {
            try {
                Object configurationData = data.getFirst("ConfigurationData");
                // if accountSid exists, then this configuration is account specific, if it doesnt then its global config
                extensionConfiguration.setConfigurationData(configurationData, extensionConfiguration.getConfigurationType());
                extensionsConfigurationDao.addAccountExtensionConfiguration(extensionConfiguration, accountSid);
            } catch (ConfigurationException exception) {
                return status(NOT_ACCEPTABLE).entity(exception.getMessage()).build();
            }
        }

        if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(extensionConfiguration), APPLICATION_JSON).build();
        } else if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(extensionConfiguration);
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else {
            return null;
        }
    }

    protected Response updateConfiguration(String extensionSid, MultivaluedMap<String, String> data, MediaType responseType) {
        if (!isSuperAdmin()) {
            throw new InsufficientPermission();
        }

        if (!Sid.pattern.matcher(extensionSid).matches()) {
            return status(BAD_REQUEST).build();
        }

        ExtensionConfiguration extensionConfiguration = extensionsConfigurationDao.getConfigurationBySid(new Sid(extensionSid));
        if (extensionConfiguration == null) {
            return status(NOT_FOUND).build();
        }

        ExtensionConfiguration updatedExtensionConfiguration = null;
        Sid accountSid = null;
        String accountSidQuery = data.getFirst("AccountSid");
        if(accountSidQuery != null && !accountSidQuery.isEmpty()){
            accountSid = new Sid(accountSidQuery);
        }

        try {
            updatedExtensionConfiguration = prepareUpdatedConfiguration(extensionConfiguration, data, responseType);
        } catch (final NullPointerException exception) {
            return status(BAD_REQUEST).entity(exception.getMessage()).build();
        }

        try {
            if (accountSid==null) {
                extensionsConfigurationDao.updateConfiguration(updatedExtensionConfiguration);
            } else {
                extensionsConfigurationDao.updateAccountExtensionConfiguration(updatedExtensionConfiguration, accountSid);
            }
        } catch (ConfigurationException exception) {
            return status(NOT_ACCEPTABLE).entity(exception.getMessage()).build();
        }

        if (APPLICATION_JSON_TYPE == responseType) {
            return ok(gson.toJson(updatedExtensionConfiguration), APPLICATION_JSON).build();
        } else if (APPLICATION_XML_TYPE == responseType) {
            final RestCommResponse response = new RestCommResponse(updatedExtensionConfiguration);
            return ok(xstream.toXML(response), APPLICATION_XML).build();
        } else {
            return null;
        }
    }

    private ExtensionConfiguration prepareUpdatedConfiguration(ExtensionConfiguration existingExtensionConfiguration, MultivaluedMap<String, String> data, MediaType responseType) {
        validate(data);
        Sid existingExtensionSid = existingExtensionConfiguration.getSid();
        String existingExtensionName = existingExtensionConfiguration.getExtensionName();
        boolean enabled = existingExtensionConfiguration.isEnabled();
        if (data.getFirst("Enabled") != null) {
            enabled = Boolean.parseBoolean(data.getFirst("Enabled"));
        }
        Object configurationData = data.getFirst("ConfigurationData");
        DateTime dateCreated = existingExtensionConfiguration.getDateCreated();
        ExtensionConfiguration.configurationType configurationType = null;
        if (responseType.equals(APPLICATION_JSON_TYPE)) {
            configurationType = ExtensionConfiguration.configurationType.JSON;
        } else if (responseType.equals(APPLICATION_XML_TYPE)) {
            configurationType = ExtensionConfiguration.configurationType.XML;
        }

        return new ExtensionConfiguration(existingExtensionSid, existingExtensionName, enabled, configurationData, configurationType, dateCreated ,DateTime.now());
    }
}
