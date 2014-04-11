package org.mobicents.servlet.restcomm.rvd.model.steps.dial;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.RvdUtils;
import org.mobicents.servlet.restcomm.rvd.exceptions.InterpreterException;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;
import org.mobicents.servlet.restcomm.rvd.storage.exceptions.StorageException;

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
        rcmlStep.callerId = callerId;
        rcmlStep.record = record;

        return rcmlStep;
    }

    /**
     * Converts a file resource to a recorded wav file into an http resource accessible over HTTP. The path generated path for the wav files is hardcoded to /restcomm/recordings
     * @param fileResource
     * @param interpreter
     * @return
     */
    private String convertRecordingFileResourceHttp(String fileResource, HttpServletRequest request) throws URISyntaxException {
        String httpResource = fileResource; // assume this is already an http resource

        URIBuilder fileUriBuilder = new URIBuilder(fileResource);

        if ( ! fileUriBuilder.isAbsolute() ) {
            logger.warn("Cannot convert file URL to http URL - " + fileResource);
            return "";
        }

        if ( fileUriBuilder.getScheme().startsWith("http") ) // http or https - nothing to worry about
            return fileResource;

        if ( fileUriBuilder.getScheme().startsWith("file") ) {
            String wavFilename = "";
            int filenameBeforeStartPos = fileResource.lastIndexOf('/');
            if ( filenameBeforeStartPos != -1 ) {
                wavFilename = fileResource.substring(filenameBeforeStartPos+1);
                URIBuilder httpUriBuilder = new URIBuilder().setScheme(request.getScheme()).setHost(request.getServerName()).setPort(request.getServerPort()).setPath("/restcomm/recordings/" + wavFilename);
                httpResource = httpUriBuilder.build().toString();
            }
        }

        return httpResource;
    }

    public void handleAction(Interpreter interpreter) throws InterpreterException, StorageException {
        logger.debug("handling dial action");
        if ( RvdUtils.isEmpty(nextModule) )
            throw new InterpreterException( "'next' module is not defined for step " + getName() );

        String restcommRecordingUrl = interpreter.getRequestParams().getFirst("RecordingUrl");
        if ( restcommRecordingUrl != null ) {
            try {
                String recordingUrl = convertRecordingFileResourceHttp(restcommRecordingUrl, interpreter.getHttpRequest());
                interpreter.getVariables().put("core_RecordingUrl", recordingUrl);
            } catch (URISyntaxException e) {
                logger.warn("Cannot convert file URL to http URL - " + restcommRecordingUrl, e);
            }
        }

        String DialCallStatus = interpreter.getRequestParams().getFirst("DialCallStatus");
        if ( DialCallStatus != null )
            interpreter.getVariables().put("core_DialCallStatus", DialCallStatus);

        String DialCallSid = interpreter.getRequestParams().getFirst("DialCallSid");
        if ( DialCallSid != null )
            interpreter.getVariables().put("core_DialCallSid", DialCallSid);

        String DialCallDuration = interpreter.getRequestParams().getFirst("DialCallDuration");
        if ( DialCallDuration != null )
            interpreter.getVariables().put("core_DialCallDuration", DialCallDuration);

        interpreter.interpret( nextModule, null );
    }

}
