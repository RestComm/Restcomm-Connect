package org.restcomm.connect.rvd.exceptions;

/**
 * Thrown when an error occurs while process Restcomm configuration file (restcomm.xml).
 * See RestcommConfig and RvdConfiguration classes.
 *
 * @author Orestis Tsakiridis
 */
public class RestcommConfigProcessError extends RestcommConfigurationException {
    public RestcommConfigProcessError() {
    }

    public RestcommConfigProcessError(String message) {
        super(message);
    }

    public RestcommConfigProcessError(String message, Throwable throwable) {
        super(message, throwable);
    }
}
