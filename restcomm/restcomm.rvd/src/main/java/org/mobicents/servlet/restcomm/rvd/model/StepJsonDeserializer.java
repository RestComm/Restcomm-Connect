package org.mobicents.servlet.restcomm.rvd.model;

import java.lang.reflect.Type;

import org.apache.log4j.Logger;
import org.mobicents.servlet.restcomm.rvd.BuildService;
import org.mobicents.servlet.restcomm.rvd.model.client.Step;
import org.mobicents.servlet.restcomm.rvd.model.steps.dial.DialNoun;
import org.mobicents.servlet.restcomm.rvd.model.steps.dial.DialNounJsonDeserializer;
import org.mobicents.servlet.restcomm.rvd.model.steps.dial.DialStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.es.ExternalServiceStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.fax.FaxStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.gather.GatherStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.hangup.HungupStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.pause.PauseStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.play.PlayStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.record.RecordStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.redirect.RedirectStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.reject.RejectStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.say.SayStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.sms.SmsStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.ussdcollect.UssdCollectStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.ussdlanguage.UssdLanguageStep;
import org.mobicents.servlet.restcomm.rvd.model.steps.ussdsay.UssdSayStep;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

public class StepJsonDeserializer implements JsonDeserializer<Step> {
    static final Logger logger = Logger.getLogger(BuildService.class.getName());

    @Override
    public Step deserialize(JsonElement rootElement, Type arg1, JsonDeserializationContext arg2) throws JsonParseException {

        JsonObject step_object = rootElement.getAsJsonObject();
        String kind = step_object.get("kind").getAsString();

        Gson gson = new GsonBuilder()
            .registerTypeAdapter(Step.class, new StepJsonDeserializer())
            .registerTypeAdapter(DialNoun.class, new DialNounJsonDeserializer())
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
        else if ("pause".equals(kind))
            step = gson.fromJson(step_object, PauseStep.class);
        else if ("sms".equals(kind))
            step = gson.fromJson(step_object, SmsStep.class);
        else if ("record".equals(kind))
            step = gson.fromJson(step_object, RecordStep.class);
        else if ("fax".equals(kind))
            step = gson.fromJson(step_object, FaxStep.class);
        else if ("ussdSay".equals(kind))
            step = gson.fromJson(step_object, UssdSayStep.class);
        else if ("ussdCollect".equals(kind))
            step = gson.fromJson(step_object, UssdCollectStep.class);
        else if ("ussdLanguage".equals(kind))
            step = gson.fromJson(step_object, UssdLanguageStep.class);
        else {
            step = null;
            logger.error("Error deserializing step. Unknown step found!"); // TODO remove me and return a nice value!!!
        }

        return step;
    }

}
