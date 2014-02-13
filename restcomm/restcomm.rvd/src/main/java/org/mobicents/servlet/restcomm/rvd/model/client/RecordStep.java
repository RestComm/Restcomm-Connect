package org.mobicents.servlet.restcomm.rvd.model.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.BuildService;
import org.mobicents.servlet.restcomm.rvd.RvdUtils;
import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlRecordStep;

public class RecordStep extends Step {
    static final Logger logger = Logger.getLogger(BuildService.class.getName());
    String next;
    String method;
    Integer timeout;
    String finishOnKey;
    Integer maxLength;
    Boolean transcribe;
    String transcribeCallback;
    public String getTranscribeCallback() {
        return transcribeCallback;
    }
    public void setTranscribeCallback(String transcribeCallback) {
        this.transcribeCallback = transcribeCallback;
    }
    Boolean playBeep;
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
    public Integer getTimeout() {
        return timeout;
    }
    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }
    public String getFinishOnKey() {
        return finishOnKey;
    }
    public void setFinishOnKey(String finishOnKey) {
        this.finishOnKey = finishOnKey;
    }
    public Integer getMaxLength() {
        return maxLength;
    }
    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }
    public Boolean getTranscribe() {
        return transcribe;
    }
    public void setTranscribe(Boolean transcribe) {
        this.transcribe = transcribe;
    }
    public Boolean getPlayBeep() {
        return playBeep;
    }
    public void setPlayBeep(Boolean playBeep) {
        this.playBeep = playBeep;
    }
    public RcmlRecordStep render(Interpreter interpreter) {
        RcmlRecordStep rcmlStep = new RcmlRecordStep();

        if ( ! RvdUtils.isEmpty(getNext()) ) {
            String newtarget = interpreter.getTarget().getNodename() + "." + getName() + ".actionhandler";
            Map<String, String> pairs = new HashMap<String, String>();
            pairs.put("target", newtarget);
            String action = interpreter.buildAction(pairs);
            rcmlStep.setAction(action);
            rcmlStep.setMethod(getMethod());
        }

        rcmlStep.setFinishOnKey(getFinishOnKey());
        rcmlStep.setMaxLength(getMaxLength());
        rcmlStep.setPlayBeep(getPlayBeep());
        rcmlStep.setTimeout(getTimeout());
        rcmlStep.setTranscribe(getTranscribe());
        rcmlStep.setTranscribeCallback(getTranscribeCallback());

        return rcmlStep;
    }
    public void handleAction(Interpreter interpreter) throws InterpreterException, IOException {
        logger.debug("handling record action");
        if ( RvdUtils.isEmpty(getNext()) )
            throw new InterpreterException( "'next' module is not defined for step " + getName() );

        String RecordingUrl = interpreter.getHttpRequest().getParameter("RecordingUrl");
        String RecordingDuration = interpreter.getHttpRequest().getParameter("RecordingDuration");
        String Digits = interpreter.getHttpRequest().getParameter("Digits");

        if ( RecordingUrl != null )
            interpreter.getVariables().put("RecordingUrl", RecordingUrl);
        if (RecordingDuration != null )
            interpreter.getVariables().put("RecordingDuration", RecordingDuration);
        if (Digits != null )
            interpreter.getVariables().put("Digits", Digits);

        interpreter.interpret( getNext(), null );
    }
}
