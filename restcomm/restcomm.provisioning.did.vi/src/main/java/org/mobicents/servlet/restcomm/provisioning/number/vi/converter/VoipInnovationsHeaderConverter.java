package org.mobicents.servlet.restcomm.provisioning.number.vi.converter;

import org.mobicents.servlet.restcomm.provisioning.number.vi.VoipInnovationsHeader;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

public final class VoipInnovationsHeaderConverter extends AbstractConverter {
    public VoipInnovationsHeaderConverter() {
        super();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(final Class klass) {
        return VoipInnovationsHeader.class.equals(klass);
    }

    @Override
    public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
        String id = null;
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            final String child = reader.getNodeName();
            if ("sessionid".equals(child)) {
                id = reader.getValue();
            }
            reader.moveUp();
        }
        return new VoipInnovationsHeader(id);
    }
}
