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
package org.restcomm.connect.dao;

import java.util.List;

import org.restcomm.connect.dao.entities.ProfileAssociation;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
public interface ProfileAssociationsDao {

    /**
     * @param sid
     * @return a single profile as per provided profile sid
     */
    ProfileAssociation getProfileAssociation(String sid);

    /**
     * @return List of all profile associations in the system
     */
    List<ProfileAssociation> getAllProfileAssociations();

    /**
     * @param profileAssociation
     * @return
     */
    int addProfileAssociation(ProfileAssociation profileAssociation);

    /**
     * @param profileAssociation
     */
    void updateProfileAssociation(ProfileAssociation profileAssociation);

    /**
     * @param profileAssociation
     */
    void deleteProfileAssociationByProfileSid(String sid);
}
