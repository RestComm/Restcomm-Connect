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

import java.sql.SQLException;
import java.util.List;

import org.restcomm.connect.dao.entities.Profile;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
public interface ProfilesDao {

    /**
     * @param sid
     * @return a single profile as per provided profile sid
     * @throws SQLException
     */
    Profile getProfile(String sid) throws SQLException;

    /**
     * @return List of all profiles in the system
     * @throws SQLException
     */
    List<Profile> getAllProfiles() throws SQLException;

    /**
     * @param profile
     * @return
     */
    int addProfile(Profile profile);

    /**
     * @param profile
     */
    void updateProfile(Profile profile);

    /**
     * @param profile
     */
    void deleteProfile(String sid);
}
