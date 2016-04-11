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

package org.mobicents.servlet.restcomm.rvd.upgrade;

import com.google.gson.JsonElement;
import org.junit.Assert;
import org.junit.Test;
import org.mobicents.servlet.restcomm.rvd.BuildService;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
import org.mobicents.servlet.restcomm.rvd.model.ModelMarshaler;
import org.mobicents.servlet.restcomm.rvd.model.client.ProjectState;
import org.mobicents.servlet.restcomm.rvd.storage.FsProjectStorage;
import org.mobicents.servlet.restcomm.rvd.storage.WorkspaceStorage;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;
import org.mobicents.servlet.restcomm.rvd.upgrade.exceptions.UpgradeException;

import java.io.File;
import java.net.URL;

/**
 * @author Orestis Tsakiridis
 */
public class ProjectUpgraderTest {

     // note that this test fails the second time it is run through intelliJ. Make sure the target/test-classes directory
     // is removed before running it again !
    @Test
    public void testVariousProjectUpgrades() throws StorageException, UpgradeException {
        ModelMarshaler marshaler = new ModelMarshaler();
        String workspaceDirName = getClass().getResource("./workspace").getFile();
        WorkspaceStorage workspaceStorage = new WorkspaceStorage(workspaceDirName, marshaler);
        UpgradeService upgradeService = new UpgradeService(workspaceStorage);
        BuildService buildService = new BuildService(workspaceStorage);

        // check the version changes
        JsonElement rootElement = upgradeService.upgradeProject("project3");
        String upgradedVersion = ProjectUpgrader10to11.getVersion(rootElement);
        Assert.assertEquals("Actual upgraded project version is wrong", RvdConfiguration.getRvdProjectVersion(), upgradedVersion);
        // make sure the project builds also
        ProjectState project = FsProjectStorage.loadProject("collectMenuProject", workspaceStorage);
        buildService.buildProject("project3", project);

        // check the collect/menu digits propert has been converted integer -> string
        rootElement = upgradeService.upgradeProject("collectMenuProject");
        Assert.assertNotNull(rootElement);
        project = FsProjectStorage.loadProject("collectMenuProject", workspaceStorage);
        buildService.buildProject("collectMenuProject",project);
    }
}
