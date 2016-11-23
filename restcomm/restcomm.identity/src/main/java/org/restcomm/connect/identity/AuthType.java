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

package org.restcomm.connect.identity;

/**
 * Holds symbolic values for different authentication types. It also contains symbolic
 * for filters of such types.
 *
 * AuthToken - implies an account's AuthToken was used for authentication
 * Password - implies an account's Password was used for authentication
 * ANY - filter value that usually means that ANY authentication type is accepted
 * Bearer - will be added in the future TODO
 *
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */

public enum AuthType {
    AuthToken,
    Password,
    ANY
    // Bearer   // TODO when a Bearer token is present (probably part of SSO)
}
