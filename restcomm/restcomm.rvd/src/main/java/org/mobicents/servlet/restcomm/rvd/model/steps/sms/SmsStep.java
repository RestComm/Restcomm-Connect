package org.mobicents.servlet.restcomm.rvd.model.steps.sms;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.BuildService;
import org.mobicents.servlet.restcomm.rvd.RvdSettings;
import org.mobicents.servlet.restcomm.rvd.utils.RvdUtils;
import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

public class SmsStep extends Step {
    static final Logger logger = Logger.getLogger(BuildService.class.getName());
    String text;
    String to;
    String from;
    String statusCallback;
    String method;
    String next;

    public static SmsStep createDefault(String name, String phrase) {
        SmsStep step = new SmsStep();
        step.setName(name);
        step.setLabel("sms");
        step.setKind("sms");
        step.setTitle("sms");
        step.setText(phrase);

        return step;
    }

    public String getNext() {
        return next;
    }
    public void setNext(String next) {
        this.next = next;
    }
    public String getMethod() {
        return method;
    }
    public void setMethod(String method) {
        this.method = method;
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }
    public String getTo() {
        return to;
    }
    public void setTo(String to) {
        this.to = to;
    }
    public String getFrom() {
        return from;
    }
    public void setFrom(String from) {
        this.from = from;
    }
    public String getStatusCallback() {
        return statusCallback;
    }
    public void setStatusCallback(String statusCallback) {
        this.statusCallback = statusCallback;
    }
    public RcmlSmsStep render(Interpreter interpreter) {
        RcmlSmsStep rcmlStep = new RcmlSmsStep();

        if ( ! RvdUtils.isEmpty(getNext()) ) {
            String newtarget = interpreter.getTarget().getNodename() + "." + getName() + ".actionhandler";
            Map<String, String> pairs = new HashMap<String, String>();
            pairs.put("target", newtarget);
            String action = interpreter.buildAction(pairs);
            rcmlStep.setAction(action);
            rcmlStep.setMethod(getMethod());
        }

        rcmlStep.setFrom(interpreter.populateVariables(getFrom()));
        rcmlStep.setTo(interpreter.populateVariables(getTo()));
        rcmlStep.setStatusCallback(getStatusCallback());
        rcmlStep.setText(interpreter.populateVariables(getText()));

        return rcmlStep;
    }
    public void handleAction(Interpreter interpreter) throws InterpreterException, StorageException {
        logger.info("handling sms action");
        if ( RvdUtils.isEmpty(getNext()) )
            throw new InterpreterException( "'next' module is not defined for step " + getName() );

        String SmsSid = interpreter.getRequestParams().getFirst("SmsSid");
        String SmsStatus = interpreter.getRequestParams().getFirst("SmsStatus");

        if ( SmsSid != null )
            interpreter.getVariables().put(RvdSettings.CORE_VARIABLE_PREFIX + "SmsSid", SmsSid);
        if (SmsStatus != null )
            interpreter.getVariables().put(RvdSettings.CORE_VARIABLE_PREFIX + "SmsStatus", SmsStatus);

        interpreter.interpret( getNext(), null, null );
    }
}
