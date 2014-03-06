package org.mobicents.servlet.restcomm.rvd.model.client;

import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlNoun;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlSipuriNoun;

public class SipuriDialNoun extends DialNoun {
    private String destination;

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    @Override
    public RcmlNoun render(Interpreter interpreter) throws InterpreterException {
        RcmlSipuriNoun rcmlNoun = new RcmlSipuriNoun();
        rcmlNoun.setDestination(getDestination());
        return rcmlNoun;
    }
}
