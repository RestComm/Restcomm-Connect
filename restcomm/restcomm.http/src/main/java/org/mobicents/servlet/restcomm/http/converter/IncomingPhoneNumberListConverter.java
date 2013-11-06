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
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.entities.IncomingPhoneNumber;
import org.mobicents.servlet.restcomm.entities.IncomingPhoneNumberList;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class IncomingPhoneNumberListConverter extends AbstractConverter {
    public IncomingPhoneNumberListConverter(final Configuration configuration) {
        super(configuration);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(final Class klass) {
        return IncomingPhoneNumberList.class.equals(klass);
    }

    @Override
    public void marshal(final Object object, final HierarchicalStreamWriter writer, final MarshallingContext context) {
        final IncomingPhoneNumberList list = (IncomingPhoneNumberList) object;
        writer.startNode("IncomingPhoneNumbers");
        for (final IncomingPhoneNumber incomingPhoneNumber : list.getIncomingPhoneNumbers()) {
            context.convertAnother(incomingPhoneNumber);
        }
        writer.endNode();
    }
}
