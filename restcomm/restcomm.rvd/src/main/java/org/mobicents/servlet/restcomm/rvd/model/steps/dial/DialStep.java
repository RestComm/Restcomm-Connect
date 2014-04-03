package org.mobicents.servlet.restcomm.rvd.model.steps.dial;

import java.util.List;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;

public class DialStep extends Step {
    static final Logger logger = Logger.getLogger(DialStep.class.getName());

    private List<DialNoun> dialNouns;
    private String action;
    private String method;
    private Integer timeout;
    private Integer timeLimit;
    private String callerId;
    private String nextModule;
    private String record;

    public RcmlDialStep render(Interpreter interpreter) throws InterpreterException {
        RcmlDialStep rcmlStep = new RcmlDialStep();

        for ( DialNoun noun: dialNouns ) {
            rcmlStep.nouns.add( noun.render(interpreter) );
        }

        // set action (from nextModule)
        String moduleUrl = interpreter.moduleUrl(nextModule);
        if ( moduleUrl != null )
            rcmlStep.action = moduleUrl;
        else {
            logger.warn("Tried to reference a non-existing module while building 'action' property: " + nextModule + ". It will be ignored.");
        }

        rcmlStep.method = method;
        rcmlStep.timeout = timeout == null ? null : timeout.toString();
        rcmlStep.timeLimit = (timeLimit == null ? null : timeLimit.toString());
        rcmlStep.callerId = callerId;
        rcmlStep.record = record;

        return rcmlStep;
    }

}
