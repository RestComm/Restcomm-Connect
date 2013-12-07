package org.mobicents.servlet.restcomm.rvd.model.client;

public class PlayStep extends Step {
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
	
}
