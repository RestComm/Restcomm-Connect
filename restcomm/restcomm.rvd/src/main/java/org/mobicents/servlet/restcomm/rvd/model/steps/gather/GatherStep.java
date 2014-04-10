package org.mobicents.servlet.restcomm.rvd.model.steps.gather;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

public class GatherStep extends Step {

    static final Logger logger = Logger.getLogger(GatherStep.class.getName());

    private String action;
    private String method;
    private Integer timeout;
    private String finishOnKey;
    private Integer numDigits;
    private List<Step> steps;
    private Menu menu;
    private Collectdigits collectdigits;
    private String gatherType;

    public final class Menu {
        private List<Mapping> mappings;
    }
    public final class Collectdigits {
        private String next;
        private String collectVariable;
    }

    public static class Mapping {
        private Integer digits;
        private String next;
    }

    public RcmlGatherStep render(Interpreter interpreter) throws InterpreterException {

        RcmlGatherStep rcmlStep = new RcmlGatherStep();
        String newtarget = interpreter.getTarget().getNodename() + "." + getName() + ".handle";
        Map<String, String> pairs = new HashMap<String, String>();
        pairs.put("target", newtarget);
        String action = interpreter.buildAction(pairs);

        rcmlStep.setAction(action);
        rcmlStep.setTimeout(timeout);
        if (finishOnKey != null && !"".equals(finishOnKey))
            rcmlStep.setFinishOnKey(finishOnKey);
        rcmlStep.setMethod(method);
        rcmlStep.setNumDigits(numDigits);

        for (Step nestedStep : steps)
            rcmlStep.getSteps().add(nestedStep.render(interpreter));

        return rcmlStep;
    }
    public void handleAction(Interpreter interpreter) throws InterpreterException, StorageException {
        logger.debug("handling gather action");
        if ("menu".equals(gatherType)) {

            boolean handled = false;
            for (Mapping mapping : menu.mappings) {
                Integer digits = Integer.parseInt(interpreter.getRequestParams().getFirst("Digits") );
                logger.debug("checking digits: " + mapping.digits + " - " + digits);

                if (mapping.digits != null && mapping.digits.equals(digits)) {
                    // seems we found out menu selection
                    logger.debug("seems we found out menu selection");
                    interpreter.interpret(mapping.next,null);
                    handled = true;
                }
            }
            if (!handled) {
                interpreter.interpret(interpreter.getTarget().getNodename() + "." + interpreter.getTarget().getStepname(),null);
            }
        }
        if ("collectdigits".equals(gatherType)) {
            String variableName = collectdigits.collectVariable;
            interpreter.getVariables().put(variableName, interpreter.getRequestParams().getFirst("Digits"));  //getHttpRequest().getParameter("Digits")); // put the string directly
            interpreter.interpret(collectdigits.next,null);
        }
    }
}
