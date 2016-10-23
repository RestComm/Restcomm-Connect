package org.restcomm.connect.rvd.model.steps.dial;

import org.restcomm.connect.rvd.exceptions.InterpreterException;
import org.restcomm.connect.rvd.interpreter.Interpreter;

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
