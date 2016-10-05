package org.restcomm.connect.rvd.model.steps.fax;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.restcomm.connect.rvd.BuildService;
import org.restcomm.connect.rvd.RvdConfiguration;
import org.restcomm.connect.rvd.utils.RvdUtils;
import org.restcomm.connect.rvd.exceptions.InterpreterException;
import org.restcomm.connect.rvd.interpreter.Interpreter;
import org.restcomm.connect.rvd.interpreter.Target;
import org.restcomm.connect.rvd.model.client.Step;
import org.restcomm.connect.rvd.storage.exceptions.StorageException;

public class FaxStep extends Step {

    static final Logger logger = Logger.getLogger(BuildService.class.getName());

    String to;
    String from;
    String text;
    String next;
    String method;
    String statusCallback;
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
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
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
    public String getStatusCallback() {
        return statusCallback;
    }
    public void setStatusCallback(String statusCallback) {
        this.statusCallback = statusCallback;
    }

    public RcmlFaxStep render(Interpreter interpreter) {
        RcmlFaxStep rcmlStep = new RcmlFaxStep();

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

    @Override
    public void handleAction(Interpreter interpreter, Target originTarget) throws InterpreterException, StorageException {
        if(logger.isInfoEnabled()) {
            logger.info("handling fax action");
        }
        if ( RvdUtils.isEmpty(getNext()) )
            throw new InterpreterException( "'next' module is not defined for step " + getName() );

        String FaxSid = interpreter.getRequestParams().getFirst("FaxSid"); //getHttpRequest().getParameter("FaxSid");
        String FaxStatus = interpreter.getRequestParams().getFirst("FaxStatus");  //.getHttpRequest().getParameter("FaxStatus");

        if ( FaxSid != null )
            interpreter.getVariables().put(RvdConfiguration.CORE_VARIABLE_PREFIX + "FaxSid", FaxSid);
        if (FaxStatus != null )
            interpreter.getVariables().put(RvdConfiguration.CORE_VARIABLE_PREFIX + "FaxStatus", FaxStatus);

        interpreter.interpret( getNext(), null, null, originTarget );
    }
}
