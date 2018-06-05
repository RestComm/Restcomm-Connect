package org.restcomm.connect.commons.dao;

/**
 * @author mariafarooq
 *
 */
public enum Error {

	QUEUE_OVERFLOW (30001, "Queue overflow");
	
	private final Integer errorCode;
    private final String errorMessage;
    
    private Error(final Integer errorCode, final String errorMessage) {
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

	public Integer getErrorCode() {
		return errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

    public static Error getErrorValue(final Integer errorCode) {
        final Error[] values = values();
        for (final Error value : values) {
            if (value.getErrorCode().equals(errorCode)) {
                return value;
            }
        }
        throw new IllegalArgumentException(errorCode + " is not a valid errorCode.");
    }
}
