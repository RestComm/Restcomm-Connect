package org.mobicents.servlet.restcomm.provisioning.number.vi.converter;

import java.util.ArrayList;
import java.util.List;

import org.mobicents.servlet.restcomm.provisioning.number.vi.NXX;
import org.mobicents.servlet.restcomm.provisioning.number.vi.TN;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

public final class NXXConverter extends AbstractConverter {
    public NXXConverter() {
        super();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(final Class klass) {
        return NXX.class.equals(klass);
    }

    @Override
    public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
        String name = null;
        final List<TN> tns = new ArrayList<TN>();
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            final String child = reader.getNodeName();
            if ("name".equals(child)) {
                name = reader.getValue();
            } else if ("tn".equals(child)) {
                final TN tn = (TN) context.convertAnother(null, TN.class);
                tns.add(tn);
            }
            reader.moveUp();
        }
        return new NXX(name, tns);
    }
}
