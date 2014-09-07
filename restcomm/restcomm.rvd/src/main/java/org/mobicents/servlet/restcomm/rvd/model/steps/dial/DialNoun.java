package org.mobicents.servlet.restcomm.rvd.model.steps.dial;

import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;

public abstract class DialNoun {
    private String dialType;

    public String getDialType() {
        return dialType;
    }

    public void setDialType(String dialType) {
        this.dialType = dialType;
    }
    public abstract RcmlNoun render(Interpreter interpreter) throws InterpreterException;
}
