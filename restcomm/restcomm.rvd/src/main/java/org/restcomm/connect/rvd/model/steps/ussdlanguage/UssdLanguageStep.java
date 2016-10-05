package org.restcomm.connect.rvd.model.steps.ussdlanguage;

import org.apache.log4j.Logger;
import org.restcomm.connect.rvd.exceptions.InterpreterException;
import org.restcomm.connect.rvd.interpreter.Interpreter;
import org.restcomm.connect.rvd.model.client.Step;

public class UssdLanguageStep extends Step {
    static final Logger logger = Logger.getLogger(UssdLanguageStep.class.getName());

    String language;

    public UssdLanguageStep() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public UssdLanguageRcml render(Interpreter interpreter) throws InterpreterException {
        UssdLanguageRcml rcml = new UssdLanguageRcml();
        rcml.language = language;
        return rcml;
    }

}
