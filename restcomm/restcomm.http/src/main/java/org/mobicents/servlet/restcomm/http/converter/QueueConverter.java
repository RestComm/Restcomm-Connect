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

import java.lang.reflect.Type;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.entities.Queue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author muhammad.bilal19@gmail.com (Muhammad Bilal)
 */

@ThreadSafe
public final class QueueConverter extends AbstractConverter implements JsonSerializer<Queue> {
    public QueueConverter(final Configuration configuration) {
        super(configuration);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(Class klass) {
        return QueueConverter.class.equals(klass);
    }

    @Override
    public void marshal(final Object object, final HierarchicalStreamWriter writer, final MarshallingContext context) {
        final Queue queue = (Queue) object;
        writer.startNode("Queue");
        writeSid(queue.getSid(), writer);
        writeFriendlyName(queue.getFriendlyName(), writer);
        writeCurrentSize(queue.getCurrentSize(), writer);
        writeAverageWaitTime(queue.getAverageWaitTime(), writer);
        writeMaxSize(queue.getMaxSize(), writer);
        writeDateCreated(queue.getDateCreated(), writer);
        writeDateUpdated(queue.getDateUpdated(), writer);
        writeUri(queue.getUri(), writer);
        writer.endNode();

    }

    @Override
    public JsonElement serialize(final Queue queue, final Type type, final JsonSerializationContext context) {
        final JsonObject object = new JsonObject();
        writeSid(queue.getSid(), object);
        writeFriendlyName(queue.getFriendlyName(), object);
        writeCurrentSize(queue.getCurrentSize(), object);
        writeAverageWaitTime(queue.getAverageWaitTime(), object);
        writeMaxSize(queue.getMaxSize(), object);
        writeDateCreated(queue.getDateCreated(), object);
        writeDateUpdated(queue.getDateUpdated(), object);
        writeUri(queue.getUri(), object);
        return object;
    }

    private void writeCurrentSize(final Integer currentSize, final HierarchicalStreamWriter writer) {
        writer.startNode("CurrentSize");
        writer.setValue(currentSize.toString());
        writer.endNode();
    }

    private void writeCurrentSize(final Integer currentSize, final JsonObject object) {
        object.addProperty("currentSize", currentSize.toString());
    }

    private void writeAverageWaitTime(final Integer averageWaitTime, final HierarchicalStreamWriter writer) {
        writer.startNode("AverageWaitTime");
        writer.setValue(averageWaitTime.toString());
        writer.endNode();
    }

    private void writeAverageWaitTime(final Integer averageWaitTime, final JsonObject object) {
        object.addProperty("averageWaitTime", averageWaitTime.toString());
    }

    private void writeMaxSize(final Integer maxSize, final HierarchicalStreamWriter writer) {
        writer.startNode("MaxSize");
        writer.setValue(maxSize.toString());
        writer.endNode();
    }

    private void writeMaxSize(final Integer maxSize, final JsonObject object) {
        object.addProperty("maxSize", maxSize.toString());
    }

}
