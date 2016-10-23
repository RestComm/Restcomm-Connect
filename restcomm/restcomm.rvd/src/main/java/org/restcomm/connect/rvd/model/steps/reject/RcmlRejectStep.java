package org.restcomm.connect.rvd.model.steps.reject;

import org.restcomm.connect.rvd.model.rcml.RcmlStep;

public class RcmlRejectStep extends RcmlStep {
    String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
