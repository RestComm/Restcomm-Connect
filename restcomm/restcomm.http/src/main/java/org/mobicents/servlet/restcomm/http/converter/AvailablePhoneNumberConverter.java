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
        writeCapabilities(number.isVoiceCapable(), number.isSmsCapable(), number.isMmsCapable(), number.isFaxCapable(), writer);
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
