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
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.entities.IdentityInstance;

import java.lang.reflect.Type;

/**
 * @author orestis.tsakiridis@company.com - Orestis Tsakiridis
 */
public class IdentityInstanceConverter extends AbstractConverter implements JsonSerializer<IdentityInstance> {
    public IdentityInstanceConverter(Configuration configuration) {
        super(configuration);
    }

    @Override
    public boolean canConvert(Class klass) {
        return false;
    }

    @Override
    public void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context) {
        final IdentityInstance identityInstance = (IdentityInstance) object;

        writer.startNode("Sid");
        writer.setValue(identityInstance.getSid().toString());
        writer.endNode();

        writer.startNode("Name");
        writer.setValue(identityInstance.getName());
        writer.endNode();
    }

    @Override
    public JsonElement serialize(IdentityInstance identityInstance, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject result = new JsonObject();
        result.addProperty("Sid", identityInstance.getSid().toString());
        result.addProperty("Name", identityInstance.getName());
        return result;
    }
}
