package org.restcomm.connect.rvd.interpreter.exceptions;

import org.restcomm.connect.rvd.exceptions.InterpreterException;

/**
 * The interpreter is requested to handle an action (target=*.$stepname.handle)
 * but the step targetted is not capable of containing any actions. For instance
 * Gather verbs may contain actions but Say verbs cannot
 *
 * @author Tsakiridis Orestis
 *
 */
public class RVDUnsupportedHandlerVerb extends InterpreterException {

    private static final long serialVersionUID = -6629326265806778425L;

}
