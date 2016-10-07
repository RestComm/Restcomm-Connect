package org.restcomm.connect.rvd.model.steps.ussdsay;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class UssdSayStepConverter implements Converter {

    public UssdSayStepConverter() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean canConvert(Class elementClass) {
        return elementClass.equals(UssdSayRcml.class);
    }

    @Override
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext arg2) {
        UssdSayRcml step = (UssdSayRcml) value;
        writer.setValue(step.text);
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader arg0, UnmarshallingContext arg1) {
        // TODO Auto-generated method stub
        return null;
    }

}
