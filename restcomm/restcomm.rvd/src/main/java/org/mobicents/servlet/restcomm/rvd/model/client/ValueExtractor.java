package org.mobicents.servlet.restcomm.rvd.model.client;

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
