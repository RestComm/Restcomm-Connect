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
package org.mobicents.servlet.restcomm.provisioning.number.vi.converter;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public abstract class AbstractConverter implements Converter {
    public AbstractConverter() {
        super();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public abstract boolean canConvert(final Class klass);

    @Override
    public void marshal(final Object object, HierarchicalStreamWriter writer, MarshallingContext context) {
        // We will not be marshalling these objects.
    }

    @Override
    public abstract Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context);

    protected int getInteger(final String number) {
        if (number != null && !number.isEmpty()) {
            try {
                return Integer.parseInt(number);
            } catch (final Exception exception) {
                return -1;
            }
        }
        return -1;
    }
}
