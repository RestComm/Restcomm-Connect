package org.mobicents.servlet.restcomm.rvd.exceptions.callcontrol;

import org.mobicents.servlet.restcomm.rvd.exceptions.RestcommConfigurationException;

/*
 * Thrown when the restcomm.xml configuration file could not be found
 */
public class RestcommConfigNotFound extends RestcommConfigurationException {

    public RestcommConfigNotFound(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    public RestcommConfigNotFound(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

    public RestcommConfigNotFound() {
        // TODO Auto-generated constructor stub
    }

}
