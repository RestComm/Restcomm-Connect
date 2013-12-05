package org.mobicents.servlet.restcomm.rvd.model;

import java.util.ArrayList;
import java.util.List;


public class RcmlGatherStep extends RcmlStep {
	private String action;
	private String method;
	private Integer timeout;
	private String finishOnKey;
	private Integer numDigits;
	
	private List<RcmlStep> steps = new ArrayList<RcmlStep>();

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public List<RcmlStep> getSteps() {
		return steps;
	}

	public void setSteps(List<RcmlStep> steps) {
		this.steps = steps;
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

	public Integer getNumDigits() {
		return numDigits;
	}

	public void setNumDigits(Integer numDigits) {
		this.numDigits = numDigits;
	}


	
	

}
