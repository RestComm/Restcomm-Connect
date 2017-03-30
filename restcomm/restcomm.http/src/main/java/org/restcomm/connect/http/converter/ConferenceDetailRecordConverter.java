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
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.apache.commons.configuration.Configuration;
import org.restcomm.connect.commons.annotations.concurrency.ThreadSafe;
import org.restcomm.connect.dao.entities.ConferenceDetailRecord;

import java.lang.reflect.Type;

/**
 * @author maria
 */
@ThreadSafe
public final class ConferenceDetailRecordConverter extends AbstractConverter implements JsonSerializer<ConferenceDetailRecord> {
    private final String apiVersion;

    public ConferenceDetailRecordConverter(final Configuration configuration) {
        super(configuration);
        apiVersion = configuration.getString("api-version");
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

    private void writeSubResources(final ConferenceDetailRecord cdr, final HierarchicalStreamWriter writer) {
        writer.startNode("SubresourceUris");
        writeParticipants(cdr, writer);
        writer.endNode();
    }

    private void writeSubResources(final ConferenceDetailRecord cdr, final JsonObject object) {
        final JsonObject other = new JsonObject();
        writeParticipants(cdr, other);
        object.add("subresource_uris", other);
    }

    private void writeParticipants(final ConferenceDetailRecord cdr, final HierarchicalStreamWriter writer) {
        writer.startNode("Participants");
        writer.setValue(prefix(cdr) + "/Participants");
        writer.endNode();
    }

    private void writeParticipants(final ConferenceDetailRecord cdr, final JsonObject object) {
        object.addProperty("participants", prefix(cdr) + "/Participants.json");
    }

    private String prefix(final ConferenceDetailRecord cdr) {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("/").append(apiVersion).append("/Accounts/");
        buffer.append(cdr.getAccountSid().toString()).append("/Conferences/");
        buffer.append(cdr.getSid());
        return buffer.toString();
    }
}
