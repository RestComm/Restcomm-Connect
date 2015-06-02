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
package com.telestax.servlet;

import javax.servlet.sip.SipServletRequest;

import org.mobicents.servlet.restcomm.telephony.CreateCall;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
public interface RestcommExtensionGeneric {
    /**
     * Use this method to initialize the Extension
     */
    void init();
    /**
     * Method that will be executed BEFORE the process of an Incoming session
     * Implement this method so you will be able to check the Incoming session
     * and either block/allow or modify the session before Restcomm process it
     * @return ExtensionResponse see com.telestax.servlet.ExtensionResponse
     */
    ExtensionResponse preInboundAction(SipServletRequest request);
    /**
     * Method that will be executed AFTER the process of an Incoming session
     * Implement this method so you will be able to check the Incoming session
     * and either block or allow or modify the session after Restcomm process it
     * @return ExtensionResponse see com.telestax.servlet.ExtensionResponse
     */
    ExtensionResponse postInboundAction(SipServletRequest request);
    /**
     * Method that will be executed before the process of an Outbound session
     * Implement this method so you will be able to check the Outbound session
     * and either block/allow it or modify the session before Restcomm process it
     * @return ExtensionResponse see com.telestax.servlet.ExtensionResponse
     */
    ExtensionResponse preOutboundAction(CreateCall createCallRequest);
    /**
     * Method that will be executed AFTER the process of an Outbound session
     * Implement this method so you will be able to check the Outgoing session
     * and either block or allow or modify the session after Restcomm process it
     * @return ExtensionResponse see com.telestax.servlet.ExtensionResponse
     */
    ExtensionResponse postOutboundAction(CreateCall createCallRequest);
}
