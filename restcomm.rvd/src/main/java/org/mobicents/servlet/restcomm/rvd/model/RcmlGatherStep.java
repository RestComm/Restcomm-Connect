package org.mobicents.servlet.restcomm.rvd.model;

import java.util.ArrayList;
import java.util.List;

import org.mobicents.servlet.restcomm.rvd.dto.Step;

public class RcmlGatherStep extends RcmlStep {
	private String action;
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


	
	

}
