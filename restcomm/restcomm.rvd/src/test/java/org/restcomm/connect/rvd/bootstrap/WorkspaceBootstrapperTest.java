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

package org.restcomm.connect.rvd.bootstrap;

import org.junit.Test;
import org.junit.Assert;
import org.restcomm.connect.rvd.TestUtils;
import org.restcomm.connect.rvd.RvdConfiguration;

import java.io.File;
import java.util.Random;

/**
 * @author Orestis Tsakiridis
 */
public class WorkspaceBootstrapperTest {
    private static String tempDirLocation = System.getProperty("java.io.tmpdir");

    @Test(expected=RuntimeException.class)
    public void workspaceBootstrapFailsIfRootDirMissing() {
        Random ran = new Random();
        String workspaceLocation = tempDirLocation + "/workspace" + ran.nextInt(10000);
        WorkspaceBootstrapper wb = new WorkspaceBootstrapper(workspaceLocation);
    }

    @Test
    public void userDirIsCreated() {
        // create workspace dir
        File workspaceDir = TestUtils.createTempWorkspace();
        String workspaceLocation = workspaceDir.getPath();
        // assert @users dir is created
        WorkspaceBootstrapper wb = new WorkspaceBootstrapper(workspaceLocation);
        wb.run();
        String userDirLocation = workspaceLocation + "/" + RvdConfiguration.USERS_DIRECTORY_NAME;
        File usersDir = new File(userDirLocation);
        Assert.assertTrue("Users directory '" + userDirLocation + "' was not created on workspace bootstrapping.", usersDir.exists() );

        TestUtils.removeTempWorkspace(workspaceLocation);
    }

}
