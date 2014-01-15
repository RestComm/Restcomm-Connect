package org.mobicents.servlet.restcomm.rvd.model.client;

import java.util.List;
import com.google.gson.JsonElement;

public class Assignment {
    private String destVariable;
    private List<JsonElement> accessOperations;
    private AccessRawOperation JsonElement;

    public String getDestVariable() {
        return destVariable;
    }
    public void setDestVariable(String destVariable) {
        this.destVariable = destVariable;
    }
    public List<JsonElement> getAccessOperations() {
        return accessOperations;
    }
    public void setAccessOperations(List<JsonElement> accessOperations) {
        this.accessOperations = accessOperations;
    }
    public AccessRawOperation getJsonElement() {
        return JsonElement;
    }
    public void setJsonElement(AccessRawOperation jsonElement) {
        JsonElement = jsonElement;
    }
}
