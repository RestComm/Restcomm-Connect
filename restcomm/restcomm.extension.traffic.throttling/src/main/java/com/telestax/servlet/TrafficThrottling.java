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

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.telephony.CreateCall;
import org.mobicents.servlet.sip.core.SipManager;
import org.mobicents.servlet.sip.message.MobicentsSipApplicationSessionFacade;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 *
 */
@RestcommExtension(author = "GeorgeVagenas", version = "7.3.1", type = {ExtensionType.CallManager, ExtensionType.SmsSession, ExtensionType.UssdCallManager})
public class TrafficThrottling implements RestcommExtensionGeneric {

    private static final Logger logger = Logger.getLogger(TrafficThrottling.class);

    @Override
    public void init() {
    }

    @Override
    public ExtensionResponse preInboundAction(SipServletRequest request) {
        SipManager sipManager = ((MobicentsSipApplicationSessionFacade) request.getApplicationSession()).getSipContext().getSipManager();
        int activeCalls = sipManager.getActiveSipApplicationSessions();
        logger.info("Active calls: "+activeCalls);
        ExtensionResponse response = new ExtensionResponse();
        response.setAllowed(false);
        return response;
    }

    @Override
    public ExtensionResponse postInboundAction(SipServletRequest request) {
        return null;
    }

    @Override
    public ExtensionResponse preOutboundAction(CreateCall createCallRequest) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ExtensionResponse postOutboundAction(CreateCall createCallRequest) {
        // TODO Auto-generated method stub
        return null;
    }
}
