/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.http.responses.ErrorResponse;

import java.lang.reflect.Type;

/**
 * @author orestis.tsakiridis@telestax.com - Orestis Tsakiridis
 */
public class ErrorResponseConverter extends AbstractConverter implements JsonSerializer<ErrorResponse> {
    public ErrorResponseConverter(final Configuration configuration) {
        super(configuration);
    }

    public JsonElement serialize(ErrorResponse errorResponse, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject object = new JsonObject();
        object.addProperty("code", errorResponse.getCode().toString());
        object.addProperty("message", errorResponse.getMessage().toString());
        return object;
    }

    @Override
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext marshallingContext) {
        ErrorResponse errorResponse = (ErrorResponse) o;
        writer.startNode("ErrorResponse");
        writeAny("Code", errorResponse.getCode().toString(), writer);
        writeAny("Message", errorResponse.getMessage(), writer);
        writer.endNode();
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader hierarchicalStreamReader, UnmarshallingContext unmarshallingContext) {
        return null;
    }

    @Override
    public boolean canConvert(Class aClass) {
        return ErrorResponse.class.equals(aClass);
    }

}
