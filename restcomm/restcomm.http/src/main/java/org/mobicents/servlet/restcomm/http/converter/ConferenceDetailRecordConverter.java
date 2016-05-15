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
import org.joda.time.DateTime;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.entities.ConferenceDetailRecord;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author maria
 */
@ThreadSafe
public final class ConferenceDetailRecordConverter extends AbstractConverter implements JsonSerializer<ConferenceDetailRecord> {

    public ConferenceDetailRecordConverter(final Configuration configuration) {
        super(configuration);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(final Class klass) {
        return ConferenceDetailRecord.class.equals(klass);
    }

    @Override
    public void marshal(final Object object, final HierarchicalStreamWriter writer, final MarshallingContext context) {
        final ConferenceDetailRecord cdr = (ConferenceDetailRecord) object;
        writer.startNode("Conference");
        writeSid(cdr.getSid(), writer);
        writeDateCreated(cdr.getDateCreated(), writer);
        writeDateUpdated(cdr.getDateUpdated(), writer);
        writeAccountSid(cdr.getAccountSid(), writer);
        writeStatus(cdr.getStatus(), writer);
        writeApiVersion(cdr.getApiVersion(), writer);
        writeFriendlyName(cdr.getFriendlyName(), writer);
        writeUri(cdr.getUri(), writer);
        writeSubResources(cdr, writer);
        writer.endNode();
    }

    @Override
    public JsonElement serialize(final ConferenceDetailRecord cdr, Type type, final JsonSerializationContext context) {
        final JsonObject object = new JsonObject();
        writeSid(cdr.getSid(), object);
        writeDateCreated(cdr.getDateCreated(), object);
        writeDateUpdated(cdr.getDateUpdated(), object);
        writeAccountSid(cdr.getAccountSid(), object);
        writeStatus(cdr.getStatus(), object);
        writeApiVersion(cdr.getApiVersion(), object);
        writeFriendlyName(cdr.getFriendlyName(), object);
        writeUri(cdr.getUri(), object);
        writeSubResources(cdr, object);
        return object;
    }

    private void writeEndTime(final DateTime endTime, final HierarchicalStreamWriter writer) {
        writer.startNode("EndTime");
        if (endTime != null) {
            writer.setValue(endTime.toString());
        }
        writer.endNode();
    }

    private void writeEndTime(final DateTime endTime, final JsonObject object) {
        if (endTime != null) {
            object.addProperty("end_time", endTime.toString());
        }
    }

    private void writeStartTime(final DateTime startTime, final HierarchicalStreamWriter writer) {
        writer.startNode("StartTime");
        if (startTime != null) {
            writer.setValue(startTime.toString());
        }
        writer.endNode();
    }

    private void writeStartTime(final DateTime startTime, final JsonObject object) {
        if (startTime != null) {
            object.addProperty("start_time", startTime.toString());
        }
    }

    private void writeSubResources(final ConferenceDetailRecord cdr, final HierarchicalStreamWriter writer) {
        writer.startNode("SubresourceUris");
        writer.endNode();
    }

    private void writeSubResources(final ConferenceDetailRecord cdr, final JsonObject object) {
        final JsonObject other = new JsonObject();
        object.add("subresource_uris", other);
    }
}
