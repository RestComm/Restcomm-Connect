package org.restcomm.connect.rvd.model.steps.es;

import com.google.gson.JsonElement;

public class AccessRawOperation {
    JsonElement rawOperation;

    //private String kind;
    //private Boolean fixed;
    //private Boolean terminal;
    //private String expression;
    //private String action;    // object, array
    //private String property;  // object,propertyNamed
    //private String position;  // array,itemAtPosition

    public JsonElement getRawOperation() {
        return rawOperation;
    }

    public void setRawOperation(JsonElement rawOperation) {
        this.rawOperation = rawOperation;
    }
}
