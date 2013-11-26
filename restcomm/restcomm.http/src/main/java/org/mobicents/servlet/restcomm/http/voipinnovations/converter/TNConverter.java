package org.mobicents.servlet.restcomm.http.voipinnovations.converter;

import org.mobicents.servlet.restcomm.http.voipinnovations.TN;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

public final class TNConverter extends AbstractConverter {
    public TNConverter() {
        super();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(final Class klass) {
        return TN.class.equals(klass);
    }

    @Override
    public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
        final String tier = reader.getAttribute("tier");
        boolean t38 = false;
        String value = reader.getAttribute("t38");
        if ("1".equals(value)) {
            t38 = true;
        }
        boolean cnam = false;
        value = reader.getAttribute("cnamStorage");
        if ("1".equals(value)) {
            cnam = true;
        }
        String number = reader.getValue();
        return new TN(tier, t38, cnam, number);
    }
}
