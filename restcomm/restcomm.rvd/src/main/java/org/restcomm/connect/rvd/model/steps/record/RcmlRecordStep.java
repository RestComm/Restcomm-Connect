package org.restcomm.connect.rvd.model.steps.record;

import org.restcomm.connect.rvd.model.rcml.RcmlStep;

public class RcmlRecordStep extends RcmlStep {
    String action;
    String method;
    Integer timeout;
    String finishOnKey;
    Integer maxLength;
    Boolean transcribe;
    String transcribeCallback;
    Boolean playBeep;
    public String getAction() {
        return action;
    }
    public void setAction(String action) {
        this.action = action;
    }
    public String getMethod() {
        return method;
    }
    public void setMethod(String method) {
        this.method = method;
    }
    public Integer getTimeout() {
        return timeout;
    }
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }
    public String getFinishOnKey() {
        return finishOnKey;
    }
    public void setFinishOnKey(String finishOnKey) {
        this.finishOnKey = finishOnKey;
    }
    public Integer getMaxLength() {
        return maxLength;
    }
    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }
    public Boolean getTranscribe() {
        return transcribe;
    }
    public void setTranscribe(Boolean transcribe) {
        this.transcribe = transcribe;
    }
    public String getTranscribeCallback() {
        return transcribeCallback;
    }
    public void setTranscribeCallback(String transcribeCallback) {
        this.transcribeCallback = transcribeCallback;
    }
    public Boolean getPlayBeep() {
        return playBeep;
    }
    public void setPlayBeep(Boolean playBeep) {
        this.playBeep = playBeep;
    }
}
