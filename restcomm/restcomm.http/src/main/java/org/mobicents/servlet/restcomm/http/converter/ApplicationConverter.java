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
import java.net.URI;

import org.apache.commons.configuration.Configuration;
import org.mobicents.servlet.restcomm.annotations.concurrency.ThreadSafe;
import org.mobicents.servlet.restcomm.entities.Application;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * @author quintana.thomas@gmail.com (Thomas Quintana)
 */
@ThreadSafe
public final class ApplicationConverter extends AbstractConverter implements JsonSerializer<Application> {
    public ApplicationConverter(final Configuration configuration) {
        super(configuration);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(final Class klass) {
        return Application.class.equals(klass);
    }

    @Override
    public void marshal(final Object object, final HierarchicalStreamWriter writer, final MarshallingContext context) {
        final Application application = (Application) object;
        writer.startNode("Application");
        writeSid(application.getSid(), writer);
        writeDateCreated(application.getDateCreated(), writer);
        writeDateUpdated(application.getDateUpdated(), writer);
        writeFriendlyName(application.getFriendlyName(), writer);
        writeAccountSid(application.getAccountSid(), writer);
        writeApiVersion(application.getApiVersion(), writer);
        writeVoiceUrl(application.getVoiceUrl(), writer);
        writeVoiceMethod(application.getVoiceMethod(), writer);
        writeVoiceFallbackUrl(application.getVoiceFallbackUrl(), writer);
        writeVoiceFallbackMethod(application.getVoiceFallbackMethod(), writer);
        writeStatusCallback(application.getStatusCallback(), writer);
        writeStatusCallbackMethod(application.getStatusCallbackMethod(), writer);
        writeVoiceCallerIdLookup(application.hasVoiceCallerIdLookup(), writer);
        writeSmsUrl(application.getSmsUrl(), writer);
        writeSmsMethod(application.getSmsMethod(), writer);
        writeSmsFallbackUrl(application.getSmsFallbackUrl(), writer);
        writeSmsFallbackMethod(application.getSmsFallbackMethod(), writer);
        writeSmsStatusCallback(application.getSmsStatusCallback(), writer);
        writeUri(application.getUri(), writer);
        writer.endNode();
    }

    @Override
    public JsonElement serialize(final Application application, final Type type, final JsonSerializationContext context) {
        final JsonObject object = new JsonObject();
        writeSid(application.getSid(), object);
        writeDateCreated(application.getDateCreated(), object);
        writeDateUpdated(application.getDateUpdated(), object);
        writeFriendlyName(application.getFriendlyName(), object);
        writeAccountSid(application.getAccountSid(), object);
        writeApiVersion(application.getApiVersion(), object);
        writeVoiceUrl(application.getVoiceUrl(), object);
        writeVoiceMethod(application.getVoiceMethod(), object);
        writeVoiceFallbackUrl(application.getVoiceFallbackUrl(), object);
        writeVoiceFallbackMethod(application.getVoiceFallbackMethod(), object);
        writeStatusCallback(application.getStatusCallback(), object);
        writeStatusCallbackMethod(application.getStatusCallbackMethod(), object);
        writeVoiceCallerIdLookup(application.hasVoiceCallerIdLookup(), object);
        writeSmsUrl(application.getSmsUrl(), object);
        writeSmsMethod(application.getSmsMethod(), object);
        writeSmsFallbackUrl(application.getSmsFallbackUrl(), object);
        writeSmsFallbackMethod(application.getSmsFallbackMethod(), object);
        writeSmsStatusCallback(application.getSmsStatusCallback(), object);
        writeUri(application.getUri(), object);
        return object;
    }

    private void writeSmsStatusCallback(final URI smsStatusCallback, final HierarchicalStreamWriter writer) {
        writer.startNode("SmsStatusCallback");
        if (smsStatusCallback != null) {
            writer.setValue(smsStatusCallback.toString());
        }
        writer.endNode();
    }

    private void writeSmsStatusCallback(final URI smsStatusCallback, final JsonObject object) {
        if (smsStatusCallback != null) {
            object.addProperty("sms_status_callback", smsStatusCallback.toString());
        } else {
            object.add("sms_status_callback", JsonNull.INSTANCE);
        }
    }
}
