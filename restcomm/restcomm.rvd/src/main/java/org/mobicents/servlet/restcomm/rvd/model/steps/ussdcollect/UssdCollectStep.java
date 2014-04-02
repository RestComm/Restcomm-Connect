package org.mobicents.servlet.restcomm.rvd.model.steps.ussdcollect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;
import org.mobicents.servlet.restcomm.rvd.model.steps.ussdsay.UssdSayStep;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

public class UssdCollectStep extends Step {

    static final Logger logger = Logger.getLogger(UssdCollectStep.class.getName());

    public static class Mapping {
        String digits;
        String next;
    }
    public final class Menu {
        private List<Mapping> mappings;
    }
    public final class Collectdigits {
        private String next;
        private String collectVariable;
    }

    String gatherType;
    String text;
    private Menu menu;
    private Collectdigits collectdigits;

    List<UssdSayStep> messages;

    public UssdCollectStep() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public UssdCollectRcml render(Interpreter interpreter) throws InterpreterException {
        // TODO Auto-generated method stub
        UssdCollectRcml rcml = new UssdCollectRcml();
        String newtarget = interpreter.getTarget().getNodename() + "." + getName() + ".handle";
        Map<String, String> pairs = new HashMap<String, String>();
        pairs.put("target", newtarget);

        rcml.action = interpreter.buildAction(pairs);
        for ( UssdSayStep message : messages ) {
            rcml.messages.add(message.render(interpreter));
        }

        return rcml;
    }
    public void handleAction(Interpreter interpreter) throws InterpreterException, StorageException {
        logger.debug("UssdCollect handler");
        if ("menu".equals(gatherType)) {

            boolean handled = false;
            for (Mapping mapping : menu.mappings) {
                // use a string for USSD collect. Alpha is supported too
                String digits = interpreter.getRequestParams().getFirst("Digits");

                logger.debug("checking digits: " + mapping.digits + " - " + digits);

                if (mapping.digits != null && mapping.digits.equals(digits)) {
                    // seems we found out menu selection
                    logger.debug("seems we found our menu selection");
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
