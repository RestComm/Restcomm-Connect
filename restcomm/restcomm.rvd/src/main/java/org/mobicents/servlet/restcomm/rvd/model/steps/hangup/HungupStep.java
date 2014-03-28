package org.mobicents.servlet.restcomm.rvd.model.steps.hangup;

import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlStep;

public class HungupStep extends Step {

    @Override
    public RcmlStep render(Interpreter interpreter) throws InterpreterException {
        return new RcmlHungupStep();
    }

}
