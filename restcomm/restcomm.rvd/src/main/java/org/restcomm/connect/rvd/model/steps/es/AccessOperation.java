package org.restcomm.connect.rvd.model.steps.es;

public class AccessOperation {
    private String kind;
    private Boolean fixed;
    private Boolean terminal;
    private String expression;
    private String action;    // object, array
    private String property;  // object,propertyNamed
    private Integer position;  // array,itemAtPosition

    public String getKind() {
        return kind;
    }
    public void setKind(String kind) {
        this.kind = kind;
    }
    public Boolean getFixed() {
        return fixed;
    }
    public void setFixed(Boolean fixed) {
        this.fixed = fixed;
    }
    public Boolean getTerminal() {
        return terminal;
    }
    public void setTerminal(Boolean terminal) {
        this.terminal = terminal;
    }
    public String getExpression() {
        return expression;
    }
    public void setExpression(String expression) {
        this.expression = expression;
    }
    public String getAction() {
        return action;
    }
    public void setAction(String action) {
        this.action = action;
    }
    public String getProperty() {
        return property;
    }
    public void setProperty(String property) {
        this.property = property;
    }
    public Integer getPosition() {
        return position;
    }
    public void setPosition(Integer position) {
        this.position = position;
    }
}
