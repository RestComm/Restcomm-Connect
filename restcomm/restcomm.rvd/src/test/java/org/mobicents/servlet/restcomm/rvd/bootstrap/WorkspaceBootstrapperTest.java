package org.mobicents.servlet.restcomm.rvd.bootstrap;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.Assert;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;

import java.io.File;
import java.util.Random;

/**
 * Created by otsakir on 12/29/15.
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
        Random ran = new Random();
        String workspaceLocation = tempDirLocation + "/workspace" + ran.nextInt(10000);
        File workspaceDir = new File(workspaceLocation);
        workspaceDir.mkdir();
        // assert @users dir is created
        WorkspaceBootstrapper wb = new WorkspaceBootstrapper(workspaceLocation);
        wb.run();
        String userDirLocation = workspaceLocation + "/" + RvdConfiguration.USERS_DIRECTORY_NAME;
        File usersDir = new File(userDirLocation);
        Assert.assertTrue("Users directory '" + userDirLocation + "' was not created on workspace bootstrapping.", usersDir.exists() );

        FileUtils.deleteQuietly(workspaceDir);
    }

}
