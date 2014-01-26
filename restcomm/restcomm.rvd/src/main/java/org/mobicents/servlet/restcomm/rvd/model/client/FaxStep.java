package org.mobicents.servlet.restcomm.rvd.model.client;

public class FaxStep extends Step {
    String to;
    String from;
    String text;
    String next;
    String method;
    String statusCallback;
    public String getTo() {
        return to;
    }
    public void setTo(String to) {
        this.to = to;
    }
    public String getFrom() {
        return from;
    }
    public void setFrom(String from) {
        this.from = from;
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public String getNext() {
        return next;
    }
    public void setNext(String next) {
        this.next = next;
    }
    public String getMethod() {
        return method;
    }
    public void setMethod(String method) {
        this.method = method;
    }
    public String getStatusCallback() {
        return statusCallback;
    }
    public void setStatusCallback(String statusCallback) {
        this.statusCallback = statusCallback;
    }
}
