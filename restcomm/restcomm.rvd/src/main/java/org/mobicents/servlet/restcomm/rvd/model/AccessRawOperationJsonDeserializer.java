package org.mobicents.servlet.restcomm.rvd.model;

import java.lang.reflect.Type;

import org.mobicents.servlet.restcomm.rvd.model.client.AccessRawOperation;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class AccessRawOperationJsonDeserializer implements JsonDeserializer<AccessRawOperation> {

    @Override
    public AccessRawOperation deserialize(JsonElement element, Type arg1, JsonDeserializationContext arg2)
            throws JsonParseException {
        AccessRawOperation operation = new AccessRawOperation();
        operation.setRawOperation(element);

        return operation;
    }

}
