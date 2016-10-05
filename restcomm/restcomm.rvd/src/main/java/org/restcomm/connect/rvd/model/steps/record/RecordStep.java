package org.restcomm.connect.rvd.model.steps.record;

import java.net.URISyntaxException;
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

    @Override
    public void handleAction(Interpreter interpreter, Target originTarget) throws InterpreterException, StorageException {
        if(logger.isInfoEnabled()) {
            logger.info("handling record action");
        }
        if ( RvdUtils.isEmpty(getNext()) )
            throw new InterpreterException( "'next' module is not defined for step " + getName() );

        String publicRecordingUrl = interpreter.getRequestParams().getFirst("PublicRecordingUrl");
        if ( publicRecordingUrl != null ) {
            interpreter.getVariables().put(RvdConfiguration.CORE_VARIABLE_PREFIX + "PublicRecordingUrl", publicRecordingUrl);
        }

        String restcommRecordingUrl = interpreter.getRequestParams().getFirst("RecordingUrl");
        if ( restcommRecordingUrl != null ) {
            try {
                String recordingUrl = interpreter.convertRecordingFileResourceHttp(restcommRecordingUrl, interpreter.getHttpRequest());
                interpreter.getVariables().put(RvdConfiguration.CORE_VARIABLE_PREFIX + "RecordingUrl", recordingUrl);
            } catch (URISyntaxException e) {
                logger.warn("Cannot convert file URL to http URL - " + restcommRecordingUrl, e);
            }
        }

        String RecordingDuration = interpreter.getRequestParams().getFirst("RecordingDuration");
        if (RecordingDuration != null )
            interpreter.getVariables().put(RvdConfiguration.CORE_VARIABLE_PREFIX + "RecordingDuration", RecordingDuration);

        String Digits = interpreter.getRequestParams().getFirst("Digits");
        if (Digits != null )
            interpreter.getVariables().put(RvdConfiguration.CORE_VARIABLE_PREFIX + "Digits", Digits);

        interpreter.interpret( getNext(), null, null, originTarget );
    }
}
