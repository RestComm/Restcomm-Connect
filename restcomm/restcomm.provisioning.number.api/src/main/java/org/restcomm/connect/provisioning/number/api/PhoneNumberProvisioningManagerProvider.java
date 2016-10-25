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

import javax.servlet.ServletContext;
import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipURI;
import java.util.List;

/**
 * A single place to create the number provisioning manager
 *
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class PhoneNumberProvisioningManagerProvider {
    Configuration configuration;
    ServletContext context;

    public PhoneNumberProvisioningManagerProvider(Configuration configuration, ServletContext context) {
        this.configuration = configuration;
        this.context = context;
    }

    private List<SipURI> getOutboundInterfaces() {
        final List<SipURI> uris = (List<SipURI>) context.getAttribute(SipServlet.OUTBOUND_INTERFACES);
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

    /**
     * Tries to retrieve the manager from the Servlet context. If it's not there it creates is, stores it
     * in the context and also returns it.
     *
     * @param context
     * @return
     */
    public PhoneNumberProvisioningManager get() {
        PhoneNumberProvisioningManager manager = (PhoneNumberProvisioningManager) context.getAttribute("PhoneNumberProvisioningManager");
        if (manager != null) // ok, it's already in the context. Return it
            return manager;
        manager = create();
        // put it into the context for next time that is requested
        context.setAttribute("PhoneNumberProvisioningManager", manager);
        return manager;
    }

}
