/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag.
 *
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

package org.restcomm.connect.rvd.model.steps.geolocation;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author <a href="mailto:fernando.mendioroz@telestax.com"> Fernando Mendioroz </a>
 *
 */
public class GeolocationStepConverter implements Converter {

    @Override
    public boolean canConvert(Class elementClass) {
        return elementClass.equals(RcmlGeolocationStep.class);
    }

    @Override
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        RcmlGeolocationStep step = (RcmlGeolocationStep) value;
        if (step.getDeviceIdentifier() != null)
            writer.addAttribute("deviceIdentifier", step.getDeviceIdentifier());
        if (step.getAction() != null)
            writer.addAttribute("action", step.getAction());
        if (step.getMethod() != null)
            writer.addAttribute("method", step.getMethod());
        if (step.getStatusCallback() != null)
            writer.addAttribute("statusCallback", step.getStatusCallback());

        writer.setValue(step.getDeviceIdentifier());
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader arg0, UnmarshallingContext arg1) {
        // TODO Auto-generated method stub
        return null;
    }
}
