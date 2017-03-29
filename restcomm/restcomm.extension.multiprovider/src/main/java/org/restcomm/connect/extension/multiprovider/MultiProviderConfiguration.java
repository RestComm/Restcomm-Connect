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
package org.restcomm.connect.extension.multiprovider;

import java.io.IOException;

import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.extension.api.ConfigurationException;

import com.google.gson.JsonObject;

public class MultiProviderConfiguration extends DefaultExtensionConfiguration{

    public MultiProviderConfiguration(){
        super();
    }
    public MultiProviderConfiguration(DaoManager daoManager, String extensionName, String localConfigPath)
            throws ConfigurationException {
        super(daoManager, extensionName, localConfigPath);
        // TODO Auto-generated constructor stub
    }

    @Override
    public JsonObject loadDefaultConfiguration(String localConfigFilePath) throws IOException {
        // TODO Auto-generated method stub
        return super.loadDefaultConfiguration(localConfigFilePath);
    }

}
