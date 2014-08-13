package org.mobicents.servlet.restcomm.rvd.model.steps.gather;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.RvdConfiguration;
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
    private Validation validation;
    private Menu menu;
    private Collectdigits collectdigits;
    private String gatherType;

    public final class Menu {
        private List<Mapping> mappings;
    }
    public final class Collectdigits {
        private String next;
        private String collectVariable;
        private String scope;
    }

    public static class Mapping {
        private Integer digits;
        private String next;
    }
    public final class Validation {
        private Step messageStep;
        private String pattern;
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
        logger.info("handling gather action");

        String digitsString = interpreter.getRequestParams().getFirst("Digits");
        if ( digitsString != null )
            interpreter.getVariables().put(RvdConfiguration.CORE_VARIABLE_PREFIX + "Digits", digitsString);

        boolean valid = true;

        if ("menu".equals(gatherType)) {
            boolean handled = false;
            for (Mapping mapping : menu.mappings) {
                Integer digits = Integer.parseInt( digitsString );
                logger.debug("checking digits: " + mapping.digits + " - " + digits);

                if (mapping.digits != null && mapping.digits.equals(digits)) {
                    // seems we found out menu selection
                    logger.debug("seems we found out menu selection");
                    interpreter.interpret(mapping.next,null, null);
                    handled = true;
                }
            }
            if (!handled)
                valid = false;
        } else
        if ("collectdigits".equals(gatherType)) {

            String variableName = collectdigits.collectVariable;
            String variableValue = interpreter.getRequestParams().getFirst("Digits");
            if ( variableValue == null ) {
                logger.warn("'Digits' parameter was null. Is this a valid restcomm request?");
                variableValue = "";
            }

            // validation
            boolean doValidation = false;
            if ( validation.pattern != null && !validation.pattern.trim().equals("")) {
                doValidation = true;
                logger.debug("Validating '" + variableValue + "' against " + validation.pattern);
                if ( !variableValue.matches(validation.pattern) )
                    valid = false;
            }

            if ( doValidation && !valid ) {
                logger.debug("Invalid input for gather/collectdigits. Will say the validation message and rerun the gather");
            } else {
                // is this an application-scoped variable ?
                if ( "application".equals(collectdigits.scope) ) {
                    logger.debug("'" + variableName + "' is application scoped");
                    interpreter.putStickyVariable(variableName, variableValue);
                }

                // in any case initialize the module-scoped variable
                interpreter.getVariables().put(variableName, variableValue);

                interpreter.interpret(collectdigits.next,null,null);
            }
        }

        if ( !valid ) { // this should always be true
            interpreter.interpret(interpreter.getTarget().getNodename() + "." + interpreter.getTarget().getStepname(),null, (validation != null) ? validation.messageStep : null);
        }
    }
}
