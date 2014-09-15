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
import org.mobicents.servlet.restcomm.entities.ShortCode;

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
public final class ShortCodeConverter extends AbstractConverter implements JsonSerializer<ShortCode> {
    public ShortCodeConverter(final Configuration configuration) {
        super(configuration);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(final Class klass) {
        return ShortCode.class.equals(klass);
    }

    @Override
    public void marshal(final Object object, final HierarchicalStreamWriter writer, final MarshallingContext context) {
        final ShortCode shortCode = (ShortCode) object;
        writer.startNode("ShortCode");
        writeSid(shortCode.getSid(), writer);
        writeDateCreated(shortCode.getDateCreated(), writer);
        writeDateUpdated(shortCode.getDateUpdated(), writer);
        writeFriendlyName(shortCode.getFriendlyName(), writer);
        writeAccountSid(shortCode.getAccountSid(), writer);
        writeShortCode(Integer.toString(shortCode.getShortCode()), writer);
        writeApiVersion(shortCode.getApiVersion(), writer);
        writeSmsUrl(shortCode.getSmsUrl(), writer);
        writeSmsMethod(shortCode.getSmsMethod(), writer);
        writeSmsFallbackUrl(shortCode.getSmsFallbackUrl(), writer);
        writeSmsFallbackMethod(shortCode.getSmsFallbackMethod(), writer);
        writeUri(shortCode.getUri(), writer);
        writer.endNode();
    }

    @Override
    public JsonElement serialize(final ShortCode shortCode, final Type type, final JsonSerializationContext context) {
        final JsonObject object = new JsonObject();
        writeSid(shortCode.getSid(), object);
        writeDateCreated(shortCode.getDateCreated(), object);
        writeDateUpdated(shortCode.getDateUpdated(), object);
        writeFriendlyName(shortCode.getFriendlyName(), object);
        writeAccountSid(shortCode.getAccountSid(), object);
        writeShortCode(Integer.toString(shortCode.getShortCode()), object);
        writeApiVersion(shortCode.getApiVersion(), object);
        writeSmsUrl(shortCode.getSmsUrl(), object);
        writeSmsMethod(shortCode.getSmsMethod(), object);
        writeSmsFallbackUrl(shortCode.getSmsFallbackUrl(), object);
        writeSmsFallbackMethod(shortCode.getSmsFallbackMethod(), object);
        writeUri(shortCode.getUri(), object);
        return object;
    }

    private void writeShortCode(final String shortCode, final HierarchicalStreamWriter writer) {
        writer.startNode("ShortCode");
        writer.setValue(shortCode);
        writer.endNode();
    }

    private void writeShortCode(final String shortCode, final JsonObject object) {
        object.addProperty("short_code", shortCode);
    }
}
