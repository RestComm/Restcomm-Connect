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

package org.restcomm.connect.provisioning.number.api;

import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.loader.ObjectFactory;
import org.restcomm.connect.commons.loader.ObjectInstantiationException;

import javax.servlet.sip.SipURI;
import java.util.List;

/**
 * A single place to create the number provisioning manager
 *
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class PhoneNumberProvisioningManagerFactory {
    Configuration configuration;
    List<SipURI> uris;

    public PhoneNumberProvisioningManagerFactory(Configuration configuration, List<SipURI> uris) {
        this.configuration = configuration;
        this.uris = uris;
    }

    @SuppressWarnings("unchecked")
    private List<SipURI> getOutboundInterfaces() {
        return uris;
    }

    public PhoneNumberProvisioningManager create() {
        final String phoneNumberProvisioningManagerClass = configuration.getString("phone-number-provisioning[@class]");
        Configuration phoneNumberProvisioningConfiguration = configuration.subset("phone-number-provisioning");
        Configuration telestaxProxyConfiguration = configuration.subset("runtime-settings").subset("telestax-proxy");
        PhoneNumberProvisioningManager phoneNumberProvisioningManager;
        try {
            phoneNumberProvisioningManager = (PhoneNumberProvisioningManager) new ObjectFactory(getClass().getClassLoader())
                    .getObjectInstance(phoneNumberProvisioningManagerClass);
            ContainerConfiguration containerConfiguration = new ContainerConfiguration(getOutboundInterfaces());
            phoneNumberProvisioningManager.init(phoneNumberProvisioningConfiguration, telestaxProxyConfiguration, containerConfiguration);
        } catch (ObjectInstantiationException e) {
            throw new RuntimeException(e);
        }
        return phoneNumberProvisioningManager;
    }

}
