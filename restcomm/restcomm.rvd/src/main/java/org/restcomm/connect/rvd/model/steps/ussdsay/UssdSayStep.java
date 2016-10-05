package org.restcomm.connect.rvd.model.steps.ussdsay;

import org.apache.log4j.Logger;
import org.restcomm.connect.rvd.exceptions.InterpreterException;
import org.restcomm.connect.rvd.interpreter.Interpreter;
import org.restcomm.connect.rvd.model.client.Step;

public class UssdSayStep extends Step {
    static final Logger logger = Logger.getLogger(UssdSayStep.class.getName());

    String text;

    public static UssdSayStep createDefault(String name, String phrase) {
        UssdSayStep step = new UssdSayStep();
        step.setName(name);
        step.setLabel("USSD Message");
        step.setKind("ussdSay");
        step.setTitle("USSD Message");
        step.setText(phrase);

        return step;
    }

    public UssdSayStep() {
        // TODO Auto-generated constructor stub
    }


    public String getText() {
        return text;
    }


    public void setText(String text) {
        this.text = text;
    }

    @Override
    public UssdSayRcml render(Interpreter interpreter) throws InterpreterException {
        UssdSayRcml rcmlModel = new UssdSayRcml();
        rcmlModel.text = interpreter.populateVariables(getText());

        return rcmlModel;
    }
}
