package org.mobicents.servlet.restcomm.rvd.model.steps.email;

import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlStep;

/**
 * Created by lefty on 6/24/15.
 */
public class RcmlEmailStep extends RcmlStep {

    String text;
    String from;
    String to;
    String subject;
    String action;
    String method;
    String statusCallback;

    public void setAction(String action) {
        this.action = action;
    }
    public String getAction() {
        return action;
    }
    public void setMethod(String method) {
        this.method = method;
    }
    public String getMethod() {
        return method;
    }
    public void setSubject(String subject) {
        this.subject = subject;
    }
    public String getSubject() {
        return subject;
    }
    public void setFrom(String from) {
        this.from = from;
    }
    public String getFrom() {
        return from;
    }
    public void setTo(String to) {
        this.to = to;
    }
    public String getTo() {
        return to;
    }
    public void setStatusCallback(String statusCallback) {
        this.statusCallback = statusCallback;
    }
    public String getStatusCallback() {
        return statusCallback;
    }
    public void setText(String text) {
        this.text = text;
    }
    public String getText() {
        return text;
    }

}
