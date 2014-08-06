package org.mobicents.servlet.restcomm.provisioning.number.vi.converter;

import java.util.ArrayList;
import java.util.List;

import org.mobicents.servlet.restcomm.provisioning.number.vi.NPA;
import org.mobicents.servlet.restcomm.provisioning.number.vi.RateCenter;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

public final class RateCenterConverter extends AbstractConverter {
    public RateCenterConverter() {
        super();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(final Class klass) {
        return RateCenter.class.equals(klass);
    }

    @Override
    public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
        String name = null;
        final List<NPA> npas = new ArrayList<NPA>();
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            final String child = reader.getNodeName();
            if ("name".equals(child)) {
                name = reader.getValue();
            } else if ("npa".equals(child)) {
                final NPA npa = (NPA) context.convertAnother(null, NPA.class);
                npas.add(npa);
            }
            reader.moveUp();
        }
        return new RateCenter(name, npas);
    }
}
