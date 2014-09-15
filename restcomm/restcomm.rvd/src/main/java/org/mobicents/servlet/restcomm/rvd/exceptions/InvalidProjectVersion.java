package org.mobicents.servlet.restcomm.rvd.exceptions;

import org.mobicents.servlet.restcomm.rvd.upgrade.exceptions.UpgradeException;

/**
 * Thrown when an invalid version identifier has been specified. For example "1.0", "1.1" are valid identifiers.
 * @author "Tsakiridis Orestis"
 *
 */
public class InvalidProjectVersion extends UpgradeException {

    public InvalidProjectVersion() {
        // TODO Auto-generated constructor stub
    }

    public InvalidProjectVersion(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    public InvalidProjectVersion(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

}
