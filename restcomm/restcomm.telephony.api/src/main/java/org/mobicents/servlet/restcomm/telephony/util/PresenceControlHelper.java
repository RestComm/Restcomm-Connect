/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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
package org.mobicents.servlet.restcomm.telephony.util;

import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.dao.ClientsDao;
import org.mobicents.servlet.restcomm.dao.DaoManager;
import org.mobicents.servlet.restcomm.entities.Account;
import org.mobicents.servlet.restcomm.entities.Client;
import org.mobicents.servlet.restcomm.entities.Sid;

/**
 * Manages {@link Client} presence information and keep them
 * up to date as new SIP messages are received.
 * 
 * @author guilherme.jansen@telestax.com
 */
public class PresenceControlHelper {
	
	/**
	 * Update client presence information, setting the date of last usage 
	 * to the current time.
	 * 
	 * @param sipMessage
	 * @param clientInfo
	 * @param storage
	 */
	public static void updateClientPresence(final String login, ClientsDao clients){
		final Client client = new Client(null, null, null, null, null, null, login, null, null, 
				null, null, null, null, null, null, DateTime.now());
		clients.updateClientPresence(client);
	}

}
