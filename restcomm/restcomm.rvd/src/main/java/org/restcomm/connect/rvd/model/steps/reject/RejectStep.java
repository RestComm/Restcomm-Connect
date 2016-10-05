package org.restcomm.connect.rvd.model.steps.reject;

import org.restcomm.connect.rvd.interpreter.Interpreter;
import org.restcomm.connect.rvd.model.client.Step;

public class RejectStep extends Step {
    String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
    public RcmlRejectStep render(Interpreter interpreter) {
        RcmlRejectStep rcmlStep = new RcmlRejectStep();
        if ( getReason() != null && !"".equals(getReason()))
            rcmlStep.setReason(getReason());
        return rcmlStep;
    }

}
