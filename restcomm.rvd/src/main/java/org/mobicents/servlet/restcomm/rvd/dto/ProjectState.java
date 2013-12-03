package org.mobicents.servlet.restcomm.rvd.dto;

import java.util.List;
import java.util.Map;

public class ProjectState {
	
	private String startNodeName;
	private Integer lastStepId;
	private List<Node> nodes;
	private Integer activeNode;
	private Integer lastNodeId;
	
	public static class Node {
		private String name;
		private String label;
		private Map<String,Step> steps;
		private List<String> stepnames;
		private NodeIface iface;
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getLabel() {
			return label;
		}
		public void setLabel(String label) {
			this.label = label;
		}
		public Map<String, Step> getSteps() {
			return steps;
		}
		public void setSteps(Map<String, Step> steps) {
			this.steps = steps;
		}
		public List<String> getStepnames() {
			return stepnames;
		}
		public void setStepnames(List<String> stepnames) {
			this.stepnames = stepnames;
		}
		public NodeIface getIface() {
			return iface;
		}
		public void setIface(NodeIface iface) {
			this.iface = iface;
		}
	}
	
	public static class Step {
		private String kind;
		private String label;
		private String title;
		private String phrase;
		private String voice;
		private String language;
		private Integer loop;
		private Boolean isCollapsed;
		private StepIface iface;
		private String name;
		
		
		public String getKind() {
			return kind;
		}
		public void setKind(String kind) {
			this.kind = kind;
		}
		public String getLabel() {
			return label;
		}
		public void setLabel(String label) {
			this.label = label;
		}
		public String getTitle() {
			return title;
		}
		public void setTitle(String title) {
			this.title = title;
		}
		public String getPhrase() {
			return phrase;
		}
		public void setPhrase(String phrase) {
			this.phrase = phrase;
		}
		public String getVoice() {
			return voice;
		}
		public void setVoice(String voice) {
			this.voice = voice;
		}
		public String getLanguage() {
			return language;
		}
		public void setLanguage(String language) {
			this.language = language;
		}
		public Integer getLoop() {
			return loop;
		}
		public void setLoop(Integer loop) {
			this.loop = loop;
		}
		public Boolean getIsCollapsed() {
			return isCollapsed;
		}
		public void setIsCollapsed(Boolean isCollapsed) {
			this.isCollapsed = isCollapsed;
		}
		public StepIface getIface() {
			return iface;
		}
		public void setIface(StepIface iface) {
			this.iface = iface;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
	}
	
	public static class NodeIface {
		private Boolean edited;
		private Boolean editLabel;
	}
	
	public static class StepIface {
		private Boolean optionsVisible;
	}
	
	
	
	

	public String getStartNodeName() {
		return startNodeName;
	}

	public void setStartNodeName(String startNodeName) {
		this.startNodeName = startNodeName;
	}

	public Integer getLastStepId() {
		return lastStepId;
	}

	public void setLastStepId(Integer lastStepId) {
		this.lastStepId = lastStepId;
	}

	public List<Node> getNodes() {
		return nodes;
	}

	public void setNodes(List<Node> nodes) {
		this.nodes = nodes;
	}

	public Integer getActiveNode() {
		return activeNode;
	}

	public void setActiveNode(Integer activeNode) {
		this.activeNode = activeNode;
	}

	public Integer getLastNodeId() {
		return lastNodeId;
	}

	public void setLastNodeId(Integer lastNodeId) {
		this.lastNodeId = lastNodeId;
	}
	
	
	
/*	
	"startNodeName":"start",
	"lastStepId":3,
	"nodes":
	[
		{
			"name":"start",
			"label":"Welcome",
			"steps":
			{
				"step1":
				{
					"kind":"say",
					"label":"say",
					"title":"say",
					"phrase":"welcome to Restcomm Service Visual Designer",
					"voice":"man",
					"language":"bf",
					"loop":1,
					"isCollapsed":false,
					"iface":{"optionsVisible":false},
					"name":"step1"
				}
			},
			"stepnames":["step1"],
			"iface":{"edited":false,"editLabel":false}
		}
	],
	"activeNode":0,
	"lastNodeId":0,
*/

}
