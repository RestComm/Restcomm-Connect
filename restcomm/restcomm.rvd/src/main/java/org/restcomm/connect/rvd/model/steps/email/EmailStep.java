package org.restcomm.connect.rvd.model.steps.email;

import org.apache.log4j.Logger;
import org.restcomm.connect.rvd.BuildService;
import org.restcomm.connect.rvd.RvdConfiguration;
import org.restcomm.connect.rvd.exceptions.InterpreterException;
import org.restcomm.connect.rvd.interpreter.Interpreter;
import org.restcomm.connect.rvd.interpreter.Target;
import org.restcomm.connect.rvd.model.client.Step;
import org.restcomm.connect.rvd.storage.exceptions.StorageException;
import org.restcomm.connect.rvd.utils.RvdUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lefty on 6/24/15.
 */
public class EmailStep extends Step {

    static final Logger logger = Logger.getLogger(BuildService.class.getName());
    String text;
    String to;
    String from;
    String bcc;
    String cc;
    String subject;
    String statusCallback;
    String method;
    String next;

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
    public String getFrom() {
        return from;
    }
    public void setFrom(String from) {
        this.from = from;
    }
    public String getTo() {
        return to;
    }
    public void setTo(String to) {
        this.to = to;
    }
    public String getCc() {
             return cc;
           }
    public void setCc(String cc) {
             this.cc = cc;
       }
    public String getBcc() {
         return bcc;
          }
    public void setBcc(String bcc) {
             this.bcc = bcc;
         }
    public String getSubject() {
        return subject;
    }
    public void setSubject(String Subject) {
        this.subject = Subject;
    }
    public String getStatusCallback() {
        return statusCallback;
    }
    public void setStatusCallback(String statusCallback) {
        this.statusCallback = statusCallback;
    }
    public String getText() {
        return text;
    }
    public void setText(String text) {
        this.text = text;
    }

    public RcmlEmailStep render(Interpreter interpreter) {
        RcmlEmailStep rcmlStep = new RcmlEmailStep();

        if ( ! RvdUtils.isEmpty(getNext()) ) {
            String newtarget = interpreter.getTarget().getNodename() + "." + getName() + ".actionhandler";
            Map<String, String> pairs = new HashMap<String, String>();
            pairs.put("target", newtarget);
            String action = interpreter.buildAction(pairs);
            rcmlStep.setAction(action);
            rcmlStep.setMethod(getMethod());
        }

        rcmlStep.setFrom(interpreter.populateVariables(getFrom()));
        rcmlStep.setSubject(interpreter.populateVariables(getSubject()));
        rcmlStep.setTo(interpreter.populateVariables(getTo()));
        rcmlStep.setCc(interpreter.populateVariables(getCc()));
        rcmlStep.setBcc(interpreter.populateVariables(getBcc()));
        rcmlStep.setStatusCallback(getStatusCallback());
        rcmlStep.setText(interpreter.populateVariables(getText()));

        return rcmlStep;
    }

    @Override
    public void handleAction(Interpreter interpreter, Target originTarget) throws InterpreterException, StorageException {
        logger.info("handling email action");
        if ( RvdUtils.isEmpty(getNext()) )
            throw new InterpreterException( "'next' module is not defined for step " + getName() );

        String EmailSid = interpreter.getRequestParams().getFirst("EmailSid");
        String EmailStatus = interpreter.getRequestParams().getFirst("EmailStatus");

        if ( EmailSid != null )
            interpreter.getVariables().put(RvdConfiguration.CORE_VARIABLE_PREFIX + "EmailSid", EmailSid);
        if (EmailStatus != null )
            interpreter.getVariables().put(RvdConfiguration.CORE_VARIABLE_PREFIX + "EmailStatus", EmailStatus);

        interpreter.interpret( getNext(), null, null, originTarget );
    }

}