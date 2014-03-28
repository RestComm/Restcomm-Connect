package org.mobicents.servlet.restcomm.rvd.model.steps.pause;

import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;

public class PauseStep extends Step {
    Integer length;

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }
    public RcmlPauseStep render(Interpreter interpreter) {
        RcmlPauseStep rcmlStep = new RcmlPauseStep();
        if ( getLength() != null )
            rcmlStep.setLength(getLength());
        return rcmlStep;
    }
}
