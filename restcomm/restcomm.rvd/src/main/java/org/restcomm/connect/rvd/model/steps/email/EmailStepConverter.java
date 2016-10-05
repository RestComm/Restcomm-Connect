package org.restcomm.connect.rvd.model.steps.email;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;


/**
 * Created by lefty on 6/24/15.
 */
public class EmailStepConverter implements Converter {

    @Override
    public boolean canConvert(Class elementClass) {
        return elementClass.equals(RcmlEmailStep.class);
    }

    @Override
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        RcmlEmailStep step = (RcmlEmailStep) value;
        if ( step.getTo() != null )
            writer.addAttribute("to", step.getTo());
        if (step.getFrom() != null )
            writer.addAttribute("from", step.getFrom());
        if (step.getCc() != null )
                writer.addAttribute("cc", step.getCc());
        if (step.getBcc() != null )
                writer.addAttribute("bcc", step.getBcc());
        if (step.getSubject() != null )
            writer.addAttribute("subject", step.getSubject());
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
