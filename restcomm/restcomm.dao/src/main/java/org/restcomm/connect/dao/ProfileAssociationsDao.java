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
     * @param targetSid
     * @return ProfileAssociation as per provided target sid
     */
    ProfileAssociation getProfileAssociationByTargetSid(String targetSid);

    /**
     * @param profileSid
     * @return List of all ProfileAssociation with a give profile sid
     */
    List<ProfileAssociation> getProfileAssociationsByProfileSid(String profileSid);

    /**
     * @param profileAssociation
     * @return
     */
    int addProfileAssociation(ProfileAssociation profileAssociation);

    /**
     * update ProfileAssociation Of TargetSid to new Profile Sid.
     * @param profileAssociation
     */
    void updateProfileAssociationOfTargetSid(ProfileAssociation profileAssociation);

    /**
     * update Associated Profile Of All Such ProfileSid
     * @param oldProfileSid
     * @param newProfileSid
     */
    void updateAssociatedProfileOfAllSuchProfileSid(String oldProfileSid, String newProfileSid);

    /**
     * will delete all associations of given profile sid
     * @param profileSid
     */
    void deleteProfileAssociationByProfileSid(String profileSid);

    /**
     * will delete all associations of given target sid
     * @param targetSid
     * @return number of associations removed
     *
     */
    int deleteProfileAssociationByTargetSid(String targetSid);

    /**
     * will delete all associations of given target sid
     * @param targetSid
     * @return number of associations removed
     *
     */
    int deleteProfileAssociationByTargetSid(String targetSid, String profileSid);
}
