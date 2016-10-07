package org.restcomm.connect.rvd.model.steps.hangup;

import org.restcomm.connect.rvd.exceptions.InterpreterException;
import org.restcomm.connect.rvd.interpreter.Interpreter;
import org.restcomm.connect.rvd.model.client.Step;
import org.restcomm.connect.rvd.model.rcml.RcmlStep;

public class HungupStep extends Step {

    @Override
    public RcmlStep render(Interpreter interpreter) throws InterpreterException {
        return new RcmlHungupStep();
    }

}
