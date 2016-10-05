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

import com.google.gson.Gson;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;
import org.junit.Assert;
import org.restcomm.connect.rvd.TestUtils;
import org.restcomm.connect.rvd.model.ModelMarshaler;
import org.restcomm.connect.rvd.model.UserProfile;

import java.io.File;
import java.io.IOException;

/**
 * @author Orestis Tsakiridis
 */
public class FsProfileDaoTest {

    @Test
    public void userProfileFileIsSavedAndValid() throws IOException {
        File workspaceDir = TestUtils.createTempWorkspace();
        try {
            ModelMarshaler marshaler = new ModelMarshaler();
            WorkspaceStorage storage = new WorkspaceStorage(workspaceDir.getPath(), marshaler);

            ProfileDao profileDao = new FsProfileDao(storage);

            UserProfile profile = new UserProfile();
            profile.setUsername("orestis.tsakiridis@telestax.com");
            profile.setToken("a very secret token");

            profileDao.saveUserProfile("orestis.tsakiridis@telestax.com", profile);
            File profileFile = new File(workspaceDir.getPath() + "/@users/orestis.tsakiridis@telestax.com");
            Assert.assertTrue("User profile file was not created", profileFile.exists());

            String data = FileUtils.fileRead(profileFile, "UTF-8");
            Gson gson = new Gson();
            UserProfile profile2 = gson.fromJson(data, UserProfile.class);
            Assert.assertEquals("Username was not stored properly in the profile", "orestis.tsakiridis@telestax.com", profile2.getUsername());
            Assert.assertEquals("Password/token was not stored properly in the profile", "a very secret token", profile2.getToken());
        } finally {
            TestUtils.removeTempWorkspace(workspaceDir.getPath());
        }
    }

    @Test
    public void userProfileIsLoadedCorrectly() throws IOException {
        File workspaceDir = TestUtils.createTempWorkspace();
        File usersDir = TestUtils.createUsersDirectory(workspaceDir.getPath());
        try {
            ModelMarshaler marshaler = new ModelMarshaler();
            WorkspaceStorage storage = new WorkspaceStorage(workspaceDir.getPath(), marshaler);

            File profileFile = new File(workspaceDir.getPath() + "/@users/orestis.tsakiridis@telestax.com");
            profileFile.createNewFile();
            String userProfileData = "{\"username\":\"orestis.tsakiridis@telestax.com\",\"token\":\"a very secret token\"}";
            FileUtils.fileWrite(profileFile, "UTF-8", userProfileData);

            ProfileDao profileDao = new FsProfileDao(storage);
            UserProfile profile = profileDao.loadUserProfile("orestis.tsakiridis@telestax.com");

            Assert.assertEquals("Username was not proper loaded from user profile", "orestis.tsakiridis@telestax.com", profile.getUsername() );
            Assert.assertEquals("Token/password wnas not properly loaded from user profile", "a very secret token", profile.getToken() );
        } finally {
            TestUtils.removeTempWorkspace(workspaceDir.getPath());
        }
    }

    @Test
    public void nullIsReturnedIfProfileDoesNotExist() {
        File workspaceDir = TestUtils.createTempWorkspace();
        File usersDir = TestUtils.createUsersDirectory(workspaceDir.getPath());
        try {
            ModelMarshaler marshaler = new ModelMarshaler();
            WorkspaceStorage storage = new WorkspaceStorage(workspaceDir.getPath(), marshaler);

            ProfileDao profileDao = new FsProfileDao(storage);
            UserProfile profile = profileDao.loadUserProfile("non-existing-profile");

            Assert.assertNull("Null should be returned if the user profile does not exists.", profile);
        } finally {
            TestUtils.removeTempWorkspace(workspaceDir.getPath());
        }
    }
}
