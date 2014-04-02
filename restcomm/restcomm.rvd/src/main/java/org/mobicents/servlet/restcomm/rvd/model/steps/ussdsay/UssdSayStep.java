package org.mobicents.servlet.restcomm.rvd.model.steps.ussdsay;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;

public class UssdSayStep extends Step {
    static final Logger logger = Logger.getLogger(UssdSayStep.class.getName());

    String text;

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
        rcmlModel.text = getText();

        return rcmlModel;
    }
}
