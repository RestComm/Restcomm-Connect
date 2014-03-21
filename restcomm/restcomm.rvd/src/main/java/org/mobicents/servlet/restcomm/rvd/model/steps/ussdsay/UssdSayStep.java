package org.mobicents.servlet.restcomm.rvd.model.steps.ussdsay;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlStep;

public class UssdSayStep extends Step {
    static final Logger logger = Logger.getLogger(UssdSayStep.class.getName());

    String text;
    String language;


    public UssdSayStep() {
        // TODO Auto-generated constructor stub
    }


    public String getText() {
        return text;
    }


    public void setText(String text) {
        this.text = text;
    }


    public String getLanguage() {
        return language;
    }


    public void setLanguage(String language) {
        this.language = language;
    }


    @Override
    public RcmlStep render(Interpreter interpreter) throws InterpreterException {
        UssdSayRcml rcmlModel = new UssdSayRcml();
        rcmlModel.text = getText();
        rcmlModel.language = getLanguage();

        return rcmlModel;
    }
}
