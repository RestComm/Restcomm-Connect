package org.restcomm.connect.rvd.exceptions.callcontrol;

/**
 * Thrown for all configuration issues. Either comming from bad RVD configuration or from errors reading the restcomm configuration.
 * @author "Tsakiridis Orestis"
 *
 */
public class CallControlInvalidConfigurationException extends CallControlException {

    public CallControlInvalidConfigurationException() {
        // TODO Auto-generated constructor stub
    }

    public CallControlInvalidConfigurationException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    public CallControlInvalidConfigurationException(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

}
