package org.restcomm.connect.rvd.storage.exceptions;

/**
 * Throw when the specific piece of data we are looking for does not exist. Used in cases where
 * such non-existance is not an indication of data invalidity
 * @author "Tsakiridis Orestis"
 *
 */
public class StorageEntityNotFound extends StorageException {

    public StorageEntityNotFound() {
        // TODO Auto-generated constructor stub
    }

    public StorageEntityNotFound(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    public StorageEntityNotFound(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

}
