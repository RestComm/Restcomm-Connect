package org.mobicents.servlet.restcomm.endpoints;

/**
 * Semantics for the result of an endpoint method. Often it's much more flexible to return these values instead of Response
 *
 * @author "Tsakiridis Orestis"
 *
 */
public enum Outcome {
    OK,
    CONFLICT,
    NOT_FOUND,
    FAILED,
    BAD_INPUT,
    NOT_ALLOWED,
    INTERNAL_ERROR;

    public static int toHttpStatus(Outcome outcome) {
        switch (outcome) {
            case OK: return 200;
            case CONFLICT: return 409;
            case NOT_FOUND: return 404;
            case FAILED: return 500;
            case NOT_ALLOWED: return 401;
            case INTERNAL_ERROR: return 500;
            case BAD_INPUT: return 400;
        }
        throw new UnsupportedOperationException("Invalid Outcome value: ");
    }

    public static Outcome fromHttpStatus(int status) {
        if (status == 200)
            return OK;
        if (status == 204)
            return OK;
        if (status == 404)
            return NOT_FOUND;
        if (status == 401 || status == 403)
            return NOT_ALLOWED;
        if (status == 400)
            return BAD_INPUT;
        if (status == 409)
            return CONFLICT;

        return FAILED;
    }
}
