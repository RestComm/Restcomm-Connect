package org.mobicents.servlet.restcomm.email.api;


import org.mobicents.servlet.restcomm.patterns.StandardResponse;

/**
 * Created by lefty on 6/16/15.
 */
public class EmailResponse<T> extends StandardResponse <T> {
    public EmailResponse(final T object) {
        super(object);
    }

    public EmailResponse(final Throwable cause) {
        super(cause);
    }

    public EmailResponse(final Throwable cause, final String message) {
        super(cause, message);
    }
}
