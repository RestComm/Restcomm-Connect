package org.restcomm.connect.rvd.model.steps.dial;

import java.lang.reflect.Type;

import org.apache.log4j.Logger;
import org.restcomm.connect.rvd.BuildService;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class DialNounJsonSerializer implements JsonSerializer<DialNoun> {
    static final Logger logger = Logger.getLogger(BuildService.class.getName());
    @Override
    public JsonElement serialize(DialNoun noun, Type arg1, JsonSerializationContext arg2) {
        Gson gson = new GsonBuilder().registerTypeAdapter(DialNoun.class, new DialNounJsonSerializer()).create();
        JsonElement resultElement = null; // TODO update this default value to something or throw an exception or something
        if (noun.getClass().equals(NumberDialNoun.class)) {
            resultElement = gson.toJsonTree((NumberDialNoun) noun);
        } else if (noun.getClass().equals(ClientDialNoun.class)  ) {
            resultElement = gson.toJsonTree((ClientDialNoun) noun);
        } else if (noun.getClass().equals(ConferenceDialNoun.class)  ) {
            resultElement = gson.toJsonTree((ConferenceDialNoun) noun);
        } else if (noun.getClass().equals(SipuriDialNoun.class)  ) {
            resultElement = gson.toJsonTree((SipuriDialNoun) noun);
        }

        return resultElement;
    }

}
