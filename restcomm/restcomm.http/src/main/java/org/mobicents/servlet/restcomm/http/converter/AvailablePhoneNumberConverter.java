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

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
public final class AvailablePhoneNumberConverter extends AbstractConverter {
    public AvailablePhoneNumberConverter(final Configuration configuration) {
        super(configuration);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(final Class klass) {
        return AvailablePhoneNumber.class.equals(klass);
    }

    @Override
    public void marshal(final Object object, final HierarchicalStreamWriter writer, final MarshallingContext context) {
        final AvailablePhoneNumber number = (AvailablePhoneNumber) object;
        writer.startNode("AvailablePhoneNumber");
        writeFriendlyName(number.getFriendlyName(), writer);
        writePhoneNumber(number.getPhoneNumber(), writer);
        writeLata(number.getLata(), writer);
        writeRateCenter(number.getRateCenter(), writer);
        writeLatitude(number.getLatitude(), writer);
        writeLongitude(number.getLongitude(), writer);
        writeRegion(number.getRegion(), writer);
        writePostalCode(number.getPostalCode(), writer);
        writeIsoCountry(number.getIsoCountry(), writer);
        writer.endNode();
    }

    private void writeLata(final Integer lata, final HierarchicalStreamWriter writer) {
        writer.startNode("Lata");
        if (lata != null) {
            writer.setValue(lata.toString());
        }
        writer.endNode();
    }

    private void writeRateCenter(final String center, final HierarchicalStreamWriter writer) {
        writer.startNode("RateCenter");
        if (center != null) {
            writer.setValue(center);
        }
        writer.endNode();
    }

    private void writeLatitude(final Double latitude, final HierarchicalStreamWriter writer) {
        writer.startNode("Latitude");
        if (latitude != null) {
            writer.setValue(latitude.toString());
        }
        writer.endNode();
    }

    private void writeLongitude(final Double longitude, final HierarchicalStreamWriter writer) {
        writer.startNode("Longitude");
        if (longitude != null) {
            writer.setValue(longitude.toString());
        }
        writer.endNode();
    }

    private void writeRegion(final String region, final HierarchicalStreamWriter writer) {
        writer.startNode("Region");
        if (region != null) {
            writer.setValue(region);
        }
        writer.endNode();
    }

    private void writePostalCode(final Integer postalCode, final HierarchicalStreamWriter writer) {
        writer.startNode("PostalCode");
        if (postalCode != null) {
            writer.setValue(postalCode.toString());
        }
        writer.endNode();
    }

    private void writeIsoCountry(final String country, final HierarchicalStreamWriter writer) {
        writer.startNode("IsoCountry");
        if (country != null) {
            writer.setValue(country);
        }
        writer.endNode();
    }
}
