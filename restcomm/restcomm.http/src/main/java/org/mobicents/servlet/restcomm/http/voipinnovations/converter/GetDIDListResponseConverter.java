/*
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.servlet.restcomm.http.voipinnovations.converter;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

import org.mobicents.servlet.restcomm.http.voipinnovations.GetDIDListResponse;
import org.mobicents.servlet.restcomm.http.voipinnovations.State;

/**
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
        State state = null;
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
                state = (State) context.convertAnother(null, State.class);
            }
            reader.moveUp();
        }
        return new GetDIDListResponse(name, status, code, state);
    }
}
