package org.mobicents.servlet.restcomm.rvd.model;

import java.lang.reflect.Type;

import org.mobicents.servlet.restcomm.rvd.model.client.GatherStep;
import org.mobicents.servlet.restcomm.rvd.model.client.PlayStep;
import org.mobicents.servlet.restcomm.rvd.model.client.SayStep;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class StepJsonSerializer implements JsonSerializer<Step> {

    @Override
    public JsonElement serialize(Step step, Type arg1, JsonSerializationContext context) {

        Gson gson = new GsonBuilder().registerTypeAdapter(Step.class, new StepJsonSerializer()).create();
        JsonElement resultElement = null; // TODO update this default value to something or throw an exception or something
        if (step.getClass().equals(SayStep.class)) {
            resultElement = gson.toJsonTree((SayStep) step);
        } else if (step.getClass().equals(PlayStep.class)  ) {
            resultElement = gson.toJsonTree((PlayStep) step);
        } else if (step.getClass().equals(GatherStep.class)) {
            resultElement = gson.toJsonTree((GatherStep) step);
        }

        return resultElement;
    }

}
