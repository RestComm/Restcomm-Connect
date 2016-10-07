package org.restcomm.connect.rvd.model.steps.pause;

import org.restcomm.connect.rvd.model.rcml.RcmlStep;

public class RcmlPauseStep extends RcmlStep {
    Integer length;

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }
}
