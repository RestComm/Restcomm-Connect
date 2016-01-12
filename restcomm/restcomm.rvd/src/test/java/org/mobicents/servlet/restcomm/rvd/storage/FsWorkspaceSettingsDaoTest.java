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

package org.mobicents.servlet.restcomm.rvd.storage;

import com.google.gson.Gson;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.mobicents.servlet.restcomm.rvd.TestUtils;
import org.mobicents.servlet.restcomm.rvd.model.HttpScheme;
import org.mobicents.servlet.restcomm.rvd.model.ModelMarshaler;
import org.mobicents.servlet.restcomm.rvd.model.WorkspaceSettings;

import java.io.File;
import java.io.IOException;

/**
 * @author Orestis Tsakiridis
 */
public class FsWorkspaceSettingsDaoTest {
    @Test
    public void completeSettingsAreLoadedOk() throws IOException {
        File workspaceDir = TestUtils.createTempWorkspace();
        try {
            ModelMarshaler marshaler = new ModelMarshaler();
            WorkspaceStorage storage = new WorkspaceStorage(workspaceDir.getPath(), marshaler);
            File settingsFile = new File(workspaceDir.getPath() + "/.settings");
            settingsFile.createNewFile();
            String fileData = "{\"apiServerHost\":\"localhost\",\"apiServerRestPort\":8080,\"apiServerScheme\":\"http\"}";
            FileUtils.fileWrite(settingsFile, "UTF-8", fileData);

            WorkspaceSettingsDao settingsDao = new FsWorkspaceSettingsDao(storage);
            WorkspaceSettings settings = settingsDao.loadWorkspaceSettings();

            Assert.assertEquals("localhost",settings.getApiServerHost());
            Assert.assertEquals(new Integer(8080),settings.getApiServerRestPort());
            Assert.assertEquals(HttpScheme.http, settings.getApiServerScheme());
        } finally {
            TestUtils.removeTempWorkspace(workspaceDir.getPath());
        }
    }

    @Test
    public void badSchemeReturnsNull() throws IOException {
        File workspaceDir = TestUtils.createTempWorkspace();
        try {
            ModelMarshaler marshaler = new ModelMarshaler();
            WorkspaceStorage storage = new WorkspaceStorage(workspaceDir.getPath(), marshaler);
            File settingsFile = new File(workspaceDir.getPath() + "/.settings");
            settingsFile.createNewFile();
            String fileData = "{\"apiServerScheme\":\"invalid-scheme\"}";
            FileUtils.fileWrite(settingsFile, "UTF-8", fileData);

            WorkspaceSettingsDao settingsDao = new FsWorkspaceSettingsDao(storage);
            WorkspaceSettings settings = settingsDao.loadWorkspaceSettings();

            Assert.assertEquals(null,settings.getApiServerHost());
            Assert.assertEquals(null,settings.getApiServerRestPort());
            Assert.assertEquals(null, settings.getApiServerScheme());
        } finally {
            TestUtils.removeTempWorkspace(workspaceDir.getPath());
        }
    }

    @Test
    public void workspaceSettingsAreSaved() throws IOException {
        File workspaceDir = TestUtils.createTempWorkspace();
        try {
            ModelMarshaler marshaler = new ModelMarshaler();
            WorkspaceStorage storage = new WorkspaceStorage(workspaceDir.getPath(), marshaler);

            WorkspaceSettingsDao settingsDao = new FsWorkspaceSettingsDao(storage);

            WorkspaceSettings settings = new WorkspaceSettings();
            settings.setApiServerHost("127.0.0.1");
            settings.setApiServerRestPort(9090);
            settings.setApiServerScheme(HttpScheme.http);

            settingsDao.saveWorkspaceSettings(settings);
            File settingsFile = new File(workspaceDir.getPath() + "/.settings");
            Assert.assertTrue("Workspace settings file was not created", settingsFile.exists());

            String data = FileUtils.fileRead(settingsFile, "UTF-8");
            Gson gson = new Gson();
            WorkspaceSettings settings2 = gson.fromJson(data, WorkspaceSettings.class);
            Assert.assertEquals("Host was not stored properly in the workspace settings", "127.0.0.1", settings2.getApiServerHost());
            Assert.assertEquals("Port was not stored properly in the workspace settings", new Integer(9090), settings2.getApiServerRestPort());
            Assert.assertEquals(HttpScheme.http,settings2.getApiServerScheme());
        } finally {
            TestUtils.removeTempWorkspace(workspaceDir.getPath());
        }
    }

    @Test
    public void missingSettingsReturnsNull() {
        File workspaceDir = TestUtils.createTempWorkspace();
        try {
            ModelMarshaler marshaler = new ModelMarshaler();
            WorkspaceStorage storage = new WorkspaceStorage(workspaceDir.getPath(), marshaler);
            WorkspaceSettingsDao settingsDao = new FsWorkspaceSettingsDao(storage);

            WorkspaceSettings settings = settingsDao.loadWorkspaceSettings();
            Assert.assertNull("Workspace settings object should be null", settings);
        } finally {
            TestUtils.removeTempWorkspace(workspaceDir.getPath());
        }
    }

}
