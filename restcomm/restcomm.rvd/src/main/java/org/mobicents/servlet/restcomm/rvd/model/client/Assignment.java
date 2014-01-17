package org.mobicents.servlet.restcomm.rvd.model.client;

import java.util.List;

public class Assignment {
    private String destVariable;
    //private List<JsonElement> accessOperations;
    private List<AccessOperation> accessOperations;
    //private AccessRawOperation JsonElement;
    private AccessOperation lastOperation;
    public String getDestVariable() {
        return destVariable;
    }
    public void setDestVariable(String destVariable) {
        this.destVariable = destVariable;
    }
    public List<AccessOperation> getAccessOperations() {
        return accessOperations;
    }
    public void setAccessOperations(List<AccessOperation> accessOperations) {
        this.accessOperations = accessOperations;
    }
    public AccessOperation getLastOperation() {
        return lastOperation;
    }
    public void setLastOperation(AccessOperation lastOperation) {
        this.lastOperation = lastOperation;
    }
}
