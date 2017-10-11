/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.restcomm.connect.extension.api;

import javax.servlet.ServletContext;
import javax.servlet.sip.SipServletRequest;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public interface RestcommExtensionGeneric {
    /**
     * Use this method to initialize the Extension
     */
    void init(ServletContext context);
    /**
     * Check if extensions is enabled
     * @return
     */
    boolean isEnabled();
    /**
     * Method that will be executed BEFORE the process of an Incoming session
     * Implement this method so you will be able to check the Incoming session
     * and either block/allow or modify the session before Restcomm process it
     * @return ExtensionResponse see ExtensionResponse
     */
    ExtensionResponse preInboundAction(SipServletRequest request);
    /**
     * Method that will be executed AFTER the process of an Incoming session
     * Implement this method so you will be able to check the Incoming session
     * and either block or allow or modify the session after Restcomm process it
     * @return ExtensionResponse see ExtensionResponse
     */
    ExtensionResponse postInboundAction(SipServletRequest request);
    /**
     * Method that will be executed before the process of an Outbound session
     * Implement this method so you will be able to check the Outbound session
     * and either block/allow it or modify the session before Restcomm process it
     * @return ExtensionResponse see ExtensionResponse
     */
    ExtensionResponse preOutboundAction(IExtensionRequest extensionRequest);
    /**
     * Method that will be executed AFTER the process of an Outbound session
     * Implement this method so you will be able to check the Outgoing session
     * and either block or allow or modify the session after Restcomm process it
     * @return ExtensionResponse see ExtensionResponse
     */
    ExtensionResponse postOutboundAction(IExtensionRequest extensionRequest);

    /**
     * Method that will be executed before the process of an API action, such as DID purchase (but after security checks)
     * @return ExtensionResponse see ExtensionResponse
     */
    ExtensionResponse preApiAction(ApiRequest apiRequest);

    /**
     * Method that will be executed after the process of an API action, such as DID purchase
     * @return
     */
    ExtensionResponse postApiAction(ApiRequest apiRequest);

    /**
     * Extension name getter
     * @return String name of Extension
     */
    String getName();

    /**
     * Extension version getter
     * @return String version of Extension
     */
    String getVersion();
}
