package org.mobicents.servlet.restcomm.rvd.model.steps.log;

import javax.servlet.http.HttpServletRequest;

import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlStep;

public class LogStep extends Step {

    private String message;

    @Override
    public RcmlStep render(Interpreter interpreter) throws InterpreterException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String process(Interpreter interpreter, HttpServletRequest httpRequest ) throws InterpreterException {
        if ( interpreter.getRvdContext().getProjectSettings().getLogging() ) {
            String expandedMessage = interpreter.populateVariables(message);
            interpreter.getProjectLogger().log(expandedMessage).tag("app",interpreter.getAppName()).tag("ES").tag("LOG").done();
        }
        return null;
    }

}
