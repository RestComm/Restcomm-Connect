package org.restcomm.connect.rvd.upgrade;

import org.restcomm.connect.rvd.upgrade.exceptions.NoUpgraderException;

public class ProjectUpgraderFactory {

    public static ProjectUpgrader create(String version) throws NoUpgraderException {
        if  ("1.7".equals(version)) {
            return new ProjectUpgrader17to18();
        } else
        if ( "rvd714".equals(version) ) {
            return new ProjectUpgrader714To10();
        } else
        if  ("1.0".equals(version)) {
            return new ProjectUpgrader10to11();
        } else
        if  ("1.1".equals(version)) {
            return new ProjectUpgraded11to12();
        } else
        if  ("1.2".equals(version)) {
            return new ProjectUpgrader12to13();
        } else
        if  ("1.3".equals(version)) {
            return new ProjectUpgrader13to14();
        } else
        if  ("1.4".equals(version)) {
            return new ProjectUpgrader14to15();
        } else
        if  ("1.5".equals(version)) {
            return new ProjectUpgrader15to16();
        } else
        if  ("1.6".equals(version)) {
            return new ProjectUpgrader16to17();
        } else
            throw new NoUpgraderException("No project upgrader found for project with version " + version);
    }
}
