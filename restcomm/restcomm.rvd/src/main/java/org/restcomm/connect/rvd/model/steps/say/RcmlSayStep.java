package org.restcomm.connect.rvd.model.steps.say;

import org.restcomm.connect.rvd.model.rcml.RcmlStep;


public class RcmlSayStep extends RcmlStep {
    private String phrase;
    private String voice;
    private String language;
    private Integer loop;

    public String getPhrase() {
        return phrase;
    }

    public void setPhrase(String phrase) {
        this.phrase = phrase;
    }

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getLoop() {
        return loop;
    }

    public void setLoop(Integer loop) {
        this.loop = loop;
    }
}
