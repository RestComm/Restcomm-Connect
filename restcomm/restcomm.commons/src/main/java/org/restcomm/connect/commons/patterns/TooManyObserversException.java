package org.restcomm.connect.commons.patterns;

public final class TooManyObserversException extends Exception {
    private static final long serialVersionUID = 1L;

    public TooManyObserversException(String message) {
        super(message);
    }

    public TooManyObserversException(Throwable cause) {
        super(cause);
    }

    public TooManyObserversException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
