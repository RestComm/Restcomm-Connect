package org.mobicents.servlet.restcomm.rvd.model.client;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.interpreter.Interpreter;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlPlayStep;
import org.mobicents.servlet.restcomm.rvd.model.rcml.RcmlStep;
import org.mobicents.servlet.restcomm.rvd.BuildService;
import org.mobicents.servlet.restcomm.rvd.ProjectService;

public class PlayStep extends Step {
    static final Logger logger = Logger.getLogger(BuildService.class.getName());
    private String wavUrl;
    private String wavLocalFilename;
    private Integer loop;
    private String playType;

    public String getWavUrl() {
        return wavUrl;
    }

    public void setWavUrl(String wavUrl) {
        this.wavUrl = wavUrl;
    }

    public String getWavLocalFilename() {
        return wavLocalFilename;
    }

    public void setWavLocalFilename(String wavLocalFilename) {
        this.wavLocalFilename = wavLocalFilename;
    }

    public Integer getLoop() {
        return loop;
    }

    public void setLoop(Integer loop) {
        this.loop = loop;
    }

    public String getPlayType() {
        return playType;
    }

    public void setPlayType(String playType) {
        this.playType = playType;
    }
    @Override
    public RcmlStep render(Interpreter interpreter) {
        RcmlPlayStep playStep = new RcmlPlayStep();
        String url = "";
        if ("local".equals(getPlayType()))
            url = interpreter.getHttpRequest().getContextPath() + "/" + ProjectService.getWorkspacedirectoryname() + "/" + interpreter.getAppName() + "/" + ProjectService.getWavsdirectoryname() + "/" + getWavLocalFilename();
        else
            url = getWavUrl();

        logger.debug("play url: " + url);
        playStep.setWavurl(url);
        playStep.setLoop(getLoop());

        return playStep;
    }

}
