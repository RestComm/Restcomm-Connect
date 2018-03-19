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
package org.restcomm.connect.extension.mock;

import javax.servlet.ServletContext;
import org.apache.log4j.Logger;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.extension.api.ApiRequest;
import org.restcomm.connect.extension.api.ExtensionResponse;
import org.restcomm.connect.extension.api.ExtensionType;
import org.restcomm.connect.extension.api.IExtensionRequest;
import org.restcomm.connect.extension.api.RestcommExtension;
import org.restcomm.connect.extension.api.RestcommExtensionGeneric;

/**
 * Extension that rejects any execution point. Provided for testing purposes
 * @author
 */
@RestcommExtension(author = "restcomm", version = "1.0.0.Alpha", type = {ExtensionType.CallManager, ExtensionType.SmsService, ExtensionType.UssdCallManager, ExtensionType.FeatureAccessControl, ExtensionType.RestApi})
public class BlockingExtensionMock implements RestcommExtensionGeneric {

    private static Logger logger = Logger.getLogger(BlockingExtensionMock.class);

    private String extensionName = "blocking";
    private final String localConfigPath = "/blocking_default_configuration.json";
    private MockConfiguration config;

    @Override
    public void init(ServletContext context) {
        try {
            DaoManager daoManager = (DaoManager) context.getAttribute(DaoManager.class.getName());
            config = new MockConfiguration(daoManager, extensionName, localConfigPath);
            config.init(daoManager, extensionName, localConfigPath);
        } catch (Exception configurationException) {
            logger.error("Exception during init", configurationException);
        }
    }

    @Override
    public ExtensionResponse preInboundAction(IExtensionRequest extensionRequest) {
        ExtensionResponse response = new ExtensionResponse();
        response.setAllowed(false);
        return response;
    }

    @Override
    public ExtensionResponse postInboundAction(IExtensionRequest extensionRequest) {
        ExtensionResponse response = new ExtensionResponse();
        response.setAllowed(false);
        return response;
    }

    @Override
    public ExtensionResponse preOutboundAction(IExtensionRequest extensionRequest) {
        ExtensionResponse response = new ExtensionResponse();
        response.setAllowed(false);
        return response;
    }

    @Override
    public ExtensionResponse postOutboundAction(IExtensionRequest extensionRequest) {
        ExtensionResponse response = new ExtensionResponse();
        response.setAllowed(false);
        return response;
    }

    @Override
    public ExtensionResponse preApiAction(ApiRequest apiRequest) {
        ExtensionResponse response = new ExtensionResponse();
        response.setAllowed(false);
        return response;
    }

    @Override
    public ExtensionResponse postApiAction(ApiRequest apiRequest) {
        ExtensionResponse response = new ExtensionResponse();
        response.setAllowed(false);
        return response;
    }

    @Override
    public String getName() {
        return this.extensionName;
    }

    @Override
    public String getVersion() {
        return ((RestcommExtension) BlockingExtensionMock.class.getAnnotation(RestcommExtension.class)).version();
    }

    @Override
    public boolean isEnabled() {
        return config.isEnabled();
    }

}
