package org.mobicents.servlet.restcomm.rvd.model.steps.play;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlStep;
import org.mobicents.servlet.restcomm.rvd.BuildService;

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
            url = interpreter.getContextPath() + "/services/apps/" + interpreter.getAppName() + "/resources/" + local.wavLocalFilename;
            //url = interpreter.getContextPath() + "/services/manager/projects/getwav?name=" + interpreter.getAppName() + "&filename=" + local.wavLocalFilename;
        }
        else
            url = interpreter.populateVariables(remote.wavUrl);

        logger.debug("play url: " + url);
        playStep.setWavurl(url);
        playStep.setLoop(loop);

        return playStep;
    }

}
