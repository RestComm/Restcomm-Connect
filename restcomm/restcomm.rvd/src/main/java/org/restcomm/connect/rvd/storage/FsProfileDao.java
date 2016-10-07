/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2016, Telestax Inc and individual contributors
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

package org.restcomm.connect.rvd.storage;

import org.restcomm.connect.rvd.model.UserProfile;
import org.restcomm.connect.rvd.storage.exceptions.StorageEntityNotFound;
import org.restcomm.connect.rvd.storage.exceptions.StorageException;

/**
 * A data access object that reads user profile information from the FS workspace.
 * It relies on WorkspaceStorage utility
 *
 * @author Orestis Tsakiridis
 */
public class FsProfileDao implements ProfileDao {

    private WorkspaceStorage workspaceStorage;

    public FsProfileDao(WorkspaceStorage workspaceStorage) {
        this.workspaceStorage = workspaceStorage;
    }

    @Override
    public void saveUserProfile(String username, UserProfile userProfile) {
        try {
            workspaceStorage.storeEntity(userProfile, UserProfile.class, username, "@users");
        } catch (StorageException e) {
            throw new RuntimeException("Error storing profile for user '" + username + "'", e);
        }
    }

    /**
     * Loads and returns the profile for the user.
     * @param username
     * @return user profile or null if it does not exist
     */
    @Override
    public UserProfile loadUserProfile(String username) {
        try {
            UserProfile profile = workspaceStorage.loadEntity(username, "@users", UserProfile.class);
            return profile;
        } catch (StorageEntityNotFound e) {
            return null;
        } catch (StorageException e) {
            throw new RuntimeException("Error loading profile for user '" + username +"'",e);
        }
    }
}
