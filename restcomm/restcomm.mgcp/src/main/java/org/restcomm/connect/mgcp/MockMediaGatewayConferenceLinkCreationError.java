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
import akka.event.Logging;
import akka.event.LoggingAdapter;
import jain.protocol.ip.mgcp.message.CreateConnectionResponse;
import jain.protocol.ip.mgcp.message.parms.ConnectionIdentifier;
import jain.protocol.ip.mgcp.message.parms.ReturnCode;

public final class MockMediaGatewayConferenceLinkCreationError extends MockMediaGateway {
    private final LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    @Override
    protected void createConnection (final Object message, final ActorRef sender) {
        final jain.protocol.ip.mgcp.message.CreateConnection crcx = (jain.protocol.ip.mgcp.message.CreateConnection) message;
        if(logger.isInfoEnabled())
            logger.info("createConnection: " +crcx.toString());
        // check if its a conference and call link request
        if(crcx.getSecondEndpointIdentifier() != null && crcx.getSecondEndpointIdentifier().getLocalEndpointName() != null && crcx.getSecondEndpointIdentifier().getLocalEndpointName().contains("cnf")){
            // if yes then fail this connection request
            if(logger.isInfoEnabled())
                logger.info("got conference and call link request, will fail it! with error code Endpoint_Unknown");
            StringBuilder buffer = new StringBuilder();
            buffer.append(connectionIdPool.get());
            ConnectionIdentifier connId = new ConnectionIdentifier(buffer.toString());
            sender.tell(new CreateConnectionResponse(self(), ReturnCode.Endpoint_Unknown, connId), self());
        }else {
            // if not then let daddy proceed with existing mocked mechanism.
            super.createConnection(message, sender);
        }
    }
}
