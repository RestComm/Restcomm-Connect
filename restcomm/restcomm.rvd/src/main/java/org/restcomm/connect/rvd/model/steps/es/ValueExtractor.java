package org.restcomm.connect.rvd.model.steps.es;

import java.util.List;

public class ValueExtractor {
    private List<AccessOperation> accessOperations;
    private AccessOperation lastOperation;
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
