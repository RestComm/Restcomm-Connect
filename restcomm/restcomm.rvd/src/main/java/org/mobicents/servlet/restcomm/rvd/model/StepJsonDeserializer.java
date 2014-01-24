package org.mobicents.servlet.restcomm.rvd.model;

import java.lang.reflect.Type;

import org.mobicents.servlet.restcomm.rvd.model.client.DialStep;
import org.mobicents.servlet.restcomm.rvd.model.client.GatherStep;
import org.mobicents.servlet.restcomm.rvd.model.client.HungupStep;
import org.mobicents.servlet.restcomm.rvd.model.client.PlayStep;
import org.mobicents.servlet.restcomm.rvd.model.client.RedirectStep;
import org.mobicents.servlet.restcomm.rvd.model.client.RejectStep;
import org.mobicents.servlet.restcomm.rvd.model.client.SayStep;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;
import org.mobicents.servlet.restcomm.rvd.model.client.ExternalServiceStep;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class StepJsonDeserializer implements JsonDeserializer<Step> {

    @Override
    public Step deserialize(JsonElement rootElement, Type arg1, JsonDeserializationContext arg2) throws JsonParseException {

        JsonObject step_object = rootElement.getAsJsonObject();
        String kind = step_object.get("kind").getAsString();

        Gson gson = new GsonBuilder()
            .registerTypeAdapter(Step.class, new StepJsonDeserializer())
            //.registerTypeAdapter(AccessRawOperation.class, new AccessRawOperationJsonDeserializer())
            .create();

        Step step;
        if ("say".equals(kind))
            step = gson.fromJson(step_object, SayStep.class);
        else if ("gather".equals(kind))
            step = gson.fromJson(step_object, GatherStep.class);
        else if ("dial".equals(kind))
            step = gson.fromJson(step_object, DialStep.class);
        else if ("hungup".equals(kind))
            step = gson.fromJson(step_object, HungupStep.class);
        else if ("play".equals(kind))
            step = gson.fromJson(step_object, PlayStep.class);
        else if ("externalService".equals(kind))
            step = gson.fromJson(step_object, ExternalServiceStep.class);
        else if ("redirect".equals(kind))
            step = gson.fromJson(step_object, RedirectStep.class);
        else if ("reject".equals(kind))
            step = gson.fromJson(step_object, RejectStep.class);
        else {
            step = null;
            System.out.println("Error deserializing step. Unknown step found!"); // TODO remove me and return a nice value!!!
        }

        return step;
    }

}
