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
import org.mobicents.servlet.restcomm.entities.OutgoingCallerId;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class OutgoingCallerIdConverter extends AbstractConverter implements JsonSerializer<OutgoingCallerId> {
    public OutgoingCallerIdConverter(final Configuration configuration) {
        super(configuration);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(final Class klass) {
        return OutgoingCallerId.class.equals(klass);
    }

    @Override
    public void marshal(final Object object, final HierarchicalStreamWriter writer, final MarshallingContext context) {
        final OutgoingCallerId outgoingCallerId = (OutgoingCallerId) object;
        writer.startNode("OutgoingCallerId");
        writeSid(outgoingCallerId.getSid(), writer);
        writeAccountSid(outgoingCallerId.getAccountSid(), writer);
        writeFriendlyName(outgoingCallerId.getFriendlyName(), writer);
        writePhoneNumber(outgoingCallerId.getPhoneNumber(), writer);
        writeDateCreated(outgoingCallerId.getDateCreated(), writer);
        writeDateUpdated(outgoingCallerId.getDateUpdated(), writer);
        writeUri(outgoingCallerId.getUri(), writer);
        writer.endNode();
    }

    @Override
    public JsonElement serialize(final OutgoingCallerId outgoingCallerId, final Type type,
            final JsonSerializationContext context) {
        final JsonObject object = new JsonObject();
        writeSid(outgoingCallerId.getSid(), object);
        writeAccountSid(outgoingCallerId.getAccountSid(), object);
        writeFriendlyName(outgoingCallerId.getFriendlyName(), object);
        writePhoneNumber(outgoingCallerId.getPhoneNumber(), object);
        writeDateCreated(outgoingCallerId.getDateCreated(), object);
        writeDateUpdated(outgoingCallerId.getDateUpdated(), object);
        writeUri(outgoingCallerId.getUri(), object);
        return object;
    }
}
