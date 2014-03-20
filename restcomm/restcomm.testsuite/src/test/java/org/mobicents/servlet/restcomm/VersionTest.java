package org.mobicents.servlet.restcomm;


import org.apache.log4j.Logger;
import org.junit.Test;

/**
 * @author <a href="mailto:gvagenas@gmail.com">gvagenas</a>
 * @author <a href="mailto:jean.deruelle@telestax.com">Jean Deruelle</a>
 */

public class VersionTest {
    private final static Logger logger = Logger.getLogger(VersionTest.class.getName());
    private static final String version = org.mobicents.servlet.restcomm.Version.getVersion();

    @Test
    public void testVersion() {
        logger.info(version);
    }
}
