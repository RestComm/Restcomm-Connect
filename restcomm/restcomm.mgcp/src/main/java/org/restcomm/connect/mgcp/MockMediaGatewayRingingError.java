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
package org.restcomm.connect.mgcp;

import akka.actor.ActorRef;
import jain.protocol.ip.mgcp.JainMgcpResponseEvent;
import jain.protocol.ip.mgcp.message.NotificationRequest;
import jain.protocol.ip.mgcp.message.NotificationRequestResponse;
import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

public final class MockMediaGatewayRingingError extends MockMediaGateway {

    protected void notificationResponse(final Object message, final ActorRef sender) {
        final ActorRef self = self();
        final NotificationRequest rqnt = (NotificationRequest) message;
        EventName[] events = rqnt.getSignalRequests();
        // Thread sleep for the maximum recording length to simulate recording from RMS side
        int sleepTime = 0;
        boolean failResponse = false;
        if (events != null && events.length > 0 && events[0].getEventIdentifier() != null) {
            if (events[0].getEventIdentifier().getName().equalsIgnoreCase("pr")) {
                // Check for the Recording Length Timer parameter if the RQNT is about PlayRecord request
                String[] paramsArray = ((EventName) events[0]).getEventIdentifier().getParms().split(" ");
                for (String param : paramsArray) {
                    if (param.startsWith("rlt")) {
                        sleepTime = Integer.parseInt(param.replace("rlt=", ""));
                    }
                }
                if (sleepTime == 3600000) {
                    // If maxLength is not set, rlt will be rlt=3600000
                    // In that case don't sleep at all
                    sleepTime = 0;
                }
            } else if (events[0].getEventIdentifier().getName().equalsIgnoreCase("pa")) {
                // If this is a Play Audio request, check that the parameter string ends with WAV
                String[] paramsArray = ((EventName) events[0]).getEventIdentifier().getParms().split(" ");
                for (String param : paramsArray) {
                    if (param.startsWith("an")) {
                        String annoUrl = param.replace("an=", "");
                        if (!annoUrl.toLowerCase().endsWith("wav")) {
                            failResponse = true;
                        }
                        // If this is a ringing.wav request, then failed response
                        if (annoUrl.toLowerCase().contains("ringing.wav")) {
                            failResponse = true;
                        }
                    }
                }
            }
        }
        System.out.println(rqnt.toString());
        ReturnCode code = null;
        if (failResponse) {
            code = ReturnCode.Transient_Error;
        } else {
            code = ReturnCode.Transaction_Executed_Normally;
        }
        final JainMgcpResponseEvent response = new NotificationRequestResponse(self, code);
        final int transaction = rqnt.getTransactionHandle();
        response.setTransactionHandle(transaction);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
        }
        System.out.println(response.toString());
        sender.tell(response, self);
    }
}
