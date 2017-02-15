package org.restcomm.connect.rvd.model;

import java.lang.reflect.Type;

import org.restcomm.connect.rvd.model.client.Step;
import org.restcomm.connect.rvd.model.steps.control.ControlStep;
import org.restcomm.connect.rvd.model.steps.dial.DialStep;
import org.restcomm.connect.rvd.model.steps.email.EmailStep;
import org.restcomm.connect.rvd.model.steps.es.ExternalServiceStep;
import org.restcomm.connect.rvd.model.steps.fax.FaxStep;
import org.restcomm.connect.rvd.model.steps.gather.GatherStep;
import org.restcomm.connect.rvd.model.steps.hangup.HungupStep;
import org.restcomm.connect.rvd.model.steps.log.LogStep;
import org.restcomm.connect.rvd.model.steps.pause.PauseStep;
import org.restcomm.connect.rvd.model.steps.play.PlayStep;
import org.restcomm.connect.rvd.model.steps.record.RecordStep;
import org.restcomm.connect.rvd.model.steps.redirect.RedirectStep;
import org.restcomm.connect.rvd.model.steps.reject.RejectStep;
import org.restcomm.connect.rvd.model.steps.say.SayStep;
import org.restcomm.connect.rvd.model.steps.sms.SmsStep;
import org.restcomm.connect.rvd.model.steps.ussdcollect.UssdCollectStep;
import org.restcomm.connect.rvd.model.steps.ussdlanguage.UssdLanguageStep;
import org.restcomm.connect.rvd.model.steps.ussdsay.UssdSayStep;

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
        } else if (step.getClass().equals(ControlStep.class)) {
            resultElement = gson.toJsonTree((ControlStep) step);
        } else if (step.getClass().equals(ExternalServiceStep.class)) {
            resultElement = gson.toJsonTree((ExternalServiceStep) step);
        } else if (step.getClass().equals(LogStep.class)) {
            resultElement = gson.toJsonTree((LogStep) step);
        } else if (step.getClass().equals(DialStep.class)) {
            resultElement = gson.toJsonTree((DialStep) step);
        } else if (step.getClass().equals(HungupStep.class)) {
            resultElement = gson.toJsonTree((HungupStep) step);
        } else if (step.getClass().equals(RedirectStep.class)) {
            resultElement = gson.toJsonTree((RedirectStep) step);
        } else if (step.getClass().equals(RejectStep.class)) {
            resultElement = gson.toJsonTree((RejectStep) step);
        } else if (step.getClass().equals(PauseStep.class)) {
            resultElement = gson.toJsonTree((PauseStep) step);
        } else if (step.getClass().equals(SmsStep.class)) {
            resultElement = gson.toJsonTree((SmsStep) step);
        } else if (step.getClass().equals(EmailStep.class)) {
            resultElement = gson.toJsonTree((EmailStep) step);
        } else if (step.getClass().equals(RecordStep.class)) {
            resultElement = gson.toJsonTree((RecordStep) step);
        } else if (step.getClass().equals(FaxStep.class)) {
            resultElement = gson.toJsonTree((FaxStep) step);
        } else if (step.getClass().equals(UssdSayStep.class)) {
            resultElement = gson.toJsonTree((UssdSayStep) step);
        } else if (step.getClass().equals(UssdCollectStep.class)) {
            resultElement = gson.toJsonTree((UssdCollectStep) step);
        } else if (step.getClass().equals(UssdLanguageStep.class)) {
            resultElement = gson.toJsonTree((UssdLanguageStep) step);
        }

        return resultElement;
    }

}
