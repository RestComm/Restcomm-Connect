package org.restcomm.connect.rvd.model.steps.dial;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class ConferenceNounConverter implements Converter {

    @Override
    public boolean canConvert(Class elementClass) {
        return elementClass.equals(RcmlConferenceNoun.class);
    }

    @Override
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        RcmlConferenceNoun step = (RcmlConferenceNoun) value;

        if (step.getBeep() != null)
            writer.addAttribute("beep", step.getBeep().toString());
        if (step.getMuted() != null)
            writer.addAttribute("muted", step.getMuted().toString());
        if (step.getEndConferenceOnExit() != null)
            writer.addAttribute("endConferenceOnExit", step.getEndConferenceOnExit().toString());
        if (step.getStartConferenceOnEnter() != null)
            writer.addAttribute("startConferenceOnEnter", step.getStartConferenceOnEnter().toString());
        if (step.getMaxParticipants() != null)
            writer.addAttribute("maxParticipants", step.getMaxParticipants().toString());
        if (step.getWaitUrl() != null)
            writer.addAttribute("waitUrl", step.getWaitUrl());
        if (step.getWaitMethod() != null)
            writer.addAttribute("waitMethod", step.getWaitMethod());

        writer.setValue(step.getDestination());
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader arg0, UnmarshallingContext arg1) {
        // TODO Auto-generated method stub
        return null;
    }

}
