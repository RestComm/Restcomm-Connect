package org.restcomm.connect.rvd.model.steps.sms;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class SmsStepConverter implements Converter {


    @Override
    public boolean canConvert(Class elementClass) {
        return elementClass.equals(RcmlSmsStep.class);
    }

    @Override
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        RcmlSmsStep step = (RcmlSmsStep) value;
        if ( step.getTo() != null )
            writer.addAttribute("to", step.getTo());
        if (step.getFrom() != null )
            writer.addAttribute("from", step.getFrom());
        if ( step.getStatusCallback() != null )
            writer.addAttribute("statusCallback", step.getStatusCallback());
        if (step.getMethod() != null )
            writer.addAttribute("method", step.getMethod());
        if (step.getAction() != null )
            writer.addAttribute("action", step.getAction());

        writer.setValue(step.getText());
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader arg0, UnmarshallingContext arg1) {
        // TODO Auto-generated method stub
        return null;
    }

}
