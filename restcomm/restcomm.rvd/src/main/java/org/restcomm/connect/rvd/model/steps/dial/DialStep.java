package org.restcomm.connect.rvd.model.steps.dial;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.restcomm.connect.rvd.RvdConfiguration;
import org.restcomm.connect.rvd.utils.RvdUtils;
import org.restcomm.connect.rvd.exceptions.InterpreterException;
import org.restcomm.connect.rvd.interpreter.Interpreter;
import org.restcomm.connect.rvd.interpreter.Target;
import org.restcomm.connect.rvd.model.client.Step;
import org.restcomm.connect.rvd.storage.exceptions.StorageException;

public class DialStep extends Step {
    static final Logger logger = Logger.getLogger(DialStep.class.getName());

    private List<DialNoun> dialNouns;
    private String action;
    private String method;
    private Integer timeout;
    private Integer timeLimit;
    private String callerId;
    private String nextModule;
    private Boolean record;

    public RcmlDialStep render(Interpreter interpreter) throws InterpreterException {
        RcmlDialStep rcmlStep = new RcmlDialStep();

        for ( DialNoun noun: dialNouns ) {
            rcmlStep.nouns.add( noun.render(interpreter) );
        }

        if ( ! RvdUtils.isEmpty(nextModule) ) {
            String newtarget = interpreter.getTarget().getNodename() + "." + getName() + ".actionhandler";
            Map<String, String> pairs = new HashMap<String, String>();
            pairs.put("target", newtarget);
            String action = interpreter.buildAction(pairs);
            rcmlStep.action = action;
            rcmlStep.method = method;
        }

        rcmlStep.timeout = timeout == null ? null : timeout.toString();
        rcmlStep.timeLimit = (timeLimit == null ? null : timeLimit.toString());
        rcmlStep.callerId = interpreter.populateVariables(callerId);
        rcmlStep.record = record;

        return rcmlStep;
    }

    @Override
    public void handleAction(Interpreter interpreter, Target originTarget) throws InterpreterException, StorageException {
        if(logger.isInfoEnabled()) {
            logger.info("handling dial action");
        }
        if ( RvdUtils.isEmpty(nextModule) )
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

        String DialCallStatus = interpreter.getRequestParams().getFirst("DialCallStatus");
        if ( DialCallStatus != null )
            interpreter.getVariables().put(RvdConfiguration.CORE_VARIABLE_PREFIX + "DialCallStatus", DialCallStatus);

        String DialCallSid = interpreter.getRequestParams().getFirst("DialCallSid");
        if ( DialCallSid != null )
            interpreter.getVariables().put(RvdConfiguration.CORE_VARIABLE_PREFIX + "DialCallSid", DialCallSid);

        String DialCallDuration = interpreter.getRequestParams().getFirst("DialCallDuration");
        if ( DialCallDuration != null )
            interpreter.getVariables().put(RvdConfiguration.CORE_VARIABLE_PREFIX + "DialCallDuration", DialCallDuration);

        String DialRingDuration = interpreter.getRequestParams().getFirst("DialRingDuration");
        if ( DialRingDuration != null )
            interpreter.getVariables().put(RvdConfiguration.CORE_VARIABLE_PREFIX + "DialRingDuration", DialRingDuration);

        interpreter.interpret( nextModule, null, null, originTarget );
    }

}
