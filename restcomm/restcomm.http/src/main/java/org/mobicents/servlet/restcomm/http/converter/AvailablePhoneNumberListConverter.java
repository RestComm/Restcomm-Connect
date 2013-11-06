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
package org.mobicents.servlet.restcomm.http.converter;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.entities.AvailablePhoneNumber;
import org.mobicents.servlet.restcomm.entities.AvailablePhoneNumberList;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class AvailablePhoneNumberListConverter extends AbstractConverter {
    public AvailablePhoneNumberListConverter(final Configuration configuration) {
        super(configuration);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(final Class klass) {
        return AvailablePhoneNumberList.class.equals(klass);
    }

    @Override
    public void marshal(final Object object, final HierarchicalStreamWriter writer, final MarshallingContext context) {
        final AvailablePhoneNumberList list = (AvailablePhoneNumberList) object;
        writer.startNode("AvailablePhoneNumbers");
        for (final AvailablePhoneNumber number : list.getAvailablePhoneNumbers()) {
            context.convertAnother(number);
        }
        writer.endNode();
    }
}
