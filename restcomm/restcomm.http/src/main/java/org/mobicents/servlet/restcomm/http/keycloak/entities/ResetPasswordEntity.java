package org.mobicents.servlet.restcomm.http.keycloak.entities;

public class ResetPasswordEntity {

    public ResetPasswordEntity() {
        // TODO Auto-generated constructor stub
    }
    private String type;
    private String value;
    private Boolean temporary;
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getValue() {
        return value;
    }
    public void setValue(String talue) {
        this.value = talue;
    }
    public Boolean getTemporary() {
        return temporary;
    }
    public void setTemporary(Boolean temporary) {
        this.temporary = temporary;
    }
}
