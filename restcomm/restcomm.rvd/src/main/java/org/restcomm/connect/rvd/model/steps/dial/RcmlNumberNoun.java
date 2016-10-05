package org.restcomm.connect.rvd.model.steps.dial;


public class RcmlNumberNoun extends RcmlNoun {
    String sendDigits;
    String url;
    String destination;
    public String getSendDigits() {
        return sendDigits;
    }
    public void setSendDigits(String sendDigits) {
        this.sendDigits = sendDigits;
    }
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public String getDestination() {
        return destination;
    }
    public void setDestination(String destination) {
        this.destination = destination;
    }
}
