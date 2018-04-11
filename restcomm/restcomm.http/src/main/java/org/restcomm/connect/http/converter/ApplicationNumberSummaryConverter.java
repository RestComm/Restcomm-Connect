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

package org.restcomm.connect.http.converter;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.lang.reflect.Type;
import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.dao.entities.ApplicationNumberSummary;

/**
 * @author otsakir@gmail.com - Orestis Tsakiridis
 */
public class ApplicationNumberSummaryConverter extends AbstractConverter<ApplicationNumberSummary> {
    public ApplicationNumberSummaryConverter(final Configuration configuration) {
        super(configuration);
    }
    @Override
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext marshallingContext) {
        ApplicationNumberSummary number = (ApplicationNumberSummary) o;
        if (number.getSid() != null ) {
            writer.startNode("Sid");
            writer.setValue(number.getSid());
            writer.endNode();
        }
        if (number.getFriendlyName() != null) {
            writer.startNode("FriendlyName");
            writer.setValue(number.getFriendlyName());
            writer.endNode();
        }
        if (number.getPhoneNumber() != null) {
            writer.startNode("PhoneNumber");
            writer.setValue(number.getPhoneNumber());
            writer.endNode();
        }
        if (number.getVoiceApplicationSid() != null) {
            writer.startNode("VoiceApplicationSid");
            writer.setValue(number.getVoiceApplicationSid());
            writer.endNode();
        }
        if (number.getSmsApplicationSid() != null) {
            writer.startNode("SmsApplicationSid");
            writer.setValue(number.getSmsApplicationSid());
            writer.endNode();
        }
        if (number.getUssdApplicationSid() != null) {
            writer.startNode("UssdApplicationSid");
            writer.setValue(number.getUssdApplicationSid());
            writer.endNode();
        }
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader hierarchicalStreamReader, UnmarshallingContext unmarshallingContext) {
        return null;
    }

    @Override
    public boolean canConvert(Class aClass) {
        return ApplicationNumberSummary.class.equals(aClass);
    }

    @Override
    public JsonElement serialize(ApplicationNumberSummary t, Type type, JsonSerializationContext jsc) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
