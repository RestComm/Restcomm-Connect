package org.mobicents.servlet.restcomm.rvd.model.client;

import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlHungupStep;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlStep;

public class HungupStep extends Step {

    @Override
    public RcmlStep render(Interpreter interpreter) throws InterpreterException {
        return new RcmlHungupStep();
    }

}
