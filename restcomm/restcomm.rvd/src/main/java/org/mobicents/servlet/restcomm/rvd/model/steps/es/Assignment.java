package org.mobicents.servlet.restcomm.rvd.model.steps.es;


public class Assignment {
    private String destVariable;
    private String moduleNameScope;
    private ValueExtractor valueExtractor;

    public String getDestVariable() {
        return destVariable;
    }
    public void setDestVariable(String destVariable) {
        this.destVariable = destVariable;
    }
    public ValueExtractor getValueExtractor() {
        return valueExtractor;
    }
    public void setValueExtractor(ValueExtractor valueExtractor) {
        this.valueExtractor = valueExtractor;
    }
    public String getModuleNameScope() {
        return moduleNameScope;
    }
    public void setModuleNameScope(String moduleNameScope) {
        this.moduleNameScope = moduleNameScope;
    }
}
