package org.restcomm.connect.rvd.model;

import java.lang.reflect.Type;

import org.restcomm.connect.rvd.model.client.Step;
import org.restcomm.connect.rvd.model.packaging.RappInfo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.xstream.XStream;

public class ModelMarshaler {

    private Gson gsonUtil;
    private XStream xstream;

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

    public <T> T toModel( String jsonData, Type gsonType ) {
        T instance = gsonUtil.fromJson(jsonData, gsonType);
        return instance;
    }

    public String toData( Object model ) {
        return gsonUtil.toJson(model);
    }

    public Gson getGson() {
        return gsonUtil;
    }

    // lazy singleton function
    public XStream getXStream() {
        if (xstream == null) {
            xstream = new XStream();
            xstream.alias("restcommApplication", RappInfo.class);
        }
        return xstream;
    }

}
