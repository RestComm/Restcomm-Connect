/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2013, Telestax Inc and individual contributors
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

package org.restcomm.connect.http.converter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.VersionEntity;

import java.lang.reflect.Type;

/**
 * Created by gvagenas on 1/19/16.
 */
public class VersionConverter extends AbstractConverter implements JsonSerializer<VersionEntity> {

    public VersionConverter(Configuration configuration) {
        super(configuration);
    }

    @Override
    public boolean canConvert(Class klass) {
        return VersionEntity.class.equals(klass);
    }

    @Override
    public void marshal(Object object, HierarchicalStreamWriter writer, MarshallingContext context) {
        final VersionEntity versionEntity = (VersionEntity) object;
        String version = versionEntity.getVersion();
        String revision = versionEntity.getRevision();
        String name = versionEntity.getName();
        String date = versionEntity.getDate();

        writer.startNode("Name");
        writer.setValue(name+" Restcomm");
        writer.endNode();

        writer.startNode("Version");
        writer.setValue(version);
        writer.endNode();

        writer.startNode("Revision");
        writer.setValue(revision);
        writer.endNode();

        writer.startNode("Date");
        writer.setValue(date);
        writer.endNode();
    }

    @Override
    public JsonElement serialize(VersionEntity versionEntity, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject result = new JsonObject();
        result.addProperty("Name", versionEntity.getName()+" Restcomm");
        result.addProperty("Version", versionEntity.getVersion());
        result.addProperty("Revision", versionEntity.getRevision());
        result.addProperty("Date", versionEntity.getDate());
        return result;
    }
}
