package org.mobicents.servlet.restcomm.rvd.upgrade;

import org.mobicents.servlet.restcomm.rvd.upgrade.exceptions.NoUpgraderException;

public class ProjectUpgraderFactory {

    public static ProjectUpgrader create(String version) throws NoUpgraderException {
        if ( "rvd714".equals(version) ) {
            return new ProjectUpgrader714To10();
        } else
            throw new NoUpgraderException("No project upgrader found for project with version " + version);
    }
}
