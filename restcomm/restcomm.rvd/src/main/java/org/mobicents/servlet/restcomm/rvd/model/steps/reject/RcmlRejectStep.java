package org.mobicents.servlet.restcomm.rvd.model.steps.reject;

import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlStep;

public class RcmlRejectStep extends RcmlStep {
    String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
