package org.mobicents.servlet.restcomm.rvd.model;

import org.mobicents.servlet.restcomm.rvd.model.client.Step;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ModelMarshaler {

    private Gson gsonUtil;

    public ModelMarshaler() {
        gsonUtil = new GsonBuilder()
            .registerTypeAdapter(Step.class, new StepJsonDeserializer())
            .registerTypeAdapter(Step.class, new StepJsonSerializer())
        .create();
    }

    public <T> T toModel( String jsonData, Class<T> modelClass ) {
        T instance = gsonUtil.fromJson(jsonData, modelClass);
        return instance;
    }

    public String toData( Object model ) {
        return gsonUtil.toJson(model);
    }

    public Gson getGson() {
        return gsonUtil;
    }

}
