package org.mobicents.servlet.restcomm.rvd.model.steps.dial;

import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;

public class ClientDialNoun extends DialNoun {
    private String destination;

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    @Override
    public RcmlNoun render(Interpreter interpreter) throws InterpreterException {
        RcmlClientNoun rcmlNoun = new RcmlClientNoun();
        rcmlNoun.setDestination(getDestination());
        return rcmlNoun;
    }
}
