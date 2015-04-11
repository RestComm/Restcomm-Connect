package org.mobicents.servlet.restcomm.rvd.restcomm;

public class CreateCallResponse {

    String sid;
    String to;
    String from;

    public CreateCallResponse() {
        // TODO Auto-generated constructor stub
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

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

}
