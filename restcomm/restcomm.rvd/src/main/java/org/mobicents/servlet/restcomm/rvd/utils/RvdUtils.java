package org.mobicents.servlet.restcomm.rvd.utils;

import java.io.File;

import org.mobicents.servlet.restcomm.rvd.exceptions.RvdException;



public class RvdUtils {

    private static final int TEMP_DIR_ATTEMPTS = 10000;

    public RvdUtils() {
        // TODO Auto-generated constructor stub
    }

    public static File createTempDir() throws RvdException {
        File baseDir = new File(System.getProperty("java.io.tmpdir"));
        String baseName = System.currentTimeMillis() + "-";

        for (int counter = 0; counter < TEMP_DIR_ATTEMPTS; counter++) {
          File tempDir = new File(baseDir, baseName + counter);
          if (tempDir.mkdir()) {
            return tempDir;
          }
        }
        throw new RvdException("Failed to create directory within "
            + TEMP_DIR_ATTEMPTS + " attempts (tried "
            + baseName + "0 to " + baseName + (TEMP_DIR_ATTEMPTS - 1) + ')');
      }
}
