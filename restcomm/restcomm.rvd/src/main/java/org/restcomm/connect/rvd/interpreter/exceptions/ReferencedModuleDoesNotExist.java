package org.restcomm.connect.rvd.interpreter.exceptions;

import org.restcomm.connect.rvd.exceptions.InterpreterException;

/**
 * A module was used (by name or by label) and did not exist. For example an external service
 * component with dynamic routing enabled responded with a 'nextModule' that did not exist.
 *
 */
public class ReferencedModuleDoesNotExist extends InterpreterException {

    public ReferencedModuleDoesNotExist() {
        super();
        // TODO Auto-generated constructor stub
    }

    public ReferencedModuleDoesNotExist(String message) {
        super(message);
        // TODO Auto-generated constructor stub
    }

}
