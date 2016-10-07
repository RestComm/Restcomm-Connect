package org.restcomm.connect.rvd.model.steps.play;

import java.net.URISyntaxException;

import org.apache.http.client.utils.URIBuilder;
import org.apache.log4j.Logger;
import org.restcomm.connect.rvd.interpreter.Interpreter;
import org.restcomm.connect.rvd.model.client.Step;
import org.restcomm.connect.rvd.model.rcml.RcmlStep;
import org.restcomm.connect.rvd.BuildService;

public class PlayStep extends Step {
    static final Logger logger = Logger.getLogger(BuildService.class.getName());
    private Integer loop;
    private String playType;
    private Local local;
    private Remote remote;

    public final class Local {
        private String wavLocalFilename;
    }
    public final class Remote {
        private String wavUrl;
    }


    @Override
    public RcmlStep render(Interpreter interpreter) {
        RcmlPlayStep playStep = new RcmlPlayStep();
        String url = "";
        if ("local".equals(playType)) {
            String rawurl = interpreter.getContextPath() + "/services/projects/" + interpreter.getAppName() + "/wavs/" + local.wavLocalFilename;
            try {
                URIBuilder uribuilder = new URIBuilder();
                uribuilder.setPath(rawurl);
                url = uribuilder.build().toString();
            } catch (URISyntaxException e) {
                logger.warn("Error parsing url for play verb: " + rawurl, e);
                url = rawurl; // best effort
            }
        }
        else {
            url = interpreter.populateVariables(remote.wavUrl);
        }

        if(logger.isDebugEnabled()) {
            logger.debug("play url: " + url);
        }
        playStep.setWavurl(url);
        playStep.setLoop(loop);

        return playStep;
    }

}
