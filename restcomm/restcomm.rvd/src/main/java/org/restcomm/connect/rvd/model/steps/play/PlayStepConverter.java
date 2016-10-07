package org.restcomm.connect.rvd.model.steps.play;

import org.apache.log4j.Logger;
import org.restcomm.connect.rvd.BuildService;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class PlayStepConverter implements Converter {
    static final Logger logger = Logger.getLogger(BuildService.class.getName());
    @Override
    public boolean canConvert(Class elementClass) {
        return elementClass.equals(RcmlPlayStep.class);
    }

    @Override
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        RcmlPlayStep step = (RcmlPlayStep) value;
        if (step.getLoop() != null)
            writer.addAttribute("loop", step.getLoop().toString());

        //System.out.println( "getloop: " + step.getLoop() );
        if ( step.getWavurl() != null )
            writer.setValue(step.getWavurl());
    }

    // will not need this for now
    @Override
    public Object unmarshal(HierarchicalStreamReader arg0, UnmarshallingContext arg1) {
        // TODO Auto-generated method stub
        return null;
    }
}
