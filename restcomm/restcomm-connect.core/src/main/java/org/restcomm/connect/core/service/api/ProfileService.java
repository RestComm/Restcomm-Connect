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

package org.restcomm.connect.core.service.api;

import com.sun.jersey.core.header.LinkHeader;
import javax.ws.rs.core.UriInfo;
import org.restcomm.connect.dao.entities.Profile;

public interface ProfileService {
    /**
     * @param accountSid
     * @return
     */
    Profile retrieveEffectiveProfileByAccountSid(String accountSid);

    /**
     * @param targetSid
     * @param info
     * @param resource
     * @return
     */
    LinkHeader composeProfileLink(String targetSid, UriInfo info, Class resource);

    /**
     * @param organizationSid
     * @return
     */
    Profile retrieveEffectiveProfileByOrganizationSid(String organizationSid);

    /**
     * @param targetSid
     * @return will return associated profile of provided target (account or
     *         organization)
     */
    Profile retrieveExplicitlyAssociatedProfile(String targetSid);
}
