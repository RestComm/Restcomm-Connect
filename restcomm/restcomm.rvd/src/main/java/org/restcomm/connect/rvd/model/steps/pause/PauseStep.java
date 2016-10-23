package org.restcomm.connect.rvd.model.steps.pause;

import org.restcomm.connect.rvd.interpreter.Interpreter;
import org.restcomm.connect.rvd.model.client.Step;

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
