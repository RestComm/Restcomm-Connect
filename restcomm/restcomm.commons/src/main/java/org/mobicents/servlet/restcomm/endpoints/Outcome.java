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
    BAD_INPUT
}
