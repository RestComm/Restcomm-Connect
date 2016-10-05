package org.restcomm.connect.rvd.model.steps.play;

import org.restcomm.connect.rvd.model.rcml.RcmlStep;


public class RcmlPlayStep extends RcmlStep {
    private String wavurl;
    private Integer loop;

    public String getWavurl() {
        return wavurl;
    }

    public void setWavurl(String wavurl) {
        this.wavurl = wavurl;
    }

    public Integer getLoop() {
        return loop;
    }

    public void setLoop(Integer loop) {
        this.loop = loop;
    }
}
