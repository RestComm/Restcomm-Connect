package org.mobicents.servlet.restcomm.rvd.model.client;


public class Step {
	
	private String kind;
	private String label;
	private String title;
	private Boolean isCollapsed;
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
	public Boolean getIsCollapsed() {
		return isCollapsed;
	}
	public void setIsCollapsed(Boolean isCollapsed) {
		this.isCollapsed = isCollapsed;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	
}

