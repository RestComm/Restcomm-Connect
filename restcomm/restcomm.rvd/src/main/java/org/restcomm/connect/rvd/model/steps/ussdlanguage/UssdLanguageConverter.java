package org.restcomm.connect.rvd.model.steps.ussdlanguage;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

public class UssdLanguageConverter implements Converter {

    public UssdLanguageConverter() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public boolean canConvert(Class elementClass) {
        return elementClass.equals(UssdLanguageRcml.class);
    }

    @Override
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext arg2) {
        UssdLanguageRcml step = (UssdLanguageRcml) value;
        if ( step.language != null )
            writer.setValue(step.language);
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader arg0, UnmarshallingContext arg1) {
        // TODO Auto-generated method stub
        return null;
    }

}
