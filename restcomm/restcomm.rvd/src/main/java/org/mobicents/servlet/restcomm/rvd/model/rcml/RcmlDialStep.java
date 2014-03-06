package org.mobicents.servlet.restcomm.rvd.model.rcml;

import java.util.ArrayList;
import java.util.List;

public class RcmlDialStep extends RcmlStep {
    private List<RcmlNoun> nouns = new ArrayList<RcmlNoun>();

    public List<RcmlNoun> getNouns() {
        return nouns;
    }

    public void setNouns(List<RcmlNoun> nouns) {
        this.nouns = nouns;
    }

    /*
    private String number;
    private String client;
    private String conference;
    private String sipuri;
    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getConference() {
        return conference;
    }

    public void setConference(String conference) {
        this.conference = conference;
    }

    public String getSipuri() {
        return sipuri;
    }

    public void setSipuri(String sipuri) {
        this.sipuri = sipuri;
    }
    */
}
