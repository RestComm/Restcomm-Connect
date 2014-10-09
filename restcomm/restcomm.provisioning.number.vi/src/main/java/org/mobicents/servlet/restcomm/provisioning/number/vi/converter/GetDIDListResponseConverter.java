/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package org.mobicents.servlet.restcomm.provisioning.number.vi.converter;

import java.util.ArrayList;
import java.util.List;

import org.mobicents.servlet.restcomm.provisioning.number.vi.GetDIDListResponse;
import org.mobicents.servlet.restcomm.provisioning.number.vi.State;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * @author jean.deruelle@telestax.com
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class GetDIDListResponseConverter extends AbstractConverter {
    public GetDIDListResponseConverter() {
        super();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(Class klass) {
        return GetDIDListResponse.class.equals(klass);
    }

    @Override
    public Object unmarshal(final HierarchicalStreamReader reader, final UnmarshallingContext context) {
        String name = null;
        String status = null;
        int code = -1;
        final List<State> states = new ArrayList<State>();
        while (reader.hasMoreChildren()) {
            reader.moveDown();
            final String child = reader.getNodeName();
            if ("name".equals(child)) {
                name = reader.getValue();
            } else if ("status".equals(child)) {
                status = reader.getValue();
            } else if ("statuscode".equals(child)) {
                final String value = reader.getValue();
                code = getInteger(value);
            } else if ("state".equals(child)) {
                final State lata = (State) context.convertAnother(null, State.class);
                states.add(lata);
            }
            reader.moveUp();
        }
        return new GetDIDListResponse(name, status, code, states);
    }
}
