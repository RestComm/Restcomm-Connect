package org.restcomm.connect.rvd.model.steps.say;

import org.restcomm.connect.rvd.interpreter.Interpreter;
import org.restcomm.connect.rvd.model.client.Step;
import org.restcomm.connect.rvd.model.rcml.RcmlStep;

public class SayStep extends Step {

    private String phrase;
    private String voice;
    private String language;
    private Integer loop;

    public static SayStep createDefault(String name, String phrase) {
        SayStep step = new SayStep();
        step.setName(name);
        step.setLabel("say");
        step.setKind("say");
        step.setTitle("say");
        step.setPhrase(phrase);

        return step;
    }

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

    public RcmlStep render(Interpreter interpreter) {

        RcmlSayStep sayStep = new RcmlSayStep();
        sayStep.setPhrase(interpreter.populateVariables(getPhrase()));
        sayStep.setVoice(getVoice());
        sayStep.setLanguage(getLanguage());
        sayStep.setLoop(getLoop());

        return sayStep;
    }
}
