package org.restcomm.connect.rvd.model.steps.say;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class SayStepConverter implements Converter {

    @Override
    public boolean canConvert(Class elementClass) {
        return elementClass.equals(RcmlSayStep.class);
    }

    @Override
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        RcmlSayStep step = (RcmlSayStep) value;

        if (step.getLoop() != null)
            writer.addAttribute("loop", step.getLoop().toString());
        if (step.getLanguage() != null)
            writer.addAttribute("language", step.getLanguage());
        if (step.getVoice() != null)
            writer.addAttribute("voice", step.getVoice());
        writer.setValue(step.getPhrase());
    }

    // will not need this for now
    @Override
    public Object unmarshal(HierarchicalStreamReader arg0, UnmarshallingContext arg1) {
        // TODO Auto-generated method stub
        return null;
    }

}
